package com.magimon.eq.violin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ViolinPlotChartLayoutEngineTest {

    @Test
    fun resolveViolinPlotChartLayout_buildsQuartilesAndDensity() {
        val result = resolveViolinPlotChartLayout(
            series = listOf(
                ViolinPlotSeries("API", listOf(10.0, 12.0, 14.0, 18.0, 24.0), payload = "api"),
                ViolinPlotSeries("Worker", listOf(6.0, 7.0, 9.0, 13.0, 15.0), payload = "worker"),
            ),
            presentation = ViolinPlotChartPresentationOptions(densityPointCount = 24),
        )

        assertEquals(2, result.series.size)
        assertEquals(12.0, result.series.first().q1, 0.0)
        assertEquals(14.0, result.series.first().median, 0.0)
        assertEquals(18.0, result.series.first().q3, 0.0)
        assertEquals(24, result.series.first().densityPoints.size)
        assertEquals("api", result.series.first().payload)
    }

    @Test
    fun resolveViolinPlotChartLayout_filtersInvalidInputAndKeepsEmptyState() {
        val result = resolveViolinPlotChartLayout(
            series = listOf(
                ViolinPlotSeries("", listOf(1.0, 2.0)),
                ViolinPlotSeries("Bad", listOf(Double.NaN)),
            ),
        )

        assertTrue(result.series.isEmpty())
        assertEquals(-1.0, result.minValue, 0.0)
        assertEquals(1.0, result.maxValue, 0.0)
    }

    @Test
    fun resolveViolinPlotChartLayout_usesFallbackDensityForFlatSeries() {
        val result = resolveViolinPlotChartLayout(
            series = listOf(
                ViolinPlotSeries("Flat", listOf(42.0, 42.0, 42.0)),
            ),
            presentation = ViolinPlotChartPresentationOptions(densityPointCount = 20),
        )

        val entry = result.series.single()
        assertEquals(42.0, entry.median, 0.0)
        assertTrue(entry.densityPoints.any { it.widthRatio > 0f })
        assertTrue(entry.densityPoints.first().widthRatio <= 0.001f)
        assertTrue(entry.densityPoints.last().widthRatio <= 0.001f)
    }
}
