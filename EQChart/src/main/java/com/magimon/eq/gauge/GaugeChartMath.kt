package com.magimon.eq.gauge

import kotlin.math.cos
import kotlin.math.sin

/**
 * Shared normalization and geometry helpers for gauge rendering.
 */
internal object GaugeChartMath {

    /**
     * Simple 2D point used while resolving gauge geometry.
     */
    internal data class Vec2(
        val x: Float,
        val y: Float,
    )

    /**
     * Validated gauge value ready for rendering.
     */
    internal data class ResolvedGaugeValue(
        val rawValue: Double,
        val clampedValue: Double,
        val minValue: Double,
        val maxValue: Double,
        val progress: Float,
        val label: String?,
        val payload: Any?,
    )

    /**
     * Validated range band ready for rendering on the gauge arc.
     */
    internal data class ResolvedGaugeRange(
        val startRatio: Float,
        val endRatio: Float,
        val color: Int,
        val label: String?,
    )

    /**
     * Validates and normalizes a single [GaugeValue].
     */
    internal fun resolveValue(value: GaugeValue?): ResolvedGaugeValue? {
        value ?: return null
        if (!value.value.isFinite() || !value.minValue.isFinite() || !value.maxValue.isFinite()) return null
        if (value.maxValue <= value.minValue) return null

        val clamped = value.value.coerceIn(value.minValue, value.maxValue)
        return ResolvedGaugeValue(
            rawValue = value.value,
            clampedValue = clamped,
            minValue = value.minValue,
            maxValue = value.maxValue,
            progress = normalize(clamped, value.minValue, value.maxValue),
            label = value.label,
            payload = value.payload,
        )
    }

    /**
     * Validates and clamps range bands into the current gauge domain.
     */
    internal fun resolveRanges(
        ranges: List<GaugeRange>,
        minValue: Double,
        maxValue: Double,
    ): List<ResolvedGaugeRange> {
        if (!minValue.isFinite() || !maxValue.isFinite() || maxValue <= minValue) return emptyList()
        return ranges.mapNotNull { range ->
            if (!range.startValue.isFinite() || !range.endValue.isFinite()) return@mapNotNull null
            val start = range.startValue.coerceIn(minValue, maxValue)
            val end = range.endValue.coerceIn(minValue, maxValue)
            if (end <= start) return@mapNotNull null
            ResolvedGaugeRange(
                startRatio = normalize(start, minValue, maxValue),
                endRatio = normalize(end, minValue, maxValue),
                color = range.color,
                label = range.label,
            )
        }
    }

    /**
     * Converts a numeric value into normalized progress within the gauge domain.
     */
    internal fun normalize(value: Double, minValue: Double, maxValue: Double): Float {
        if (!value.isFinite() || !minValue.isFinite() || !maxValue.isFinite()) return 0f
        val span = maxValue - minValue
        if (span <= 0.0) return 0f
        return ((value - minValue) / span).toFloat().coerceIn(0f, 1f)
    }

    /**
     * Converts normalized gauge progress into an absolute angle.
     */
    internal fun valueToAngle(ratio: Float, startAngleDeg: Float, sweepAngleDeg: Float): Float {
        return startAngleDeg + (sweepAngleDeg * ratio.coerceIn(0f, 1f))
    }

    /**
     * Resolves a point on a circle for the supplied center, radius, and angle.
     */
    internal fun pointOnCircle(
        centerX: Float,
        centerY: Float,
        radius: Float,
        angleDeg: Float,
    ): Vec2 {
        val radians = Math.toRadians(angleDeg.toDouble())
        return Vec2(
            x = centerX + (cos(radians) * radius).toFloat(),
            y = centerY + (sin(radians) * radius).toFloat(),
        )
    }
}
