package com.magimon.eq.heatmap

/**
 * Individual stock item rendered in the heatmap.
 *
 * @param symbol Ticker symbol (for example, AAPL)
 * @param name Display name
 * @param sector Sector name used for section grouping (for example, Technology)
 * @param price Latest traded price
 * @param changePct Percentage change over the reference period (for example, -1.23 means -1.23%)
 * @param marketCap Market capitalization (assumes the same currency unit across items)
 * @param sizeRatio Optional relative size ratio for block area.
 * If present and greater than zero, this value is used instead of `marketCap` as the area weight.
 */
data class StockHeatmapItem(
    val symbol: String,
    val name: String,
    val sector: String,
    val price: Double,
    val changePct: Double,
    val marketCap: Double,
    val sizeRatio: Double? = null,
)
