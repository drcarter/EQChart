package com.magimon.eq.common

import com.magimon.eq.bar.BarChartPresentationOptions
import com.magimon.eq.bar.BarChartStyleOptions
import com.magimon.eq.bar.BarDatum
import com.magimon.eq.bar.BarLayoutMode
import com.magimon.eq.bar.BarOrientation
import com.magimon.eq.bar.BarSeries
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BarContractsTest {

    @Test
    fun barModelsAndEnums_preserveValues() {
        val payload = linkedMapOf("bar" to 1)
        val defaultDatum = BarDatum(category = "Q1", value = 10.0)
        val datum = BarDatum(category = "Q2", value = 25.5, payload = payload)
        val defaultSeries = BarSeries(
            name = "Default",
            color = 0xFF000000.toInt(),
            points = listOf(defaultDatum),
        )
        val series = BarSeries(
            name = "Sales",
            color = 0xFF224466.toInt(),
            points = listOf(defaultDatum, datum),
            payload = payload,
        )
        val copied = series.copy(name = "Copied")

        assertEquals("Q1", defaultDatum.category)
        assertEquals(10.0, defaultDatum.value, 0.0)
        assertNull(defaultDatum.payload)

        assertEquals("Q2", datum.category)
        assertEquals(25.5, datum.value, 0.0)
        assertSame(payload, datum.payload)

        assertEquals("Default", defaultSeries.name)
        assertNull(defaultSeries.payload)

        assertEquals("Sales", series.name)
        assertEquals(0xFF224466.toInt(), series.color)
        assertEquals(2, series.points.size)
        assertSame(payload, series.payload)
        assertEquals("Copied", copied.name)

        assertArrayEquals(arrayOf(BarLayoutMode.GROUPED, BarLayoutMode.STACKED), BarLayoutMode.values())
        assertEquals(BarLayoutMode.STACKED, BarLayoutMode.valueOf("STACKED"))

        assertArrayEquals(arrayOf(BarOrientation.VERTICAL, BarOrientation.HORIZONTAL), BarOrientation.values())
        assertEquals(BarOrientation.HORIZONTAL, BarOrientation.valueOf("HORIZONTAL"))
    }

    @Test
    fun barPresentationDefaultsAndCustomValues_areAccessible() {
        val defaults = BarChartPresentationOptions()
        val custom = BarChartPresentationOptions(
            showLegend = false,
            showGrid = false,
            showAxes = false,
            showBarLabels = false,
            showBars = false,
            animateOnDataChange = false,
            enterAnimationDurationMs = 320L,
            enterAnimationDelayMs = 15L,
            animationDirection = false,
            layoutMode = BarLayoutMode.STACKED,
            orientation = BarOrientation.HORIZONTAL,
            legendTextSizeSp = 13f,
            axisLabelTextSizeSp = 14f,
            emptyText = "EMPTY",
            xLabelFormatter = { value -> "x:$value" },
            yLabelFormatter = { value -> "y=${value.toInt()}" },
            xTickCount = 7,
            yTickCount = 8,
        )

        assertTrue(defaults.showLegend)
        assertTrue(defaults.showGrid)
        assertTrue(defaults.showAxes)
        assertTrue(defaults.showBarLabels)
        assertTrue(defaults.showBars)
        assertTrue(defaults.animateOnDataChange)
        assertEquals(680L, defaults.enterAnimationDurationMs)
        assertEquals(30L, defaults.enterAnimationDelayMs)
        assertTrue(defaults.animationDirection)
        assertEquals(BarLayoutMode.GROUPED, defaults.layoutMode)
        assertEquals(BarOrientation.VERTICAL, defaults.orientation)
        assertEquals(12f, defaults.legendTextSizeSp, 0.0f)
        assertEquals(11.5f, defaults.axisLabelTextSizeSp, 0.0f)
        assertEquals("No data", defaults.emptyText)
        assertEquals("Jan", defaults.xLabelFormatter("Jan"))
        assertEquals("12", defaults.yLabelFormatter(12.9))
        assertEquals(5, defaults.xTickCount)
        assertEquals(6, defaults.yTickCount)

        assertFalse(custom.showLegend)
        assertFalse(custom.showGrid)
        assertFalse(custom.showAxes)
        assertFalse(custom.showBarLabels)
        assertFalse(custom.showBars)
        assertFalse(custom.animateOnDataChange)
        assertEquals(320L, custom.enterAnimationDurationMs)
        assertEquals(15L, custom.enterAnimationDelayMs)
        assertFalse(custom.animationDirection)
        assertEquals(BarLayoutMode.STACKED, custom.layoutMode)
        assertEquals(BarOrientation.HORIZONTAL, custom.orientation)
        assertEquals(13f, custom.legendTextSizeSp, 0.0f)
        assertEquals(14f, custom.axisLabelTextSizeSp, 0.0f)
        assertEquals("EMPTY", custom.emptyText)
        assertEquals("x:Feb", custom.xLabelFormatter("Feb"))
        assertEquals("y=-4", custom.yLabelFormatter(-4.8))
        assertEquals(7, custom.xTickCount)
        assertEquals(8, custom.yTickCount)
    }

    @Test
    fun barStyleDefaultsAndCustomValues_areAccessible() {
        val defaults = BarChartStyleOptions()
        val custom = BarChartStyleOptions(
            backgroundColor = 1,
            gridColor = 2,
            axisColor = 3,
            axisLabelColor = 4,
            legendTextColor = 5,
            barValueTextColor = 6,
            barSpacingDp = 7f,
            categorySpacingDp = 8f,
            barCornerRadiusDp = 9f,
            selectedBarPaddingDp = 10f,
            contentPaddingDp = 11f,
            legendMarkerSizeDp = 12f,
            legendItemSpacingDp = 13f,
            legendRowSpacingDp = 14f,
            legendMarkerTextGapDp = 15f,
        )

        assertEquals(4f, defaults.barSpacingDp, 0.0f)
        assertEquals(12f, defaults.categorySpacingDp, 0.0f)
        assertEquals(3f, defaults.barCornerRadiusDp, 0.0f)
        assertEquals(1.5f, defaults.selectedBarPaddingDp, 0.0f)
        assertEquals(14f, defaults.contentPaddingDp, 0.0f)
        assertEquals(9f, defaults.legendMarkerSizeDp, 0.0f)
        assertEquals(8f, defaults.legendItemSpacingDp, 0.0f)
        assertEquals(6f, defaults.legendRowSpacingDp, 0.0f)
        assertEquals(6f, defaults.legendMarkerTextGapDp, 0.0f)

        assertEquals(1, custom.backgroundColor)
        assertEquals(2, custom.gridColor)
        assertEquals(3, custom.axisColor)
        assertEquals(4, custom.axisLabelColor)
        assertEquals(5, custom.legendTextColor)
        assertEquals(6, custom.barValueTextColor)
        assertEquals(7f, custom.barSpacingDp, 0.0f)
        assertEquals(8f, custom.categorySpacingDp, 0.0f)
        assertEquals(9f, custom.barCornerRadiusDp, 0.0f)
        assertEquals(10f, custom.selectedBarPaddingDp, 0.0f)
        assertEquals(11f, custom.contentPaddingDp, 0.0f)
        assertEquals(12f, custom.legendMarkerSizeDp, 0.0f)
        assertEquals(13f, custom.legendItemSpacingDp, 0.0f)
        assertEquals(14f, custom.legendRowSpacingDp, 0.0f)
        assertEquals(15f, custom.legendMarkerTextGapDp, 0.0f)
    }
}
