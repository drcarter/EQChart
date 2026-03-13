package com.magimon.eq.compose

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.magimon.eq.line.LineChartPresentationOptions
import com.magimon.eq.line.LineChartStyleOptions
import com.magimon.eq.line.LineDatum
import com.magimon.eq.line.LineSeries
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

private data class RenderLinePoint(
    val x: Float,
    val y: Float,
    val valueX: Double,
    val valueY: Double,
    val seriesIndex: Int,
    val pointIndex: Int,
    val source: LineDatum,
)

private data class LineChartLayout(
    val chartRect: Rect,
    val xMin: Double,
    val xMax: Double,
    val yMin: Double,
    val yMax: Double,
    val baselineY: Float,
    val seriesPoints: List<List<RenderLinePoint>>,
    val xTicks: List<Double>,
    val yTicks: List<Double>,
    val legendTop: Float,
)

private fun dpToPx(value: Float, density: Float): Float = value * density

private fun resolveAxisRange(minValue: Double, maxValue: Double): Pair<Double, Double> {
    val span = maxValue - minValue
    return if (!span.isFinite() || abs(span) <= 1e-12) {
        (minValue - 1.0) to (maxValue + 1.0)
    } else {
        minValue to maxValue
    }
}

private fun buildTicks(minValue: Double, maxValue: Double, count: Int): List<Double> {
    val safe = max(2, count)
    if (safe == 2) return listOf(minValue, maxValue)
    return List(safe) { index ->
        minValue + (maxValue - minValue) * index.toDouble() / (safe - 1)
    }
}

private fun baselineValue(min: Double, max: Double): Double = if (min <= 0.0 && max >= 0.0) 0.0 else min

private fun mapX(value: Double, minX: Double, maxX: Double, chartRect: Rect): Float {
    val ratio = if (maxX == minX) 0.5f else ((value - minX) / (maxX - minX)).toFloat()
    return chartRect.left + ratio * chartRect.width
}

private fun mapY(value: Double, minY: Double, maxY: Double, chartRect: Rect): Float {
    val ratio = if (maxY == minY) 0.5f else ((value - minY) / (maxY - minY)).toFloat()
    return chartRect.bottom - ratio * chartRect.height
}

private fun buildAnimatedLinePath(points: List<RenderLinePoint>, progress: Float, outPath: Path) {
    outPath.reset()
    if (points.isEmpty()) return

    val segmentCount = (points.size - 1).coerceAtLeast(0)
    val animatedSegments = (segmentCount * progress).coerceIn(0f, segmentCount.toFloat())
    val fullSegments = animatedSegments.toInt()
    val segmentFraction = animatedSegments - fullSegments

    outPath.moveTo(points[0].x, points[0].y)
    for (index in 1..fullSegments) {
        outPath.lineTo(points[index].x, points[index].y)
    }

    if (fullSegments < points.lastIndex && progress > 0f) {
        val from = points[fullSegments]
        val to = points[fullSegments + 1]
        val x = from.x + (to.x - from.x) * segmentFraction
        val y = from.y + (to.y - from.y) * segmentFraction
        outPath.lineTo(x, y)
    }
}

private fun visiblePointCount(pointCount: Int, progress: Float): Int {
    if (pointCount <= 0) return 0
    if (progress >= 1f) return pointCount
    return ((pointCount - 1).coerceAtLeast(1) * progress).toInt() + 1
}

