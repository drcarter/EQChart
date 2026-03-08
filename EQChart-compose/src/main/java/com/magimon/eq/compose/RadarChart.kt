package com.magimon.eq.compose

import android.graphics.Color
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.magimon.eq.radar.RadarAxis
import com.magimon.eq.radar.RadarChartPresentationOptions
import com.magimon.eq.radar.RadarChartStyleOptions
import com.magimon.eq.radar.RadarSeries
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private data class RadarVec2(
    val x: Float,
    val y: Float,
)

private data class RadarHitResult(
    val seriesIndex: Int,
    val axisIndex: Int,
    val distance: Float,
)

private data class RadarLegendItemDraw(
    val color: Int,
    val label: String,
    val markerLeft: Float,
    val markerTop: Float,
    val textX: Float,
    val baselineY: Float,
)

private data class RadarComputed(
    val axes: List<RadarAxis>,
    val series: List<RadarSeries>,
    val chartRect: Rect,
    val centerX: Float,
    val centerY: Float,
    val radius: Float,
    val pointsBySeries: List<List<RadarVec2>>,
    val legendItems: List<RadarLegendItemDraw>,
)

@Composable
fun RadarChart(
    axes: List<RadarAxis>,
    series: List<RadarSeries>,
    modifier: Modifier = Modifier,
    valueMax: Double = 100.0,
    styleOptions: RadarChartStyleOptions = RadarChartStyleOptions(),
    presentationOptions: RadarChartPresentationOptions = RadarChartPresentationOptions(),
    onPointClick: ((seriesIndex: Int, axisIndex: Int, value: Double, payload: Any?) -> Unit)? = null,
) {
    val density = LocalDensity.current
    var selectedPoint by remember { mutableStateOf<RadarHitResult?>(null) }

    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val computed = remember(
            axes,
            series,
            widthPx,
            heightPx,
            valueMax,
            styleOptions,
            presentationOptions,
        ) {
            computeRadarChart(
                width = widthPx,
                height = heightPx,
                axes = axes,
                series = series,
                valueMax = valueMax,
                styleOptions = styleOptions,
                presentationOptions = presentationOptions,
                density = density.density,
                scaledDensity = density.fontScale * density.density,
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(computed.pointsBySeries, styleOptions.touchHitRadiusDp) {
                    detectTapGestures { tap ->
                        val hit = radarNearestPoint(
                            touchX = tap.x,
                            touchY = tap.y,
                            pointsBySeries = computed.pointsBySeries,
                            hitRadiusPx = styleOptions.touchHitRadiusDp * density.density,
                        )
                        selectedPoint = hit
                        if (hit != null) {
                            val seriesItem = computed.series.getOrNull(hit.seriesIndex)
                            val value = seriesItem?.values?.getOrNull(hit.axisIndex)
                            if (seriesItem != null && value != null) {
                                onPointClick?.invoke(hit.seriesIndex, hit.axisIndex, value, seriesItem.payload)
                            }
                        }
                    }
                },
        ) {
            drawRect(styleOptions.backgroundColor.toComposeColor())

            val legendTextPaint = newTextPaint(
                color = styleOptions.legendTextColor,
                textSizePx = with(density) { presentationOptions.legendTextSizeSp.sp.toPx() },
                align = Paint.Align.LEFT,
            )
            val axisLabelPaint = newTextPaint(
                color = styleOptions.axisLabelColor,
                textSizePx = with(density) { presentationOptions.axisLabelTextSizeSp.sp.toPx() },
                align = Paint.Align.CENTER,
            )

            computed.legendItems.forEach { item ->
                drawRect(
                    color = item.color.toComposeColor(),
                    topLeft = Offset(item.markerLeft, item.markerTop),
                    size = androidx.compose.ui.geometry.Size(
                        width = with(density) { styleOptions.legendMarkerSizeDp.dp.toPx() },
                        height = with(density) { styleOptions.legendMarkerSizeDp.dp.toPx() },
                    ),
                )
                drawContext.canvas.nativeCanvas.drawText(
                    item.label,
                    item.textX,
                    item.baselineY,
                    legendTextPaint,
                )
            }

            val axisCount = computed.axes.size
            val levels = presentationOptions.gridLevels.coerceAtLeast(1)

            for (level in 1..levels) {
                val levelRadius = computed.radius * (level.toFloat() / levels.toFloat())
                val path = Path()
                for (axisIndex in 0 until axisCount) {
                    val point = radarVertex(
                        centerX = computed.centerX,
                        centerY = computed.centerY,
                        radius = levelRadius,
                        axisIndex = axisIndex,
                        axisCount = axisCount,
                        startAngleDeg = presentationOptions.startAngleDeg,
                    )
                    if (axisIndex == 0) {
                        path.moveTo(point.x, point.y)
                    } else {
                        path.lineTo(point.x, point.y)
                    }
                }
                path.close()
                drawPath(
                    path = path,
                    color = styleOptions.gridColor.toComposeColor(),
                    style = Stroke(width = with(density) { styleOptions.gridStrokeWidthDp.dp.toPx() }),
                )
            }

            for (axisIndex in 0 until axisCount) {
                val outer = radarVertex(
                    centerX = computed.centerX,
                    centerY = computed.centerY,
                    radius = computed.radius,
                    axisIndex = axisIndex,
                    axisCount = axisCount,
                    startAngleDeg = presentationOptions.startAngleDeg,
                )
                drawLine(
                    color = styleOptions.axisColor.toComposeColor(),
                    start = Offset(computed.centerX, computed.centerY),
                    end = Offset(outer.x, outer.y),
                    strokeWidth = with(density) { styleOptions.axisStrokeWidthDp.dp.toPx() },
                )
            }

            computed.pointsBySeries.forEachIndexed { seriesIndex, points ->
                if (points.isEmpty()) return@forEachIndexed
                val seriesItem = computed.series[seriesIndex]

                val polygon = Path()
                points.forEachIndexed { index, point ->
                    if (index == 0) {
                        polygon.moveTo(point.x, point.y)
                    } else {
                        polygon.lineTo(point.x, point.y)
                    }
                }
                polygon.close()

                drawPath(
                    path = polygon,
                    color = radarApplyAlpha(seriesItem.color, styleOptions.fillAlpha).toComposeColor(),
                )

                val strokeWidth = if (selectedPoint?.seriesIndex == seriesIndex) {
                    with(density) { styleOptions.selectedStrokeWidthDp.dp.toPx() }
                } else {
                    with(density) { styleOptions.polygonStrokeWidthDp.dp.toPx() }
                }

                drawPath(
                    path = polygon,
                    color = seriesItem.color.toComposeColor(),
                    style = Stroke(width = strokeWidth.coerceAtLeast(1f)),
                )

                if (presentationOptions.showPoints) {
                    points.forEachIndexed { axisIndex, point ->
                        drawCircle(
                            color = radarApplyAlpha(seriesItem.color, styleOptions.pointGlowAlpha).toComposeColor(),
                            radius = with(density) { styleOptions.pointGlowRadiusDp.dp.toPx() },
                            center = Offset(point.x, point.y),
                        )
                        drawCircle(
                            color = seriesItem.color.toComposeColor(),
                            radius = with(density) { styleOptions.pointRadiusDp.dp.toPx() },
                            center = Offset(point.x, point.y),
                        )
                        drawCircle(
                            color = androidx.compose.ui.graphics.Color.White,
                            radius = with(density) { styleOptions.pointCoreRadiusDp.dp.toPx() },
                            center = Offset(point.x, point.y),
                        )

                        if (selectedPoint?.seriesIndex == seriesIndex && selectedPoint?.axisIndex == axisIndex) {
                            drawCircle(
                                color = androidx.compose.ui.graphics.Color.White,
                                radius = with(density) { styleOptions.selectedPointRadiusDp.dp.toPx() },
                                center = Offset(point.x, point.y),
                                style = Stroke(width = with(density) { styleOptions.selectedStrokeWidthDp.dp.toPx() }),
                            )
                        }
                    }
                }
            }

            if (presentationOptions.showAxisLabels) {
                val labelOffset = with(density) { styleOptions.axisLabelOffsetDp.dp.toPx() }
                for (axisIndex in 0 until axisCount) {
                    val point = radarVertex(
                        centerX = computed.centerX,
                        centerY = computed.centerY,
                        radius = computed.radius + labelOffset,
                        axisIndex = axisIndex,
                        axisCount = axisCount,
                        startAngleDeg = presentationOptions.startAngleDeg,
                    )
                    val dx = point.x - computed.centerX
                    axisLabelPaint.textAlign = when {
                        abs(dx) < with(density) { 8.dp.toPx() } -> Paint.Align.CENTER
                        dx > 0f -> Paint.Align.LEFT
                        else -> Paint.Align.RIGHT
                    }
                    val baseline = point.y + (axisLabelPaint.textSize * 0.35f)
                    drawContext.canvas.nativeCanvas.drawText(
                        computed.axes[axisIndex].label,
                        point.x,
                        baseline,
                        axisLabelPaint,
                    )
                }
            }
        }
    }
}

