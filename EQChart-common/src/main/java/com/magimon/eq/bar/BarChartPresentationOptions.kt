package com.magimon.eq.bar

/**
 * Behavioral options for bar charts.
 *
 * @property showLegend Whether to draw legend.
 * @property showGrid Whether to draw grid lines.
 * @property showAxes Whether to draw axes.
 * @property showPointLabels Whether to show value labels above bars.
 * @property showBars Whether to render bars (can be disabled to show only axes/labels).
 * @property animateOnDataChange Whether to animate on data changes.
 * @property enterAnimationDurationMs Duration in milliseconds for enter animation.
 * @property enterAnimationDelayMs Delay in milliseconds before enter animation starts.
 * @property animationDirection If true, bar growth animates from baseline toward value.
 * @property layoutMode How bars within a category are arranged.
 * @property orientation Bar orientation.
 * @property emptyText Message shown when there is no renderable data.
 * @property xLabelFormatter Converts category labels when needed.
 * @property yLabelFormatter Converts numeric axis values.
 * @property xTickCount Number of x ticks for numeric orientation.
 * @property yTickCount Number of y ticks for numeric orientation.
 */
data class BarChartPresentationOptions(
    val showLegend: Boolean = true,
    val showGrid: Boolean = true,
    val showAxes: Boolean = true,
    val showBarLabels: Boolean = true,
    val showBars: Boolean = true,
    val animateOnDataChange: Boolean = true,
    val enterAnimationDurationMs: Long = 680L,
    val enterAnimationDelayMs: Long = 30L,
    val animationDirection: Boolean = true,
    val layoutMode: BarLayoutMode = BarLayoutMode.GROUPED,
    val orientation: BarOrientation = BarOrientation.VERTICAL,
    val legendTextSizeSp: Float = 12f,
    val axisLabelTextSizeSp: Float = 11.5f,
    val emptyText: String = "No data",
    val xLabelFormatter: (String) -> String = { it },
    val yLabelFormatter: (Double) -> String = { it.toInt().toString() },
    val xTickCount: Int = 5,
    val yTickCount: Int = 6,
)

