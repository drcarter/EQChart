package com.magimon.eq.common

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
        assertEquals("No data", defaults.emptyText)
        assertEquals("0-10", defaults.binLabelFormatter(HistogramBin(0.0, 10.0, 3.0)))
        assertEquals(6, defaults.yTickCount)

        assertFalse(custom.showGrid)
        assertFalse(custom.showAxes)
        assertFalse(custom.showBarLabels)
        assertFalse(custom.animateOnDataChange)
        assertFalse(custom.animationDirection)
        assertEquals("EMPTY", custom.emptyText)
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
        val layout = resolveHistogramChartLayout(
            bins = listOf(
                HistogramBin(0.0, 10.0, 4.0),
                HistogramBin(10.0, 20.0, 9.0, label = "10-20", payload = "b"),
                HistogramBin(20.0, 20.0, 2.0),
                HistogramBin(20.0, 30.0, Double.NaN),
            ),
        )

        assertEquals(2, layout.bins.size)
        assertEquals("0-10", layout.bins[0].label)
        assertEquals("10-20", layout.bins[1].label)
        assertEquals(0.0, layout.baselineValue, 0.0)
        assertEquals(9.0, layout.maxValue, 0.0)
        assertEquals(0.0, layout.minValue, 0.0)
        assertEquals("b", layout.bins[1].payload)
        assertEquals(layout.minValue, histogramAxisTicks(layout.minValue, layout.maxValue, 1).first(), 0.0)
        assertEquals(0.0, resolveHistogramBaseline(-4.0, 12.0), 0.0)
    }
}
