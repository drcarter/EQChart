package com.magimon.eq.treemap

/**
 * One group in a two-level Treemap chart.
 *
 * @property label Group label rendered in the optional header strip.
 * @property color Optional base color used for the header and derived item colors.
 * @property items Rendered child tiles that belong to this group.
 */
data class TreemapGroup(
    val label: String,
    val color: Int? = null,
    val items: List<TreemapItem>,
)
