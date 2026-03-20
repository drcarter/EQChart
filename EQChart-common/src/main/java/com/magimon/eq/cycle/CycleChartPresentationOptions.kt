package com.magimon.eq.cycle

/**
 * Behavioral and layout options for the cycle diagram chart.
 *
 * This type controls label visibility, animation, node ordering, and connector curvature shared by
 * View and Compose renderers.
 *
 * @property showNodeLabels Whether node labels are rendered around the ring
 * @property showLinkLabels Whether link labels are rendered near each connector midpoint
 * @property animateOnDataChange Whether the chart plays an enter animation when data changes
 * @property animationDurationMs Duration of the enter animation when enabled
 * @property startAngleDeg Angle of the first node. `-90` starts from the top of the ring
 * @property clockwise Whether nodes are laid out clockwise in input order
 * @property linkInnerRadiusFactor Ratio in `[0, 1]` that pulls link control points toward the ring
 * @property linkAlpha Baseline link opacity used when nothing is selected
 * @property emptyText Message shown when there is nothing renderable
 */
data class CycleChartPresentationOptions(
    val showNodeLabels: Boolean = true,
    val showLinkLabels: Boolean = false,
    val animateOnDataChange: Boolean = true,
    val animationDurationMs: Long = 650L,
    val startAngleDeg: Float = -90f,
    val clockwise: Boolean = true,
    val linkInnerRadiusFactor: Float = 0.34f,
    val linkAlpha: Float = 0.42f,
    val emptyText: String? = "No data",
)