private fun computeLineChart(
    widthPx: Float,
    heightPx: Float,
    series: List<LineSeries>,
    style: LineChartStyleOptions,
    presentation: LineChartPresentationOptions,
    density: Float,
    scaledDensity: Float,
): LineChartLayout? {
    if (widthPx <= 0f || heightPx <= 0f) return null

    val contentPadding = dpToPx(style.contentPaddingDp, density)
    val xAxisReserve = dpToPx(style.axisLabelOffsetDp, density) + (presentation.axisLabelTextSizeSp * scaledDensity) + 12f * density
    val legendReserve = if (presentation.showLegend) (presentation.legendTextSizeSp * scaledDensity + 14f * density) else 0f
    val chartRect = Rect(
        left = contentPadding,
        top = contentPadding,
        right = widthPx - contentPadding,
        bottom = heightPx - contentPadding - xAxisReserve - legendReserve,
    )
    if (chartRect.width <= 0f || chartRect.height <= 0f) return null

    val sanitized = series
        .filter { it.points.any { point -> point.x.isFinite() && point.y.isFinite() } }
        .map {
            it to it.points
                .filter { point -> point.x.isFinite() && point.y.isFinite() }
                .sortedBy { point -> point.x }
        }
        .filter { it.second.isNotEmpty() }

    if (sanitized.isEmpty()) return null

    val allPoints = sanitized.flatMap { it.second }
    val (minX, maxX) = resolveAxisRange(
        allPoints.minOfOrNull { it.x } ?: 0.0,
        allPoints.maxOfOrNull { it.x } ?: 0.0,
    )
    val (minY, maxY) = resolveAxisRange(
        allPoints.minOfOrNull { it.y } ?: 0.0,
        allPoints.maxOfOrNull { it.y } ?: 0.0,
    )

    val xTicks = buildTicks(minX, maxX, presentation.xTickCount)
    val yTicks = buildTicks(minY, maxY, presentation.yTickCount)
    val baseline = baselineValue(minY, maxY)
    val baselineY = mapY(baseline, minY, maxY, chartRect)

    val points = sanitized.mapIndexed { seriesIndex, pair ->
        val (item, pointsInSeries) = pair
        pointsInSeries.mapIndexed { pointIndex, point ->
            RenderLinePoint(
                x = mapX(point.x, minX, maxX, chartRect),
                y = mapY(point.y, minY, maxY, chartRect),
                valueX = point.x,
                valueY = point.y,
                seriesIndex = seriesIndex,
                pointIndex = pointIndex,
                source = point,
            )
        }
    }

    val legendTop = chartRect.bottom + xAxisReserve + 4f * density

    return LineChartLayout(
        chartRect = chartRect,
        xMin = minX,
        xMax = maxX,
        yMin = minY,
        yMax = maxY,
        baselineY = baselineY,
        seriesPoints = points,
        xTicks = xTicks,
        yTicks = yTicks,
        legendTop = legendTop,
    )
}

private fun withAlpha(color: Int, alpha: Int): Int {
    val alphaValue = alpha.coerceIn(0, 255)
    return (alphaValue shl 24) or (color and 0x00FFFFFF)
}

/**
 * Canvas-based line chart composable.
 *
 * @param series Input line series grouped by color and legend label.
 * @param styleOptions Visual style values (colors, dimensions, typography).
 * @param presentationOptions Display and animation behavior.
 * @param onPointClick Invoked when a point is clicked.
 */
