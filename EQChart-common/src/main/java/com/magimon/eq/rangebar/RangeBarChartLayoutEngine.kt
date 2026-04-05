package com.magimon.eq.rangebar

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Render-ready interval entry produced by [resolveRangeBarChartLayout].
 */
data class RangeBarLayoutEntry(
    val index: Int,
    val label: String,
    val title: String?,
    val rawStartValue: Double,
    val rawEndValue: Double,
    val startValue: Double,
    val endValue: Double,
    val duration: Double,
    val color: Int,
    val payload: Any?,
)

/**
 * Shared numeric layout for range bar and timeline charts.
 */
data class RangeBarChartLayout(
    val entries: List<RangeBarLayoutEntry>,
    val minValue: Double,
    val maxValue: Double,
    val ticks: List<Double>,
)

/**
 * Pixel layout configuration consumed by [resolveRangeBarChartPlacement].
 */
data class RangeBarChartLayoutConfig(
    val widthPx: Float,
    val heightPx: Float,
    val contentPaddingPx: Float,
    val rowSpacingPx: Float,
    val minBarWidthPx: Float,
    val rowLabelReservedWidthPx: Float,
    val xAxisLabelReservedHeightPx: Float,
    val barHeightRatio: Float = 0.58f,
)

/**
 * One formatted X-axis tick positioned in pixel space.
 */
data class RangeBarAxisTick(
    val value: Double,
    val xPx: Float,
    val label: String,
)

/**
 * One formatted row label positioned in pixel space.
 */
data class RangeBarRowLabel(
    val index: Int,
    val label: String,
    val centerYPx: Float,
)

/**
 * One interval bar placed inside the plot area.
 */
data class RangeBarPlacedEntry(
    val entry: RangeBarLayoutEntry,
    val leftPx: Float,
    val topPx: Float,
    val rightPx: Float,
    val bottomPx: Float,
    val labelText: String,
) {
    val centerXPx: Float
        get() = (leftPx + rightPx) / 2f

    val centerYPx: Float
        get() = (topPx + bottomPx) / 2f
}

/**
 * Shared pixel placement result for View and Compose renderers.
 */
data class RangeBarChartPlacement(
    val entries: List<RangeBarPlacedEntry>,
    val ticks: List<RangeBarAxisTick>,
    val rowLabels: List<RangeBarRowLabel>,
    val plotLeftPx: Float,
    val plotTopPx: Float,
    val plotRightPx: Float,
    val plotBottomPx: Float,
    val rowHeightPx: Float,
)

private fun emptyRangeBarPlacement(
    plotLeft: Float,
    plotTop: Float,
    plotRight: Float,
    plotBottom: Float,
): RangeBarChartPlacement {
    return RangeBarChartPlacement(
        entries = emptyList(),
        ticks = emptyList(),
        rowLabels = emptyList(),
        plotLeftPx = plotLeft,
        plotTopPx = plotTop,
        plotRightPx = plotRight,
        plotBottomPx = plotBottom,
        rowHeightPx = 0f,
    )
}

/**
 * Resolves sanitized entries and the shared numeric axis for range bar charts.
 */
fun resolveRangeBarChartLayout(
    entries: List<RangeBarEntry>,
    style: RangeBarChartStyleOptions = RangeBarChartStyleOptions(),
    tickCount: Int = 6,
): RangeBarChartLayout {
    val sanitized = entries.mapIndexedNotNull { index, entry ->
        if (entry.label.isBlank()) return@mapIndexedNotNull null
        if (!entry.start.isFinite()) return@mapIndexedNotNull null
        if (!entry.end.isFinite()) return@mapIndexedNotNull null

        val startValue = min(entry.start, entry.end)
        val endValue = max(entry.start, entry.end)
        RangeBarLayoutEntry(
            index = index,
            label = entry.label,
            title = entry.title?.takeIf { it.isNotBlank() },
            rawStartValue = entry.start,
            rawEndValue = entry.end,
            startValue = startValue,
            endValue = endValue,
            duration = endValue - startValue,
            color = entry.color ?: style.defaultBarColor,
            payload = entry.payload,
        )
    }

    if (sanitized.isEmpty()) {
        return RangeBarChartLayout(
            entries = emptyList(),
            minValue = -1.0,
            maxValue = 1.0,
            ticks = rangeBarAxisTicks(-1.0, 1.0, tickCount),
        )
    }

    var rawMin = sanitized.first().startValue
    var rawMax = sanitized.first().endValue
    for (entry in sanitized) {
        if (entry.startValue < rawMin) rawMin = entry.startValue
        if (entry.endValue > rawMax) rawMax = entry.endValue
    }
    val minValue: Double
    val maxValue: Double
    if (abs(rawMax - rawMin) <= 1e-12) {
        minValue = rawMin - 1.0
        maxValue = rawMax + 1.0
    } else {
        minValue = rawMin
        maxValue = rawMax
    }

    return RangeBarChartLayout(
        entries = sanitized,
        minValue = minValue,
        maxValue = maxValue,
        ticks = rangeBarAxisTicks(minValue, maxValue, tickCount),
    )
}

/**
 * Resolves pixel placement for a numeric [RangeBarChartLayout].
 */
