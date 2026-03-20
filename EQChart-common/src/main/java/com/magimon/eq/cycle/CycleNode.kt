package com.magimon.eq.cycle

/**
 * Node model used by the cycle diagram chart.
 *
 * Nodes are arranged around a ring in input order, and [CycleLink] references them by [id].
 *
 * @property id Stable node identifier referenced by [CycleLink]
 * @property label Display label rendered near the node
 * @property color Primary fill color for the node
 * @property payload Optional source payload delivered to click callbacks
 */
data class CycleNode(
    val id: String,
    val label: String,
    val color: Int,
    val payload: Any? = null,
)
