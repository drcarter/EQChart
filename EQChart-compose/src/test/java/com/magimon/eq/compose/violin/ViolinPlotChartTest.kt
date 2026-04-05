package com.magimon.eq.compose.violin

import com.magimon.eq.violin.ViolinPlotChartPresentationOptions
import com.magimon.eq.violin.ViolinPlotChartStyleOptions
import com.magimon.eq.violin.ViolinPlotSeries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ViolinPlotChartTest {

    @Test
    fun computeViolinPlotLayout_buildsRenderEntries() {
        val computed = computeViolinPlotLayout(
            widthPx = 360f,
            heightPx = 240f,
            series = listOf(
                ViolinPlotSeries("API", listOf(10.0, 12.0, 16.0, 22.0)),
                ViolinPlotSeries("Worker", listOf(8.0, 9.0, 11.0, 14.0)),
            ),
            style = ViolinPlotChartStyleOptions(),
            presentation = ViolinPlotChartPresentationOptions(animateOnDataChange = false),
            progress = 1f,
            density = 1f,
            scaledDensity = 1f,
        )

        assertEquals(2, computed.entries.size)
        assertEquals(8.0, computed.minValue, 0.0)
        assertEquals(22.0, computed.maxValue, 0.0)
        assertTrue(computed.entries.first().outlinePoints.isNotEmpty())
    }

    @Test
    fun computeViolinPlotLayout_returnsEmptyForInvalidBounds() {
        val computed = computeViolinPlotLayout(
            widthPx = 0f,
            heightPx = 240f,
            series = listOf(
                ViolinPlotSeries("API", listOf(10.0, 12.0, 16.0, 22.0)),
            ),
            style = ViolinPlotChartStyleOptions(),
            presentation = ViolinPlotChartPresentationOptions(animateOnDataChange = false),
            progress = 1f,
            density = 1f,
            scaledDensity = 1f,
        )

        assertFalse(computed.entries.isNotEmpty())
        assertTrue(computed.entries.isEmpty())
    }
}
