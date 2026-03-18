package com.magimon.eq.sankey

/**
 * Behavioral and layout options for the Sankey chart.
 *
 * This type controls label visibility, animation, and spacing used by the layout engine.
 *
 * @property showNodeLabels Whether node labels are rendered next to each node
 * @property showLinkValues Whether link labels/values are rendered near the link midpoint
 * @property animateOnDataChange Whether the chart fades in when data changes
 * @property animationDurationMs Duration of the enter animation when enabled
 * @property nodeGapDp Vertical gap between adjacent nodes in the same stage
 * @property columnGapDp Preferred horizontal gap between stages
 * @property linkAlpha Baseline link opacity used when nothing is selected
 * @property emptyText Message shown when the graph is invalid or there is nothing renderable
 */
data class SankeyChartPresentationOptions(
    val showNodeLabels: Boolean = true,
    val showLinkValues: Boolean = false,
    val animateOnDataChange: Boolean = true,
    val animationDurationMs: Long = 650L,
    val nodeGapDp: Float = 12f,
    val columnGapDp: Float = 48f,
    val linkAlpha: Float = 0.38f,
    val emptyText: String? = "No data",
)
