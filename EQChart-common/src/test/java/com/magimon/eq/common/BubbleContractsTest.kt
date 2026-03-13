package com.magimon.eq.common

import com.magimon.eq.bubble.BubbleAxisOptions
import com.magimon.eq.bubble.BubbleDatum
import com.magimon.eq.bubble.BubbleLayoutMode
import com.magimon.eq.bubble.BubbleLegendItem
import com.magimon.eq.bubble.BubbleLegendMode
import com.magimon.eq.bubble.BubblePresentationOptions
import com.magimon.eq.bubble.BubbleScaleOverride
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

class BubbleContractsTest {

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
    fun bubbleAxisOptions_defaultsExposeFlagsAndFormatters() {
        val options = BubbleAxisOptions()

        assertTrue(options.showAxes)
        assertTrue(options.showGrid)
        assertTrue(options.showTicks)
        assertEquals("1.0B", options.xLabelFormatter(1_000_000_000.0))
        assertEquals("500", options.yLabelFormatter(500.0))
    }

    @Test
    fun bubbleAxisOptions_defaultFormatter_coversAllThresholdBranches() {
        val formatter = BubbleAxisOptions().xLabelFormatter

        assertEquals("1.0B", formatter(1_000_000_000.0))
        assertEquals("1.5M", formatter(1_500_000.0))
        assertEquals("2.5K", formatter(2_500.0))
        assertEquals("100", formatter(100.0))
        assertEquals("10.5", formatter(10.5))
        assertEquals("9.50", formatter(9.5))
        assertEquals("-2.5K", formatter(-2_500.0))
    }

    @Test
    fun bubbleAxisOptions_customFormattersAreUsed() {
        val options = BubbleAxisOptions(
            showAxes = false,
            showGrid = false,
            showTicks = false,
            xLabelFormatter = { value -> "x=${value.toInt()}" },
            yLabelFormatter = { value -> "y=${value.toInt()}" },
        )

        assertFalse(options.showAxes)
        assertFalse(options.showGrid)
        assertFalse(options.showTicks)
        assertEquals("x=42", options.xLabelFormatter(42.8))
        assertEquals("y=13", options.yLabelFormatter(13.4))
    }

    @Test
    fun bubbleModelsAndEnums_preserveProvidedValues() {
        val payload = linkedMapOf("id" to 7)
        val defaultDatum = BubbleDatum(
            x = 1.0,
            y = 2.0,
            size = 3.0,
            color = 0xFF000000.toInt(),
        )
        val datum = BubbleDatum(
            x = 12.5,
            y = -4.25,
            size = 99.0,
            color = 0xFF336699.toInt(),
            label = "Food",
            legendGroup = "Retail",
            payload = payload,
        )
        val copied = datum.copy(size = 100.0, label = "Copied")
        val legendItem = BubbleLegendItem(
            label = "Group A",
            color = 0xFFAA5500.toInt(),
        )
        val defaultScaleOverride = BubbleScaleOverride()
        val scaleOverride = BubbleScaleOverride(
            xMin = 1.0,
            xMax = 2.0,
            yMin = 3.0,
            yMax = 4.0,
            sizeMin = 5.0,
            sizeMax = 6.0,
        )

        assertEquals(1.0, defaultDatum.x, 0.0)
        assertEquals(2.0, defaultDatum.y, 0.0)
        assertEquals(3.0, defaultDatum.size, 0.0)
        assertEquals(0xFF000000.toInt(), defaultDatum.color)
        assertNull(defaultDatum.label)
        assertNull(defaultDatum.legendGroup)
        assertNull(defaultDatum.payload)

        assertEquals(12.5, datum.x, 0.0)
        assertEquals(-4.25, datum.y, 0.0)
        assertEquals(99.0, datum.size, 0.0)
        assertEquals(0xFF336699.toInt(), datum.color)
        assertEquals("Food", datum.label)
        assertEquals("Retail", datum.legendGroup)
        assertSame(payload, datum.payload)

        assertEquals(100.0, copied.size, 0.0)
        assertEquals("Copied", copied.label)
        assertSame(payload, copied.payload)

        assertEquals("Group A", legendItem.label)
        assertEquals(0xFFAA5500.toInt(), legendItem.color)

        assertNull(defaultScaleOverride.xMin)
        assertNull(defaultScaleOverride.xMax)
        assertNull(defaultScaleOverride.yMin)
        assertNull(defaultScaleOverride.yMax)
        assertNull(defaultScaleOverride.sizeMin)
        assertNull(defaultScaleOverride.sizeMax)

        assertEquals(1.0, requireNotNull(scaleOverride.xMin), 0.0)
        assertEquals(2.0, requireNotNull(scaleOverride.xMax), 0.0)
        assertEquals(3.0, requireNotNull(scaleOverride.yMin), 0.0)
        assertEquals(4.0, requireNotNull(scaleOverride.yMax), 0.0)
        assertEquals(5.0, requireNotNull(scaleOverride.sizeMin), 0.0)
        assertEquals(6.0, requireNotNull(scaleOverride.sizeMax), 0.0)

        assertArrayEquals(
            arrayOf(BubbleLayoutMode.SCATTER, BubbleLayoutMode.PACKED),
            BubbleLayoutMode.values(),
        )
        assertEquals(BubbleLayoutMode.PACKED, BubbleLayoutMode.valueOf("PACKED"))

        assertArrayEquals(
            arrayOf(BubbleLegendMode.AUTO, BubbleLegendMode.EXPLICIT, BubbleLegendMode.AUTO_WITH_OVERRIDE),
            BubbleLegendMode.values(),
        )
        assertEquals(BubbleLegendMode.AUTO_WITH_OVERRIDE, BubbleLegendMode.valueOf("AUTO_WITH_OVERRIDE"))
    }

