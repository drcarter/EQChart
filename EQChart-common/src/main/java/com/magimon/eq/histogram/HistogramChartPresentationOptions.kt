package com.magimon.eq.histogram

/**
 * Behavioral options for histogram charts.
 *
 * @property showGrid Whether to draw horizontal grid lines.
 * @property showAxes Whether to draw chart axes.
 * @property showBarLabels Whether to draw value labels above bars.
 * @property animateOnDataChange Whether to animate on data changes.
 * @property enterAnimationDurationMs Duration for enter animation.
 * @property enterAnimationDelayMs Delay before enter animation starts.
 * @property animationDirection If true, bars animate from baseline toward value.
 * @property emptyText Message shown when there is no renderable data.
 * @property axisLabelTextSizeSp Text size for axis and bin labels.
 * @property valueLabelTextSizeSp Text size for bar labels.
 * @property yLabelFormatter Converts numeric axis values.
 * @property valueLabelFormatter Converts bar values for labels.
 * @property binLabelFormatter Converts a bin into its x-axis label.
 * @property yTickCount Number of horizontal grid ticks.
 */
data class HistogramChartPresentationOptions(
    val showGrid: Boolean = true,
    val showAxes: Boolean = true,
    val showBarLabels: Boolean = true,
    val animateOnDataChange: Boolean = true,
    val enterAnimationDurationMs: Long = 680L,
    val enterAnimationDelayMs: Long = 30L,
    val animationDirection: Boolean = true,
    val emptyText: String = "No data",
    val axisLabelTextSizeSp: Float = 11.5f,
    val valueLabelTextSizeSp: Float = 11.5f,
    val yLabelFormatter: (Double) -> String = { it.toInt().toString() },
    val valueLabelFormatter: (Double) -> String = { it.toInt().toString() },
    val binLabelFormatter: (HistogramBin) -> String = { bin ->
        bin.label ?: "${bin.start.toInt()}-${bin.end.toInt()}"
    },
    val yTickCount: Int = 6,
)
