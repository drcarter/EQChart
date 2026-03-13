package com.magimon.eq.compose

import org.junit.Assert.assertEquals
import org.junit.Test

class BarChartTest {

    @Test
    fun resolveBarValueRange_includesZeroForPositiveSeries() {
        val range = resolveBarValueRange(listOf(12.0, 18.0, 24.0))

        assertEquals(0.0, range.first, 0.0)
        assertEquals(24.0, range.second, 0.0)
    }

    @Test
    fun resolveBarValueRange_includesZeroForNegativeSeries() {
        val range = resolveBarValueRange(listOf(-9.0, -4.0, -1.0))

        assertEquals(-9.0, range.first, 0.0)
        assertEquals(0.0, range.second, 0.0)
    }

    @Test
    fun resolveBarValueRange_preservesMixedSeriesRange() {
        val range = resolveBarValueRange(listOf(-5.0, 3.0, 11.0))

        assertEquals(-5.0, range.first, 0.0)
        assertEquals(11.0, range.second, 0.0)
    }
}
