package com.magimon.eq.compose.heatmap

import android.graphics.Color
import android.graphics.RectF
import com.magimon.eq.heatmap.StockHeatmapItem
import com.magimon.eq.heatmap.StockHeatmapSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StockHeatmapChartTest {

    private val tech = StockHeatmapSection(
        "Tech",
        0xFF1565C0.toInt(),
        listOf(
            StockHeatmapItem("AAPL", "Apple", "Tech", 210.0, 2.1, 3000.0),
            StockHeatmapItem("MSFT", "Microsoft", "Tech", 420.0, -1.4, 2800.0),
        ),
    )

    @Test
    fun computeHeatmapLayout_buildsSectionsAndFlatLayouts() {
        val grouped = computeHeatmapLayout(
            sections = listOf(
                tech,
                StockHeatmapSection("Finance", 0xFF2E7D32.toInt(), listOf(StockHeatmapItem("JPM", "JPMorgan", "Finance", 200.0, 0.7, 800.0))),
            ),
            width = 720f,
            height = 520f,
            density = 1f,
        )
        val flat = computeHeatmapLayout(
            sections = listOf(tech),
            width = 240f,
            height = 160f,
            density = 1f,
        )

        assertTrue(grouped.blocks.isNotEmpty() || grouped.sectionHeaders.isNotEmpty())
        assertTrue(grouped.sectionHeaders.isNotEmpty())
        assertFalse(flat.blocks.isEmpty())
        assertTrue(flat.sectionHeaders.isEmpty())
    }

    @Test
    fun heatmapHelpers_coverFormattingColorsAndSquarify() {
        val squarified = heatmapLayoutSquarified(
            items = listOf(
                HeatmapWeightedItem("A", 3f),
                HeatmapWeightedItem("B", 2f),
            ),
            bounds = RectF(0f, 0f, 100f, 60f),
        )

        assertEquals("+1.25%", heatmapFormatChange(1.25))
        assertTrue(heatmapItemWeight(tech.stocks.first()) > 0f)
        assertTrue(heatmapWorstAspectRatio(listOf(HeatmapWeightedItem("A", 4f)), 10f) > 0f)
        assertEquals(128, heatmapWithAlpha(0xFF0000FF.toInt(), 128).ushr(24) and 0xFF)
        assertTrue(heatmapMapSectorToColor("Tech") != 0)
        assertTrue(heatmapMapChangeToColor(5.0) != heatmapMapChangeToColor(-5.0))
        assertTrue(heatmapInterpolateColor(0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0.5f) != 0)
        assertEquals(2, squarified.size)
    }

    @Test
    fun computeHeatmapLayout_returnsEmptyForInvalidBounds() {
        val computed = computeHeatmapLayout(
            sections = listOf(tech),
            width = 0f,
            height = 100f,
            density = 1f,
        )

        assertTrue(computed.blocks.isEmpty())
        assertTrue(computed.sectionHeaders.isEmpty())
        assertEquals(0f, heatmapItemWeight(StockHeatmapItem("X", "Zero", "None", 0.0, 0.0, 0.0)), 0f)
    }
}
