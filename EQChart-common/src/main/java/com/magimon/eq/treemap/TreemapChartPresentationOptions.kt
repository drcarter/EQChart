package com.magimon.eq.treemap

/**
 * Behavioral and label options for Treemap charts.
 *
 * @property showGroupHeaders Whether group headers should be drawn when multiple groups exist.
 * @property showSupportingText Whether optional second-line tile text should be drawn.
 * @property emptyText Message shown when there is no renderable data.
 * @property headerTextSizeSp Group header text size.
 * @property itemLabelTextSizeSp Primary tile label text size.
 * @property itemSupportingTextSizeSp Secondary tile label text size.
 */
data class TreemapChartPresentationOptions(
    val showGroupHeaders: Boolean = true,
    val showSupportingText: Boolean = true,
    val emptyText: String = "No data",
    val headerTextSizeSp: Float = 11f,
    val itemLabelTextSizeSp: Float = 12f,
    val itemSupportingTextSizeSp: Float = 10f,
)
