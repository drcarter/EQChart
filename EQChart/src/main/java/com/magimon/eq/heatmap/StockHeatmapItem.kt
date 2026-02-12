package com.magimon.eq.heatmap

/**
 * Represents a single stock item in the heatmap
 */
data class StockHeatmapItem(
    val symbol: String,
    val name: String,
    val change: Float, // Price change percentage
    val marketCap: Long, // Market capitalization
    val sector: String,
    val price: Float = 0f
)

/**
 * Represents a group of stocks by sector
 */
data class SectorGroup(
    val sector: String,
    val items: List<StockHeatmapItem>
) {
    val totalMarketCap: Long
        get() = items.sumOf { it.marketCap }
}

