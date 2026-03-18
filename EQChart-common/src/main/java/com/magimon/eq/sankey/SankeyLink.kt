package com.magimon.eq.sankey

/**
 * Directed flow edge between two [SankeyNode] ids.
 *
 * Links define both graph connectivity and band thickness. Only finite positive [value] inputs are
 * renderable. Links that refer to missing nodes or have non-positive values are ignored by the
 * layout engine.
 *
 * @property sourceId Source node id
 * @property targetId Target node id
 * @property value Positive flow value used for link thickness
 * @property color Optional link override color. Defaults to the source node color when omitted.
 * @property label Optional display label. Used when link-value labels are enabled.
 * @property payload Optional source payload delivered to click callbacks
 */
data class SankeyLink(
    val sourceId: String,
    val targetId: String,
    val value: Double,
    val color: Int? = null,
    val label: String? = null,
    val payload: Any? = null,
)
