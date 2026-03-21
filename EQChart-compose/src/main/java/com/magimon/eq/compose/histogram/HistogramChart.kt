package com.magimon.eq.compose.histogram

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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import com.magimon.eq.compose.internal.newTextPaint
import com.magimon.eq.compose.internal.toComposeColor
import com.magimon.eq.histogram.HistogramBin
import com.magimon.eq.histogram.HistogramChartPresentationOptions
import com.magimon.eq.histogram.HistogramChartStyleOptions
import com.magimon.eq.histogram.HistogramLayoutBin
import com.magimon.eq.histogram.resolveHistogramChartLayout
import kotlin.math.max
import kotlin.math.min

internal data class RenderHistogramBar(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val bin: HistogramLayoutBin,
)

internal data class ComputedHistogramChart(
    val chartRect: Rect,
    val minValue: Double,
    val maxValue: Double,
    val baselineValue: Double,
    val ticks: List<Double>,
    val bars: List<RenderHistogramBar>,
)

private fun dpToPx(value: Float, density: Float): Float = value * density

private fun valueToY(chartRect: Rect, value: Double, minValue: Double, maxValue: Double): Float {
    val ratio = if (maxValue == minValue) 0.5f else ((value - minValue) / (maxValue - minValue)).toFloat()
    return chartRect.bottom - ratio * chartRect.height
}

private fun valueToX(chartRect: Rect, value: Double, minValue: Double, maxValue: Double): Float {
    val ratio = if (maxValue == minValue) 0.5f else ((value - minValue) / (maxValue - minValue)).toFloat()
    return chartRect.left + ratio * chartRect.width
}

private fun animatedValue(
    value: Double,
    baselineValue: Double,
    presentation: HistogramChartPresentationOptions,
    progress: Float,
): Double {
    if (!presentation.animationDirection) return value
    return baselineValue + (value - baselineValue) * progress.toDouble()
}

