package com.magimon.eq.compose.rangebar

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
import com.magimon.eq.rangebar.RangeBarChartPresentationOptions
import com.magimon.eq.rangebar.RangeBarChartStyleOptions
import com.magimon.eq.rangebar.RangeBarEntry
import com.magimon.eq.rangebar.RangeBarLayoutEntry
import com.magimon.eq.rangebar.resolveRangeBarChartLayout
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal data class RenderRangeBar(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val entry: RangeBarLayoutEntry,
)

internal data class ComputedRangeBarChart(
    val chartRect: Rect,
    val minValue: Double,
    val maxValue: Double,
    val ticks: List<Double>,
    val bars: List<RenderRangeBar>,
)

private fun dpToPx(value: Float, density: Float): Float = value * density

private fun valueToX(chartRect: Rect, value: Double, minValue: Double, maxValue: Double): Float {
    val ratio = if (maxValue == minValue) {
        0.5f
    } else {
        ((value - minValue) / (maxValue - minValue)).toFloat()
    }
    return chartRect.left + ratio * chartRect.width
}

private fun animatedEndValue(
    entry: RangeBarLayoutEntry,
    presentation: RangeBarChartPresentationOptions,
    progress: Float,
): Double {
    if (!presentation.animationDirection) return entry.endValue
    return entry.startValue + (entry.endValue - entry.startValue) * progress.toDouble()
}

internal fun computeRangeBarLayout(
    widthPx: Float,
    heightPx: Float,
    entries: List<RangeBarEntry>,
    style: RangeBarChartStyleOptions,
    presentation: RangeBarChartPresentationOptions,
    progress: Float,
    density: Float,
    scaledDensity: Float,
): ComputedRangeBarChart {
    val resolved = resolveRangeBarChartLayout(
        entries = entries,
        style = style,
        tickCount = presentation.xTickCount,
    )
    val contentPadding = dpToPx(style.contentPaddingDp, density)
    val categoryPaint = newTextPaint(
        color = style.axisLabelColor,
        textSizePx = presentation.axisLabelTextSizeSp * scaledDensity,
        align = Paint.Align.RIGHT,
    )
    val maxCategoryWidth = resolved.entries.maxOfOrNull {
        categoryPaint.measureText(presentation.rowLabelFormatter(it.label))
    } ?: 0f
    val bottomReserve = contentPadding + categoryPaint.fontSpacing + 18f * density
    val topReserve = contentPadding + if (presentation.showBarLabels) {
        presentation.barLabelTextSizeSp * scaledDensity + 8f * density
    } else {
        8f * density
    }
    val leftReserve = contentPadding + maxCategoryWidth + 16f * density
    val chartRect = Rect(
        left = leftReserve,
        top = topReserve,
        right = widthPx - contentPadding,
        bottom = heightPx - bottomReserve,
    )

    if (resolved.entries.isEmpty() || chartRect.width <= 0f || chartRect.height <= 0f) {
        return ComputedRangeBarChart(
            chartRect = chartRect,
            minValue = resolved.minValue,
            maxValue = resolved.maxValue,
            ticks = resolved.ticks,
            bars = emptyList(),
        )
    }

    val slotHeight = chartRect.height / resolved.entries.size.toFloat()
    val usableSlot = (slotHeight - dpToPx(style.rowSpacingDp, density)).coerceAtLeast(1f)
    val barHeight = (usableSlot * style.barHeightRatio.coerceIn(0.15f, 1f)).coerceAtLeast(1f)
    val bars = resolved.entries.mapIndexed { index, entry ->
        val top = chartRect.top + index * slotHeight + (slotHeight - barHeight) * 0.5f
        val bottom = top + barHeight
        val left = valueToX(chartRect, entry.startValue, resolved.minValue, resolved.maxValue)
        val rawRight = valueToX(
            chartRect,
            animatedEndValue(entry, presentation, progress),
            resolved.minValue,
            resolved.maxValue,
        )
        RenderRangeBar(
            left = min(left, rawRight),
            top = top,
            right = max(left + dpToPx(style.minBarWidthDp, density), max(left, rawRight)),
            bottom = bottom,
            entry = entry,
        )
    }

    return ComputedRangeBarChart(
        chartRect = chartRect,
        minValue = resolved.minValue,
        maxValue = resolved.maxValue,
        ticks = resolved.ticks,
        bars = bars,
    )
}

/**
 * Canvas-based range bar chart composable.
 *
 * The chart renders ordered horizontal bars whose lengths represent start and end positions on a
 * shared numeric axis, which makes it suitable for roadmap and lightweight timeline views.
 *
 * @param entries Ordered range items rendered from top to bottom
 * @param modifier Standard Compose modifier for layout and gesture handling
 * @param styleOptions Colors, spacing, radii, and typography used while drawing
 * @param presentationOptions Axis, label, and animation behavior
 * @param onEntryClick Optional callback invoked as `(index, entry, duration)`
 */
