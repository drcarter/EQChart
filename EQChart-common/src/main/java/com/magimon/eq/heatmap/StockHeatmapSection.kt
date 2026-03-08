package com.magimon.eq.heatmap

/**
 * Heatmap section (group) model.
 *
 * @param name Name shown in the section header
 * @param color Section header background color (ARGB)
 * @param stocks Stock items included in this section
 */
data class StockHeatmapSection(
    val name: String,
    val color: Int,
    val stocks: List<StockHeatmapItem>,
)