fun resolveRangeBarChartPlacement(
    layout: RangeBarChartLayout,
    config: RangeBarChartLayoutConfig,
    presentation: RangeBarChartPresentationOptions = RangeBarChartPresentationOptions(),
): RangeBarChartPlacement {
    val plotLeft = config.contentPaddingPx + config.rowLabelReservedWidthPx
    val plotTop = config.contentPaddingPx
    val plotRight = config.widthPx - config.contentPaddingPx
    val plotBottom = config.heightPx - config.contentPaddingPx - config.xAxisLabelReservedHeightPx
    val plotWidth = plotRight - plotLeft
    val plotHeight = plotBottom - plotTop

    if (layout.entries.isEmpty()) return emptyRangeBarPlacement(
        plotLeft = plotLeft,
        plotTop = plotTop,
        plotRight = max(plotLeft, plotRight),
        plotBottom = max(plotTop, plotBottom),
    )
    if (plotWidth <= 0f) return emptyRangeBarPlacement(
        plotLeft = plotLeft,
        plotTop = plotTop,
        plotRight = max(plotLeft, plotRight),
        plotBottom = max(plotTop, plotBottom),
    )
    if (plotHeight <= 0f) return emptyRangeBarPlacement(
        plotLeft = plotLeft,
        plotTop = plotTop,
        plotRight = max(plotLeft, plotRight),
        plotBottom = max(plotTop, plotBottom),
    )

    val rowCount = layout.entries.size
    val safeRowSpacing = max(0f, config.rowSpacingPx)
    val availableHeight = max(0f, plotHeight - safeRowSpacing * (rowCount - 1))
    val rowHeight = availableHeight / rowCount
    val barHeight = rowHeight * config.barHeightRatio.coerceIn(0.2f, 1f)

    fun mapValueToX(value: Double): Float {
        val ratio = if (abs(layout.maxValue - layout.minValue) <= 1e-12) {
            0f
        } else {
            ((value - layout.minValue) / (layout.maxValue - layout.minValue)).toFloat().coerceIn(0f, 1f)
        }
        return plotLeft + plotWidth * ratio
    }

    val placedEntries = layout.entries.mapIndexed { index, entry ->
        val rowTop = plotTop + index * (rowHeight + safeRowSpacing)
        val top = rowTop + (rowHeight - barHeight) / 2f
        val bottom = top + barHeight
        val mappedStart = mapValueToX(entry.startValue)
        val mappedEnd = mapValueToX(entry.endValue)
        var left = min(mappedStart, mappedEnd)
        var right = max(mappedStart, mappedEnd)
        if (right - left < config.minBarWidthPx) {
            if (left + config.minBarWidthPx <= plotRight) {
                right = left + config.minBarWidthPx
            } else {
                right = plotRight
                left = max(plotLeft, plotRight - config.minBarWidthPx)
            }
        }
        RangeBarPlacedEntry(
            entry = entry,
            leftPx = left,
            topPx = top,
            rightPx = right,
            bottomPx = bottom,
            labelText = presentation.barLabelFormatter(entry),
        )
    }

    val ticks = layout.ticks.map { value ->
        RangeBarAxisTick(
            value = value,
            xPx = mapValueToX(value),
            label = presentation.xLabelFormatter(value),
        )
    }

    val rowLabels = layout.entries.mapIndexed { index, entry ->
        RangeBarRowLabel(
            index = index,
            label = presentation.rowLabelFormatter(entry.label),
            centerYPx = placedEntries[index].centerYPx,
        )
    }

    return RangeBarChartPlacement(
        entries = placedEntries,
        ticks = ticks,
        rowLabels = rowLabels,
        plotLeftPx = plotLeft,
        plotTopPx = plotTop,
        plotRightPx = plotRight,
        plotBottomPx = plotBottom,
        rowHeightPx = rowHeight,
    )
}

/**
 * Returns the topmost placed interval that contains the provided point.
 */
fun hitTestRangeBarEntry(
    placement: RangeBarChartPlacement,
    xPx: Float,
    yPx: Float,
): RangeBarPlacedEntry? {
    for (index in placement.entries.indices.reversed()) {
        val entry = placement.entries[index]
        if (xPx < entry.leftPx || xPx > entry.rightPx) continue
        if (yPx < entry.topPx || yPx > entry.bottomPx) continue
        return entry
    }
    return null
}

/**
 * Produces evenly spaced X-axis ticks for the resolved range.
 */
fun rangeBarAxisTicks(minValue: Double, maxValue: Double, count: Int): List<Double> {
    val safeCount = max(2, count)
    if (safeCount == 2) return listOf(minValue, maxValue)
    return List(safeCount) { index ->
        minValue + (maxValue - minValue) * index.toDouble() / (safeCount - 1).toDouble()
    }
}

/**
 * Default X-axis formatter used by [RangeBarChartPresentationOptions].
 */
fun formatRangeBarAxisValue(value: Double): String {
    if (!value.isFinite()) return "0"
    val absolute = abs(value)
    return when {
        absolute >= 1_000_000.0 -> "${trimRangeBarAxisValue(value / 1_000_000.0)}M"
        absolute >= 1_000.0 -> "${trimRangeBarAxisValue(value / 1_000.0)}K"
        abs(value - value.toInt().toDouble()) <= 1e-9 -> value.toInt().toString()
        else -> trimRangeBarAxisValue(value)
    }
}

private fun trimRangeBarAxisValue(value: Double): String {
    val rounded = ((value * 10.0).toInt()) / 10.0
    return if (abs(rounded - rounded.toInt().toDouble()) <= 1e-9) {
        rounded.toInt().toString()
    } else {
        rounded.toString()
    }
}