private fun computeRadarChart(
    width: Float,
    height: Float,
    axes: List<RadarAxis>,
    series: List<RadarSeries>,
    valueMax: Double,
    styleOptions: RadarChartStyleOptions,
    presentationOptions: RadarChartPresentationOptions,
    density: Float,
    scaledDensity: Float,
): RadarComputed {
    val validAxes = axes.filter { it.label.isNotBlank() }
    val axisCount = validAxes.size
    val validSeries = if (axisCount < 3) {
        emptyList()
    } else {
        series.filter { item ->
            item.values.size == axisCount && item.values.any { value -> value.isFinite() }
        }
    }

    val contentPadding = styleOptions.contentPaddingDp * density
    val chartWidth = width.coerceAtLeast(0f)
    val chartHeight = height.coerceAtLeast(0f)

    val legendLayout = if (presentationOptions.showLegend && validSeries.isNotEmpty()) {
        computeRadarLegendLayout(
            width = chartWidth,
            series = validSeries,
            styleOptions = styleOptions,
            presentationOptions = presentationOptions,
            density = density,
            scaledDensity = scaledDensity,
        )
    } else {
        emptyList()
    }

    val legendBottom = legendLayout.maxOfOrNull { it.baselineY } ?: (contentPadding + presentationOptions.legendTopMarginDp * density)
    val chartTop = if (legendLayout.isEmpty()) {
        contentPadding
    } else {
        legendBottom + styleOptions.legendBottomGapDp * density + contentPadding
    }

    val chartRect = Rect(
        left = contentPadding,
        top = chartTop,
        right = (chartWidth - contentPadding).coerceAtLeast(contentPadding),
        bottom = (chartHeight - contentPadding).coerceAtLeast(chartTop + 1f),
    )

    val centerX = chartRect.center.x
    val centerY = chartRect.center.y
    val maxRadius = min(chartRect.width, chartRect.height) * 0.5f

    val labelReserve = if (presentationOptions.showAxisLabels) {
        (styleOptions.axisLabelOffsetDp * density) + (presentationOptions.axisLabelTextSizeSp * scaledDensity * 0.9f)
    } else {
        0f
    }

    val radius = (maxRadius - labelReserve).coerceAtLeast(0f)
    val maxValueSafe = if (valueMax.isFinite() && valueMax > 0.0) valueMax else 100.0

    val pointsBySeries = validSeries.map { item ->
        radarPolygonPoints(
            values = item.values,
            maxValue = maxValueSafe,
            centerX = centerX,
            centerY = centerY,
            radius = radius,
            axisCount = axisCount,
            progress = 1f,
            startAngleDeg = presentationOptions.startAngleDeg,
        )
    }

    return RadarComputed(
        axes = validAxes,
        series = validSeries,
        chartRect = chartRect,
        centerX = centerX,
        centerY = centerY,
        radius = radius,
        pointsBySeries = pointsBySeries,
        legendItems = legendLayout,
    )
}

