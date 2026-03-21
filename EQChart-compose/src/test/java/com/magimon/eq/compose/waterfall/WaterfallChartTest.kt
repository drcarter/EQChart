package com.magimon.eq.compose.waterfall

import com.magimon.eq.waterfall.WaterfallChartPresentationOptions
import com.magimon.eq.waterfall.WaterfallChartStyleOptions
import com.magimon.eq.waterfall.WaterfallEntry
import com.magimon.eq.waterfall.WaterfallEntryKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WaterfallChartTest {

    @Test
    fun computeWaterfallLayout_buildsBarsForDeltaSubtotalAndTotal() {
        val computed = computeWaterfallLayout(
            widthPx = 360f,
            heightPx = 240f,
            entries = listOf(
                WaterfallEntry("Revenue", 120.0, payload = "revenue"),
                WaterfallEntry("Costs", -35.0, payload = "costs"),
                WaterfallEntry("Subtotal", 0.0, kind = WaterfallEntryKind.SUBTOTAL, payload = "subtotal"),
                WaterfallEntry("Tax", -15.0, payload = "tax"),
                WaterfallEntry("Total", 0.0, kind = WaterfallEntryKind.TOTAL, payload = "total"),
            ),
            style = WaterfallChartStyleOptions(),
            presentation = WaterfallChartPresentationOptions(
                animateOnDataChange = false,
                showConnectorLines = true,
            ),
            progress = 1f,
            density = 1f,
            scaledDensity = 1f,
        )

        assertEquals(5, computed.bars.size)
        assertEquals(4, computed.connectors.size)
        assertEquals(120.0, computed.bars[0].entry.endValue, 0.0)
        assertEquals(85.0, computed.bars[2].entry.displayValue, 0.0)
        assertEquals(70.0, computed.bars[4].entry.endValue, 0.0)
        assertTrue(computed.bars[2].bottom > computed.bars[2].top)
    }

    @Test
    fun computeWaterfallLayout_filtersInvalidEntriesAndKeepsEmptyState() {
        val computed = computeWaterfallLayout(
            widthPx = 300f,
            heightPx = 220f,
            entries = listOf(
                WaterfallEntry("", 12.0),
                WaterfallEntry("Bad", Double.NaN),
            ),
            style = WaterfallChartStyleOptions(),
            presentation = WaterfallChartPresentationOptions(animateOnDataChange = false),
            progress = 1f,
            density = 1f,
            scaledDensity = 1f,
        )

        assertTrue(computed.bars.isEmpty())
        assertTrue(computed.connectors.isEmpty())
        assertEquals(-1.0, computed.minValue, 0.0)
        assertEquals(1.0, computed.maxValue, 0.0)
    }
}