    @Test
    fun bubblePresentationOptions_defaultsAndCustomValues_areAccessible() {
        val defaults = BubblePresentationOptions()
        val custom = BubblePresentationOptions(
            title = "Loans",
            showLegend = true,
            legendMode = BubbleLegendMode.EXPLICIT,
            titleColor = 0xFF123456.toInt(),
            titleTextSizeSp = 18f,
            titleBottomSpacingDp = 10f,
            legendTextColor = 0xFF654321.toInt(),
            legendTextSizeSp = 14f,
            legendMarkerSizeDp = 12f,
            legendItemSpacingDp = 7f,
            legendSectionTopPaddingDp = 9f,
            legendBottomMarginDp = 11f,
            legendLeftMarginDp = 13f,
            legendMarkerTextGapDp = 5f,
        )
        val defaultTitleColor = defaults.titleColor
        val defaultLegendTextColor = defaults.legendTextColor

        assertNull(defaults.title)
        assertFalse(defaults.showLegend)
        assertEquals(BubbleLegendMode.AUTO, defaults.legendMode)
        assertEquals(defaultTitleColor, defaults.titleColor)
        assertEquals(20f, defaults.titleTextSizeSp, 0.0f)
        assertEquals(8f, defaults.titleBottomSpacingDp, 0.0f)
        assertEquals(defaultLegendTextColor, defaults.legendTextColor)
        assertEquals(12f, defaults.legendTextSizeSp, 0.0f)
        assertEquals(10f, defaults.legendMarkerSizeDp, 0.0f)
        assertEquals(6f, defaults.legendItemSpacingDp, 0.0f)
        assertEquals(8f, defaults.legendSectionTopPaddingDp, 0.0f)
        assertEquals(8f, defaults.legendBottomMarginDp, 0.0f)
        assertEquals(8f, defaults.legendLeftMarginDp, 0.0f)
        assertEquals(6f, defaults.legendMarkerTextGapDp, 0.0f)

        assertEquals("Loans", custom.title)
        assertTrue(custom.showLegend)
        assertEquals(BubbleLegendMode.EXPLICIT, custom.legendMode)
        assertEquals(0xFF123456.toInt(), custom.titleColor)
        assertEquals(18f, custom.titleTextSizeSp, 0.0f)
        assertEquals(10f, custom.titleBottomSpacingDp, 0.0f)
        assertEquals(0xFF654321.toInt(), custom.legendTextColor)
        assertEquals(14f, custom.legendTextSizeSp, 0.0f)
        assertEquals(12f, custom.legendMarkerSizeDp, 0.0f)
        assertEquals(7f, custom.legendItemSpacingDp, 0.0f)
        assertEquals(9f, custom.legendSectionTopPaddingDp, 0.0f)
        assertEquals(11f, custom.legendBottomMarginDp, 0.0f)
        assertEquals(13f, custom.legendLeftMarginDp, 0.0f)
        assertEquals(5f, custom.legendMarkerTextGapDp, 0.0f)
    }
}