private fun computeRadarLegendLayout(
    width: Float,
    series: List<RadarSeries>,
    styleOptions: RadarChartStyleOptions,
    presentationOptions: RadarChartPresentationOptions,
    density: Float,
    scaledDensity: Float,
): List<RadarLegendItemDraw> {
    if (series.isEmpty()) return emptyList()

    val markerSize = styleOptions.legendMarkerSizeDp * density
    val itemGap = styleOptions.legendItemGapDp * density
    val rowGap = styleOptions.legendRowGapDp * density
    val startX = (styleOptions.contentPaddingDp + presentationOptions.legendLeftMarginDp) * density
    val maxX = width - (styleOptions.contentPaddingDp * density)
    val textPaint = newTextPaint(
        color = styleOptions.legendTextColor,
        textSizePx = presentationOptions.legendTextSizeSp * scaledDensity,
        align = Paint.Align.LEFT,
    )

    val baselineOffset = abs(textPaint.fontMetrics.ascent)
    val rowHeight = max(markerSize, textPaint.fontMetrics.bottom - textPaint.fontMetrics.top)

    var cursorX = startX
    var baselineY = (styleOptions.contentPaddingDp + presentationOptions.legendTopMarginDp) * density + baselineOffset

    return buildList {
        series.forEach { item ->
            val textWidth = textPaint.measureText(item.name)
            val itemWidth = markerSize + (6f * density) + textWidth

            if (cursorX + itemWidth > maxX && cursorX > startX) {
                cursorX = startX
                baselineY += rowHeight + rowGap
            }

            val markerTop = baselineY - baselineOffset + ((rowHeight - markerSize) * 0.5f)
            add(
                RadarLegendItemDraw(
                    color = item.color,
                    label = item.name,
                    markerLeft = cursorX,
                    markerTop = markerTop,
                    textX = cursorX + markerSize + (6f * density),
                    baselineY = baselineY,
                ),
            )

            cursorX += itemWidth + itemGap
        }
    }
}

