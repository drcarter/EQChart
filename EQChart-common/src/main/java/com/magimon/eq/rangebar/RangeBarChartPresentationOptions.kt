package com.magimon.eq.rangebar

/**
 * Behavioral options for range bar and timeline charts.
 *
 * @property showGrid Whether to draw vertical grid lines.
 * @property showAxes Whether to draw chart axes.
 * @property showBarLabels Whether to draw interval value labels.
 * @property animateOnDataChange Whether to animate when data changes.
 * @property enterAnimationDurationMs Duration for the enter animation.
 * @property enterAnimationDelayMs Delay before the enter animation starts.
 * @property animationDirection If true, the interval animates from its start value toward its end.
 * @property emptyText Message shown when there is no renderable data.
 * @property axisLabelTextSizeSp Text size for row and axis labels.
 * @property barLabelTextSizeSp Text size for interval labels.
 * @property xLabelFormatter Converts numeric X values into axis labels.
 * @property rowLabelFormatter Converts each row label into display text.
 * @property barLabelFormatter Converts each resolved layout entry into a label rendered on or near
 * the bar.
 * @property xTickCount Number of X-axis ticks.
 */
data class RangeBarChartPresentationOptions(
    val showGrid: Boolean = true,
    val showAxes: Boolean = true,
    val showBarLabels: Boolean = true,
    val animateOnDataChange: Boolean = true,
    val enterAnimationDurationMs: Long = 680L,
    val enterAnimationDelayMs: Long = 30L,
    val animationDirection: Boolean = true,
    val emptyText: String = "No data",
    val axisLabelTextSizeSp: Float = 11.5f,
    val barLabelTextSizeSp: Float = 11.5f,
    val xLabelFormatter: (Double) -> String = ::formatRangeBarAxisValue,
    val rowLabelFormatter: (String) -> String = { it },
    val barLabelFormatter: (RangeBarLayoutEntry) -> String = { entry ->
        entry.title ?: "${entry.label}: ${formatRangeBarAxisValue(entry.startValue)}-${formatRangeBarAxisValue(entry.endValue)}"
    },
    val xTickCount: Int = 6,
)
