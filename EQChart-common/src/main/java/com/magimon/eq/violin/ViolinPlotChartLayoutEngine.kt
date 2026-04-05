package com.magimon.eq.violin

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * One normalized violin width sample at a numeric value.
 */
data class ViolinPlotDensityPoint(
    val value: Double,
    val widthRatio: Float,
)

/**
 * Render-ready series resolved from raw violin plot samples.
 */
data class ViolinPlotLayoutSeries(
    val index: Int,
    val label: String,
    val sampleCount: Int,
    val min: Double,
    val q1: Double,
    val median: Double,
    val q3: Double,
    val max: Double,
    val densityPoints: List<ViolinPlotDensityPoint>,
    val color: Int,
    val payload: Any?,
    val sourceSeries: ViolinPlotSeries,
)

/**
 * Shared numeric layout for violin plot charts.
 */
data class ViolinPlotChartLayout(
    val series: List<ViolinPlotLayoutSeries>,
    val minValue: Double,
    val maxValue: Double,
    val ticks: List<Double>,
)

private const val EPSILON = 1e-12
private const val FALLBACK_HALF_RANGE = 0.5
private const val MIN_POINT_COUNT = 16
private const val SQRT_TWO_PI = 2.5066282746310002

/**
 * Resolves sanitized violin plot series and shared Y-axis ticks.
 */
fun resolveViolinPlotChartLayout(
    series: List<ViolinPlotSeries>,
    style: ViolinPlotChartStyleOptions = ViolinPlotChartStyleOptions(),
    presentation: ViolinPlotChartPresentationOptions = ViolinPlotChartPresentationOptions(),
): ViolinPlotChartLayout {
    val pointCount = max(MIN_POINT_COUNT, presentation.densityPointCount)
    val sanitized = series.mapIndexedNotNull { index, source ->
        val label = source.label.trim()
        if (label.isEmpty()) return@mapIndexedNotNull null
        val samples = source.samples.filter { it.isFinite() }.sorted()
        if (samples.isEmpty()) return@mapIndexedNotNull null

        val minSample = samples.first()
        val maxSample = samples.last()
        ViolinPlotLayoutSeries(
            index = index,
            label = label,
            sampleCount = samples.size,
            min = minSample,
            q1 = percentile(samples, 0.25),
            median = percentile(samples, 0.5),
            q3 = percentile(samples, 0.75),
            max = maxSample,
            densityPoints = buildDensityPoints(samples, pointCount),
            color = source.color ?: style.defaultViolinColor,
            payload = source.payload,
            sourceSeries = source,
        )
    }

    if (sanitized.isEmpty()) {
        return ViolinPlotChartLayout(
            series = emptyList(),
            minValue = -1.0,
            maxValue = 1.0,
            ticks = violinPlotAxisTicks(-1.0, 1.0, presentation.yTickCount),
        )
    }

    var minValue = sanitized.first().min
    var maxValue = sanitized.first().max
    sanitized.forEach { entry ->
        if (entry.min < minValue) minValue = entry.min
        if (entry.max > maxValue) maxValue = entry.max
    }
    if (abs(maxValue - minValue) <= EPSILON) {
        minValue -= 1.0
        maxValue += 1.0
    }

    return ViolinPlotChartLayout(
        series = sanitized,
        minValue = minValue,
        maxValue = maxValue,
        ticks = violinPlotAxisTicks(minValue, maxValue, presentation.yTickCount),
    )
}

/**
 * Produces evenly spaced Y-axis ticks for the resolved range.
 */
fun violinPlotAxisTicks(minValue: Double, maxValue: Double, count: Int): List<Double> {
    val safeCount = max(2, count)
    if (safeCount == 2) return listOf(minValue, maxValue)
    return List(safeCount) { index ->
        minValue + (maxValue - minValue) * index.toDouble() / (safeCount - 1).toDouble()
    }
}

/**
 * Default Y-axis formatter used by [ViolinPlotChartPresentationOptions].
 */
fun formatViolinPlotAxisValue(value: Double): String {
    if (!value.isFinite()) return "0"
    val absolute = abs(value)
    return when {
        absolute >= 1_000_000.0 -> "${trimViolinValue(value / 1_000_000.0)}M"
        absolute >= 1_000.0 -> "${trimViolinValue(value / 1_000.0)}K"
        abs(value - value.roundToLong().toDouble()) <= 1e-9 -> value.roundToLong().toString()
        else -> trimViolinValue(value)
    }
}

