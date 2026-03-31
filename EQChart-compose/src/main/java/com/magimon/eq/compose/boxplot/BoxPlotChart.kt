package com.magimon.eq.compose.boxplot

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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import com.magimon.eq.boxplot.BoxPlotChartPresentationOptions
import com.magimon.eq.boxplot.BoxPlotChartStyleOptions
import com.magimon.eq.boxplot.BoxPlotEntry
import com.magimon.eq.boxplot.BoxPlotLayoutEntry
import com.magimon.eq.boxplot.resolveBoxPlotChartLayout
import com.magimon.eq.compose.internal.newTextPaint
import com.magimon.eq.compose.internal.toComposeColor
import kotlin.math.max
import kotlin.math.min

internal data class RenderBoxPlotEntry(
    val touchRect: Rect,
    val boxRect: Rect,
    val centerX: Float,
    val minY: Float,
    val maxY: Float,
    val medianY: Float,
    val capHalfWidth: Float,
    val outliers: List<Offset>,
    val entry: BoxPlotLayoutEntry,
)

internal data class ComputedBoxPlotChart(
    val chartRect: Rect,
    val minValue: Double,
    val maxValue: Double,
    val ticks: List<Double>,
    val entries: List<RenderBoxPlotEntry>,
)

private fun dpToPx(value: Float, density: Float): Float = value * density

private fun valueToY(chartRect: Rect, value: Double, minValue: Double, maxValue: Double): Float {
    val ratio = if (maxValue == minValue) 0.5f else ((value - minValue) / (maxValue - minValue)).toFloat()
    return chartRect.bottom - ratio * chartRect.height
}

private fun animatedValue(
    value: Double,
    minValue: Double,
    presentation: BoxPlotChartPresentationOptions,
    progress: Float,
): Double {
    if (!presentation.animationDirection) return value
    return minValue + (value - minValue) * progress.toDouble()
}

internal fun computeBoxPlotLayout(
    widthPx: Float,
    heightPx: Float,
    entries: List<BoxPlotEntry>,
    style: BoxPlotChartStyleOptions,
    presentation: BoxPlotChartPresentationOptions,
    progress: Float,
    density: Float,
    scaledDensity: Float,
): ComputedBoxPlotChart {
    val resolved = resolveBoxPlotChartLayout(entries, style, presentation)
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

    if (resolved.entries.isEmpty() || chartRect.width <= 0f || chartRect.height <= 0f) {
        return ComputedBoxPlotChart(
            chartRect = chartRect,
            minValue = resolved.minValue,
            maxValue = resolved.maxValue,
            ticks = resolved.ticks,
            entries = emptyList(),
        )
    }

    val categoryGap = dpToPx(style.categorySpacingDp, density)
    val slotWidth = chartRect.width / resolved.entries.size.toFloat()
    val usableSlot = (slotWidth - categoryGap).coerceAtLeast(1f)
    val boxWidth = (usableSlot * style.boxWidthRatio.coerceIn(0.2f, 0.9f)).coerceAtLeast(8f)
    val capHalfWidth = boxWidth * 0.35f

    val renderEntries = resolved.entries.mapIndexed { index, entry ->
        val slotLeft = chartRect.left + slotWidth * index
        val slotRight = slotLeft + slotWidth
        val centerX = (slotLeft + slotRight) * 0.5f
        val minY = valueToY(chartRect, animatedValue(entry.min, resolved.minValue, presentation, progress), resolved.minValue, resolved.maxValue)
        val q1Y = valueToY(chartRect, animatedValue(entry.q1, resolved.minValue, presentation, progress), resolved.minValue, resolved.maxValue)
        val medianY = valueToY(chartRect, animatedValue(entry.median, resolved.minValue, presentation, progress), resolved.minValue, resolved.maxValue)
        val q3Y = valueToY(chartRect, animatedValue(entry.q3, resolved.minValue, presentation, progress), resolved.minValue, resolved.maxValue)
        val maxY = valueToY(chartRect, animatedValue(entry.max, resolved.minValue, presentation, progress), resolved.minValue, resolved.maxValue)
        val boxRect = Rect(
            left = centerX - boxWidth * 0.5f,
            top = min(q3Y, q1Y),
            right = centerX + boxWidth * 0.5f,
            bottom = max(q3Y, q1Y),
        )
        val touchRect = Rect(
            left = slotLeft,
            top = min(maxY, boxRect.top),
            right = slotRight,
            bottom = max(minY, boxRect.bottom),
        )
        RenderBoxPlotEntry(
            touchRect = touchRect,
            boxRect = boxRect,
            centerX = centerX,
            minY = minY,
            maxY = maxY,
            medianY = medianY,
            capHalfWidth = capHalfWidth,
            outliers = entry.outliers.map { outlier ->
                Offset(centerX, valueToY(chartRect, animatedValue(outlier, resolved.minValue, presentation, progress), resolved.minValue, resolved.maxValue))
            },
            entry = entry,
        )
    }

    return ComputedBoxPlotChart(
        chartRect = chartRect,
        minValue = resolved.minValue,
        maxValue = resolved.maxValue,
        ticks = resolved.ticks,
        entries = renderEntries,
    )
}

/**
 * Canvas-based box plot chart composable.
 */
