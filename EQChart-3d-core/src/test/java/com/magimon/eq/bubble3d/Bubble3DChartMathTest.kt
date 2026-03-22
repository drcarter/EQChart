package com.magimon.eq.bubble3d

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the math helpers that drive the Bubble3D renderer and overlay.
 */
class Bubble3DChartMathTest {

    @Test
    fun resolveRange_expandsCollapsedValues() {
        val range = Bubble3DChartMath.resolveRange(
            values = listOf(5.0, 5.0, 5.0),
            overrideMin = null,
            overrideMax = null,
        )

        assertTrue(range.min < 5.0)
        assertTrue(range.max > 5.0)
        assertTrue(range.span > 0.0)
    }

    @Test
    fun worldMappingAndRadiusMapping_stayWithinExpectedBounds() {
        val valueRange = Bubble3DChartMath.NumericRange(min = 10.0, max = 30.0)
        val sizeRange = Bubble3DChartMath.NumericRange(min = 100.0, max = 400.0)

        assertEquals(-1f, Bubble3DChartMath.mapToWorld(10.0, valueRange), 0.0001f)
        assertEquals(0f, Bubble3DChartMath.mapToWorld(20.0, valueRange), 0.0001f)
        assertEquals(1f, Bubble3DChartMath.mapToWorld(30.0, valueRange), 0.0001f)

        val radius = Bubble3DChartMath.mapRadius(
            sizeValue = 250.0,
            sizeRange = sizeRange,
            minRadius = 0.08f,
            maxRadius = 0.26f,
        )
        assertTrue(radius in 0.08f..0.26f)

        val clamped = Bubble3DChartMath.clampWorldCoordinate(value = 0.95f, radius = 0.2f)
        assertEquals(0.8f, clamped, 0.0001f)
    }

    @Test
    fun projectWorldToScreen_returnsViewportCoordinatesForVisiblePoints() {
        val matrix = Bubble3DChartMath.buildViewProjectionMatrix(
            camera = Bubble3DCameraOptions(),
            aspectRatio = 1f,
        )
        val eyePosition = Bubble3DChartMath.computeEyePosition(Bubble3DCameraOptions())

        assertEquals(16, matrix.size)
        assertEquals(3, eyePosition.size)

        val visiblePoint = Bubble3DChartMath.projectWorldToScreen(
            x = 0.5f,
            y = 0.5f,
            z = 0.5f,
            viewProjectionMatrix = matrix,
            viewportWidth = 400,
            viewportHeight = 400,
        )

        visiblePoint?.let { point ->
            assertTrue(point.x in 0f..400f)
            assertTrue(point.y in 0f..400f)
        }

        val invalidViewportPoint = Bubble3DChartMath.projectWorldToScreen(
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
