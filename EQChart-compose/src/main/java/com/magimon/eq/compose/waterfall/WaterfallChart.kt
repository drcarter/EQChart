package com.magimon.eq.compose.waterfall

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
import com.magimon.eq.waterfall.WaterfallChartPresentationOptions
import com.magimon.eq.waterfall.WaterfallChartStyleOptions
import com.magimon.eq.waterfall.WaterfallEntry
import com.magimon.eq.waterfall.WaterfallEntryKind
import com.magimon.eq.waterfall.WaterfallLayoutEntry
import com.magimon.eq.waterfall.resolveWaterfallChartLayout
import kotlin.math.max
import kotlin.math.min

internal data class RenderWaterfallBar(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val entry: WaterfallLayoutEntry,
)

internal data class RenderWaterfallConnector(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
)

internal data class ComputedWaterfallChart(
    val chartRect: Rect,
    val minValue: Double,
    val maxValue: Double,
    val baselineValue: Double,
    val ticks: List<Double>,
    val bars: List<RenderWaterfallBar>,
    val connectors: List<RenderWaterfallConnector>,
)

private fun dpToPx(value: Float, density: Float): Float = value * density

private fun valueToY(chartRect: Rect, value: Double, minValue: Double, maxValue: Double): Float {
    val ratio = if (maxValue == minValue) 0.5f else ((value - minValue) / (maxValue - minValue)).toFloat()
    return chartRect.bottom - ratio * chartRect.height
}

private fun animatedEndValue(
    entry: WaterfallLayoutEntry,
    presentation: WaterfallChartPresentationOptions,
    progress: Float,
): Double {
    if (!presentation.animationDirection) return entry.endValue
    return entry.startValue + (entry.endValue - entry.startValue) * progress.toDouble()
}

private fun connectorAnchorValue(
    entry: WaterfallLayoutEntry,
    presentation: WaterfallChartPresentationOptions,
    progress: Float,
): Double {
    return if (entry.kind == WaterfallEntryKind.DELTA) entry.startValue else animatedEndValue(entry, presentation, progress)
}

