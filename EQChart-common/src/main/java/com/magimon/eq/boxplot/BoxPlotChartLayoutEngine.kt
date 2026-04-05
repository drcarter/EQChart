package com.magimon.eq.boxplot

import kotlin.math.abs
import kotlin.math.max

/**
 * Render-ready box plot entry produced by [resolveBoxPlotChartLayout].
 */
data class BoxPlotLayoutEntry(
    val index: Int,
    val label: String,
    val title: String?,
    val min: Double,
    val q1: Double,
    val median: Double,
    val q3: Double,
    val max: Double,
    val outliers: List<Double>,
    val color: Int,
    val payload: Any?,
    val sourceEntry: BoxPlotEntry,
)

/**
 * Shared numeric layout for box plot charts.
 */
data class BoxPlotChartLayout(
    val entries: List<BoxPlotLayoutEntry>,
    val minValue: Double,
    val maxValue: Double,
    val ticks: List<Double>,
)

/**
 * Resolves sanitized entries and shared Y-axis ticks for box plot charts.
 */
fun resolveBoxPlotChartLayout(
    entries: List<BoxPlotEntry>,
    style: BoxPlotChartStyleOptions = BoxPlotChartStyleOptions(),
    presentation: BoxPlotChartPresentationOptions = BoxPlotChartPresentationOptions(),
): BoxPlotChartLayout {
    val sanitized = entries.mapIndexedNotNull { index, entry ->
        if (entry.label.isBlank()) return@mapIndexedNotNull null
        val values = listOf(entry.min, entry.q1, entry.median, entry.q3, entry.max)
        if (values.any { !it.isFinite() }) return@mapIndexedNotNull null

        val sorted = values.sorted()
        val outliers = entry.outliers.filter { it.isFinite() }
        BoxPlotLayoutEntry(
            index = index,
            label = entry.label,
            title = entry.title?.takeIf { it.isNotBlank() },
            min = sorted[0],
            q1 = sorted[1],
            median = sorted[2],
            q3 = sorted[3],
            max = sorted[4],
            outliers = outliers,
            color = entry.color ?: style.defaultBoxColor,
            payload = entry.payload,
            sourceEntry = entry,
        )
    }

    if (sanitized.isEmpty()) {
        return BoxPlotChartLayout(
            entries = emptyList(),
            minValue = -1.0,
            maxValue = 1.0,
            ticks = boxPlotAxisTicks(-1.0, 1.0, presentation.yTickCount),
        )
    }

    var minValue = sanitized.first().min
    var maxValue = sanitized.first().max
    sanitized.forEach { entry ->
        if (entry.min < minValue) minValue = entry.min
        if (entry.max > maxValue) maxValue = entry.max
        entry.outliers.forEach { outlier ->
            if (outlier < minValue) minValue = outlier
            if (outlier > maxValue) maxValue = outlier
        }
    }

    if (abs(maxValue - minValue) <= 1e-12) {
        minValue -= 1.0
        maxValue += 1.0
    }

    return BoxPlotChartLayout(
        entries = sanitized,
        minValue = minValue,
        maxValue = maxValue,
        ticks = boxPlotAxisTicks(minValue, maxValue, presentation.yTickCount),
    )
}

/**
 * Produces evenly spaced Y-axis ticks for the resolved range.
 */
fun boxPlotAxisTicks(minValue: Double, maxValue: Double, count: Int): List<Double> {
    val safeCount = max(2, count)
    if (safeCount == 2) return listOf(minValue, maxValue)
    return List(safeCount) { index ->
        minValue + (maxValue - minValue) * index.toDouble() / (safeCount - 1).toDouble()
    }
}

/**
 * Default Y-axis formatter used by [BoxPlotChartPresentationOptions].
 */
fun formatBoxPlotAxisValue(value: Double): String {
    if (!value.isFinite()) return "0"
    val absolute = abs(value)
    return when {
        absolute >= 1_000_000.0 -> "${trimBoxPlotAxisValue(value / 1_000_000.0)}M"
        absolute >= 1_000.0 -> "${trimBoxPlotAxisValue(value / 1_000.0)}K"
        abs(value - value.toInt().toDouble()) <= 1e-9 -> value.toInt().toString()
        else -> trimBoxPlotAxisValue(value)
    }
}

private fun trimBoxPlotAxisValue(value: Double): String {
    val rounded = ((value * 10.0).toInt()) / 10.0
    return if (abs(rounded - rounded.toInt().toDouble()) <= 1e-9) {
        rounded.toInt().toString()
    } else {
        rounded.toString()
    }
}
