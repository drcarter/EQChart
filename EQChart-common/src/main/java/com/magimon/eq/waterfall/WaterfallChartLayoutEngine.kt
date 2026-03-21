package com.magimon.eq.waterfall

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Render-ready waterfall bar description resolved from input entries.
 */
data class WaterfallLayoutEntry(
    val index: Int,
    val label: String,
    val kind: WaterfallEntryKind,
    val rawValue: Double,
    val displayValue: Double,
    val startValue: Double,
    val endValue: Double,
    val color: Int,
    val payload: Any?,
)

/**
 * Connector between two adjacent bars.
 */
data class WaterfallConnector(
    val fromIndex: Int,
    val toIndex: Int,
    val fromValue: Double,
    val toValue: Double,
)

/**
 * Shared numeric layout result for waterfall charts.
 */
data class WaterfallChartLayout(
    val entries: List<WaterfallLayoutEntry>,
    val connectors: List<WaterfallConnector>,
    val minValue: Double,
    val maxValue: Double,
    val baselineValue: Double,
    val ticks: List<Double>,
)

/**
 * Resolves sanitized entries, cumulative totals, axis range, and connectors for waterfall charts.
 */
fun resolveWaterfallChartLayout(
    entries: List<WaterfallEntry>,
    style: WaterfallChartStyleOptions = WaterfallChartStyleOptions(),
    tickCount: Int = 6,
): WaterfallChartLayout {
    val sanitized = entries.filter { entry ->
        entry.label.isNotBlank() && (entry.kind != WaterfallEntryKind.DELTA || entry.value.isFinite())
    }

    if (sanitized.isEmpty()) {
        val min = -1.0
        val max = 1.0
        return WaterfallChartLayout(
            entries = emptyList(),
            connectors = emptyList(),
            minValue = min,
            maxValue = max,
            baselineValue = resolveWaterfallBaseline(min, max),
            ticks = waterfallAxisTicks(min, max, tickCount),
        )
    }

    var runningTotal = 0.0
    val layoutEntries = mutableListOf<WaterfallLayoutEntry>()
    sanitized.forEachIndexed { index, entry ->
        val resolved = when (entry.kind) {
            WaterfallEntryKind.DELTA -> {
                val start = runningTotal
                val end = runningTotal + entry.value
                runningTotal = end
                WaterfallLayoutEntry(
                    index = index,
                    label = entry.label,
                    kind = entry.kind,
                    rawValue = entry.value,
                    displayValue = entry.value,
                    startValue = start,
                    endValue = end,
                    color = entry.color ?: if (entry.value >= 0.0) style.positiveBarColor else style.negativeBarColor,
                    payload = entry.payload,
                )
            }
            WaterfallEntryKind.SUBTOTAL -> WaterfallLayoutEntry(
                index = index,
                label = entry.label,
                kind = entry.kind,
                rawValue = entry.value,
                displayValue = runningTotal,
                startValue = 0.0,
                endValue = runningTotal,
                color = entry.color ?: style.subtotalBarColor,
                payload = entry.payload,
            )
            WaterfallEntryKind.TOTAL -> WaterfallLayoutEntry(
                index = index,
                label = entry.label,
                kind = entry.kind,
                rawValue = entry.value,
                displayValue = runningTotal,
                startValue = 0.0,
                endValue = runningTotal,
                color = entry.color ?: style.totalBarColor,
                payload = entry.payload,
            )
        }
        layoutEntries.add(resolved)
    }

    val values = buildList {
        add(0.0)
        layoutEntries.forEach { entry ->
            add(entry.startValue)
            add(entry.endValue)
        }
    }
    val rawMin = values.minOrNull() ?: -1.0
    val rawMax = values.maxOrNull() ?: 1.0
    val minValue = min(rawMin, 0.0)
    val maxValue = max(rawMax, 0.0)
    val resolvedMin: Double
    val resolvedMax: Double
    if (!minValue.isFinite() || !maxValue.isFinite() || abs(maxValue - minValue) <= 1e-12) {
        resolvedMin = minValue - 1.0
        resolvedMax = maxValue + 1.0
    } else {
        resolvedMin = minValue
        resolvedMax = maxValue
    }

    val connectors = layoutEntries.zipWithNext { previous, current ->
        val fromValue = previous.endValue
        val toValue = if (current.kind == WaterfallEntryKind.DELTA) current.startValue else current.endValue
        WaterfallConnector(
            fromIndex = previous.index,
            toIndex = current.index,
            fromValue = fromValue,
            toValue = toValue,
        )
    }

    return WaterfallChartLayout(
        entries = layoutEntries,
        connectors = connectors,
        minValue = resolvedMin,
        maxValue = resolvedMax,
        baselineValue = resolveWaterfallBaseline(resolvedMin, resolvedMax),
        ticks = waterfallAxisTicks(resolvedMin, resolvedMax, tickCount),
    )
}

fun resolveWaterfallBaseline(minValue: Double, maxValue: Double): Double {
    return if (minValue <= 0.0 && maxValue >= 0.0) 0.0 else minValue
}

fun waterfallAxisTicks(minValue: Double, maxValue: Double, count: Int): List<Double> {
    val safeCount = max(2, count)
    if (safeCount == 2) return listOf(minValue, maxValue)
    return List(safeCount) { index ->
        minValue + (maxValue - minValue) * index.toDouble() / (safeCount - 1)
    }
}
