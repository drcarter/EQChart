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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import com.magimon.eq.bar.BarChartPresentationOptions
import com.magimon.eq.bar.BarChartStyleOptions
import com.magimon.eq.bar.BarDatum
import com.magimon.eq.bar.BarLayoutMode
import com.magimon.eq.bar.BarOrientation
import com.magimon.eq.bar.BarSeries
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

private data class RenderBar(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val seriesIndex: Int,
    val categoryIndex: Int,
    val value: Double,
    val payload: Any?,
)

private data class ComputedBarChart(
    val chartRect: Rect,
    val categories: List<String>,
    val minValue: Double,
    val maxValue: Double,
    val baselineValue: Double,
    val ticks: List<Double>,
    val bars: List<RenderBar>,
    val legendTop: Float,
)

private fun dpToPx(value: Float, density: Float): Float = value * density

internal fun resolveBarValueRange(values: Collection<Double>): Pair<Double, Double> {
    if (values.isEmpty()) return -1.0 to 1.0

    val rawMin = values.minOrNull() ?: -1.0
    val rawMax = values.maxOrNull() ?: 1.0
    val min = min(rawMin, 0.0)
    val max = max(rawMax, 0.0)

    return if (!min.isFinite() || !max.isFinite() || abs(max - min) <= 1e-12) {
        (min - 1.0) to (max + 1.0)
    } else {
        min to max
    }
}

private fun baselineValue(min: Double, max: Double): Double = if (min <= 0.0 && max >= 0.0) 0.0 else min

private fun valueToY(chartRect: Rect, value: Double, min: Double, max: Double): Float {
    val ratio = if (max == min) 0.5f else ((value - min) / (max - min)).toFloat()
    return chartRect.bottom - ratio * chartRect.height
}

private fun valueToX(chartRect: Rect, value: Double, min: Double, max: Double): Float {
    val ratio = if (max == min) 0.5f else ((value - min) / (max - min)).toFloat()
    return chartRect.left + ratio * chartRect.width
}

private fun axisTicks(min: Double, max: Double, count: Int): List<Double> {
    val safe = max(2, count)
    if (safe == 2) return listOf(min, max)
    return List(safe) { index ->
        min + (max - min) * index.toDouble() / (safe - 1)
    }
}

private fun valueFor(series: List<BarSeries>, seriesIndex: Int, category: String): Double? {
    return series.getOrNull(seriesIndex)
        ?.points
        ?.firstOrNull { it.category == category }
        ?.value
}

private fun payloadFor(series: List<BarSeries>, seriesIndex: Int, category: String): Any? {
    return series.getOrNull(seriesIndex)
        ?.points
        ?.firstOrNull { it.category == category }
        ?.payload
}

private fun resolveLegendReserveHeight(
    widthPx: Float,
    contentPadding: Float,
    series: List<BarSeries>,
    style: BarChartStyleOptions,
    presentation: BarChartPresentationOptions,
    density: Float,
    scaledDensity: Float,
): Float {
    if (!presentation.showLegend || series.isEmpty()) return 0f

    val legendPaint = newTextPaint(
        color = style.legendTextColor,
        textSizePx = presentation.legendTextSizeSp * scaledDensity,
        align = Paint.Align.LEFT,
    )
    val markerSize = dpToPx(style.legendMarkerSizeDp, density)
    val markerTextGap = dpToPx(style.legendMarkerTextGapDp, density)
    val itemSpacing = dpToPx(style.legendItemSpacingDp, density)
    val rowSpacing = dpToPx(style.legendRowSpacingDp, density)
    val startX = contentPadding + 4f * density
    val maxX = widthPx - contentPadding
    val textHeight = legendPaint.fontMetrics.bottom - legendPaint.fontMetrics.top
    val rowHeight = max(markerSize, textHeight)

    var rows = 1
    var x = startX
    series.forEach { item ->
        val itemWidth = markerSize + markerTextGap + legendPaint.measureText(item.name)
        if (x > startX && x + itemWidth > maxX) {
            rows += 1
            x = startX
        }
        x += itemWidth + itemSpacing
    }

    return 8f * density + rows * rowHeight + (rows - 1) * rowSpacing + 4f * density
}

