package com.magimon.eq.compose.radar

import com.magimon.eq.radar.RadarAxis
import com.magimon.eq.radar.RadarChartPresentationOptions
import com.magimon.eq.radar.RadarChartStyleOptions
import com.magimon.eq.radar.RadarSeries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RadarChartTest {

    @Test
    fun computeRadarChart_filtersInvalidSeries_andBuildsLegend() {
        val computed = computeRadarChart(
            width = 360f,
            height = 300f,
            axes = listOf(RadarAxis("A"), RadarAxis("B"), RadarAxis("C"), RadarAxis("D")),
            series = listOf(
                RadarSeries("one", 0xFFE53935.toInt(), listOf(70.0, 65.0, 80.0, 50.0)),
                RadarSeries("bad", 0xFF000000.toInt(), listOf(1.0, 2.0)),
            ),
            valueMax = 100.0,
            styleOptions = RadarChartStyleOptions(),
            presentationOptions = RadarChartPresentationOptions(showLegend = true, showAxisLabels = true),
            density = 1f,
            scaledDensity = 1f,
        )

        assertEquals(1, computed.series.size)
        assertEquals(4, computed.axes.size)
        assertFalse(computed.legendItems.isEmpty())
        assertTrue(computed.radius > 0f)
    }

    @Test
    fun radarHelpers_coverGeometryAndSelection() {
        val vertex = radarVertex(100f, 100f, 50f, 1, 4, -90f)
        val points = radarPolygonPoints(
            values = listOf(100.0, 50.0, Double.NaN, 0.0),
            maxValue = 100.0,
            centerX = 100f,
            centerY = 100f,
            radius = 80f,
            axisCount = 4,
            progress = 1f,
            startAngleDeg = -90f,
        )
        val hit = radarNearestPoint(vertex.x, vertex.y, listOf(points), 20f)

        assertTrue(radarNormalize(50.0, 100.0) > 0f)
        assertEquals(4, points.size)
        assertNotNull(hit)
        assertEquals(0, hit?.seriesIndex)
        assertNotEquals(0, radarApplyAlpha(0xFF00FF00.toInt(), 100))
    }

    @Test
    fun computeRadarChart_returnsNoRenderableSeriesForShortAxisLists() {
        val computed = computeRadarChart(
            width = 240f,
            height = 200f,
            axes = listOf(RadarAxis("A"), RadarAxis("B")),
            series = listOf(RadarSeries("one", 0xFFE53935.toInt(), listOf(70.0, 65.0))),
            valueMax = -1.0,
            styleOptions = RadarChartStyleOptions(),
            presentationOptions = RadarChartPresentationOptions(showLegend = true, showAxisLabels = false),
            density = 1f,
            scaledDensity = 1f,
        )

        assertTrue(computed.series.isEmpty())
        assertEquals(0f, radarNormalize(10.0, 0.0), 0f)
        assertEquals(null, radarNearestPoint(0f, 0f, emptyList(), 12f))
    }
}
