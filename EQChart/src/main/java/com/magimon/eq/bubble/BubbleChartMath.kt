package com.magimon.eq.bubble

import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.sqrt

internal object BubbleChartMath {

    data class NumericRange(
        val min: Double,
        val max: Double,
    ) {
        val span: Double
            get() = max - min
    }

    fun resolveRange(
        values: List<Double>,
        overrideMin: Double?,
        overrideMax: Double?,
        paddingRatio: Double = 0.05,
    ): NumericRange {
        val baseMin = values.minOrNull() ?: 0.0
        val baseMax = values.maxOrNull() ?: 1.0

        var min = overrideMin ?: baseMin
        var max = overrideMax ?: baseMax

        if (min > max) {
            val temp = min
            min = max
            max = temp
        }

        if (min == max) {
            val pad = maxOf(1.0, abs(min) * paddingRatio)
            min -= pad
            max += pad
        }

        return NumericRange(min = min, max = max)
    }

    fun normalize(value: Double, range: NumericRange): Double {
        if (range.span <= 0.0) return 0.5
        return ((value - range.min) / range.span).coerceIn(0.0, 1.0)
    }

    fun mapX(value: Double, range: NumericRange, plotRect: RectF): Float {
        return mapLinear(value, range, plotRect.left, plotRect.right)
    }

    fun mapY(value: Double, range: NumericRange, plotRect: RectF): Float {
        return mapLinearInverted(value, range, plotRect.top, plotRect.bottom)
    }

    fun mapLinear(value: Double, range: NumericRange, outMin: Float, outMax: Float): Float {
        val t = normalize(value, range).toFloat()
        return outMin + (outMax - outMin) * t
    }

    fun mapLinearInverted(value: Double, range: NumericRange, outMin: Float, outMax: Float): Float {
        val t = normalize(value, range).toFloat()
        return outMax - (outMax - outMin) * t
    }

    fun mapRadius(
        sizeValue: Double,
        sizeRange: NumericRange,
        minRadius: Float,
        maxRadius: Float,
    ): Float {
        val t = normalize(sizeValue, sizeRange)
        val eased = sqrt(t).toFloat()
        return minRadius + (maxRadius - minRadius) * eased
    }

    fun tickValues(range: NumericRange, tickCount: Int): List<Double> {
        if (tickCount <= 1) return listOf(range.min, range.max)
        val step = range.span / (tickCount - 1)
        return (0 until tickCount).map { idx ->
            range.min + step * idx
        }
    }

    fun defaultNumberFormat(value: Double): String {
        val absValue = abs(value)
        return when {
            absValue >= 1_000_000_000.0 -> String.format("%.1fB", value / 1_000_000_000.0)
            absValue >= 1_000_000.0 -> String.format("%.1fM", value / 1_000_000.0)
            absValue >= 1_000.0 -> String.format("%.1fK", value / 1_000.0)
            absValue >= 100.0 -> String.format("%.0f", value)
            absValue >= 10.0 -> String.format("%.1f", value)
            else -> String.format("%.2f", value)
        }
    }
}