private fun buildBars(
    series: List<BarSeries>,
    categories: List<String>,
    style: BarChartStyleOptions,
    presentation: BarChartPresentationOptions,
    styleMin: Double,
    styleMax: Double,
    progress: Float,
    chartRect: Rect,
    density: Float,
): List<RenderBar> {
    if (categories.isEmpty() || chartRect.width <= 0f || chartRect.height <= 0f) return emptyList()
    if (series.isEmpty()) return emptyList()

    val bars = mutableListOf<RenderBar>()
    val animatedProgress = if (presentation.animationDirection) progress else 1f
    val baseline = baselineValue(styleMin, styleMax)
    val innerGap = if (categories.size > 0) chartRect.width / categories.size.toFloat() else 0f

    when (presentation.orientation) {
        BarOrientation.VERTICAL -> {
            val slot = chartRect.width / categories.size.toFloat()
            val usableSlot = (slot - dpToPx(style.categorySpacingDp, density)).coerceAtLeast(1f)
            when (presentation.layoutMode) {
                BarLayoutMode.GROUPED -> {
                    val barGap = dpToPx(style.barSpacingDp, density)
                    val barWidth = max(1f, (usableSlot - barGap * max(0, series.size - 1)) / series.size.toFloat())
                    categories.forEachIndexed { categoryIndex, category ->
                        val leftBase = chartRect.left + categoryIndex * slot + dpToPx(style.categorySpacingDp, density) * 0.5f
                        val baselineY = valueToY(chartRect, baseline, styleMin, styleMax)
                        for (seriesIndex in series.indices) {
                            val value = valueFor(series, seriesIndex, category) ?: continue
                            val scaled = value * animatedProgress
                            val valueY = valueToY(chartRect, scaled, styleMin, styleMax)
                            bars.add(
                                RenderBar(
                                    left = leftBase + seriesIndex * (barWidth + barGap),
                                    top = min(valueY, baselineY),
                                    right = leftBase + seriesIndex * (barWidth + barGap) + barWidth,
                                    bottom = max(valueY, baselineY),
                                    seriesIndex = seriesIndex,
                                    categoryIndex = categoryIndex,
                                    value = value,
                                    payload = payloadFor(series, seriesIndex, category),
                                ),
                            )
                        }
                    }
                }
                BarLayoutMode.STACKED -> {
                    categories.forEachIndexed { categoryIndex, category ->
                        val left = chartRect.left + categoryIndex * innerGap + dpToPx(style.categorySpacingDp, density) * 0.5f
                        val right = left + usableSlot
                        var positiveBase = baseline
                        var negativeBase = baseline
                        for (seriesIndex in series.indices) {
                            val value = valueFor(series, seriesIndex, category) ?: continue
                            val scaled = value * animatedProgress
                            val from = if (scaled >= 0.0) positiveBase else negativeBase
                            val to = if (scaled >= 0.0) positiveBase + scaled else negativeBase + scaled
                            val fromY = valueToY(chartRect, from, styleMin, styleMax)
                            val toY = valueToY(chartRect, to, styleMin, styleMax)
                            bars.add(
                                RenderBar(
                                    left = left,
                                    top = min(fromY, toY),
                                    right = right,
                                    bottom = max(fromY, toY),
                                    seriesIndex = seriesIndex,
                                    categoryIndex = categoryIndex,
                                    value = value,
                                    payload = payloadFor(series, seriesIndex, category),
                                ),
                            )
                            if (scaled >= 0.0) {
                                positiveBase = to
                            } else {
                                negativeBase = to
                            }
                        }
                    }
                }
            }
        }
        BarOrientation.HORIZONTAL -> {
            val slot = chartRect.height / categories.size.toFloat()
            val usableSlot = (slot - dpToPx(style.categorySpacingDp, density)).coerceAtLeast(1f)
            when (presentation.layoutMode) {
                BarLayoutMode.GROUPED -> {
                    val barGap = dpToPx(style.barSpacingDp, density)
                    val barHeight = max(1f, (usableSlot - barGap * max(0, series.size - 1)) / series.size.toFloat())
                    val baselineX = valueToX(chartRect, baseline, styleMin, styleMax)
                    categories.forEachIndexed { categoryIndex, category ->
                        val topBase = chartRect.top + categoryIndex * slot + dpToPx(style.categorySpacingDp, density) * 0.5f
                        for (seriesIndex in series.indices) {
                            val value = valueFor(series, seriesIndex, category) ?: continue
                            val scaled = value * animatedProgress
                            val valueX = valueToX(chartRect, scaled, styleMin, styleMax)
                            bars.add(
                                RenderBar(
                                    left = min(valueX, baselineX),
                                    top = topBase + seriesIndex * (barHeight + barGap),
                                    right = max(valueX, baselineX),
                                    bottom = topBase + seriesIndex * (barHeight + barGap) + barHeight,
                                    seriesIndex = seriesIndex,
                                    categoryIndex = categoryIndex,
                                    value = value,
                                    payload = payloadFor(series, seriesIndex, category),
                                ),
                            )
                        }
                    }
                }
                BarLayoutMode.STACKED -> {
                    val baselineX = valueToX(chartRect, baseline, styleMin, styleMax)
                    categories.forEachIndexed { categoryIndex, category ->
                        val topBase = chartRect.top + categoryIndex * slot + dpToPx(style.categorySpacingDp, density) * 0.5f
                        val bottomBase = topBase + usableSlot
                        var positiveBase = baseline
                        var negativeBase = baseline
                        for (seriesIndex in series.indices) {
                            val value = valueFor(series, seriesIndex, category) ?: continue
                            val scaled = value * animatedProgress
                            val from = if (scaled >= 0.0) positiveBase else negativeBase
                            val to = if (scaled >= 0.0) positiveBase + scaled else negativeBase + scaled
                            val fromX = valueToX(chartRect, from, styleMin, styleMax)
                            val toX = valueToX(chartRect, to, styleMin, styleMax)
                            bars.add(
                                RenderBar(
                                    left = min(fromX, toX),
                                    top = topBase,
                                    right = max(fromX, toX),
                                    bottom = bottomBase,
                                    seriesIndex = seriesIndex,
                                    categoryIndex = categoryIndex,
                                    value = value,
                                    payload = payloadFor(series, seriesIndex, category),
                                ),
                            )
                            if (scaled >= 0.0) {
                                positiveBase = to
                            } else {
                                negativeBase = to
                            }
                        }
                    }
                }
            }
        }
    }

    return bars
}

