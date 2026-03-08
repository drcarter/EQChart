package com.magimon.eq.bubble

import kotlin.math.abs

/**
 * Axis and grid rendering options.
 */
data class BubbleAxisOptions(
    val showAxes: Boolean = true,
    val showGrid: Boolean = true,
    val showTicks: Boolean = true,
    val xLabelFormatter: (Double) -> String = { defaultBubbleNumberFormat(it) },
    val yLabelFormatter: (Double) -> String = { defaultBubbleNumberFormat(it) },
)

private fun defaultBubbleNumberFormat(value: Double): String {
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
