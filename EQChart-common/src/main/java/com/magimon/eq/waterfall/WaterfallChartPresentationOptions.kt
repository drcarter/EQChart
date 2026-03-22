package com.magimon.eq.waterfall

/**
 * Behavioral options for waterfall charts.
 *
 * @property showGrid Whether to draw horizontal grid lines.
 * @property showAxes Whether to draw chart axes.
 * @property showBarLabels Whether to draw value labels near each bar.
 * @property showConnectorLines Whether to draw step connectors between bars.
 * @property animateOnDataChange Whether to animate bars on data changes.
 * @property enterAnimationDurationMs Duration for enter animation.
 * @property enterAnimationDelayMs Delay before enter animation starts.
 * @property animationDirection If true, bars animate from their start value toward their end value.
 * @property emptyText Message shown when there is no renderable data.
 * @property axisLabelTextSizeSp Text size for axis and category labels.
 * @property valueLabelTextSizeSp Text size for value labels.
 * @property yLabelFormatter Converts numeric axis values.
 * @property valueLabelFormatter Converts delta or summary values for bar labels.
 * @property yTickCount Number of horizontal grid ticks.
 */
data class WaterfallChartPresentationOptions(
    val showGrid: Boolean = true,
    val showAxes: Boolean = true,
    val showBarLabels: Boolean = true,
    val showConnectorLines: Boolean = true,
    val animateOnDataChange: Boolean = true,
    val enterAnimationDurationMs: Long = 680L,
    val enterAnimationDelayMs: Long = 30L,
    val animationDirection: Boolean = true,
    val emptyText: String = "No data",
    val axisLabelTextSizeSp: Float = 11.5f,
    val valueLabelTextSizeSp: Float = 11.5f,
    val yLabelFormatter: (Double) -> String = { it.toInt().toString() },
    val valueLabelFormatter: (Double) -> String = { value ->
        when {
            value > 0.0 -> "+${value.toInt()}"
            value < 0.0 -> value.toInt().toString()
            else -> "0"
        }
    },
    val yTickCount: Int = 6,
)