internal fun computeHistogramLayout(
    widthPx: Float,
    heightPx: Float,
    bins: List<HistogramBin>,
    style: HistogramChartStyleOptions,
    presentation: HistogramChartPresentationOptions,
    progress: Float,
    density: Float,
    scaledDensity: Float,
): ComputedHistogramChart {
    val resolved = resolveHistogramChartLayout(
        bins = bins,
        style = style,
        presentation = presentation,
    )
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
    val topReserve = contentPadding + if (presentation.showBarLabels) {
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

    if (resolved.bins.isEmpty() || chartRect.width <= 0f || chartRect.height <= 0f) {
        return ComputedHistogramChart(
            chartRect = chartRect,
            minValue = resolved.minValue,
            maxValue = resolved.maxValue,
            baselineValue = resolved.baselineValue,
            ticks = resolved.ticks,
            bars = emptyList(),
        )
    }

    val categoryGap = dpToPx(style.categorySpacingDp, density)
    val bars = resolved.bins.map { bin ->
        val rawLeft = valueToX(chartRect, bin.start, resolved.minBinStart, resolved.maxBinEnd)
        val rawRight = valueToX(chartRect, bin.end, resolved.minBinStart, resolved.maxBinEnd)
        val maxInset = ((rawRight - rawLeft) * 0.5f) - 0.5f
        val inset = min(categoryGap * 0.5f, maxInset).coerceAtLeast(0f)
        val left = rawLeft + inset
        val right = max(left + 1f, rawRight - inset)
        val startY = valueToY(chartRect, resolved.baselineValue, resolved.minValue, resolved.maxValue)
        val endY = valueToY(
            chartRect,
            animatedValue(bin.value, resolved.baselineValue, presentation, progress),
            resolved.minValue,
            resolved.maxValue,
        )
        RenderHistogramBar(
            left = left,
            top = min(startY, endY),
            right = right,
            bottom = max(startY, endY),
            bin = bin,
        )
    }

    return ComputedHistogramChart(
        chartRect = chartRect,
        minValue = resolved.minValue,
        maxValue = resolved.maxValue,
        baselineValue = resolved.baselineValue,
        ticks = resolved.ticks,
        bars = bars,
    )
}

/**
 * Canvas-based histogram chart composable.
 *
 * The chart renders an ordered list of histogram bins with value labels and bin click callbacks.
 *
 * @param bins Ordered histogram bins
 * @param modifier Standard Compose modifier for layout and gesture handling
 * @param styleOptions Colors, spacing, radii, and typography used while drawing
 * @param presentationOptions Axis and animation behavior
 * @param onBinClick Optional callback invoked as `(index, bin, value)`
 */
@Composable
fun HistogramChart(
    bins: List<HistogramBin>,
    modifier: Modifier = Modifier,
    styleOptions: HistogramChartStyleOptions = HistogramChartStyleOptions(),
    presentationOptions: HistogramChartPresentationOptions = HistogramChartPresentationOptions(),
    onBinClick: ((index: Int, bin: HistogramBin, value: Double) -> Unit)? = null,
) {
    val density = LocalDensity.current
    val densityPx = density.density
    val scaledDensity = density.fontScale * densityPx
    val progress = remember { Animatable(1f) }
    var selectedBar by remember { mutableStateOf<RenderHistogramBar?>(null) }
    val animateOnDataChange = presentationOptions.animateOnDataChange
    val enterAnimationDurationMs = presentationOptions.enterAnimationDurationMs
    val enterAnimationDelayMs = presentationOptions.enterAnimationDelayMs

    LaunchedEffect(bins, animateOnDataChange, enterAnimationDurationMs, enterAnimationDelayMs) {
        if (animateOnDataChange && bins.isNotEmpty()) {
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
            bins,
            styleOptions,
            widthPx,
            heightPx,
            current,
            densityPx,
            scaledDensity,
            presentationOptions,
        ) {
            computeHistogramLayout(
                widthPx = widthPx,
                heightPx = heightPx,
                bins = bins,
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
            color = styleOptions.barValueTextColor,
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
                .pointerInput(computed.bars, onBinClick) {
                    detectTapGestures { tap ->
                        val hit = computed.bars.lastOrNull { bar ->
                            tap.x in bar.left..bar.right && tap.y in bar.top..bar.bottom
                        }
                        selectedBar = hit
                        hit?.let {
                            onBinClick?.invoke(
                                it.bin.index,
                                it.bin.sourceBin,
                                it.bin.value,
                            )
                        }
                    }
                },
        ) {
            drawRect(styleOptions.backgroundColor.toComposeColor())

            if (computed.bars.isEmpty()) {
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
                val baselineY = valueToY(chartRect, computed.baselineValue, computed.minValue, computed.maxValue)
                drawLine(
                    color = styleOptions.axisColor.toComposeColor(),
                    start = Offset(chartRect.left, baselineY),
                    end = Offset(chartRect.right, baselineY),
                    strokeWidth = 1.1f * densityPx,
                )
                drawLine(
                    color = styleOptions.axisColor.toComposeColor(),
                    start = Offset(chartRect.left, chartRect.top),
                    end = Offset(chartRect.left, chartRect.bottom),
                    strokeWidth = 1.1f * densityPx,
                )
            }

            val barRadius = dpToPx(styleOptions.barCornerRadiusDp, densityPx)
            val selectionPadding = dpToPx(styleOptions.selectedBarPaddingDp, densityPx)
            computed.bars.forEach { bar ->
                drawRoundRect(
                    color = bar.bin.color.toComposeColor(),
                    topLeft = Offset(bar.left, bar.top),
                    size = androidx.compose.ui.geometry.Size(bar.right - bar.left, bar.bottom - bar.top),
                    cornerRadius = CornerRadius(barRadius, barRadius),
                )
                if (selectedBar == bar) {
                    drawRoundRect(
                        color = styleOptions.axisColor.toComposeColor(),
                        topLeft = Offset(bar.left - selectionPadding, bar.top - selectionPadding),
                        size = androidx.compose.ui.geometry.Size(
                            (bar.right - bar.left) + selectionPadding * 2f,
                            (bar.bottom - bar.top) + selectionPadding * 2f,
                        ),
                        cornerRadius = CornerRadius(barRadius, barRadius),
                        style = Stroke(width = 2f * densityPx),
                    )
                }
                if (presentationOptions.showBarLabels) {
                    val labelY = if (bar.bin.value >= computed.baselineValue) {
                        bar.top - 6f * densityPx
                    } else {
                        bar.bottom + valueLabelPaint.textSize + 2f * densityPx
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        presentationOptions.valueLabelFormatter(bar.bin.value),
                        (bar.left + bar.right) * 0.5f,
                        labelY,
                        valueLabelPaint,
                    )
                }
                drawContext.canvas.nativeCanvas.drawText(
                    bar.bin.label,
                    (bar.left + bar.right) * 0.5f,
                    chartRect.bottom + categoryLabelPaint.textSize + 6f * densityPx,
                    categoryLabelPaint,
                )
            }
        }
    }
}
