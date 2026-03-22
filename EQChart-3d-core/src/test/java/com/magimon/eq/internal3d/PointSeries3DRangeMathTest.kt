package com.magimon.eq.internal3d

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for range mapping helpers shared by point-series charts.
 */
class PointSeries3DRangeMathTest {

    @Test
    fun mapToRange_respectsBoundsAndMidpoint() {
        val range = PointSeries3DMath.NumericRange(min = 10.0, max = 30.0)

        assertEquals(4f, PointSeries3DMath.mapToRange(10.0, range, 4f, 16f), 0.0001f)
        assertEquals(10f, PointSeries3DMath.mapToRange(20.0, range, 4f, 16f), 0.0001f)
        assertEquals(16f, PointSeries3DMath.mapToRange(30.0, range, 4f, 16f), 0.0001f)
    }
}
