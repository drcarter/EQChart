package com.magimon.eq.bubble

/**
 * Axis and grid rendering options.
 */
data class BubbleAxisOptions(
    val showAxes: Boolean = true,
    val showGrid: Boolean = true,
    val showTicks: Boolean = true,
    val xLabelFormatter: (Double) -> String = { BubbleChartMath.defaultNumberFormat(it) },
    val yLabelFormatter: (Double) -> String = { BubbleChartMath.defaultNumberFormat(it) },
)
