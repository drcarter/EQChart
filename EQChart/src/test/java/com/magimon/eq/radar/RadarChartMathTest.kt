package com.magimon.eq.radar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RadarChartMathTest {

    @Test
    fun normalize_clampsToRange() {
        assertEquals(0f, RadarChartMath.normalize(-10.0, 100.0), 0.0001f)
        assertEquals(0.5f, RadarChartMath.normalize(50.0, 100.0), 0.0001f)
        assertEquals(1f, RadarChartMath.normalize(180.0, 100.0), 0.0001f)
    }

    @Test
    fun vertex_axisZeroIsAtTopByDefault() {
        val point = RadarChartMath.vertex(
            centerX = 100f,
            centerY = 100f,
            radius = 50f,
            axisIndex = 0,
            axisCount = 5,
        )
        assertEquals(100f, point.x, 0.01f)
        assertEquals(50f, point.y, 0.01f)
    }

    @Test
    fun polygonPoints_matchesAxisCount() {
        val points = RadarChartMath.polygonPoints(
            values = listOf(100.0, 80.0, 60.0, 40.0, 20.0),
            maxValue = 100.0,
            centerX = 0f,
            centerY = 0f,
            radius = 100f,
            axisCount = 5,
            progress = 1f,
        )
        assertEquals(5, points.size)
        assertTrue(points[0].y < 0f)
    }

    @Test
    fun nearestPoint_returnsClosestHitWithinRadius() {
        val pointsBySeries = listOf(
            listOf(
                RadarChartMath.Vec2(10f, 10f),
                RadarChartMath.Vec2(20f, 20f),
            ),
            listOf(
                RadarChartMath.Vec2(100f, 100f),
            ),
        )

        val hit = RadarChartMath.nearestPoint(
            touchX = 11f,
            touchY = 11f,
            pointsBySeries = pointsBySeries,
            hitRadiusPx = 8f,
        )

        assertNotNull(hit)
        assertEquals(0, hit?.seriesIndex)
        assertEquals(0, hit?.axisIndex)
    }

    @Test
    fun nearestPoint_returnsNullOutsideRadius() {
        val pointsBySeries = listOf(
            listOf(RadarChartMath.Vec2(10f, 10f)),
        )

        val hit = RadarChartMath.nearestPoint(
            touchX = 100f,
            touchY = 100f,
            pointsBySeries = pointsBySeries,
            hitRadiusPx = 10f,
        )
        assertNull(hit)
    }

    @Test
    fun normalize_returnsZeroForInvalidInputs() {
        assertEquals(0f, RadarChartMath.normalize(Double.NaN, 100.0), 0.0001f)
        assertEquals(0f, RadarChartMath.normalize(10.0, 0.0), 0.0001f)
        assertEquals(0f, RadarChartMath.normalize(10.0, Double.NaN), 0.0001f)
    }

    @Test
    fun vertex_returnsCenterWhenAxisCountIsInvalid() {
        val point = RadarChartMath.vertex(
            centerX = 12f,
            centerY = 24f,
            radius = 50f,
            axisIndex = 0,
            axisCount = 0,
        )

        assertEquals(12f, point.x, 0.0001f)
        assertEquals(24f, point.y, 0.0001f)
    }

    @Test
    fun polygonPoints_returnsEmptyWithoutRenderableAxesOrValues() {
        assertTrue(
            RadarChartMath.polygonPoints(
                values = emptyList(),
                maxValue = 100.0,
                centerX = 0f,
                centerY = 0f,
                radius = 100f,
                axisCount = 5,
                progress = 1f,
            ).isEmpty(),
        )
        assertTrue(
            RadarChartMath.polygonPoints(
                values = listOf(1.0, 2.0),
                maxValue = 100.0,
                centerX = 0f,
                centerY = 0f,
                radius = 100f,
                axisCount = 0,
                progress = 1f,
            ).isEmpty(),
        )
    }
}