internal fun computeWaterfallLayout(
    widthPx: Float,
    heightPx: Float,
    entries: List<WaterfallEntry>,
    style: WaterfallChartStyleOptions,
    presentation: WaterfallChartPresentationOptions,
    progress: Float,
    density: Float,
    scaledDensity: Float,
): ComputedWaterfallChart {
    val resolved = resolveWaterfallChartLayout(
        entries = entries,
        style = style,
        tickCount = presentation.yTickCount,
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

    if (resolved.entries.isEmpty() || chartRect.width <= 0f || chartRect.height <= 0f) {
        return ComputedWaterfallChart(
            chartRect = chartRect,
            minValue = resolved.minValue,
            maxValue = resolved.maxValue,
            baselineValue = resolved.baselineValue,
            ticks = resolved.ticks,
            bars = emptyList(),
            connectors = emptyList(),
        )
    }

    val slotWidth = chartRect.width / resolved.entries.size.toFloat()
    val usableSlot = (slotWidth - dpToPx(style.categorySpacingDp, density)).coerceAtLeast(1f)
    val barWidth = (usableSlot * style.barWidthRatio.coerceIn(0.1f, 1f)).coerceAtLeast(1f)
    val bars = resolved.entries.mapIndexed { index, entry ->
        val left = chartRect.left + index * slotWidth + (slotWidth - barWidth) * 0.5f
        val right = left + barWidth
        val startY = valueToY(chartRect, entry.startValue, resolved.minValue, resolved.maxValue)
        val endY = valueToY(chartRect, animatedEndValue(entry, presentation, progress), resolved.minValue, resolved.maxValue)
        RenderWaterfallBar(
            left = left,
            top = min(startY, endY),
            right = right,
            bottom = max(startY, endY),
            entry = entry,
        )
    }
    val connectors = if (presentation.showConnectorLines) {
        bars.zipWithNext { previous, current ->
            RenderWaterfallConnector(
                startX = previous.right,
                startY = valueToY(chartRect, animatedEndValue(previous.entry, presentation, progress), resolved.minValue, resolved.maxValue),
                endX = current.left,
                endY = valueToY(chartRect, connectorAnchorValue(current.entry, presentation, progress), resolved.minValue, resolved.maxValue),
            )
        }
    } else {
        emptyList()
    }

    return ComputedWaterfallChart(
        chartRect = chartRect,
        minValue = resolved.minValue,
        maxValue = resolved.maxValue,
        baselineValue = resolved.baselineValue,
        ticks = resolved.ticks,
        bars = bars,
        connectors = connectors,
    )
}

/**
 * Canvas-based waterfall chart composable.
 *
 * The chart renders ordered cumulative deltas with optional subtotal and total bars.
 *
 * @param entries Ordered waterfall entries
 * @param modifier Standard Compose modifier for layout and gesture handling
 * @param styleOptions Colors, spacing, radii, and typography used while drawing
 * @param presentationOptions Axis, connector, and animation behavior
 * @param onEntryClick Optional callback invoked as `(index, entry, cumulativeTotal)`
 */
@Composable
fun WaterfallChart(
    entries: List<WaterfallEntry>,
    modifier: Modifier = Modifier,
    styleOptions: WaterfallChartStyleOptions = WaterfallChartStyleOptions(),
    presentationOptions: WaterfallChartPresentationOptions = WaterfallChartPresentationOptions(),
    onEntryClick: ((index: Int, entry: WaterfallEntry, cumulativeTotal: Double) -> Unit)? = null,
) {
    val density = LocalDensity.current
    val densityPx = density.density
    val scaledDensity = density.fontScale * densityPx
    val progress = remember { Animatable(1f) }
    var selectedBar by remember { mutableStateOf<RenderWaterfallBar?>(null) }
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
            widthPx,
            heightPx,
            current,
            densityPx,
            scaledDensity,
            presentationOptions,
        ) {
            computeWaterfallLayout(
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
                .pointerInput(computed.bars, onEntryClick) {
                    detectTapGestures { tap ->
                        val hit = computed.bars.lastOrNull { bar ->
                            tap.x in bar.left..bar.right && tap.y in bar.top..bar.bottom
                        }
                        selectedBar = hit
                        hit?.let {
                            onEntryClick?.invoke(
                                it.entry.index,
                                WaterfallEntry(
                                    label = it.entry.label,
                                    value = it.entry.rawValue,
                                    color = it.entry.color,
                                    kind = it.entry.kind,
                                    payload = it.entry.payload,
                                ),
                                it.entry.endValue,
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

            computed.connectors.forEach { connector ->
                drawLine(
                    color = styleOptions.connectorColor.toComposeColor(),
                    start = Offset(connector.startX, connector.startY),
                    end = Offset(connector.endX, connector.endY),
                    strokeWidth = dpToPx(styleOptions.connectorStrokeWidthDp, densityPx),
                )
            }

            val barRadius = dpToPx(styleOptions.barCornerRadiusDp, densityPx)
            val selectionPadding = dpToPx(styleOptions.selectedBarPaddingDp, densityPx)
            computed.bars.forEach { bar ->
                drawRoundRect(
                    color = bar.entry.color.toComposeColor(),
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
                    val label = presentationOptions.valueLabelFormatter(bar.entry.displayValue)
                    val isPositive = bar.entry.endValue >= bar.entry.startValue
                    val labelY = if (isPositive) {
                        bar.top - 6f * densityPx
                    } else {
                        bar.bottom + valueLabelPaint.textSize + 2f * densityPx
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        (bar.left + bar.right) * 0.5f,
                        labelY,
                        valueLabelPaint,
                    )
                }
                drawContext.canvas.nativeCanvas.drawText(
                    bar.entry.label,
                    (bar.left + bar.right) * 0.5f,
                    chartRect.bottom + categoryLabelPaint.textSize + 6f * densityPx,
                    categoryLabelPaint,
                )
            }
        }
    }
}
