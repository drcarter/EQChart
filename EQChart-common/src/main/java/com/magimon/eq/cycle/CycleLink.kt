package com.magimon.eq.cycle

/**
 * Directed connection between two [CycleNode] ids.
 *
 * Links are rendered as curved connectors inside the node ring. Only finite positive [value]
 * inputs that reference existing node ids are renderable.
 *
 * @property sourceId Source node id
 * @property targetId Target node id
 * @property value Positive weight used for link thickness scaling
 * @property color Optional link color override. Defaults to the source node color when omitted.
 * @property label Optional display label rendered near the link midpoint
 * @property payload Optional source payload delivered to click callbacks
 */
data class CycleLink(
    val sourceId: String,
    val targetId: String,
    val value: Double,
    val color: Int? = null,
    val label: String? = null,
    val payload: Any? = null,
)
