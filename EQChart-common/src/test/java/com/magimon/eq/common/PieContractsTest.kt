package com.magimon.eq.common

import com.magimon.eq.pie.PieDonutPresentationOptions
import com.magimon.eq.pie.PieDonutStyleOptions
import com.magimon.eq.pie.PieLabelPosition
import com.magimon.eq.pie.PieSlice
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PieContractsTest {

    @Test
    fun pieLabelPosition_enumValuesAreAccessible() {
        assertArrayEquals(
            arrayOf(PieLabelPosition.INSIDE, PieLabelPosition.OUTSIDE, PieLabelPosition.AUTO),
            PieLabelPosition.values(),
        )
        assertEquals(PieLabelPosition.OUTSIDE, PieLabelPosition.valueOf("OUTSIDE"))
    }

    @Test
    fun pieSlice_preservesPropertiesAndCopy() {
        val payload = mutableMapOf("channel" to "direct")
        val defaultPayloadSlice = PieSlice(
            label = "Search",
            value = 26.0,
            color = 0xFFFF9F1C.toInt(),
        )
        val slice = PieSlice(
            label = "Direct",
            value = 43.0,
            color = 0xFF2B80FF.toInt(),
            payload = payload,
        )
        val copied = slice.copy(value = 18.0, label = "Social")

        assertEquals("Search", defaultPayloadSlice.label)
        assertEquals(26.0, defaultPayloadSlice.value, 0.0)
        assertEquals(0xFFFF9F1C.toInt(), defaultPayloadSlice.color)
        assertNull(defaultPayloadSlice.payload)

        assertEquals("Direct", slice.label)
        assertEquals(43.0, slice.value, 0.0)
        assertEquals(0xFF2B80FF.toInt(), slice.color)
        assertSame(payload, slice.payload)

        assertEquals("Social", copied.label)
        assertEquals(18.0, copied.value, 0.0)
        assertSame(payload, copied.payload)
    }

    @Test
    fun pieDonutPresentationOptions_defaultsAndCustomValues_areAccessible() {
        val defaults = PieDonutPresentationOptions()
        val custom = PieDonutPresentationOptions(
            showLegend = false,
            showLabels = false,
            labelPosition = PieLabelPosition.INSIDE,
            enableSelectionExpand = true,
            selectedSliceExpandDp = 12f,
            selectedSliceExpandAnimMs = 240L,
            startAngleDeg = 180f,
            clockwise = false,
            animateOnDataChange = false,
            enterAnimationDurationMs = 500L,
            enterAnimationDelayMs = 32L,
            legendTopMarginDp = 10f,
            legendBottomMarginDp = 11f,
            legendLeftMarginDp = 12f,
            centerText = "TOTAL",
            centerSubText = "42",
            emptyText = "EMPTY",
        )

        assertTrue(defaults.showLegend)
        assertTrue(defaults.showLabels)
        assertEquals(PieLabelPosition.AUTO, defaults.labelPosition)
        assertFalse(defaults.enableSelectionExpand)
        assertEquals(8f, defaults.selectedSliceExpandDp, 0.0f)
        assertEquals(140L, defaults.selectedSliceExpandAnimMs)
        assertEquals(-90f, defaults.startAngleDeg, 0.0f)
        assertTrue(defaults.clockwise)
        assertTrue(defaults.animateOnDataChange)
        assertEquals(650L, defaults.enterAnimationDurationMs)
        assertEquals(0L, defaults.enterAnimationDelayMs)
        assertEquals(8f, defaults.legendTopMarginDp, 0.0f)
        assertEquals(8f, defaults.legendBottomMarginDp, 0.0f)
        assertEquals(4f, defaults.legendLeftMarginDp, 0.0f)
        assertNull(defaults.centerText)
        assertNull(defaults.centerSubText)
        assertEquals("No data", defaults.emptyText)

        assertFalse(custom.showLegend)
        assertFalse(custom.showLabels)
        assertEquals(PieLabelPosition.INSIDE, custom.labelPosition)
        assertTrue(custom.enableSelectionExpand)
        assertEquals(12f, custom.selectedSliceExpandDp, 0.0f)
        assertEquals(240L, custom.selectedSliceExpandAnimMs)
        assertEquals(180f, custom.startAngleDeg, 0.0f)
        assertFalse(custom.clockwise)
        assertFalse(custom.animateOnDataChange)
        assertEquals(500L, custom.enterAnimationDurationMs)
        assertEquals(32L, custom.enterAnimationDelayMs)
        assertEquals(10f, custom.legendTopMarginDp, 0.0f)
        assertEquals(11f, custom.legendBottomMarginDp, 0.0f)
        assertEquals(12f, custom.legendLeftMarginDp, 0.0f)
        assertEquals("TOTAL", custom.centerText)
        assertEquals("42", custom.centerSubText)
        assertEquals("EMPTY", custom.emptyText)
    }

    @Test
    fun pieDonutStyleOptions_defaultsAndCustomValues_areAccessible() {
        val defaults = PieDonutStyleOptions()
        val custom = PieDonutStyleOptions(
            backgroundColor = 0xFFF7FAFC.toInt(),
            sliceStrokeColor = 0xFF101820.toInt(),
            sliceStrokeWidthDp = 2.2f,
            labelTextColor = 0xFF111111.toInt(),
            labelTextSizeSp = 14f,
            labelLineColor = 0xFF222222.toInt(),
            legendTextColor = 0xFF333333.toInt(),
            legendTextSizeSp = 15f,
            legendMarkerSizeDp = 11f,
            legendItemSpacingDp = 9f,
            legendRowSpacingDp = 7f,
            legendMarkerTextGapDp = 5f,
            contentPaddingDp = 13f,
            centerTextColor = 0xFF444444.toInt(),
            centerTextSizeSp = 20f,
            centerSubTextColor = 0xFF555555.toInt(),
            centerSubTextSizeSp = 16f,
        )
        val defaultBackgroundColor = defaults.backgroundColor
        val defaultSliceStrokeColor = defaults.sliceStrokeColor
        val defaultLabelTextColor = defaults.labelTextColor
        val defaultLabelLineColor = defaults.labelLineColor
        val defaultLegendTextColor = defaults.legendTextColor
        val defaultCenterTextColor = defaults.centerTextColor
        val defaultCenterSubTextColor = defaults.centerSubTextColor

        assertEquals(defaultBackgroundColor, defaults.backgroundColor)
        assertEquals(defaultSliceStrokeColor, defaults.sliceStrokeColor)
        assertEquals(1.6f, defaults.sliceStrokeWidthDp, 0.0f)
        assertEquals(defaultLabelTextColor, defaults.labelTextColor)
        assertEquals(12f, defaults.labelTextSizeSp, 0.0f)
        assertEquals(defaultLabelLineColor, defaults.labelLineColor)
        assertEquals(defaultLegendTextColor, defaults.legendTextColor)
        assertEquals(12f, defaults.legendTextSizeSp, 0.0f)
        assertEquals(10f, defaults.legendMarkerSizeDp, 0.0f)
        assertEquals(8f, defaults.legendItemSpacingDp, 0.0f)
        assertEquals(8f, defaults.legendRowSpacingDp, 0.0f)
        assertEquals(6f, defaults.legendMarkerTextGapDp, 0.0f)
        assertEquals(12f, defaults.contentPaddingDp, 0.0f)
        assertEquals(defaultCenterTextColor, defaults.centerTextColor)
        assertEquals(18f, defaults.centerTextSizeSp, 0.0f)
        assertEquals(defaultCenterSubTextColor, defaults.centerSubTextColor)
        assertEquals(12f, defaults.centerSubTextSizeSp, 0.0f)

        assertEquals(0xFFF7FAFC.toInt(), custom.backgroundColor)
        assertEquals(0xFF101820.toInt(), custom.sliceStrokeColor)
        assertEquals(2.2f, custom.sliceStrokeWidthDp, 0.0f)
        assertEquals(0xFF111111.toInt(), custom.labelTextColor)
        assertEquals(14f, custom.labelTextSizeSp, 0.0f)
        assertEquals(0xFF222222.toInt(), custom.labelLineColor)
        assertEquals(0xFF333333.toInt(), custom.legendTextColor)
        assertEquals(15f, custom.legendTextSizeSp, 0.0f)
        assertEquals(11f, custom.legendMarkerSizeDp, 0.0f)
        assertEquals(9f, custom.legendItemSpacingDp, 0.0f)
        assertEquals(7f, custom.legendRowSpacingDp, 0.0f)
        assertEquals(5f, custom.legendMarkerTextGapDp, 0.0f)
        assertEquals(13f, custom.contentPaddingDp, 0.0f)
        assertEquals(0xFF444444.toInt(), custom.centerTextColor)
        assertEquals(20f, custom.centerTextSizeSp, 0.0f)
        assertEquals(0xFF555555.toInt(), custom.centerSubTextColor)
        assertEquals(16f, custom.centerSubTextSizeSp, 0.0f)
    }
}
