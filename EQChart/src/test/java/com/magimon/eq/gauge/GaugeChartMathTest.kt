package com.magimon.eq.gauge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GaugeChartMathTest {

    @Test
    fun resolveValue_clampsOutOfRangeValues() {
        val resolved = GaugeChartMath.resolveValue(
            GaugeValue(value = 130.0, minValue = 0.0, maxValue = 100.0, label = "Load"),
        )

        requireNotNull(resolved)
        assertEquals(100.0, resolved.clampedValue, 0.0)
        assertEquals(1f, resolved.progress, 0.0f)
        assertEquals("Load", resolved.label)
    }

    @Test
    fun resolveValue_returnsNullForInvalidBounds() {
        assertNull(GaugeChartMath.resolveValue(GaugeValue(value = 30.0, minValue = 10.0, maxValue = 10.0)))
        assertNull(GaugeChartMath.resolveValue(GaugeValue(value = Double.NaN, minValue = 0.0, maxValue = 100.0)))
    }

    @Test
    fun resolveRanges_filtersAndClampsRanges() {
        val ranges = GaugeChartMath.resolveRanges(
            ranges = listOf(
                GaugeRange(startValue = -10.0, endValue = 40.0, color = 1),
                GaugeRange(startValue = 80.0, endValue = 120.0, color = 2),
                GaugeRange(startValue = 60.0, endValue = 60.0, color = 3),
            ),
            minValue = 0.0,
            maxValue = 100.0,
        )

        assertEquals(2, ranges.size)
        assertEquals(0f, ranges[0].startRatio, 0.0f)
        assertEquals(0.4f, ranges[0].endRatio, 0.0f)
        assertEquals(0.8f, ranges[1].startRatio, 0.0f)
        assertEquals(1f, ranges[1].endRatio, 0.0f)
    }

    @Test
    fun valueToAngle_andPointOnCircle_areDeterministic() {
        assertEquals(270f, GaugeChartMath.valueToAngle(0.5f, 180f, 180f), 0.0f)

        val point = GaugeChartMath.pointOnCircle(100f, 100f, 40f, 180f)
        assertEquals(60f, point.x, 0.0001f)
        assertEquals(100f, point.y, 0.0001f)
        assertTrue(GaugeChartMath.normalize(50.0, 0.0, 100.0) > 0f)
    }
}
