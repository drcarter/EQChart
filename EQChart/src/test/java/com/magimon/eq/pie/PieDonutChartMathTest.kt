package com.magimon.eq.pie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PieDonutChartMathTest {

    @Test
    fun buildSegments_sumsToFullCircle_clockwise() {
        val segments = PieDonutChartMath.buildSegments(
            slices = listOf(
                PieSlice("A", 1.0, 0),
                PieSlice("B", 1.0, 0),
                PieSlice("C", 2.0, 0),
            ),
            startAngleDeg = -90f,
            clockwise = true,
        )

        assertEquals(3, segments.size)
        val total = segments.sumOf { kotlin.math.abs(it.sweepAngleDeg.toDouble()) }
        assertEquals(360.0, total, 0.001)
        assertEquals(0.25f, segments[0].ratio, 0.0001f)
        assertEquals(0.5f, segments[2].ratio, 0.0001f)
    }

    @Test
    fun buildSegments_setsNegativeSweep_whenCounterClockwise() {
        val segments = PieDonutChartMath.buildSegments(
            slices = listOf(
                PieSlice("A", 1.0, 0),
                PieSlice("B", 1.0, 0),
            ),
            startAngleDeg = 0f,
            clockwise = false,
        )

        assertTrue(segments.all { it.sweepAngleDeg < 0f })
    }

    @Test
    fun buildSegments_filtersInvalidValues() {
        val segments = PieDonutChartMath.buildSegments(
            slices = listOf(
                PieSlice("A", 10.0, 0),
                PieSlice("B", -1.0, 0),
                PieSlice("C", Double.NaN, 0),
                PieSlice("D", 5.0, 0),
            ),
            startAngleDeg = 0f,
            clockwise = true,
        )

        assertEquals(2, segments.size)
        assertEquals("A", segments[0].slice.label)
        assertEquals("D", segments[1].slice.label)
    }

    @Test
    fun hitTest_returnsNullInsideDonutHole() {
        val segments = PieDonutChartMath.buildSegments(
            slices = listOf(PieSlice("A", 1.0, 0)),
            startAngleDeg = -90f,
            clockwise = true,
        )

        val hit = PieDonutChartMath.hitTest(
            touchX = 100f,
            touchY = 100f,
            centerX = 100f,
            centerY = 100f,
            innerRadius = 24f,
            outerRadius = 80f,
            segments = segments,
        )

        assertNull(hit)
    }

    @Test
    fun hitTest_resolvesSliceIndexByAngle() {
        val segments = PieDonutChartMath.buildSegments(
            slices = listOf(
                PieSlice("A", 1.0, 0),
                PieSlice("B", 1.0, 0),
            ),
            startAngleDeg = -90f,
            clockwise = true,
        )

        val first = PieDonutChartMath.hitTest(
            touchX = 140f,
            touchY = 80f,
            centerX = 100f,
            centerY = 100f,
            innerRadius = 0f,
            outerRadius = 80f,
            segments = segments,
        )
        val second = PieDonutChartMath.hitTest(
            touchX = 60f,
            touchY = 120f,
            centerX = 100f,
            centerY = 100f,
            innerRadius = 0f,
            outerRadius = 80f,
            segments = segments,
        )

        assertNotNull(first)
        assertNotNull(second)
        assertEquals(0, first?.index)
        assertEquals(1, second?.index)
    }

    @Test
    fun validSlices_keepsOnlyFinitePositiveValues() {
        val valid = PieDonutChartMath.validSlices(
            listOf(
                PieSlice("A", 1.0, 0),
                PieSlice("B", 0.0, 0),
                PieSlice("C", -3.0, 0),
                PieSlice("D", Double.POSITIVE_INFINITY, 0),
                PieSlice("E", 2.0, 0),
            ),
        )

        assertEquals(listOf("A", "E"), valid.map { it.label })
    }

    @Test
    fun pointOnCircle_andNormalizeAngle_returnExpectedCoordinates() {
        val point = PieDonutChartMath.pointOnCircle(10f, 10f, 5f, 90f)

        assertEquals(10f, point.x, 0.0001f)
        assertEquals(15f, point.y, 0.0001f)
        assertEquals(315f, PieDonutChartMath.normalizeAngle(-45f), 0.0001f)
        assertEquals(5f, PieDonutChartMath.normalizeAngle(725f), 0.0001f)
    }

    @Test
    fun containsAngle_handlesZeroAndCounterClockwiseSweeps() {
        assertFalse(PieDonutChartMath.containsAngle(10f, 0f, 0f))
        assertTrue(PieDonutChartMath.containsAngle(350f, 0f, -20f))
        assertFalse(PieDonutChartMath.containsAngle(320f, 0f, -20f))
    }
}
