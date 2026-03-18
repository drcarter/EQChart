package com.magimon.eq.compose

import com.magimon.eq.gauge.GaugeRange
import com.magimon.eq.gauge.GaugeValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GaugeChartTest {

    @Test
    fun resolveComposeGaugeValue_clampsValueIntoRange() {
        val resolved = resolveComposeGaugeValue(
            GaugeValue(
                value = 140.0,
                minValue = 0.0,
                maxValue = 100.0,
                label = "CPU",
            ),
        )

        requireNotNull(resolved)
        assertEquals(100.0, resolved.clampedValue, 0.0)
        assertEquals(1f, resolved.progress, 0.0f)
        assertEquals("CPU", resolved.label)
    }

    @Test
    fun resolveComposeGaugeValue_returnsNullForInvalidDomain() {
        val resolved = resolveComposeGaugeValue(
            GaugeValue(
                value = 50.0,
                minValue = 10.0,
                maxValue = 10.0,
            ),
        )

        assertNull(resolved)
    }

    @Test
    fun resolveComposeGaugeRanges_filtersInvalidRanges() {
        val ranges = resolveComposeGaugeRanges(
            ranges = listOf(
                GaugeRange(0.0, 50.0, 1),
                GaugeRange(60.0, 60.0, 2),
                GaugeRange(70.0, 110.0, 3),
            ),
            minValue = 0.0,
            maxValue = 100.0,
        )

        assertEquals(2, ranges.size)
        assertEquals(0f, ranges[0].startRatio, 0.0f)
        assertEquals(0.5f, ranges[0].endRatio, 0.0f)
        assertEquals(0.7f, ranges[1].startRatio, 0.0f)
        assertEquals(1f, ranges[1].endRatio, 0.0f)
    }

    @Test
    fun formatComposeGaugeNumber_formatsWholeAndFractionalValues() {
        assertEquals("72", formatComposeGaugeNumber(72.0))
        assertEquals("72.5", formatComposeGaugeNumber(72.5))
        assertTrue(composeGaugeAngle(0.5f, 180f, 180f) > 180f)
    }
}
