package com.magimon.eq.common

import android.graphics.Color
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
        assertEquals("4", defaults.yLabelFormatter(4.8))
        assertEquals("+12", defaults.valueLabelFormatter(12.4))
        assertEquals("-4", defaults.valueLabelFormatter(-4.8))
        assertEquals("0", defaults.valueLabelFormatter(0.0))
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

        assertEquals(Color.WHITE, defaults.backgroundColor)
        assertEquals(Color.parseColor("#D7DFE8"), defaults.gridColor)
        assertEquals(Color.parseColor("#8F9CAB"), defaults.axisColor)
        assertEquals(Color.parseColor("#5E6878"), defaults.axisLabelColor)
        assertEquals(Color.parseColor("#93A1B2"), defaults.connectorColor)
        assertEquals(Color.parseColor("#263244"), defaults.barValueTextColor)
        assertEquals(Color.parseColor("#13C3A3"), defaults.positiveBarColor)
        assertEquals(Color.parseColor("#EF476F"), defaults.negativeBarColor)
        assertEquals(Color.parseColor("#FF9F1C"), defaults.subtotalBarColor)
        assertEquals(Color.parseColor("#2B80FF"), defaults.totalBarColor)
        assertEquals(1, custom.backgroundColor)
        assertEquals(5, custom.connectorColor)
        assertEquals(10, custom.totalBarColor)
        assertEquals(11f, custom.categorySpacingDp, 0.0f)
        assertEquals(0.75f, custom.barWidthRatio, 0.0f)
        assertEquals(15f, custom.connectorStrokeWidthDp, 0.0f)
    }

    @Test
    fun waterfallLayoutEngine_buildsCumulativeEntriesAndConnectors() {
        val payload = linkedMapOf("bar" to "revenue")
        val layout = resolveWaterfallChartLayout(
            entries = listOf(
                WaterfallEntry("Revenue", 120.0, color = 0xFF123456.toInt(), payload = payload),
                WaterfallEntry("Cost", -35.0),
                WaterfallEntry("Subtotal", Double.NaN, color = 0xFF234567.toInt(), kind = WaterfallEntryKind.SUBTOTAL),
                WaterfallEntry("Tax", -15.0),
                WaterfallEntry("Total", 0.0, color = 0xFF345678.toInt(), kind = WaterfallEntryKind.TOTAL),
                WaterfallEntry("", 50.0),
                WaterfallEntry("Bad", Double.NaN),
            ),
        )

        assertEquals(5, layout.entries.size)
        assertEquals(4, layout.connectors.size)
        assertEquals("Revenue", layout.entries[0].label)
        assertEquals(120.0, layout.entries[0].rawValue, 0.0)
        assertEquals(0xFF123456.toInt(), layout.entries[0].color)
        assertSame(payload, layout.entries[0].payload)
        assertEquals(0.0, layout.baselineValue, 0.0)
        assertEquals(120.0, layout.entries[0].endValue, 0.0)
        assertEquals(85.0, layout.entries[1].endValue, 0.0)
        assertEquals(85.0, layout.entries[2].displayValue, 0.0)
        assertEquals(0.0, layout.entries[2].startValue, 0.0)
        assertEquals(70.0, layout.entries[4].endValue, 0.0)
        assertEquals(0xFF234567.toInt(), layout.entries[2].color)
        assertEquals(0xFF345678.toInt(), layout.entries[4].color)
        assertEquals(0, layout.connectors[0].fromIndex)
        assertEquals(1, layout.connectors[0].toIndex)
        assertEquals(85.0, layout.connectors[1].fromValue, 0.0)
        assertEquals(85.0, layout.connectors[1].toValue, 0.0)
        assertTrue(layout.maxValue >= 120.0)
        assertTrue(layout.minValue <= 0.0)
        assertEquals(layout.maxValue, layout.ticks.last(), 0.0)
        assertEquals(layout.minValue, waterfallAxisTicks(layout.minValue, layout.maxValue, 1).first(), 0.0)
        assertEquals(0.0, resolveWaterfallBaseline(-4.0, 12.0), 0.0)
        assertEquals(5.0, resolveWaterfallBaseline(5.0, 12.0), 0.0)
    }

    @Test
    fun waterfallLayoutEngine_handlesEmptyAndCollapsedRanges() {
        val emptyLayout = resolveWaterfallChartLayout(emptyList())
        val flatLayout = resolveWaterfallChartLayout(listOf(WaterfallEntry("Flat", 0.0)))

        assertTrue(emptyLayout.entries.isEmpty())
        assertTrue(emptyLayout.connectors.isEmpty())
        assertEquals(-1.0, emptyLayout.minValue, 0.0)
        assertEquals(1.0, emptyLayout.maxValue, 0.0)
        assertEquals(0.0, emptyLayout.baselineValue, 0.0)
        assertEquals(-1.0, emptyLayout.ticks.first(), 0.0)
        assertEquals(1.0, emptyLayout.ticks.last(), 0.0)

        assertEquals(1, flatLayout.entries.size)
        assertEquals("Flat", flatLayout.entries[0].label)
        assertEquals(0.0, flatLayout.entries[0].rawValue, 0.0)
        assertEquals(0.0, flatLayout.entries[0].displayValue, 0.0)
        assertEquals(0.0, flatLayout.entries[0].startValue, 0.0)
        assertEquals(0.0, flatLayout.entries[0].endValue, 0.0)
        assertTrue(flatLayout.connectors.isEmpty())
        assertEquals(-1.0, flatLayout.minValue, 0.0)
        assertEquals(1.0, flatLayout.maxValue, 0.0)
    }

    @Test
    fun waterfallLayoutEngine_appliesDefaultColorsPerEntryKind() {
        val style = WaterfallChartStyleOptions(
            positiveBarColor = 11,
            negativeBarColor = 22,
            subtotalBarColor = 33,
            totalBarColor = 44,
        )
        val layout = resolveWaterfallChartLayout(
            entries = listOf(
                WaterfallEntry("Gain", 5.0),
                WaterfallEntry("Loss", -2.0),
                WaterfallEntry("Subtotal", Double.NaN, kind = WaterfallEntryKind.SUBTOTAL),
                WaterfallEntry("Total", 0.0, kind = WaterfallEntryKind.TOTAL),
            ),
            style = style,
        )

        assertEquals(11, layout.entries[0].color)
        assertEquals(22, layout.entries[1].color)
        assertEquals(33, layout.entries[2].color)
        assertEquals(44, layout.entries[3].color)
    }
}
