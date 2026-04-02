package com.magimon.eq.treemap

/**
 * One rendered tile in a Treemap chart.
 *
 * @property label Primary text rendered inside the tile.
 * @property value Positive numeric value used as the tile area weight.
 * @property color Optional tile fill color override.
 * @property supportingText Optional second-line text rendered when the tile is large enough.
 * @property payload Optional source object returned in click callbacks.
 */
data class TreemapItem(
    val label: String,
    val value: Double,
    val color: Int? = null,
    val supportingText: String? = null,
    val payload: Any? = null,
)