@Composable
fun LineChart(
    series: List<LineSeries>,
    modifier: Modifier = Modifier,
    styleOptions: LineChartStyleOptions = LineChartStyleOptions(),
    presentationOptions: LineChartPresentationOptions = LineChartPresentationOptions(),
    onPointClick: ((seriesIndex: Int, pointIndex: Int, point: LineDatum, payload: Any?) -> Unit)? = null,
) {
    val density = LocalDensity.current
    val pxDensity = density.density
    val scaledDensity = density.fontScale * pxDensity
    val animation = remember { Animatable(1f) }
    var selectedPoint by remember { mutableStateOf<RenderLinePoint?>(null) }
    val animateOnDataChange = presentationOptions.animateOnDataChange
    val enterAnimationDurationMs = presentationOptions.enterAnimationDurationMs
    val enterAnimationDelayMs = presentationOptions.enterAnimationDelayMs

    LaunchedEffect(series, animateOnDataChange, enterAnimationDurationMs, enterAnimationDelayMs) {
        if (animateOnDataChange && series.isNotEmpty()) {
            animation.snapTo(0f)
            animation.animateTo(
                1f,
                tween(
                    durationMillis = enterAnimationDurationMs.toInt().coerceAtLeast(0),
                    delayMillis = enterAnimationDelayMs.toInt().coerceAtLeast(0),
                ),
            )
        } else {
            animation.snapTo(1f)
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val computed = remember(
            series,
            styleOptions,
            widthPx,
            heightPx,
            pxDensity,
            scaledDensity,
            presentationOptions.showLegend,
            presentationOptions.legendTextSizeSp,
            presentationOptions.axisLabelTextSizeSp,
            presentationOptions.xTickCount,
            presentationOptions.yTickCount,
        ) {
            computeLineChart(
                widthPx = widthPx,
                heightPx = heightPx,
                series = series,
                style = styleOptions,
                presentation = presentationOptions,
                density = pxDensity,
                scaledDensity = scaledDensity,
            )
        }

        val axisLabelPx = presentationOptions.axisLabelTextSizeSp * scaledDensity
        val legendTextPx = presentationOptions.legendTextSizeSp * scaledDensity
        val pointTextPx = styleOptions.pointLabelTextSizeSp * scaledDensity
        val lineStrokePx = styleOptions.lineStrokeWidthDp * pxDensity
        val pointRadiusPx = styleOptions.pointRadiusDp * pxDensity
        val selectedPointRadiusPx = styleOptions.selectedPointRadiusDp * pxDensity

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(computed?.seriesPoints, styleOptions.touchHitRadiusDp, series, onPointClick) {
                    detectTapGestures { tap ->
                        val nearest = computed?.seriesPoints
                            ?.flatten()
                            ?.minByOrNull { point ->
                                hypot((point.x - tap.x).toDouble(), (point.y - tap.y).toDouble()).toFloat()
                            }

                        selectedPoint = nearest
                        if (nearest != null) {
                            val hitRadius = styleOptions.touchHitRadiusDp * pxDensity
                            val distance = hypot(
                                (nearest.x - tap.x).toDouble(),
                                (nearest.y - tap.y).toDouble(),
                            ).toFloat()
                            if (distance <= hitRadius) {
                                val payload = nearest.source.payload ?: series.getOrNull(nearest.seriesIndex)?.payload
                                onPointClick?.invoke(
                                    nearest.seriesIndex,
                                    nearest.pointIndex,
                                    nearest.source,
                                    payload,
                                )
                            }
                        }
                    }
                },
        ) {
            drawRect(styleOptions.backgroundColor.toComposeColor())

            if (computed == null) {
                presentationOptions.emptyText.takeIf { it.isNotBlank() }?.let { empty ->
                    drawContext.canvas.nativeCanvas.drawText(
                        empty,
                        size.width * 0.5f,
                        size.height * 0.5f,
                        newTextPaint(color = styleOptions.axisLabelColor, textSizePx = pointTextPx, align = Paint.Align.CENTER),
                    )
                }
                return@Canvas
            }

            if (computed.seriesPoints.isEmpty()) {
                presentationOptions.emptyText.takeIf { it.isNotBlank() }?.let { empty ->
                    drawContext.canvas.nativeCanvas.drawText(
                        empty,
                        size.width * 0.5f,
                        size.height * 0.5f,
                        newTextPaint(color = styleOptions.axisLabelColor, textSizePx = pointTextPx, align = Paint.Align.CENTER),
                    )
                }
                return@Canvas
            }

            val linePath = Path()
            val fillPath = Path()
            val chartRect = computed.chartRect
            val progress = animation.value.coerceIn(0f, 1f)

            val xAxisPaint = newTextPaint(
                color = styleOptions.axisColor,
                textSizePx = axisLabelPx,
                align = Paint.Align.RIGHT,
            )
            val gridPaint = newTextPaint(
                color = styleOptions.gridColor,
                textSizePx = axisLabelPx,
                align = Paint.Align.RIGHT,
            )
            val legendPaint = newTextPaint(
                color = styleOptions.legendTextColor,
                textSizePx = legendTextPx,
                align = Paint.Align.LEFT,
            )
            val xLabelPaint = newTextPaint(
                color = styleOptions.axisLabelColor,
                textSizePx = axisLabelPx,
                align = Paint.Align.CENTER,
            )

            if (presentationOptions.showGrid) {
                computed.yTicks.forEach { value ->
                    val y = mapY(value, computed.yMin, computed.yMax, chartRect)
                    drawLine(
                        color = styleOptions.gridColor.toComposeColor(),
                        start = Offset(chartRect.left, y),
                        end = Offset(chartRect.right, y),
                        strokeWidth = 1f * pxDensity,
                    )
                    val axisText = presentationOptions.yLabelFormatter(value)
                    drawContext.canvas.nativeCanvas.drawText(
                        axisText,
                        chartRect.left - 8f * pxDensity,
                        y + gridPaint.textSize * 0.35f,
                        xAxisPaint,
                    )
                }

                computed.xTicks.forEach { value ->
                    val x = mapX(value, computed.xMin, computed.xMax, chartRect)
                    drawLine(
                        color = styleOptions.gridColor.toComposeColor(),
                        start = Offset(x, chartRect.top),
                        end = Offset(x, chartRect.bottom),
                        strokeWidth = 1f * pxDensity,
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        presentationOptions.xLabelFormatter(value),
                        x,
                        chartRect.bottom + axisLabelPx + 8f * pxDensity,
                        xLabelPaint,
                    )
                }
            }

            if (presentationOptions.showAxes) {
                drawLine(
                    color = styleOptions.axisColor.toComposeColor(),
                    start = Offset(chartRect.left, chartRect.bottom),
                    end = Offset(chartRect.right, chartRect.bottom),
                    strokeWidth = 1.2f * pxDensity,
                )
                drawLine(
                    color = styleOptions.axisColor.toComposeColor(),
                    start = Offset(chartRect.left, chartRect.top),
                    end = Offset(chartRect.left, chartRect.bottom),
                    strokeWidth = 1.2f * pxDensity,
                )
            }

            for ((seriesIndex, points) in computed.seriesPoints.withIndex()) {
                if (points.isEmpty()) continue
                val color = series.getOrNull(seriesIndex)?.color ?: styleOptions.axisColor
                val composeColor = color.toComposeColor()

                buildAnimatedLinePath(points, progress, linePath)

                if (presentationOptions.showAreaFill && points.size > 1) {
                    fillPath.reset()
                    buildAnimatedLinePath(points, progress, fillPath)
                    val first = points.first()
                    val last = points.last()
                    fillPath.lineTo(last.x, computed.baselineY)
                    fillPath.lineTo(first.x, computed.baselineY)
                    fillPath.close()
                    drawPath(
                        path = fillPath,
                        color = withAlpha(color, styleOptions.areaFillAlpha).toComposeColor(),
                    )
                }

                drawPath(
                    path = linePath,
                    color = composeColor,
                    style = Stroke(width = lineStrokePx),
                )

                if (presentationOptions.showPoints) {
                    val showCount = visiblePointCount(points.size, progress)
                    for (pointIndex in points.indices) {
                        if (pointIndex >= showCount) break
                        val point = points[pointIndex]
                        val isSelected = selectedPoint == point
                        drawCircle(
                            color = if (isSelected) androidx.compose.ui.graphics.Color.White else composeColor,
                            center = Offset(point.x, point.y),
                            radius = if (isSelected) selectedPointRadiusPx else pointRadiusPx,
                        )
                    }
                }
            }

            if (presentationOptions.showLegend) {
                var x = chartRect.left + dpToPx(presentationOptions.legendLeftMarginDp, pxDensity)
                var y = computed.legendTop
                val markerSize = styleOptions.legendMarkerSizeDp * pxDensity
                val markerTextGap = styleOptions.legendMarkerTextGapDp * pxDensity
                val rowSpacing = styleOptions.legendRowSpacingDp * pxDensity

                for (item in series) {
                    val markerPaint = newTextPaint(color = item.color, textSizePx = legendTextPx)
                    markerPaint.style = Paint.Style.FILL
                    val textX = x + markerSize + markerTextGap
                    val measure = legendPaint.measureText(item.name)

                    if (x + markerSize + markerTextGap + measure > chartRect.right) {
                        x = chartRect.left + dpToPx(presentationOptions.legendLeftMarginDp, pxDensity)
                        y += (legendPaint.fontSpacing + rowSpacing)
                    }

                    drawContext.canvas.nativeCanvas.drawRect(
                        x,
                        y - markerSize,
                        x + markerSize,
                        y,
                        markerPaint,
                    )
                    drawContext.canvas.nativeCanvas.drawText(item.name, textX, y, legendPaint)
                    x += markerSize + markerTextGap + measure + styleOptions.legendItemSpacingDp * pxDensity
                }
            }
        }
    }
}

/**
 * Area chart variant that reuses [LineChart] with fill enabled.
 */
@Composable
fun AreaChart(
    series: List<LineSeries>,
    modifier: Modifier = Modifier,
    styleOptions: LineChartStyleOptions = LineChartStyleOptions(),
    presentationOptions: LineChartPresentationOptions = LineChartPresentationOptions(showAreaFill = true),
    onPointClick: ((seriesIndex: Int, pointIndex: Int, point: LineDatum, payload: Any?) -> Unit)? = null,
) {
    LineChart(
        series = series,
        modifier = modifier,
        styleOptions = styleOptions,
        presentationOptions = presentationOptions.copy(showAreaFill = true),
        onPointClick = onPointClick,
    )
}
