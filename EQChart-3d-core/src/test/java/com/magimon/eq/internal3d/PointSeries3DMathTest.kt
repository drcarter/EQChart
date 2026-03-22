package com.magimon.eq.internal3d

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the shared math helpers that drive point-series 3D charts.
 */
class PointSeries3DMathTest {

    @Test
    fun resolveRange_expandsCollapsedValues() {
        val range = PointSeries3DMath.resolveRange(
            values = listOf(5.0, 5.0, 5.0),
            overrideMin = null,
            overrideMax = null,
        )

        assertTrue(range.min < 5.0)
        assertTrue(range.max > 5.0)
        assertTrue(range.span > 0.0)
    }

    @Test
    fun mapToWorld_mapsEndpointsAndMidpoint() {
        val valueRange = PointSeries3DMath.NumericRange(min = 10.0, max = 30.0)

        assertEquals(-1f, PointSeries3DMath.mapToWorld(10.0, valueRange), 0.0001f)
        assertEquals(0f, PointSeries3DMath.mapToWorld(20.0, valueRange), 0.0001f)
        assertEquals(1f, PointSeries3DMath.mapToWorld(30.0, valueRange), 0.0001f)
    }

    @Test
    fun buildViewProjectionAndProjectionToScreen_returnVisibleCoordinates() {
        val matrix = PointSeries3DMath.buildViewProjectionMatrix(
            camera = PointSeries3DMath.OrbitCamera(
                yawDegrees = -34f,
                pitchDegrees = 20f,
                distance = 4.8f,
                minDistance = 2.4f,
                maxDistance = 12f,
                fovDegrees = 45f,
            ),
            aspectRatio = 1f,
        )
        val eyePosition = PointSeries3DMath.computeEyePosition(
            PointSeries3DMath.OrbitCamera(
                yawDegrees = -34f,
                pitchDegrees = 20f,
                distance = 4.8f,
                minDistance = 2.4f,
                maxDistance = 12f,
                fovDegrees = 45f,
            ),
        )

        assertEquals(16, matrix.size)
        assertEquals(3, eyePosition.size)

        val visiblePoint = PointSeries3DMath.projectWorldToScreen(
            x = 0.4f,
            y = 0.2f,
            z = 0.3f,
            viewProjectionMatrix = matrix,
            viewportWidth = 400,
            viewportHeight = 400,
        )

        visiblePoint?.let { point ->
            assertTrue(point.x in 0f..400f)
            assertTrue(point.y in 0f..400f)
            assertTrue(point.depth in -1f..1f)
        }

        val invalidViewportPoint = PointSeries3DMath.projectWorldToScreen(
            x = 0f,
            y = 0f,
            z = 0f,
            viewProjectionMatrix = matrix,
            viewportWidth = 0,
            viewportHeight = 400,
        )

        assertNull(invalidViewportPoint)
    }
}
