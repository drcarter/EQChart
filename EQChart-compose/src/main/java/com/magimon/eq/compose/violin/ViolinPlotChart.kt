package com.magimon.eq.compose.violin

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
import com.magimon.eq.compose.internal.newTextPaint
import com.magimon.eq.compose.internal.toComposeColor
import com.magimon.eq.violin.ViolinPlotChartPresentationOptions
import com.magimon.eq.violin.ViolinPlotChartStyleOptions
import com.magimon.eq.violin.ViolinPlotLayoutSeries
import com.magimon.eq.violin.ViolinPlotSeries
import com.magimon.eq.violin.resolveViolinPlotChartLayout
import kotlin.math.max
import kotlin.math.min

internal data class RenderViolinPlotEntry(
    val touchRect: Rect,
    val centerX: Float,
    val maxHalfWidth: Float,
    val topY: Float,
    val bottomY: Float,
    val q1Y: Float,
    val medianY: Float,
    val q3Y: Float,
    val outlinePoints: List<Offset>,
    val entry: ViolinPlotLayoutSeries,
)

internal data class ComputedViolinPlotChart(
    val chartRect: Rect,
    val minValue: Double,
    val maxValue: Double,
    val ticks: List<Double>,
    val entries: List<RenderViolinPlotEntry>,
)

private fun dpToPx(value: Float, density: Float): Float = value * density

private fun valueToY(chartRect: Rect, value: Double, minValue: Double, maxValue: Double): Float {
    val ratio = if (maxValue == minValue) 0.5f else ((value - minValue) / (maxValue - minValue)).toFloat()
    return chartRect.bottom - ratio * chartRect.height
}

internal fun computeViolinPlotLayout(
    widthPx: Float,
    heightPx: Float,
    series: List<ViolinPlotSeries>,
    style: ViolinPlotChartStyleOptions,
    presentation: ViolinPlotChartPresentationOptions,
    progress: Float,
    density: Float,
    scaledDensity: Float,
): ComputedViolinPlotChart {
    val resolved = resolveViolinPlotChartLayout(series, style, presentation)
    val contentPadding = dpToPx(style.contentPaddingDp, density)
    val axisPaint = newTextPaint(
        color = style.axisLabelColor,
        textSizePx = presentation.axisLabelTextSizeSp * scaledDensity,
        align = Paint.Align.RIGHT,
    )
    val maxTickWidth = resolved.ticks.maxOfOrNull {
        axisPaint.measureText(presentation.yLabelFormatter(it))
    } ?: 0f
    val leftReserve = contentPadding + maxTickWidth + 12f * density
    val bottomReserve = contentPadding + axisPaint.fontSpacing + 18f * density
    val topReserve = contentPadding + if (presentation.showValueLabels) {
        presentation.valueLabelTextSizeSp * scaledDensity + 8f * density
    } else {
        8f * density
    }
    val chartRect = Rect(
        left = leftReserve,
        top = topReserve,
        right = widthPx - contentPadding,
        bottom = heightPx - bottomReserve,
    )

    if (resolved.series.isEmpty() || chartRect.width <= 0f || chartRect.height <= 0f) {
        return ComputedViolinPlotChart(
            chartRect = chartRect,
            minValue = resolved.minValue,
            maxValue = resolved.maxValue,
            ticks = resolved.ticks,
            entries = emptyList(),
        )
    }

    val categoryGap = dpToPx(style.categorySpacingDp, density)
    val slotWidth = chartRect.width / resolved.series.size.toFloat()
    val usableSlot = (slotWidth - categoryGap).coerceAtLeast(1f)
    val maxHalfWidth = (usableSlot * style.violinWidthRatio.coerceIn(0.2f, 0.95f) * 0.5f).coerceAtLeast(6f)

    val renderEntries = resolved.series.mapIndexed { index, entry ->
        val slotLeft = chartRect.left + slotWidth * index
        val slotRight = slotLeft + slotWidth
        val centerX = (slotLeft + slotRight) * 0.5f
        val outlinePoints = entry.densityPoints.map { point ->
            Offset(
                x = centerX + maxHalfWidth * point.widthRatio * progress,
                y = valueToY(chartRect, point.value, resolved.minValue, resolved.maxValue),
            )
        }
        val topY = outlinePoints.minOfOrNull { it.y } ?: chartRect.top
        val bottomY = outlinePoints.maxOfOrNull { it.y } ?: chartRect.bottom
        RenderViolinPlotEntry(
            touchRect = Rect(
                left = slotLeft,
                top = topY,
                right = slotRight,
                bottom = bottomY,
            ),
            centerX = centerX,
            maxHalfWidth = maxHalfWidth * progress,
            topY = topY,
            bottomY = bottomY,
            q1Y = valueToY(chartRect, entry.q1, resolved.minValue, resolved.maxValue),
            medianY = valueToY(chartRect, entry.median, resolved.minValue, resolved.maxValue),
            q3Y = valueToY(chartRect, entry.q3, resolved.minValue, resolved.maxValue),
            outlinePoints = outlinePoints,
            entry = entry,
        )
    }

    return ComputedViolinPlotChart(
        chartRect = chartRect,
        minValue = resolved.minValue,
        maxValue = resolved.maxValue,
        ticks = resolved.ticks,
        entries = renderEntries,
    )
}