private fun radarNormalize(value: Double, maxValue: Double): Float {
    if (!value.isFinite() || !maxValue.isFinite() || maxValue <= 0.0) return 0f
    return (value / maxValue).coerceIn(0.0, 1.0).toFloat()
}

private fun radarVertex(
    centerX: Float,
    centerY: Float,
    radius: Float,
    axisIndex: Int,
    axisCount: Int,
    startAngleDeg: Float,
): RadarVec2 {
    if (axisCount <= 0) return RadarVec2(centerX, centerY)
    val angleDeg = startAngleDeg + (360f * axisIndex.toFloat() / axisCount.toFloat())
    val angleRad = angleDeg * (PI / 180.0)
    return RadarVec2(
        x = centerX + (cos(angleRad) * radius).toFloat(),
        y = centerY + (sin(angleRad) * radius).toFloat(),
    )
}

private fun radarPolygonPoints(
    values: List<Double>,
    maxValue: Double,
    centerX: Float,
    centerY: Float,
    radius: Float,
    axisCount: Int,
    progress: Float,
    startAngleDeg: Float,
): List<RadarVec2> {
    if (axisCount <= 0 || values.isEmpty()) return emptyList()
    val count = min(axisCount, values.size)
    val renderProgress = progress.coerceIn(0f, 1f)
    return List(count) { axisIndex ->
        val norm = radarNormalize(values[axisIndex], maxValue)
        val localRadius = radius * norm * renderProgress
        radarVertex(
            centerX = centerX,
            centerY = centerY,
            radius = localRadius,
            axisIndex = axisIndex,
            axisCount = axisCount,
            startAngleDeg = startAngleDeg,
        )
    }
}

private fun radarNearestPoint(
    touchX: Float,
    touchY: Float,
    pointsBySeries: List<List<RadarVec2>>,
    hitRadiusPx: Float,
): RadarHitResult? {
    if (pointsBySeries.isEmpty()) return null

    val hitRadius = max(1f, hitRadiusPx)
    var best: RadarHitResult? = null
    var bestDistance = Float.MAX_VALUE

    pointsBySeries.forEachIndexed { seriesIndex, points ->
        points.forEachIndexed { axisIndex, point ->
            val distance = hypot(touchX - point.x, touchY - point.y)
            if (distance <= hitRadius && distance < bestDistance) {
                bestDistance = distance
                best = RadarHitResult(seriesIndex, axisIndex, distance)
            }
        }
    }

    return best
}

private fun radarApplyAlpha(color: Int, alpha: Int): Int {
    return Color.argb(
        alpha.coerceIn(0, 255),
        Color.red(color),
        Color.green(color),
        Color.blue(color),
    )
}
