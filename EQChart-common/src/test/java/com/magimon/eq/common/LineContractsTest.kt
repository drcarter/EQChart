package com.magimon.eq.common

import com.magimon.eq.line.LineChartPresentationOptions
import com.magimon.eq.line.LineChartStyleOptions
import com.magimon.eq.line.LineDatum
import com.magimon.eq.line.LineSeries
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

class LineContractsTest {

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
    fun lineModels_preserveValuesAndCopy() {
        val payload = linkedMapOf("point" to 3)
        val defaultDatum = LineDatum(x = 1.0, y = 2.0)
        val datum = LineDatum(x = 12.5, y = -4.25, payload = payload)
        val defaultSeries = LineSeries(
            name = "Default",
            color = 0xFF000000.toInt(),
            points = listOf(defaultDatum),
        )
        val series = LineSeries(
            name = "Revenue",
            color = 0xFF336699.toInt(),
            points = listOf(defaultDatum, datum),
            payload = payload,
            areaFillColor = 0x55223344,
        )
        val copied = series.copy(name = "Copied")

        assertEquals(1.0, defaultDatum.x, 0.0)
        assertEquals(2.0, defaultDatum.y, 0.0)
        assertNull(defaultDatum.payload)

        assertEquals(12.5, datum.x, 0.0)
        assertEquals(-4.25, datum.y, 0.0)
        assertSame(payload, datum.payload)

        assertEquals("Default", defaultSeries.name)
        assertNull(defaultSeries.payload)
        assertNull(defaultSeries.areaFillColor)

        assertEquals("Revenue", series.name)
        assertEquals(0xFF336699.toInt(), series.color)
        assertEquals(2, series.points.size)
        assertSame(payload, series.payload)
        assertEquals(0x55223344, requireNotNull(series.areaFillColor).toInt())

        assertEquals("Copied", copied.name)
        assertEquals(0x55223344, requireNotNull(copied.areaFillColor).toInt())
    }

    @Test
    fun linePresentationDefaults_coverFormatterBranches() {
        val options = LineChartPresentationOptions()

        assertTrue(options.showLegend)
        assertTrue(options.showGrid)
        assertTrue(options.showAxes)
        assertTrue(options.showPoints)
        assertFalse(options.showAreaFill)
        assertTrue(options.animateOnDataChange)
        assertEquals(700L, options.enterAnimationDurationMs)
        assertEquals(30L, options.enterAnimationDelayMs)
        assertEquals(12f, options.legendTextSizeSp, 0.0f)
        assertEquals(12f, options.axisLabelTextSizeSp, 0.0f)
        assertEquals(4f, options.legendLeftMarginDp, 0.0f)
        assertEquals(4f, options.legendTopMarginDp, 0.0f)
        assertEquals(10f, options.legendBottomMarginDp, 0.0f)
        assertEquals("No data", options.emptyText)
        assertEquals("8", options.xLabelFormatter(8.0))
        assertEquals("8.4", options.xLabelFormatter(8.44))
        assertEquals("3", options.yLabelFormatter(3.0))
        assertEquals("3.5", options.yLabelFormatter(3.5))
        assertEquals(6, options.xTickCount)
        assertEquals(6, options.yTickCount)
    }

    @Test
    fun linePresentationCustomValues_areAccessible() {
        val options = LineChartPresentationOptions(
            showLegend = false,
            showGrid = false,
            showAxes = false,
            showPoints = false,
            showAreaFill = true,
            animateOnDataChange = false,
            enterAnimationDurationMs = 420L,
            enterAnimationDelayMs = 12L,
            legendTextSizeSp = 14f,
            axisLabelTextSizeSp = 13f,
            legendLeftMarginDp = 5f,
            legendTopMarginDp = 6f,
            legendBottomMarginDp = 7f,
            emptyText = "EMPTY",
            xLabelFormatter = { value -> "x=${value.toInt()}" },
            yLabelFormatter = { value -> "y=${value.toInt()}" },
            xTickCount = 4,
            yTickCount = 5,
        )

        assertFalse(options.showLegend)
        assertFalse(options.showGrid)
        assertFalse(options.showAxes)
        assertFalse(options.showPoints)
        assertTrue(options.showAreaFill)
        assertFalse(options.animateOnDataChange)
        assertEquals(420L, options.enterAnimationDurationMs)
        assertEquals(12L, options.enterAnimationDelayMs)
        assertEquals(14f, options.legendTextSizeSp, 0.0f)
        assertEquals(13f, options.axisLabelTextSizeSp, 0.0f)
        assertEquals(5f, options.legendLeftMarginDp, 0.0f)
        assertEquals(6f, options.legendTopMarginDp, 0.0f)
        assertEquals(7f, options.legendBottomMarginDp, 0.0f)
        assertEquals("EMPTY", options.emptyText)
        assertEquals("x=9", options.xLabelFormatter(9.7))
        assertEquals("y=-4", options.yLabelFormatter(-4.2))
        assertEquals(4, options.xTickCount)
        assertEquals(5, options.yTickCount)
    }

