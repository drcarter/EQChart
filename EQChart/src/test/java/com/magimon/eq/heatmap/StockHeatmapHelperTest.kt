package com.magimon.eq.heatmap

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

class StockHeatmapHelperTest {

    private lateinit var previousLocale: Locale

    @Before
    fun setUp() {
        previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(previousLocale)
    }

    @Test
    fun createSampleSections_returnsGroupedSamplePayload() {
        val sections = StockHeatmapHelper.createSampleSections()

        assertEquals(5, sections.size)
        assertTrue(sections.all { it.name.isNotBlank() })
        assertTrue(sections.all { it.stocks.size == 5 })
        assertTrue(sections.flatMap { it.stocks }.all { it.symbol.isNotBlank() })
    }

    @Test
    fun createSampleData_flattensAllSectionStocks() {
        val sections = StockHeatmapHelper.createSampleSections()
        val flat = StockHeatmapHelper.createSampleData()

        assertEquals(sections.sumOf { it.stocks.size }, flat.size)
        assertEquals(sections.first().stocks.first().symbol, flat.first().symbol)
    }

    @Test
    fun mapSectorToColor_isDeterministicForSameInput() {
        val first = StockHeatmapHelper.mapSectorToColor("Technology")
        val second = StockHeatmapHelper.mapSectorToColor("Technology")

        assertEquals(first, second)
    }

    @Test
    fun formatChange_andFormatMarketCap_applyExpectedFormatting() {
        assertEquals("+1.23%", StockHeatmapHelper.formatChange(1.234))
        assertEquals("-0.40%", StockHeatmapHelper.formatChange(-0.4))
        assertEquals("1.2T", StockHeatmapHelper.formatMarketCap(1_200_000_000_000.0))
        assertEquals("850.0B", StockHeatmapHelper.formatMarketCap(850_000_000_000.0))
        assertEquals("12.5M", StockHeatmapHelper.formatMarketCap(12_500_000.0))
        assertEquals("999", StockHeatmapHelper.formatMarketCap(999.0))
    }

    @Test
    fun mapChangeToColor_returnsStableValueForNeutralAndDirectionalChanges() {
        val neutral = StockHeatmapHelper.mapChangeToColor(0.0)
        val positive = StockHeatmapHelper.mapChangeToColor(3.0)
        val negative = StockHeatmapHelper.mapChangeToColor(-3.0)

        assertEquals(neutral, StockHeatmapHelper.mapChangeToColor(0.0))
        assertFalse(positive == Int.MIN_VALUE)
        assertFalse(negative == Int.MIN_VALUE)
    }
}
