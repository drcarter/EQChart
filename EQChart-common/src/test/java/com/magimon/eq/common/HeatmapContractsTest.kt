package com.magimon.eq.common

import com.magimon.eq.heatmap.StockHeatmapItem
import com.magimon.eq.heatmap.StockHeatmapSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class HeatmapContractsTest {

    @Test
    fun stockHeatmapItem_defaultsAndCustomValues_areAccessible() {
        val defaultRatioItem = StockHeatmapItem(
            symbol = "AAPL",
            name = "Apple",
            sector = "Technology",
            price = 200.0,
            changePct = 1.2,
            marketCap = 3_000_000_000_000.0,
        )
        val customRatioItem = StockHeatmapItem(
            symbol = "MSFT",
            name = "Microsoft",
            sector = "Technology",
            price = 380.0,
            changePct = -0.8,
            marketCap = 2_800_000_000_000.0,
            sizeRatio = 22.0,
        )

        assertEquals("AAPL", defaultRatioItem.symbol)
        assertEquals("Apple", defaultRatioItem.name)
        assertEquals("Technology", defaultRatioItem.sector)
        assertEquals(200.0, defaultRatioItem.price, 0.0)
        assertEquals(1.2, defaultRatioItem.changePct, 0.0)
        assertEquals(3_000_000_000_000.0, defaultRatioItem.marketCap, 0.0)
        assertNull(defaultRatioItem.sizeRatio)

        assertEquals("MSFT", customRatioItem.symbol)
        assertEquals("Microsoft", customRatioItem.name)
        assertEquals("Technology", customRatioItem.sector)
        assertEquals(380.0, customRatioItem.price, 0.0)
        assertEquals(-0.8, customRatioItem.changePct, 0.0)
        assertEquals(2_800_000_000_000.0, customRatioItem.marketCap, 0.0)
        assertEquals(22.0, requireNotNull(customRatioItem.sizeRatio), 0.0)
    }

    @Test
    fun stockHeatmapSection_preservesNameColorAndItems() {
        val item = StockHeatmapItem(
            symbol = "NVDA",
            name = "NVIDIA",
            sector = "Technology",
            price = 900.0,
            changePct = 4.2,
            marketCap = 2_200_000_000_000.0,
            sizeRatio = 18.0,
        )
        val stocks = listOf(item)
        val section = StockHeatmapSection(
            name = "Technology",
            color = 0xFF1E88E5.toInt(),
            stocks = stocks,
        )

        assertEquals("Technology", section.name)
        assertEquals(0xFF1E88E5.toInt(), section.color)
        assertSame(stocks, section.stocks)
    }
}
