package com.magimon.eq.bubble

/**
 * 축/그리드 렌더링 옵션.
 */
data class BubbleAxisOptions(
    val showAxes: Boolean = true,
    val showGrid: Boolean = true,
    val showTicks: Boolean = true,
    val xLabelFormatter: (Double) -> String = { BubbleChartMath.defaultNumberFormat(it) },
    val yLabelFormatter: (Double) -> String = { BubbleChartMath.defaultNumberFormat(it) },
)