private fun computeBarLayout(
    widthPx: Float,
    heightPx: Float,
    series: List<BarSeries>,
    style: BarChartStyleOptions,
    presentation: BarChartPresentationOptions,
    progress: Float,
    density: Float,
    scaledDensity: Float,
): ComputedBarChart {
    val contentPadding = dpToPx(style.contentPaddingDp, density)
    val allValues = series.flatMap { item -> item.points.map { it.value } }
    val (minValue, maxValue) = resolveBarValueRange(allValues)
    val categories = linkedSetOf<String>()
    series.forEach { item ->
        item.points.forEach { point ->
            if (point.category.isNotBlank()) categories.add(point.category)
        }
    }.let {}

    val axisReserve = dpToPx(presentation.axisLabelTextSizeSp, density) + 16f * density
    val legendReserve = resolveLegendReserveHeight(
        widthPx = widthPx,
        contentPadding = contentPadding,
        series = series,
        style = style,
        presentation = presentation,
        density = density,
        scaledDensity = scaledDensity,
    )
    val chartRect = Rect(
        left = contentPadding,
        top = contentPadding,
        right = widthPx - contentPadding,
        bottom = heightPx - contentPadding - axisReserve - legendReserve,
    )

    if (chartRect.width <= 0f || chartRect.height <= 0f) {
        return ComputedBarChart(
            chartRect = chartRect,
            categories = categories.toList(),
            minValue = minValue,
            maxValue = maxValue,
            baselineValue = baselineValue(minValue, maxValue),
            ticks = axisTicks(minValue, maxValue, if (presentation.orientation == BarOrientation.VERTICAL) presentation.yTickCount else presentation.xTickCount),
            bars = emptyList(),
            legendTop = chartRect.bottom + axisReserve + 8f * density,
        )
    }

    val bars = buildBars(
        series = series,
        categories = categories.toList(),
        style = style,
        presentation = presentation,
        styleMin = minValue,
        styleMax = maxValue,
        progress = progress,
        chartRect = chartRect,
        density = density,
    )

    return ComputedBarChart(
        chartRect = chartRect,
        categories = categories.toList(),
        minValue = minValue,
        maxValue = maxValue,
        baselineValue = baselineValue(minValue, maxValue),
        ticks = axisTicks(minValue, maxValue, if (presentation.orientation == BarOrientation.VERTICAL) presentation.yTickCount else presentation.xTickCount),
        bars = bars,
        legendTop = chartRect.bottom + axisReserve + 8f * density,
    )
}

