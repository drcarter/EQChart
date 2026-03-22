package com.magimon.eq.sunburst

/**
 * Presentation and animation options for Sunburst charts.
 *
 * @property showLabels Whether segment labels should be drawn.
 * @property minLabelSweepDeg Minimum sweep angle required before a label is rendered.
 * @property innerHoleRatio Center hole radius expressed as a ratio of the outer radius.
 * @property ringGapRatio Gap between adjacent depth rings expressed as a ratio of the outer radius.
 * @property startAngleDeg Starting angle of the first root segment in degrees.
 * @property clockwise Whether segments should expand clockwise instead of counterclockwise.
 * @property animateOnDataChange Whether renderers should play the enter animation after data changes.
 * @property enterAnimationDurationMs Duration of the enter animation in milliseconds.
 * @property enterAnimationDelayMs Delay before the enter animation starts in milliseconds.
 * @property centerText Optional primary text rendered inside the center hole.
 * @property centerSubText Optional secondary text rendered below [centerText] inside the center hole.
 * @property emptyText Optional placeholder text shown when no renderable nodes exist.
 * @see SunburstNode
 * @see SunburstChartStyleOptions
 * @see SunburstChartLayoutEngine
 */
data class SunburstChartPresentationOptions(
    val showLabels: Boolean = true,
    val minLabelSweepDeg: Float = 14f,
    val innerHoleRatio: Float = 0.22f,
    val ringGapRatio: Float = 0.025f,
    val startAngleDeg: Float = -90f,
    val clockwise: Boolean = true,
    val animateOnDataChange: Boolean = true,
    val enterAnimationDurationMs: Long = 650L,
    val enterAnimationDelayMs: Long = 0L,
    val centerText: String? = null,
    val centerSubText: String? = null,
    val emptyText: String? = "No data",
)
