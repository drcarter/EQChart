package com.magimon.eq.common

import com.magimon.eq.waterfall.WaterfallChartPresentationOptions
import com.magimon.eq.waterfall.WaterfallChartStyleOptions
import com.magimon.eq.waterfall.WaterfallEntry
import com.magimon.eq.waterfall.WaterfallEntryKind
import com.magimon.eq.waterfall.resolveWaterfallBaseline
import com.magimon.eq.waterfall.resolveWaterfallChartLayout
import com.magimon.eq.waterfall.waterfallAxisTicks
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class WaterfallContractsTest {

    @Test
    fun waterfallModels_preserveValuesAndDefaults() {
        val payload = linkedMapOf("wf" to 1)
        val defaultEntry = WaterfallEntry(label = "Revenue", value = 120.0)
        val summaryEntry = WaterfallEntry(
            label = "Subtotal",
            value = Double.NaN,
            color = 0xFF112233.toInt(),
            kind = WaterfallEntryKind.SUBTOTAL,
            payload = payload,
        )
        val copied = summaryEntry.copy(label = "Total", kind = WaterfallEntryKind.TOTAL)

        assertEquals("Revenue", defaultEntry.label)
        assertEquals(120.0, defaultEntry.value, 0.0)
        assertNull(defaultEntry.color)
        assertEquals(WaterfallEntryKind.DELTA, defaultEntry.kind)
        assertNull(defaultEntry.payload)

        assertEquals("Subtotal", summaryEntry.label)
        assertEquals(WaterfallEntryKind.SUBTOTAL, summaryEntry.kind)
        assertEquals(0xFF112233.toInt(), summaryEntry.color)
        assertSame(payload, summaryEntry.payload)
        assertEquals("Total", copied.label)
        assertEquals(WaterfallEntryKind.TOTAL, copied.kind)

        assertArrayEquals(
            arrayOf(WaterfallEntryKind.DELTA, WaterfallEntryKind.SUBTOTAL, WaterfallEntryKind.TOTAL),
            WaterfallEntryKind.values(),
        )
        assertEquals(WaterfallEntryKind.TOTAL, WaterfallEntryKind.valueOf("TOTAL"))
    }

    @Test
    fun waterfallPresentationDefaultsAndCustomValues_areAccessible() {
        val defaults = WaterfallChartPresentationOptions()
        val custom = WaterfallChartPresentationOptions(
            showGrid = false,
            showAxes = false,
            showBarLabels = false,
            showConnectorLines = false,
            animateOnDataChange = false,
            enterAnimationDurationMs = 320L,
            enterAnimationDelayMs = 15L,
            animationDirection = false,
            emptyText = "EMPTY",
            axisLabelTextSizeSp = 13f,
            valueLabelTextSizeSp = 14f,
            yLabelFormatter = { value -> "y:${value.toInt()}" },
            valueLabelFormatter = { value -> "v:${value.toInt()}" },
            yTickCount = 7,
        )

        assertTrue(defaults.showGrid)
        assertTrue(defaults.showAxes)
        assertTrue(defaults.showBarLabels)
        assertTrue(defaults.showConnectorLines)
        assertTrue(defaults.animateOnDataChange)
        assertTrue(defaults.animationDirection)
        assertEquals("No data", defaults.emptyText)
        assertEquals(11.5f, defaults.axisLabelTextSizeSp, 0.0f)
        assertEquals(11.5f, defaults.valueLabelTextSizeSp, 0.0f)
        assertEquals("+12", defaults.valueLabelFormatter(12.4))
        assertEquals("-4", defaults.valueLabelFormatter(-4.8))
        assertEquals(6, defaults.yTickCount)

        assertFalse(custom.showGrid)
        assertFalse(custom.showAxes)
        assertFalse(custom.showBarLabels)
        assertFalse(custom.showConnectorLines)
        assertFalse(custom.animateOnDataChange)
        assertFalse(custom.animationDirection)
        assertEquals(320L, custom.enterAnimationDurationMs)
        assertEquals(15L, custom.enterAnimationDelayMs)
        assertEquals("EMPTY", custom.emptyText)
        assertEquals(13f, custom.axisLabelTextSizeSp, 0.0f)
        assertEquals(14f, custom.valueLabelTextSizeSp, 0.0f)
        assertEquals("y:4", custom.yLabelFormatter(4.8))
        assertEquals("v:-3", custom.valueLabelFormatter(-3.2))
        assertEquals(7, custom.yTickCount)
    }

    @Test
    fun waterfallStyleDefaultsAndCustomValues_areAccessible() {
        val defaults = WaterfallChartStyleOptions()
        val custom = WaterfallChartStyleOptions(
            backgroundColor = 1,
            gridColor = 2,
            axisColor = 3,
            axisLabelColor = 4,
            connectorColor = 5,
            barValueTextColor = 6,
            positiveBarColor = 7,
            negativeBarColor = 8,
            subtotalBarColor = 9,
            totalBarColor = 10,
            categorySpacingDp = 11f,
            barWidthRatio = 0.75f,
            barCornerRadiusDp = 12f,
            selectedBarPaddingDp = 13f,
            contentPaddingDp = 14f,
            connectorStrokeWidthDp = 15f,
        )

        assertEquals(12f, defaults.categorySpacingDp, 0.0f)
        assertEquals(0.62f, defaults.barWidthRatio, 0.0f)
        assertEquals(3f, defaults.barCornerRadiusDp, 0.0f)
        assertEquals(1.5f, defaults.selectedBarPaddingDp, 0.0f)
        assertEquals(14f, defaults.contentPaddingDp, 0.0f)
        assertEquals(1.5f, defaults.connectorStrokeWidthDp, 0.0f)

        assertEquals(1, custom.backgroundColor)
        assertEquals(5, custom.connectorColor)
        assertEquals(10, custom.totalBarColor)
        assertEquals(11f, custom.categorySpacingDp, 0.0f)
        assertEquals(0.75f, custom.barWidthRatio, 0.0f)
        assertEquals(15f, custom.connectorStrokeWidthDp, 0.0f)
    }

    @Test
    fun waterfallLayoutEngine_buildsCumulativeEntriesAndConnectors() {
        val layout = resolveWaterfallChartLayout(
            entries = listOf(
                WaterfallEntry("Revenue", 120.0),
                WaterfallEntry("Cost", -35.0),
                WaterfallEntry("Subtotal", Double.NaN, kind = WaterfallEntryKind.SUBTOTAL),
                WaterfallEntry("Tax", -15.0),
                WaterfallEntry("Total", 0.0, kind = WaterfallEntryKind.TOTAL),
                WaterfallEntry("", 50.0),
                WaterfallEntry("Bad", Double.NaN),
            ),
        )

        assertEquals(5, layout.entries.size)
        assertEquals(4, layout.connectors.size)
        assertEquals(0.0, layout.baselineValue, 0.0)
        assertEquals(120.0, layout.entries[0].endValue, 0.0)
        assertEquals(85.0, layout.entries[1].endValue, 0.0)
        assertEquals(85.0, layout.entries[2].displayValue, 0.0)
        assertEquals(0.0, layout.entries[2].startValue, 0.0)
        assertEquals(70.0, layout.entries[4].endValue, 0.0)
        assertEquals(85.0, layout.connectors[1].fromValue, 0.0)
        assertEquals(85.0, layout.connectors[1].toValue, 0.0)
        assertTrue(layout.maxValue >= 120.0)
        assertTrue(layout.minValue <= 0.0)
        assertEquals(layout.minValue, waterfallAxisTicks(layout.minValue, layout.maxValue, 1).first(), 0.0)
        assertEquals(0.0, resolveWaterfallBaseline(-4.0, 12.0), 0.0)
    }
}