/**
 * Canvas-based violin plot chart composable.
 */
@Composable
fun ViolinPlotChart(
    series: List<ViolinPlotSeries>,
    modifier: Modifier = Modifier,
    styleOptions: ViolinPlotChartStyleOptions = ViolinPlotChartStyleOptions(),
    presentationOptions: ViolinPlotChartPresentationOptions = ViolinPlotChartPresentationOptions(),
    onSeriesClick: ((index: Int, series: ViolinPlotSeries) -> Unit)? = null,
) {
    val density = LocalDensity.current
    val densityPx = density.density
    val scaledDensity = density.fontScale * densityPx
    val progress = remember { Animatable(1f) }
    var selectedEntry by remember { mutableStateOf<RenderViolinPlotEntry?>(null) }
    val animateOnDataChange = presentationOptions.animateOnDataChange
    val enterAnimationDurationMs = presentationOptions.enterAnimationDurationMs
    val enterAnimationDelayMs = presentationOptions.enterAnimationDelayMs

    LaunchedEffect(series, animateOnDataChange, enterAnimationDurationMs, enterAnimationDelayMs) {
        if (animateOnDataChange && series.isNotEmpty()) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = enterAnimationDurationMs.toInt().coerceAtLeast(0),
                    delayMillis = enterAnimationDelayMs.toInt().coerceAtLeast(0),
                ),
            )
        } else {
            progress.snapTo(1f)
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val current = progress.value.coerceIn(0f, 1f)
        val computed = remember(
            series,
            styleOptions,
            presentationOptions,
            widthPx,
            heightPx,
            current,
            densityPx,
            scaledDensity,
        ) {
            computeViolinPlotLayout(
                widthPx = widthPx,
                heightPx = heightPx,
                series = series,
                style = styleOptions,
                presentation = presentationOptions,
                progress = current,
                density = densityPx,
                scaledDensity = scaledDensity,
            )
        }

        val axisLabelPaint = newTextPaint(
            color = styleOptions.axisLabelColor,
            textSizePx = presentationOptions.axisLabelTextSizeSp * scaledDensity,
            align = Paint.Align.RIGHT,
        )
        val categoryLabelPaint = newTextPaint(
            color = styleOptions.axisLabelColor,
            textSizePx = presentationOptions.axisLabelTextSizeSp * scaledDensity,
            align = Paint.Align.CENTER,
        )
        val valueLabelPaint = newTextPaint(
            color = styleOptions.valueLabelTextColor,
            textSizePx = presentationOptions.valueLabelTextSizeSp * scaledDensity,
            align = Paint.Align.CENTER,
        )
        val emptyTextPaint = newTextPaint(
            color = styleOptions.axisLabelColor,
            textSizePx = presentationOptions.axisLabelTextSizeSp * scaledDensity,
            align = Paint.Align.CENTER,
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(computed.entries, onSeriesClick) {
                    detectTapGestures { tap ->
                        val hit = computed.entries.lastOrNull { entry ->
                            tap.x in entry.touchRect.left..entry.touchRect.right &&
                                tap.y in entry.touchRect.top..entry.touchRect.bottom
                        }
                        selectedEntry = hit
                        hit?.let {
                            onSeriesClick?.invoke(it.entry.index, it.entry.sourceSeries)
                        }
                    }
                },
        ) {
            drawRect(styleOptions.backgroundColor.toComposeColor())

            if (computed.entries.isEmpty()) {
                if (presentationOptions.emptyText.isNotBlank()) {
                    drawContext.canvas.nativeCanvas.drawText(
                        presentationOptions.emptyText,
                        size.width * 0.5f,
                        size.height * 0.5f,
                        emptyTextPaint,
                    )
                }
                return@Canvas
            }

            val chartRect = computed.chartRect
            if (presentationOptions.showGrid) {
                computed.ticks.forEach { tick ->
                    val y = valueToY(chartRect, tick, computed.minValue, computed.maxValue)
                    drawLine(
                        color = styleOptions.gridColor.toComposeColor(),
                        start = Offset(chartRect.left, y),
                        end = Offset(chartRect.right, y),
                        strokeWidth = 1f * densityPx,
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        presentationOptions.yLabelFormatter(tick),
                        chartRect.left - 8f * densityPx,
                        y + axisLabelPaint.textSize * 0.35f,
                        axisLabelPaint,
                    )
                }
            }

            if (presentationOptions.showAxes) {
                drawLine(
                    color = styleOptions.axisColor.toComposeColor(),
                    start = Offset(chartRect.left, chartRect.bottom),
                    end = Offset(chartRect.right, chartRect.bottom),
                    strokeWidth = 1.1f * densityPx,
                )
                drawLine(
                    color = styleOptions.axisColor.toComposeColor(),
                    start = Offset(chartRect.left, chartRect.top),
                    end = Offset(chartRect.left, chartRect.bottom),
                    strokeWidth = 1.1f * densityPx,
                )
            }

            val selectionPadding = dpToPx(styleOptions.selectedPaddingDp, densityPx)
            computed.entries.forEach { entry ->
                val path = buildViolinPath(entry)
                drawPath(
                    path = path,
                    color = entry.entry.color.toComposeColor(),
                )

                if (presentationOptions.showQuartileBand) {
                    val bandHalfWidth = entry.maxHalfWidth * 0.22f
                    drawRoundRect(
                        color = styleOptions.quartileBandColor.toComposeColor(),
                        topLeft = Offset(entry.centerX - bandHalfWidth, min(entry.q3Y, entry.q1Y)),
                        size = androidx.compose.ui.geometry.Size(
                            width = bandHalfWidth * 2f,
                            height = max(entry.q1Y, entry.q3Y) - min(entry.q1Y, entry.q3Y),
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(bandHalfWidth, bandHalfWidth),
                    )
                }

                if (presentationOptions.showMedianLine) {
                    val medianHalfWidth = entry.maxHalfWidth * 0.68f
                    drawLine(
                        color = styleOptions.medianLineColor.toComposeColor(),
                        start = Offset(entry.centerX - medianHalfWidth, entry.medianY),
                        end = Offset(entry.centerX + medianHalfWidth, entry.medianY),
                        strokeWidth = max(1.4f * densityPx, dpToPx(styleOptions.violinStrokeWidthDp, densityPx)),
                    )
                }

                if (selectedEntry === entry) {
                    val rect = Rect(
                        left = entry.touchRect.left - selectionPadding,
                        top = entry.touchRect.top - selectionPadding,
                        right = entry.touchRect.right + selectionPadding,
                        bottom = entry.touchRect.bottom + selectionPadding,
                    )
                    drawRoundRect(
                        color = styleOptions.selectedOutlineColor.toComposeColor(),
                        topLeft = rect.topLeft,
                        size = rect.size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(selectionPadding, selectionPadding),
                        style = Stroke(width = 2f * densityPx),
                    )
                }

                drawContext.canvas.nativeCanvas.drawText(
                    presentationOptions.xLabelFormatter(entry.entry),
                    entry.centerX,
                    chartRect.bottom + categoryLabelPaint.fontSpacing,
                    categoryLabelPaint,
                )

                if (presentationOptions.showValueLabels) {
                    drawContext.canvas.nativeCanvas.drawText(
                        presentationOptions.valueLabelFormatter(entry.entry),
                        entry.centerX,
                        entry.topY - 6f * densityPx,
                        valueLabelPaint,
                    )
                }
            }
        }
    }
}

private fun buildViolinPath(entry: RenderViolinPlotEntry): Path {
    val path = Path()
    val first = entry.outlinePoints.firstOrNull() ?: return path
    path.moveTo(entry.centerX, first.y)
    entry.outlinePoints.forEach { point ->
        path.lineTo(point.x, point.y)
    }
    for (index in entry.outlinePoints.lastIndex downTo 0) {
        val point = entry.outlinePoints[index]
        val mirroredX = entry.centerX - (point.x - entry.centerX)
        path.lineTo(mirroredX, point.y)
    }
    path.close()
    return path
}
