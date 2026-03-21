package com.magimon.eq.common

import android.graphics.Color
import com.magimon.eq.histogram.HistogramBin
import com.magimon.eq.histogram.HistogramChartPresentationOptions
import com.magimon.eq.histogram.HistogramChartStyleOptions
import com.magimon.eq.histogram.histogramAxisTicks
import com.magimon.eq.histogram.resolveHistogramBaseline
import com.magimon.eq.histogram.resolveHistogramChartLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HistogramContractsTest {

    @Test
    fun histogramBin_preservesValuesAndDefaults() {
        val payload = linkedMapOf("bin" to 1)
        val defaultBin = HistogramBin(start = 0.0, end = 10.0, value = 4.0)
        val custom = HistogramBin(
            start = 10.0,
            end = 20.0,
            value = 8.0,
            label = "10-20",
            color = 0xFF224466.toInt(),
            payload = payload,
        )
        val copied = custom.copy(value = 9.0)

        assertEquals(0.0, defaultBin.start, 0.0)
        assertEquals(10.0, defaultBin.end, 0.0)
        assertEquals(4.0, defaultBin.value, 0.0)
        assertNull(defaultBin.label)
        assertNull(defaultBin.color)
        assertNull(defaultBin.payload)

        assertEquals("10-20", custom.label)
        assertEquals(0xFF224466.toInt(), custom.color)
        assertSame(payload, custom.payload)
        assertEquals(9.0, copied.value, 0.0)
    }

    @Test
    fun histogramPresentationDefaultsAndCustomValues_areAccessible() {
        val defaults = HistogramChartPresentationOptions()
        val custom = HistogramChartPresentationOptions(
            showGrid = false,
            showAxes = false,
            showBarLabels = false,
            animateOnDataChange = false,
            enterAnimationDurationMs = 320L,
            enterAnimationDelayMs = 15L,
            animationDirection = false,
            emptyText = "EMPTY",
            axisLabelTextSizeSp = 13f,
            valueLabelTextSizeSp = 14f,
            yLabelFormatter = { value -> "y:${value.toInt()}" },
            valueLabelFormatter = { value -> "v:${value.toInt()}" },
            binLabelFormatter = { bin -> "bin:${bin.start.toInt()}" },
            yTickCount = 7,
        )

        assertTrue(defaults.showGrid)
        assertTrue(defaults.showAxes)
        assertTrue(defaults.showBarLabels)
        assertTrue(defaults.animateOnDataChange)
        assertTrue(defaults.animationDirection)
        assertEquals(680L, defaults.enterAnimationDurationMs)
        assertEquals(30L, defaults.enterAnimationDelayMs)
        assertEquals("No data", defaults.emptyText)
        assertEquals(11.5f, defaults.axisLabelTextSizeSp, 0.0f)
        assertEquals(11.5f, defaults.valueLabelTextSizeSp, 0.0f)
        assertEquals("3", defaults.yLabelFormatter(3.9))
        assertEquals("7", defaults.valueLabelFormatter(7.1))
        assertEquals("0-10", defaults.binLabelFormatter(HistogramBin(0.0, 10.0, 3.0)))
        assertEquals("0.5-1.25", defaults.binLabelFormatter(HistogramBin(0.5, 1.25, 3.0)))
        assertEquals(6, defaults.yTickCount)

        assertFalse(custom.showGrid)
        assertFalse(custom.showAxes)
        assertFalse(custom.showBarLabels)
        assertFalse(custom.animateOnDataChange)
        assertFalse(custom.animationDirection)
        assertEquals(320L, custom.enterAnimationDurationMs)
        assertEquals(15L, custom.enterAnimationDelayMs)
        assertEquals("EMPTY", custom.emptyText)
        assertEquals(13f, custom.axisLabelTextSizeSp, 0.0f)
        assertEquals(14f, custom.valueLabelTextSizeSp, 0.0f)
        assertEquals("y:4", custom.yLabelFormatter(4.8))
        assertEquals("v:5", custom.valueLabelFormatter(5.2))
        assertEquals("bin:10", custom.binLabelFormatter(HistogramBin(10.0, 20.0, 1.0)))
        assertEquals(7, custom.yTickCount)
    }

    @Test
    fun histogramStyleDefaultsAndCustomValues_areAccessible() {
        val defaults = HistogramChartStyleOptions()
        val custom = HistogramChartStyleOptions(
            backgroundColor = 1,
            gridColor = 2,
            axisColor = 3,
            axisLabelColor = 4,
            barValueTextColor = 5,
            barColor = 6,
            categorySpacingDp = 7f,
            barCornerRadiusDp = 8f,
            selectedBarPaddingDp = 9f,
            contentPaddingDp = 10f,
        )

        assertEquals(Color.WHITE, defaults.backgroundColor)
        assertEquals(Color.parseColor("#D7DFE8"), defaults.gridColor)
        assertEquals(Color.parseColor("#8F9CAB"), defaults.axisColor)
        assertEquals(Color.parseColor("#5E6878"), defaults.axisLabelColor)
        assertEquals(Color.parseColor("#263244"), defaults.barValueTextColor)
        assertEquals(Color.parseColor("#2B80FF"), defaults.barColor)
        assertEquals(6f, defaults.categorySpacingDp, 0.0f)
        assertEquals(3f, defaults.barCornerRadiusDp, 0.0f)
        assertEquals(1.5f, defaults.selectedBarPaddingDp, 0.0f)
        assertEquals(14f, defaults.contentPaddingDp, 0.0f)

        assertEquals(1, custom.backgroundColor)
        assertEquals(6, custom.barColor)
        assertEquals(7f, custom.categorySpacingDp, 0.0f)
        assertEquals(10f, custom.contentPaddingDp, 0.0f)
    }

    @Test
    fun histogramLayoutEngine_buildsLabelsAndFiltersInvalidBins() {
        val original = HistogramBin(
            start = 10.0,
            end = 20.0,
            value = 9.0,
            label = "10-20",
            color = 0xFF224466.toInt(),
            payload = "b",
        )
        val layout = resolveHistogramChartLayout(
            bins = listOf(
                HistogramBin(0.0, 10.0, 4.0),
                original,
                HistogramBin(Double.NEGATIVE_INFINITY, 0.0, 1.0),
                HistogramBin(20.0, Double.POSITIVE_INFINITY, 2.0),
                HistogramBin(20.0, 20.0, 2.0),
                HistogramBin(20.0, 30.0, Double.NaN),
                HistogramBin(30.0, 40.0, 1.0, label = " "),
            ),
        )

        assertEquals(2, layout.bins.size)
        assertEquals(0, layout.bins[0].index)
        assertEquals("0-10", layout.bins[0].label)
        assertEquals(HistogramChartStyleOptions().barColor, layout.bins[0].color)
        assertEquals("10-20", layout.bins[1].label)
        assertEquals(0xFF224466.toInt(), layout.bins[1].color)
        assertSame(original, layout.bins[1].sourceBin)
        assertEquals(0.0, layout.minBinStart, 0.0)
        assertEquals(20.0, layout.maxBinEnd, 0.0)
        assertEquals(0.0, layout.baselineValue, 0.0)
        assertEquals(9.0, layout.maxValue, 0.0)
        assertEquals(0.0, layout.minValue, 0.0)
        assertEquals("b", layout.bins[1].payload)
        assertEquals(layout.minValue, histogramAxisTicks(layout.minValue, layout.maxValue, 1).first(), 0.0)
        assertEquals(0.0, resolveHistogramBaseline(-4.0, 12.0), 0.0)
        assertEquals(5.0, resolveHistogramBaseline(5.0, 12.0), 0.0)
    }

    @Test
    fun histogramLayoutEngine_handlesEmptyAndFlatRanges() {
        val emptyLayout = resolveHistogramChartLayout(
            bins = listOf(
                HistogramBin(Double.NaN, 1.0, 2.0),
                HistogramBin(0.0, Double.POSITIVE_INFINITY, 2.0),
                HistogramBin(0.0, 1.0, Double.NaN),
                HistogramBin(3.0, 3.0, 1.0),
                HistogramBin(4.0, 5.0, 1.0, label = "   "),
            ),
            presentation = HistogramChartPresentationOptions(yTickCount = 4),
        )

        assertTrue(emptyLayout.bins.isEmpty())
        assertEquals(0.0, emptyLayout.minBinStart, 0.0)
        assertEquals(1.0, emptyLayout.maxBinEnd, 0.0)
        assertEquals(-1.0, emptyLayout.minValue, 0.0)
        assertEquals(1.0, emptyLayout.maxValue, 0.0)
        assertEquals(0.0, emptyLayout.baselineValue, 0.0)
        assertEquals(4, emptyLayout.ticks.size)
        assertEquals(-1.0, emptyLayout.ticks.first(), 0.0)
        assertEquals(1.0, emptyLayout.ticks.last(), 0.0)

        val flatSource = HistogramBin(
            start = -2.0,
            end = 1.5,
            value = 0.0,
            color = 0xFF123456.toInt(),
            payload = "flat",
        )
        val flatLayout = resolveHistogramChartLayout(
            bins = listOf(flatSource),
            style = HistogramChartStyleOptions(barColor = 0xFFABCDEF.toInt()),
        )
        val flatBin = flatLayout.bins.single()

        assertEquals(0, flatBin.index)
        assertEquals(-2.0, flatBin.start, 0.0)
        assertEquals(1.5, flatBin.end, 0.0)
        assertEquals(0.0, flatBin.value, 0.0)
        assertEquals("-2-1.5", flatBin.label)
        assertEquals(0xFF123456.toInt(), flatBin.color)
        assertSame(flatSource, flatBin.sourceBin)
        assertEquals("flat", flatBin.payload)
        assertEquals(-2.0, flatLayout.minBinStart, 0.0)
        assertEquals(1.5, flatLayout.maxBinEnd, 0.0)
        assertEquals(-1.0, flatLayout.minValue, 0.0)
        assertEquals(1.0, flatLayout.maxValue, 0.0)
        assertEquals(0.0, flatLayout.baselineValue, 0.0)
    }

    @Test
    fun histogramHelpers_generateExpectedNegativeBaselineAndTicks() {
        val ticks = histogramAxisTicks(2.0, 8.0, 4)

        assertEquals(-5.0, resolveHistogramBaseline(-5.0, -1.0), 0.0)
        assertEquals(4, ticks.size)
        assertEquals(2.0, ticks[0], 0.0)
        assertEquals(4.0, ticks[1], 0.0)
        assertEquals(6.0, ticks[2], 0.0)
        assertEquals(8.0, ticks[3], 0.0)
    }

    @Test
    fun histogramLayoutEngine_resolvesUnsortedNegativeBins() {
        val layout = resolveHistogramChartLayout(
            bins = listOf(
                HistogramBin(10.0, 12.0, -3.0),
                HistogramBin(0.0, 20.0, -1.0),
            ),
        )

        assertEquals(0.0, layout.minBinStart, 0.0)
        assertEquals(20.0, layout.maxBinEnd, 0.0)
        assertEquals(-3.0, layout.minValue, 0.0)
        assertEquals(0.0, layout.maxValue, 0.0)
        assertEquals(0.0, layout.baselineValue, 0.0)
        assertEquals(2, layout.bins.size)
    }

    @Test
    fun histogramLayoutEngine_keepsExistingExtremaWhenLaterBinsStayInsideRange() {
        val layout = resolveHistogramChartLayout(
            bins = listOf(
                HistogramBin(10.0, 20.0, 5.0),
                HistogramBin(0.0, 12.0, 1.0),
            ),
        )

        assertEquals(0.0, layout.minBinStart, 0.0)
        assertEquals(20.0, layout.maxBinEnd, 0.0)
        assertEquals(0.0, layout.minValue, 0.0)
        assertEquals(5.0, layout.maxValue, 0.0)
        assertEquals(0.0, layout.baselineValue, 0.0)
    }
}
