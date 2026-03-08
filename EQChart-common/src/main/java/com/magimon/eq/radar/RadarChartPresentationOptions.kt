package com.magimon.eq.radar

/**
 * Radar chart presentation and animation options.
 *
 * @property showLegend Whether to show the legend
 * @property showAxisLabels Whether to render axis labels
 * @property showPoints Whether to render point markers
 * @property gridLevels Number of concentric polygon grid levels
 * @property animateOnDataChange Whether to auto-play enter animation on data changes
 * @property enterAnimationDurationMs Enter animation duration in milliseconds
 * @property enterAnimationDelayMs Enter animation start delay in milliseconds
 * @property startAngleDeg Start angle of the first axis in degrees. `-90` starts at 12 o'clock.
 * @property legendTextSizeSp Legend text size in sp
 * @property axisLabelTextSizeSp Axis label text size in sp
 * @property legendLeftMarginDp Legend left margin in dp
 * @property legendTopMarginDp Legend top margin in dp
 */
data class RadarChartPresentationOptions(
    val showLegend: Boolean = true,
    val showAxisLabels: Boolean = true,
    val showPoints: Boolean = true,
    val gridLevels: Int = 5,
    val animateOnDataChange: Boolean = true,
    val enterAnimationDurationMs: Long = 700L,
    val enterAnimationDelayMs: Long = 40L,
    val startAngleDeg: Float = -90f,
    val legendTextSizeSp: Float = 13f,
    val axisLabelTextSizeSp: Float = 15f,
    val legendLeftMarginDp: Float = 4f,
    val legendTopMarginDp: Float = 4f,
)