@Composable
fun BoxPlotChart(
    entries: List<BoxPlotEntry>,
    modifier: Modifier = Modifier,
    styleOptions: BoxPlotChartStyleOptions = BoxPlotChartStyleOptions(),
    presentationOptions: BoxPlotChartPresentationOptions = BoxPlotChartPresentationOptions(),
    onEntryClick: ((index: Int, entry: BoxPlotEntry) -> Unit)? = null,
) {
    val density = LocalDensity.current
    val densityPx = density.density
    val scaledDensity = density.fontScale * densityPx
    val progress = remember { Animatable(1f) }
    var selectedEntry by remember { mutableStateOf<RenderBoxPlotEntry?>(null) }

    LaunchedEffect(entries, presentationOptions.animateOnDataChange, presentationOptions.enterAnimationDurationMs, presentationOptions.enterAnimationDelayMs) {
        if (presentationOptions.animateOnDataChange && entries.isNotEmpty()) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = presentationOptions.enterAnimationDurationMs.toInt().coerceAtLeast(0),
                    delayMillis = presentationOptions.enterAnimationDelayMs.toInt().coerceAtLeast(0),
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
            computeBoxPlotLayout(
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
                .pointerInput(computed.entries, onEntryClick) {
                    detectTapGestures { tap ->
                        val hit = computed.entries.lastOrNull { entry ->
                            tap.x in entry.touchRect.left..entry.touchRect.right &&
                                tap.y in entry.touchRect.top..entry.touchRect.bottom
                        }
                        selectedEntry = hit
                        hit?.let {
                            onEntryClick?.invoke(it.entry.index, it.entry.sourceEntry)
                        }
                    }
                },
        ) {
            drawRect(styleOptions.backgroundColor.toComposeColor())

            if (computed.entries.isEmpty()) {
                drawContext.canvas.nativeCanvas.drawText(
                    presentationOptions.emptyText,
                    size.width * 0.5f,
                    size.height * 0.5f,
                    emptyTextPaint,
                )
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

            val boxRadius = dpToPx(styleOptions.boxCornerRadiusDp, densityPx)
            val outlierRadius = dpToPx(styleOptions.outlierRadiusDp, densityPx)
            val selectedPadding = dpToPx(styleOptions.selectedPaddingDp, densityPx)
            computed.entries.forEach { renderEntry ->
                drawLine(
                    color = styleOptions.axisColor.toComposeColor(),
                    start = Offset(renderEntry.centerX, renderEntry.maxY),
                    end = Offset(renderEntry.centerX, renderEntry.boxRect.top),
                    strokeWidth = dpToPx(styleOptions.whiskerStrokeWidthDp, densityPx),
                )
                drawLine(
                    color = styleOptions.axisColor.toComposeColor(),
                    start = Offset(renderEntry.centerX, renderEntry.boxRect.bottom),
                    end = Offset(renderEntry.centerX, renderEntry.minY),
                    strokeWidth = dpToPx(styleOptions.whiskerStrokeWidthDp, densityPx),
                )
                drawLine(
                    color = styleOptions.axisColor.toComposeColor(),
                    start = Offset(renderEntry.centerX - renderEntry.capHalfWidth, renderEntry.maxY),
                    end = Offset(renderEntry.centerX + renderEntry.capHalfWidth, renderEntry.maxY),
                    strokeWidth = dpToPx(styleOptions.whiskerStrokeWidthDp, densityPx),
                )
                drawLine(
                    color = styleOptions.axisColor.toComposeColor(),
                    start = Offset(renderEntry.centerX - renderEntry.capHalfWidth, renderEntry.minY),
                    end = Offset(renderEntry.centerX + renderEntry.capHalfWidth, renderEntry.minY),
                    strokeWidth = dpToPx(styleOptions.whiskerStrokeWidthDp, densityPx),
                )
                drawRoundRect(
                    color = renderEntry.entry.color.toComposeColor(),
                    topLeft = Offset(renderEntry.boxRect.left, renderEntry.boxRect.top),
                    size = Size(renderEntry.boxRect.width, renderEntry.boxRect.height),
                    cornerRadius = CornerRadius(boxRadius, boxRadius),
                )
                drawLine(
                    color = styleOptions.medianLineColor.toComposeColor(),
                    start = Offset(renderEntry.boxRect.left, renderEntry.medianY),
                    end = Offset(renderEntry.boxRect.right, renderEntry.medianY),
                    strokeWidth = dpToPx(styleOptions.whiskerStrokeWidthDp, densityPx),
                )
                renderEntry.outliers.forEach { outlier ->
                    drawCircle(
                        color = styleOptions.outlierColor.toComposeColor(),
                        center = outlier,
                        radius = outlierRadius,
                    )
                }
                if (selectedEntry == renderEntry) {
                    drawRoundRect(
                        color = styleOptions.selectedOutlineColor.toComposeColor(),
                        topLeft = Offset(
                            renderEntry.boxRect.left - selectedPadding,
                            min(renderEntry.maxY, renderEntry.boxRect.top) - selectedPadding,
                        ),
                        size = Size(
                            renderEntry.boxRect.width + selectedPadding * 2f,
                            max(renderEntry.minY, renderEntry.boxRect.bottom) - min(renderEntry.maxY, renderEntry.boxRect.top) + selectedPadding * 2f,
                        ),
                        cornerRadius = CornerRadius(boxRadius, boxRadius),
                        style = Stroke(width = 2f * densityPx),
                    )
                }
            }

            drawContext.canvas.nativeCanvas.apply {
                computed.entries.forEach { renderEntry ->
                    drawText(
                        presentationOptions.xLabelFormatter(renderEntry.entry),
                        renderEntry.centerX,
                        chartRect.bottom + categoryLabelPaint.fontSpacing,
                        categoryLabelPaint,
                    )
                    if (presentationOptions.showValueLabels) {
                        drawText(
                            presentationOptions.valueLabelFormatter(renderEntry.entry),
                            renderEntry.centerX,
                            min(renderEntry.maxY, renderEntry.boxRect.top) - 6f * densityPx,
                            valueLabelPaint,
                        )
                    }
                }
            }
        }
    }
}
