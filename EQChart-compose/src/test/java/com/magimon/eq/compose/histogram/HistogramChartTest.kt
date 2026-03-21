package com.magimon.eq.compose.histogram

import com.magimon.eq.histogram.HistogramBin
import com.magimon.eq.histogram.HistogramChartPresentationOptions
import com.magimon.eq.histogram.HistogramChartStyleOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistogramChartTest {

    @Test
    fun computeHistogramLayout_buildsBarsForValidBins() {
        val computed = computeHistogramLayout(
            widthPx = 360f,
            heightPx = 240f,
            bins = listOf(
                HistogramBin(0.0, 10.0, 4.0, payload = "a"),
                HistogramBin(10.0, 20.0, 9.0, payload = "b"),
                HistogramBin(20.0, 30.0, 3.0, payload = "c"),
            ),
            style = HistogramChartStyleOptions(),
            presentation = HistogramChartPresentationOptions(
                animateOnDataChange = false,
            ),
            progress = 1f,
            density = 1f,
            scaledDensity = 1f,
        )

        assertEquals(3, computed.bars.size)
        assertEquals(9.0, computed.maxValue, 0.0)
        assertEquals(0.0, computed.baselineValue, 0.0)
        assertTrue(computed.bars[1].bottom > computed.bars[1].top)
    }

    @Test
    fun computeHistogramLayout_filtersInvalidBinsAndKeepsEmptyState() {
        val computed = computeHistogramLayout(
            widthPx = 300f,
            heightPx = 220f,
            bins = listOf(
                HistogramBin(0.0, 0.0, 12.0),
                HistogramBin(10.0, 20.0, Double.NaN),
            ),
            style = HistogramChartStyleOptions(),
            presentation = HistogramChartPresentationOptions(animateOnDataChange = false),
            progress = 1f,
            density = 1f,
            scaledDensity = 1f,
        )

        assertTrue(computed.bars.isEmpty())
        assertEquals(-1.0, computed.minValue, 0.0)
        assertEquals(1.0, computed.maxValue, 0.0)
    }
}