    @Test
    fun lineStyleDefaultsAndCustomValues_areAccessible() {
        val defaults = LineChartStyleOptions()
        val custom = LineChartStyleOptions(
            backgroundColor = 1,
            gridColor = 2,
            axisColor = 3,
            axisLabelColor = 4,
            legendTextColor = 5,
            lineStrokeWidthDp = 6f,
            pointRadiusDp = 7f,
            selectedPointRadiusDp = 8f,
            selectedStrokeWidthDp = 9f,
            areaFillAlpha = 100,
            pointLabelTextSizeSp = 10f,
            legendTextSizeSp = 11f,
            legendMarkerSizeDp = 12f,
            legendItemSpacingDp = 13f,
            legendRowSpacingDp = 14f,
            legendMarkerTextGapDp = 15f,
            contentPaddingDp = 16f,
            axisLabelOffsetDp = 17f,
            touchHitRadiusDp = 18f,
        )

        assertEquals(2.2f, defaults.lineStrokeWidthDp, 0.0f)
        assertEquals(3.2f, defaults.pointRadiusDp, 0.0f)
        assertEquals(5.5f, defaults.selectedPointRadiusDp, 0.0f)
        assertEquals(2.3f, defaults.selectedStrokeWidthDp, 0.0f)
        assertEquals(78, defaults.areaFillAlpha)
        assertEquals(11f, defaults.pointLabelTextSizeSp, 0.0f)
        assertEquals(12f, defaults.legendTextSizeSp, 0.0f)
        assertEquals(9f, defaults.legendMarkerSizeDp, 0.0f)
        assertEquals(8f, defaults.legendItemSpacingDp, 0.0f)
        assertEquals(6f, defaults.legendRowSpacingDp, 0.0f)
        assertEquals(6f, defaults.legendMarkerTextGapDp, 0.0f)
        assertEquals(14f, defaults.contentPaddingDp, 0.0f)
        assertEquals(15f, defaults.axisLabelOffsetDp, 0.0f)
        assertEquals(15f, defaults.touchHitRadiusDp, 0.0f)

        assertEquals(1, custom.backgroundColor)
        assertEquals(2, custom.gridColor)
        assertEquals(3, custom.axisColor)
        assertEquals(4, custom.axisLabelColor)
        assertEquals(5, custom.legendTextColor)
        assertEquals(6f, custom.lineStrokeWidthDp, 0.0f)
        assertEquals(7f, custom.pointRadiusDp, 0.0f)
        assertEquals(8f, custom.selectedPointRadiusDp, 0.0f)
        assertEquals(9f, custom.selectedStrokeWidthDp, 0.0f)
        assertEquals(100, custom.areaFillAlpha)
        assertEquals(10f, custom.pointLabelTextSizeSp, 0.0f)
        assertEquals(11f, custom.legendTextSizeSp, 0.0f)
        assertEquals(12f, custom.legendMarkerSizeDp, 0.0f)
        assertEquals(13f, custom.legendItemSpacingDp, 0.0f)
        assertEquals(14f, custom.legendRowSpacingDp, 0.0f)
        assertEquals(15f, custom.legendMarkerTextGapDp, 0.0f)
        assertEquals(16f, custom.contentPaddingDp, 0.0f)
        assertEquals(17f, custom.axisLabelOffsetDp, 0.0f)
        assertEquals(18f, custom.touchHitRadiusDp, 0.0f)
    }
}
