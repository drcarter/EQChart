package com.magimon.eq.pie

/**
 * Pie/donut presentation and animation options.
 *
 * @property showLegend Whether to show the legend
 * @property showLabels Whether to render slice labels
 * @property labelPosition Label placement policy (inside/outside/auto)
 * @property enableSelectionExpand Enables explode-style outward movement on selection
 * @property selectedSliceExpandDp Outward offset distance for selected slice (dp)
 * @property selectedSliceExpandAnimMs Selection/deselection transition duration (ms)
 * @property startAngleDeg Start angle of the first slice in degrees. `-90` starts at 12 o'clock.
 * @property clockwise Slice progression direction. `true` for clockwise.
 * @property animateOnDataChange Whether to auto-play enter animation on data changes
 * @property enterAnimationDurationMs Enter animation duration (ms)
 * @property enterAnimationDelayMs Enter animation start delay (ms)
 * @property legendTopMarginDp Top gap between chart body and legend (dp)
 * @property legendBottomMarginDp Bottom legend margin (dp)
 * @property legendLeftMarginDp Left legend margin (dp)
 * @property centerText Donut center primary text
 * @property centerSubText Donut center secondary text
 * @property emptyText Message shown when there are no valid slices
 */
data class PieDonutPresentationOptions(
    val showLegend: Boolean = true,
    val showLabels: Boolean = true,
    val labelPosition: PieLabelPosition = PieLabelPosition.AUTO,
    val enableSelectionExpand: Boolean = false,
    val selectedSliceExpandDp: Float = 8f,
    val selectedSliceExpandAnimMs: Long = 140L,
    val startAngleDeg: Float = -90f,
    val clockwise: Boolean = true,
    val animateOnDataChange: Boolean = true,
    val enterAnimationDurationMs: Long = 650L,
    val enterAnimationDelayMs: Long = 0L,
    val legendTopMarginDp: Float = 8f,
    val legendBottomMarginDp: Float = 8f,
    val legendLeftMarginDp: Float = 4f,
    val centerText: String? = null,
    val centerSubText: String? = null,
    val emptyText: String? = "No data",
)
