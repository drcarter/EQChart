package com.magimon.eq.sunburst

/**
 * Hierarchical node model for a Sunburst chart.
 *
 * If [children] contains at least one valid descendant, the rendered value is derived from the
 * sum of those descendants. Otherwise [value] must be finite and greater than zero to render.
 *
 * @property label Segment label rendered for this node.
 * @property value Explicit value for a leaf node. Parent nodes derive their value from [children].
 * @property color Optional segment fill color override for this node.
 * @property children Child nodes rendered in the next outer ring.
 * @property payload Optional source payload returned by chart click callbacks.
 * @see SunburstChartLayoutEngine
 * @see SunburstChartStyleOptions
 * @see SunburstChartPresentationOptions
 */
data class SunburstNode(
    val label: String,
    val value: Double = Double.NaN,
    val color: Int? = null,
    val children: List<SunburstNode> = emptyList(),
    val payload: Any? = null,
)
