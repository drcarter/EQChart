package com.magimon.eq.bubble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BubbleChartMathTest {

    @Test
    fun resolveRange_usesAutoMinMax() {
        val range = BubbleChartMath.resolveRange(listOf(-5.0, 2.0, 8.0), null, null)
        assertEquals(-5.0, range.min, 0.0001)
        assertEquals(8.0, range.max, 0.0001)
    }

    @Test
    fun resolveRange_addsPaddingWhenValuesAreEqual() {
        val range = BubbleChartMath.resolveRange(listOf(10.0, 10.0), null, null)
        assertTrue(range.min < 10.0)
        assertTrue(range.max > 10.0)
        assertTrue(range.span > 0.0)
    }

    @Test
    fun resolveRange_appliesOverrides() {
        val range = BubbleChartMath.resolveRange(
            values = listOf(1.0, 2.0, 3.0),
            overrideMin = 0.0,
            overrideMax = 10.0,
        )
        assertEquals(0.0, range.min, 0.0001)
        assertEquals(10.0, range.max, 0.0001)
    }

    @Test
    fun mapRadius_isMonotonic() {
        val range = BubbleChartMath.NumericRange(0.0, 100.0)
        val minRadius = 8f
        val maxRadius = 40f

        val rSmall = BubbleChartMath.mapRadius(10.0, range, minRadius, maxRadius)
        val rMid = BubbleChartMath.mapRadius(50.0, range, minRadius, maxRadius)
        val rLarge = BubbleChartMath.mapRadius(90.0, range, minRadius, maxRadius)

        assertTrue(rSmall < rMid)
        assertTrue(rMid < rLarge)
    }

    @Test
    fun mapCoordinates_stayInsideOutputBounds() {
        val xRange = BubbleChartMath.NumericRange(-1.0, 1.0)
        val yRange = BubbleChartMath.NumericRange(0.0, 100.0)
        val outMin = 20f
        val outMax = 220f

        val x = BubbleChartMath.mapLinear(0.2, xRange, outMin, outMax)
        val y = BubbleChartMath.mapLinearInverted(60.0, yRange, 10f, 110f)

        assertTrue(x in outMin..outMax)
        assertTrue(y in 10f..110f)
    }
}