private fun buildDensityPoints(samples: List<Double>, pointCount: Int): List<ViolinPlotDensityPoint> {
    val minSample = samples.first()
    val maxSample = samples.last()
    val median = percentile(samples, 0.5)
    val localMin: Double
    val localMax: Double
    if (abs(maxSample - minSample) <= EPSILON) {
        localMin = median - FALLBACK_HALF_RANGE
        localMax = median + FALLBACK_HALF_RANGE
    } else {
        localMin = minSample
        localMax = maxSample
    }

    val bandwidth = silvermanBandwidth(samples)
    if (samples.size < 2 || bandwidth <= EPSILON) {
        return buildFallbackDensityPoints(localMin, localMax, pointCount)
    }

    val raw = MutableList(pointCount) { index ->
        val fraction = index.toDouble() / (pointCount - 1).toDouble()
        val value = localMin + (localMax - localMin) * fraction
        value to gaussianKernelDensity(value, samples, bandwidth)
    }
    val maxDensity = raw.maxOfOrNull { it.second } ?: 0.0
    if (maxDensity <= EPSILON) {
        return buildFallbackDensityPoints(localMin, localMax, pointCount)
    }

    return raw.map { (value, density) ->
        ViolinPlotDensityPoint(
            value = value,
            widthRatio = (density / maxDensity).toFloat().coerceIn(0f, 1f),
        )
    }
}

private fun buildFallbackDensityPoints(
    localMin: Double,
    localMax: Double,
    pointCount: Int,
): List<ViolinPlotDensityPoint> {
    return List(pointCount) { index ->
        val fraction = index.toDouble() / (pointCount - 1).toDouble()
        val value = localMin + (localMax - localMin) * fraction
        val amplitude = sin(PI * fraction).coerceAtLeast(0.0).toFloat()
        ViolinPlotDensityPoint(value = value, widthRatio = amplitude * 0.28f)
    }
}

private fun gaussianKernelDensity(value: Double, samples: List<Double>, bandwidth: Double): Double {
    var sum = 0.0
    samples.forEach { sample ->
        val u = (value - sample) / bandwidth
        sum += exp(-0.5 * u * u)
    }
    return sum / (samples.size.toDouble() * bandwidth * SQRT_TWO_PI)
}

private fun silvermanBandwidth(samples: List<Double>): Double {
    if (samples.size < 2) return 0.0
    val mean = samples.sum() / samples.size.toDouble()
    var squaredDeviation = 0.0
    samples.forEach { sample ->
        val delta = sample - mean
        squaredDeviation += delta * delta
    }
    val standardDeviation = sqrt(squaredDeviation / (samples.size - 1).toDouble())
    val q1 = percentile(samples, 0.25)
    val q3 = percentile(samples, 0.75)
    val iqrBased = (q3 - q1) / 1.34
    val scale = when {
        standardDeviation <= EPSILON -> iqrBased
        iqrBased <= EPSILON -> standardDeviation
        else -> min(standardDeviation, iqrBased)
    }
    if (scale <= EPSILON) return 0.0
    return 0.9 * scale * samples.size.toDouble().pow(-0.2)
}

private fun percentile(sortedValues: List<Double>, fraction: Double): Double {
    if (sortedValues.isEmpty()) return 0.0
    if (sortedValues.size == 1) return sortedValues.first()
    val clamped = fraction.coerceIn(0.0, 1.0)
    val position = clamped * (sortedValues.size - 1).toDouble()
    val lowerIndex = position.toInt()
    val upperIndex = min(sortedValues.lastIndex, lowerIndex + 1)
    if (lowerIndex == upperIndex) return sortedValues[lowerIndex]
    val weight = position - lowerIndex.toDouble()
    return sortedValues[lowerIndex] + (sortedValues[upperIndex] - sortedValues[lowerIndex]) * weight
}

private fun trimViolinValue(value: Double): String {
    val rounded = (value * 10.0).roundToLong() / 10.0
    return if (abs(rounded - rounded.roundToLong().toDouble()) <= 1e-9) {
        rounded.roundToLong().toString()
    } else {
        rounded.toString()
    }
}
