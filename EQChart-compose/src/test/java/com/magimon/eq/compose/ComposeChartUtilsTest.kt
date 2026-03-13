package com.magimon.eq.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposeChartUtilsTest {

    @Test
    fun normalizeAngle_wrapsPositiveAndNegativeValues() {
        assertEquals(10f, normalizeAngle(370f), 0.0001f)
        assertEquals(315f, normalizeAngle(-45f), 0.0001f)
        assertEquals(0f, normalizeAngle(720f), 0.0001f)
    }

    @Test
    fun isAngleInSweep_handlesClockwiseAndCounterClockwise() {
        assertTrue(isAngleInSweep(angleDeg = 20f, startDeg = 0f, sweepDeg = 30f))
        assertFalse(isAngleInSweep(angleDeg = 40f, startDeg = 0f, sweepDeg = 30f))

        assertTrue(isAngleInSweep(angleDeg = 340f, startDeg = 0f, sweepDeg = -30f))
        assertFalse(isAngleInSweep(angleDeg = 320f, startDeg = 0f, sweepDeg = -30f))
    }

    @Test
    fun degreeToOffset_returnsExpectedCardinalPoints() {
        val right = degreeToOffset(angleDeg = 0f, radius = 10f)
        val down = degreeToOffset(angleDeg = 90f, radius = 10f)

        assertEquals(10f, right.x, 0.0001f)
        assertEquals(0f, right.y, 0.0001f)

        assertEquals(0f, down.x, 0.0001f)
        assertEquals(10f, down.y, 0.0001f)
    }

    @Test
    fun toComposeColor_preservesArgbValue() {
        val color = 0xFF336699.toInt().toComposeColor()

        assertEquals(1f, color.alpha, 0.0001f)
        assertEquals(0x33 / 255f, color.red, 0.0001f)
        assertEquals(0x66 / 255f, color.green, 0.0001f)
        assertEquals(0x99 / 255f, color.blue, 0.0001f)
    }
}
