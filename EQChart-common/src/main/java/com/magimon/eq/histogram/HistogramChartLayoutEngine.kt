package com.magimon.eq.histogram

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Render-ready histogram bar description resolved from input bins.
 */
data class HistogramLayoutBin(
    val index: Int,
    val start: Double,
    val end: Double,
    val value: Double,
    val label: String,
    val color: Int,
    val sourceBin: HistogramBin,
    val payload: Any?,
)

/**
 * Shared numeric layout result for histogram charts.
 */
data class HistogramChartLayout(
    val bins: List<HistogramLayoutBin>,
    val minBinStart: Double,
    val maxBinEnd: Double,
    val minValue: Double,
    val maxValue: Double,
    val baselineValue: Double,
    val ticks: List<Double>,
)

/**
 * Resolves sanitized bins, axis range, and labels for histogram charts.
 */
fun resolveHistogramChartLayout(
    bins: List<HistogramBin>,
    style: HistogramChartStyleOptions = HistogramChartStyleOptions(),
    presentation: HistogramChartPresentationOptions = HistogramChartPresentationOptions(),
): HistogramChartLayout {
    val sanitizedSource = bins.filter { bin ->
        bin.start.isFinite() &&
            bin.end.isFinite() &&
            bin.value.isFinite() &&
            bin.end > bin.start &&
            (bin.label?.isNotBlank() != false)
    }

    if (sanitizedSource.isEmpty()) {
        val min = -1.0
        val max = 1.0
        return HistogramChartLayout(
            bins = emptyList(),
            minBinStart = 0.0,
            maxBinEnd = 1.0,
            minValue = min,
            maxValue = max,
            baselineValue = resolveHistogramBaseline(min, max),
            ticks = histogramAxisTicks(min, max, presentation.yTickCount),
        )
    }

    val layoutBins = sanitizedSource.mapIndexed { index, bin ->
        HistogramLayoutBin(
            index = index,
            start = bin.start,
            end = bin.end,
            value = bin.value,
            label = presentation.binLabelFormatter(bin),
            color = bin.color ?: style.barColor,
            sourceBin = bin,
            payload = bin.payload,
        )
    }

    val minBinStart = layoutBins.minOfOrNull { it.start } ?: 0.0
    val maxBinEnd = layoutBins.maxOfOrNull { it.end } ?: 1.0

    val rawMin = min(layoutBins.minOfOrNull { it.value } ?: -1.0, 0.0)
    val rawMax = max(layoutBins.maxOfOrNull { it.value } ?: 1.0, 0.0)
    val resolvedMin: Double
    val resolvedMax: Double
    if (!rawMin.isFinite() || !rawMax.isFinite() || abs(rawMax - rawMin) <= 1e-12) {
        resolvedMin = rawMin - 1.0
        resolvedMax = rawMax + 1.0
    } else {
        resolvedMin = rawMin
        resolvedMax = rawMax
    }

    return HistogramChartLayout(
        bins = layoutBins,
        minBinStart = minBinStart,
        maxBinEnd = maxBinEnd,
        minValue = resolvedMin,
        maxValue = resolvedMax,
        baselineValue = resolveHistogramBaseline(resolvedMin, resolvedMax),
        ticks = histogramAxisTicks(resolvedMin, resolvedMax, presentation.yTickCount),
    )
}

fun resolveHistogramBaseline(minValue: Double, maxValue: Double): Double {
    return if (minValue <= 0.0 && maxValue >= 0.0) 0.0 else minValue
}

fun histogramAxisTicks(minValue: Double, maxValue: Double, count: Int): List<Double> {
    val safeCount = max(2, count)
    if (safeCount == 2) return listOf(minValue, maxValue)
    return List(safeCount) { index ->
        minValue + (maxValue - minValue) * index.toDouble() / (safeCount - 1)
    }
}

internal fun formatHistogramBoundary(value: Double): String {
    return if (abs(value - value.toLong().toDouble()) <= 1e-9) {
        value.toLong().toString()
    } else {
        value.toString().trimEnd('0').trimEnd('.')
    }
}