@Composable
fun RangeBarChart(
    entries: List<RangeBarEntry>,
    modifier: Modifier = Modifier,
    styleOptions: RangeBarChartStyleOptions = RangeBarChartStyleOptions(),
    presentationOptions: RangeBarChartPresentationOptions = RangeBarChartPresentationOptions(),
    onEntryClick: ((index: Int, entry: RangeBarEntry, duration: Double) -> Unit)? = null,
) {
    val density = LocalDensity.current
    val densityPx = density.density
    val scaledDensity = density.fontScale * densityPx
    val progress = remember { Animatable(1f) }
    var selectedBar by remember { mutableStateOf<RenderRangeBar?>(null) }
    val animateOnDataChange = presentationOptions.animateOnDataChange
    val enterAnimationDurationMs = presentationOptions.enterAnimationDurationMs
    val enterAnimationDelayMs = presentationOptions.enterAnimationDelayMs

    LaunchedEffect(entries, animateOnDataChange, enterAnimationDurationMs, enterAnimationDelayMs) {
        if (animateOnDataChange && entries.isNotEmpty()) {
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
            entries,
            styleOptions,
            presentationOptions,
            widthPx,
            heightPx,
            current,
            densityPx,
            scaledDensity,
        ) {
            computeRangeBarLayout(
                widthPx = widthPx,
                heightPx = heightPx,
                entries = entries,
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
            align = Paint.Align.CENTER,
        )
        val categoryLabelPaint = newTextPaint(
            color = styleOptions.axisLabelColor,
            textSizePx = presentationOptions.axisLabelTextSizeSp * scaledDensity,
            align = Paint.Align.RIGHT,
        )
        val rangeLabelPaint = newTextPaint(
            color = styleOptions.barLabelTextColor,
            textSizePx = presentationOptions.barLabelTextSizeSp * scaledDensity,
            align = Paint.Align.LEFT,
        )
        val emptyTextPaint = newTextPaint(
            color = styleOptions.axisLabelColor,
            textSizePx = presentationOptions.axisLabelTextSizeSp * scaledDensity,
            align = Paint.Align.CENTER,
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(computed.bars, entries, onEntryClick) {
                    detectTapGestures { tap ->
                        val hit = computed.bars.lastOrNull { bar ->
                            tap.x in bar.left..bar.right && tap.y in bar.top..bar.bottom
                        }
                        selectedBar = hit
                        hit?.let {
                            val sourceEntry = entries.getOrNull(it.entry.index) ?: return@let
                            onEntryClick?.invoke(
                                it.entry.index,
                                sourceEntry,
                                abs(it.entry.endValue - it.entry.startValue),
                            )
                        }
                    }
                },
        ) {
            drawRect(color = styleOptions.backgroundColor.toComposeColor())

            if (computed.bars.isEmpty()) {
                drawContext.canvas.nativeCanvas.drawText(
                    presentationOptions.emptyText,
                    size.width * 0.5f,
                    size.height * 0.5f,
                    emptyTextPaint,
                )
                return@Canvas
            }

            if (presentationOptions.showGrid) {
                computed.ticks.forEach { tick ->
                    val x = valueToX(computed.chartRect, tick, computed.minValue, computed.maxValue)
                    drawLine(
                        color = styleOptions.gridColor.toComposeColor(),
                        start = Offset(x, computed.chartRect.top),
                        end = Offset(x, computed.chartRect.bottom),
                        strokeWidth = dpToPx(1f, densityPx),
                    )
                }
            }

            if (presentationOptions.showAxes) {
                drawLine(
                    color = styleOptions.axisColor.toComposeColor(),
                    start = Offset(computed.chartRect.left, computed.chartRect.bottom),
                    end = Offset(computed.chartRect.right, computed.chartRect.bottom),
                    strokeWidth = dpToPx(1.5f, densityPx),
                )
            }

            computed.bars.forEach { bar ->
                drawRoundRect(
                    color = bar.entry.color.toComposeColor(),
                    topLeft = Offset(bar.left, bar.top),
                    size = androidx.compose.ui.geometry.Size(
                        width = (bar.right - bar.left).coerceAtLeast(1f),
                        height = (bar.bottom - bar.top).coerceAtLeast(1f),
                    ),
                    cornerRadius = CornerRadius(
                        dpToPx(styleOptions.barCornerRadiusDp, densityPx),
                        dpToPx(styleOptions.barCornerRadiusDp, densityPx),
                    ),
                )

                if (selectedBar?.entry?.index == bar.entry.index) {
                    drawRoundRect(
                        color = styleOptions.axisColor.toComposeColor(),
                        topLeft = Offset(
                            bar.left - dpToPx(styleOptions.selectedBarPaddingDp, densityPx),
                            bar.top - dpToPx(styleOptions.selectedBarPaddingDp, densityPx),
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            width = (bar.right - bar.left) + dpToPx(styleOptions.selectedBarPaddingDp, densityPx) * 2f,
                            height = (bar.bottom - bar.top) + dpToPx(styleOptions.selectedBarPaddingDp, densityPx) * 2f,
                        ),
                        cornerRadius = CornerRadius(
                            dpToPx(styleOptions.barCornerRadiusDp, densityPx),
                            dpToPx(styleOptions.barCornerRadiusDp, densityPx),
                        ),
                        style = Stroke(width = dpToPx(1.5f, densityPx)),
                    )
                }
            }

            drawContext.canvas.nativeCanvas.apply {
                computed.bars.forEach { bar ->
                    val categoryY = bar.top + (bar.bottom - bar.top) * 0.5f + categoryLabelPaint.textSize * 0.35f
                    drawText(
                        presentationOptions.rowLabelFormatter(bar.entry.label),
                        computed.chartRect.left - 12f * densityPx,
                        categoryY,
                        categoryLabelPaint,
                    )

                    if (presentationOptions.showBarLabels) {
                        val label = presentationOptions.barLabelFormatter(bar.entry)
                        drawText(
                            label,
                            min(bar.right + 8f * densityPx, size.width - 4f * densityPx),
                            bar.top - 4f * densityPx,
                            rangeLabelPaint,
                        )
                    }
                }

                computed.ticks.forEach { tick ->
                    val x = valueToX(computed.chartRect, tick, computed.minValue, computed.maxValue)
                    drawText(
                        presentationOptions.xLabelFormatter(tick),
                        x,
                        computed.chartRect.bottom + axisLabelPaint.fontSpacing,
                        axisLabelPaint,
                    )
                }
            }
        }
    }
}