/**
 * Canvas-based bar/column chart composable.
 */
@Composable
fun BarChart(
    series: List<BarSeries>,
    modifier: Modifier = Modifier,
    styleOptions: BarChartStyleOptions = BarChartStyleOptions(),
    presentationOptions: BarChartPresentationOptions = BarChartPresentationOptions(),
    onBarClick: ((seriesIndex: Int, categoryIndex: Int, value: Double, payload: Any?) -> Unit)? = null,
) {
    val density = LocalDensity.current
    val densityPx = density.density
    val scaledDensity = density.fontScale * densityPx
    val progress = remember { Animatable(1f) }
    var selectedBar by remember { mutableStateOf<RenderBar?>(null) }
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
            widthPx,
            heightPx,
            current,
            densityPx,
            scaledDensity,
            presentationOptions.animationDirection,
            presentationOptions.layoutMode,
            presentationOptions.orientation,
            presentationOptions.showLegend,
            presentationOptions.legendTextSizeSp,
            presentationOptions.axisLabelTextSizeSp,
            presentationOptions.xTickCount,
            presentationOptions.yTickCount,
        ) {
            computeBarLayout(
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

        val categoryLabelPx = presentationOptions.axisLabelTextSizeSp * scaledDensity
        val axisLabelPx = presentationOptions.axisLabelTextSizeSp * scaledDensity
        val valueLabelPx = presentationOptions.axisLabelTextSizeSp * scaledDensity
        val legendTextPx = presentationOptions.legendTextSizeSp * scaledDensity

        val isVertical = presentationOptions.orientation == BarOrientation.VERTICAL
        val categoryLabelPaint = newTextPaint(
            color = styleOptions.axisLabelColor,
            textSizePx = categoryLabelPx,
            align = if (isVertical) Paint.Align.CENTER else Paint.Align.RIGHT,
        )
        val axisLabelPaint = newTextPaint(
            color = styleOptions.axisLabelColor,
            textSizePx = axisLabelPx,
            align = Paint.Align.RIGHT,
        )
        val gridPaint = newTextPaint(
            color = styleOptions.gridColor,
            textSizePx = categoryLabelPx,
            align = Paint.Align.RIGHT,
        )
        val valueLabelPaint = newTextPaint(
            color = styleOptions.barValueTextColor,
            textSizePx = valueLabelPx,
        )
        val legendPaint = newTextPaint(
            color = styleOptions.legendTextColor,
            textSizePx = legendTextPx,
            align = Paint.Align.LEFT,
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(computed.bars, series, onBarClick) {
                    detectTapGestures { tap ->
                        val hit = computed.bars.firstOrNull { bar ->
                            tap.x in bar.left..bar.right && tap.y in bar.top..bar.bottom
                        }
                        selectedBar = hit
                        hit?.let {
                            val payload = it.payload ?: series.getOrNull(it.seriesIndex)?.payload
                            onBarClick?.invoke(it.seriesIndex, it.categoryIndex, it.value, payload)
                        }
                    }
                },
        ) {
            drawRect(styleOptions.backgroundColor.toComposeColor())

            if (computed.bars.isEmpty() && computed.categories.isEmpty()) {
                if (presentationOptions.emptyText.isNotBlank()) {
                    drawContext.canvas.nativeCanvas.drawText(
                        presentationOptions.emptyText,
                        size.width * 0.5f,
                        size.height * 0.5f,
                        newTextPaint(
                            color = styleOptions.axisLabelColor,
                            textSizePx = categoryLabelPx,
                            align = Paint.Align.CENTER,
                        ),
                    )
                }
                return@Canvas
            }

            val chartRect = computed.chartRect
            val tickCount = if (isVertical) presentationOptions.yTickCount else presentationOptions.xTickCount
            val showGrid = presentationOptions.showGrid
            val barRadius = dpToPx(styleOptions.barCornerRadiusDp, densityPx)
            val baselineY = valueToY(chartRect, computed.baselineValue, computed.minValue, computed.maxValue)
            val baselineX = valueToX(chartRect, computed.baselineValue, computed.minValue, computed.maxValue)

            if (showGrid) {
                computed.ticks.forEach { tick ->
                    if (isVertical) {
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
                    } else {
                        val x = valueToX(chartRect, tick, computed.minValue, computed.maxValue)
                        drawLine(
                            color = styleOptions.gridColor.toComposeColor(),
                            start = Offset(x, chartRect.top),
                            end = Offset(x, chartRect.bottom),
                            strokeWidth = 1f * densityPx,
                        )
	                        drawContext.canvas.nativeCanvas.drawText(
	                            presentationOptions.yLabelFormatter(tick),
	                            x,
	                            chartRect.bottom + categoryLabelPx + 8f * densityPx,
	                            categoryLabelPaint,
	                        )
                    }
                }
                val centers = if (isVertical) {
                    List(computed.categories.size) { index ->
                        chartRect.left + (index + 0.5f) * (chartRect.width / computed.categories.size.toFloat())
                    }
                } else {
                    List(computed.categories.size) { index ->
                        chartRect.top + (index + 0.5f) * (chartRect.height / computed.categories.size.toFloat())
                    }
                }
                centers.forEach { center ->
                    if (isVertical) {
                        drawLine(
                            color = styleOptions.gridColor.toComposeColor(),
                            start = Offset(center, chartRect.top),
                            end = Offset(center, chartRect.bottom),
                            strokeWidth = 1f * densityPx,
                        )
                    } else {
                        drawLine(
                            color = styleOptions.gridColor.toComposeColor(),
                            start = Offset(chartRect.left, center),
                            end = Offset(chartRect.right, center),
                            strokeWidth = 1f * densityPx,
                        )
                    }
                }
            }

            if (presentationOptions.showAxes) {
                if (isVertical) {
                    drawLine(
                        color = styleOptions.axisColor.toComposeColor(),
                        start = Offset(chartRect.left, chartRect.top),
                        end = Offset(chartRect.left, chartRect.bottom),
                        strokeWidth = 1.1f * densityPx,
                    )
                    drawLine(
                        color = styleOptions.axisColor.toComposeColor(),
                        start = Offset(chartRect.left, chartRect.bottom),
                        end = Offset(chartRect.right, chartRect.bottom),
                        strokeWidth = 1.1f * densityPx,
                    )
                } else {
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
            }

            if (presentationOptions.showBars) {
                computed.bars.forEach { bar ->
                    val color = series.getOrNull(bar.seriesIndex)?.color ?: styleOptions.axisColor
                    drawRoundRect(
                        color = color.toComposeColor(),
                        topLeft = Offset(bar.left, bar.top),
                        size = androidx.compose.ui.geometry.Size(width = bar.right - bar.left, height = bar.bottom - bar.top),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(x = barRadius, y = barRadius),
                    )

                        if (selectedBar == bar) {
                            drawRoundRect(
                                color = androidx.compose.ui.graphics.Color.White,
                                topLeft = Offset(
                                    bar.left - dpToPx(styleOptions.selectedBarPaddingDp, densityPx),
                                    bar.top - dpToPx(styleOptions.selectedBarPaddingDp, densityPx),
                                ),
                                size = androidx.compose.ui.geometry.Size(
                                    (bar.right - bar.left) + dpToPx(styleOptions.selectedBarPaddingDp, densityPx) * 2f,
                                    (bar.bottom - bar.top) + dpToPx(styleOptions.selectedBarPaddingDp, densityPx) * 2f,
                                ),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(x = barRadius, y = barRadius),
                            style = Stroke(width = 2.1f),
                        )
                    }

                    if (presentationOptions.showBarLabels) {
                        val labelText = presentationOptions.yLabelFormatter(bar.value)
                        val labelX = (bar.left + bar.right) * 0.5f
                        if (isVertical) {
                            drawContext.canvas.nativeCanvas.drawText(
                                labelText,
                                labelX,
                                bar.top - 4f * densityPx,
                                valueLabelPaint,
                            )
                        } else {
                            drawContext.canvas.nativeCanvas.drawText(
                                labelText,
                                if (bar.value >= 0.0) bar.right + 4f * densityPx else bar.left - 4f * densityPx,
                                (bar.top + bar.bottom) * 0.5f + valueLabelPx * 0.35f,
                                valueLabelPaint.apply {
                                    textAlign = if (bar.value >= 0.0) Paint.Align.LEFT else Paint.Align.RIGHT
                                },
                            )
                        }
                    }
                }

                if (isVertical) {
                    computed.categories.forEachIndexed { index, category ->
                        val slot = chartRect.width / computed.categories.size.toFloat()
                        drawContext.canvas.nativeCanvas.drawText(
                            presentationOptions.xLabelFormatter(category),
                            chartRect.left + slot * (index + 0.5f),
                            chartRect.bottom + categoryLabelPx + 8f * densityPx,
                            categoryLabelPaint,
                        )
                    }
                } else {
                    computed.categories.forEachIndexed { index, category ->
                        val slot = chartRect.height / computed.categories.size.toFloat()
                        drawContext.canvas.nativeCanvas.drawText(
                            presentationOptions.xLabelFormatter(category),
                            chartRect.left - 6f * densityPx,
                            chartRect.top + slot * (index + 0.5f) + axisLabelPaint.textSize * 0.35f,
                            axisLabelPaint,
                        )
                    }
                }
            }

            if (presentationOptions.showLegend) {
                var x = chartRect.left + 4f * densityPx
                var rowTop = computed.legendTop
                val markerSize = dpToPx(styleOptions.legendMarkerSizeDp, densityPx)
                val markerTextGap = dpToPx(styleOptions.legendMarkerTextGapDp, densityPx)
                val itemSpacing = dpToPx(styleOptions.legendItemSpacingDp, densityPx)
                val rowSpacing = dpToPx(styleOptions.legendRowSpacingDp, densityPx)
                val textHeight = legendPaint.fontMetrics.bottom - legendPaint.fontMetrics.top
                val rowHeight = max(markerSize, textHeight)
                val baselineOffset = ((rowHeight - textHeight) * 0.5f) - legendPaint.fontMetrics.top

                for (item in series) {
                    val measure = legendPaint.measureText(item.name)
                    val itemWidth = markerSize + markerTextGap + measure
                    if (x > chartRect.left + 4f * densityPx && x + itemWidth > chartRect.right) {
                        x = chartRect.left + 4f * densityPx
                        rowTop += rowHeight + rowSpacing
                    }
                    val markerTop = rowTop + (rowHeight - markerSize) * 0.5f
                    drawRect(
                        color = item.color.toComposeColor(),
                        topLeft = Offset(x, markerTop),
                        size = androidx.compose.ui.geometry.Size(markerSize, markerSize),
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        item.name,
                        x + markerSize + markerTextGap,
                        rowTop + baselineOffset,
                        legendPaint,
                    )
                    x += itemWidth + itemSpacing
                }
            }
        }
    }
}
