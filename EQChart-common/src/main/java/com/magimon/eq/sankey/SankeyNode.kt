package com.magimon.eq.sankey

/**
 * Node definition used by the Sankey chart.
 *
 * Sankey nodes are identified by [id] and referenced by [SankeyLink.sourceId] /
 * [SankeyLink.targetId]. A node may optionally pin itself to a fixed [stage] (column); when
 * omitted, the chart infers the stage from graph topology.
 *
 * @property id Stable node identifier referenced by [SankeyLink]
 * @property label Display label rendered next to the node
 * @property color Primary fill color of the node
 * @property stage Optional fixed stage/column. When omitted, stage is inferred from links.
 * @property payload Optional source payload delivered to click callbacks
 */
data class SankeyNode(
    val id: String,
    val label: String,
    val color: Int,
    val stage: Int? = null,
    val payload: Any? = null,
)
