package com.magimon.eq.heatmap

import android.graphics.RectF
import com.magimon.eq.testutil.layoutAndDraw
import com.magimon.eq.testutil.readPrivate
import com.magimon.eq.testutil.touchUp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class StockHeatmapViewTest {

    @Test
    fun setData_groupsFlatItems_andDispatchesBlockClick() {
        val context = RuntimeEnvironment.getApplication()
        val view = StockHeatmapView(context)
        var clickedSymbol: String? = null

        view.setOnItemClickListener { clickedSymbol = it.symbol }
        view.setData(
            listOf(
                StockHeatmapItem(
                    symbol = "AAPL",
                    name = "Apple",
                    sector = "Technology",
                    price = 220.0,
                    changePct = 2.4,
                    marketCap = 3_000_000_000_000.0,
                ),
            ),
        )

        layoutAndDraw(view, width = 420, height = 420)

        val blocks = view.readPrivate<List<*>>("blocks")
        val firstBlock = blocks.firstOrNull() ?: error("Expected at least one block")
        val firstRect = firstBlock.readPrivate<RectF>("rect")

        touchUp(view, firstRect.centerX(), firstRect.centerY())

        assertEquals("AAPL", clickedSymbol)
    }

    @Test
    fun setSections_filtersInvalidSections_andBuildsSectionHeaders() {
        val context = RuntimeEnvironment.getApplication()
        val view = StockHeatmapView(context)

        val validItem = StockHeatmapItem(
            symbol = "NVDA",
            name = "NVIDIA",
            sector = "Technology",
            price = 950.0,
            changePct = -1.2,
            marketCap = 2_200_000_000_000.0,
        )

        view.setSections(
            listOf(
                StockHeatmapSection(name = "", color = 0xFF0000, stocks = listOf(validItem)),
                StockHeatmapSection(
                    name = "Empty",
                    color = 0x00FF00,
                    stocks = listOf(validItem.copy(symbol = "ZERO", marketCap = 0.0, sizeRatio = 0.0)),
                ),
                StockHeatmapSection(name = "Tech", color = 0x0000FF, stocks = listOf(validItem)),
                StockHeatmapSection(name = "AI", color = 0x00FFFF, stocks = listOf(validItem.copy(symbol = "MSFT"))),
            ),
        )

        layoutAndDraw(view, width = 460, height = 420)

        val sections = view.readPrivate<List<*>>("sections")
        val sectionLayouts = view.readPrivate<List<*>>("sectionLayouts")
        val blocks = view.readPrivate<List<*>>("blocks")

        assertEquals(2, sections.size)
        assertFalse(sectionLayouts.isEmpty())
        assertTrue(blocks.size >= 2)
    }
}
