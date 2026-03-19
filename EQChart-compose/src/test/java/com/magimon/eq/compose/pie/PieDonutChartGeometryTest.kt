package com.magimon.eq.compose.pie

import com.magimon.eq.compose.internal.degreeToOffset

import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import com.magimon.eq.pie.PieDonutPresentationOptions
import com.magimon.eq.pie.PieDonutStyleOptions
import com.magimon.eq.pie.PieLabelPosition
import com.magimon.eq.pie.PieSlice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PieDonutChartGeometryTest {

    @Test
    fun buildPieSegments_filtersInvalidValues_andCompletesFullCircle() {
        val segments = buildPieSegments(
            slices = listOf(
                PieSlice("A", 40.0, 0xFF1E88E5.toInt()),
                PieSlice("B", 60.0, 0xFF43A047.toInt()),
                PieSlice("bad-zero", 0.0, 0xFF000000.toInt()),
                PieSlice("bad-nan", Double.NaN, 0xFF000000.toInt()),
            ),
            startAngle = 270f,
            clockwise = false,
        )

        assertEquals(2, segments.size)
        assertTrue(segments.first().sweep < 0f)
        assertEquals(360f, segments.sumOf { kotlin.math.abs(it.sweep).toDouble() }.toFloat(), 0.001f)
        assertEquals(0.4f, segments.first().ratio, 0.001f)
    }

    @Test
    fun computePieChartGeometry_reservesLegendHeight_andSupportsHitTesting() {
        val density = Density(1f, 1f)
        val segments = buildPieSegments(
            slices = listOf(
                PieSlice("Alpha", 50.0, 0xFF1E88E5.toInt(), payload = "alpha"),
                PieSlice("Beta", 50.0, 0xFF43A047.toInt(), payload = "beta"),
            ),
            startAngle = 0f,
            clockwise = true,
        )
        val geometry = computePieChartGeometry(
            availableWidth = 200f,
            availableHeight = 180f,
            segments = segments,
            styleOptions = PieDonutStyleOptions(),
            presentationOptions = PieDonutPresentationOptions(showLegend = true),
            density = density,
            legendTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 12f },
            donutInnerRatio = 0.5f,
        )

        assertTrue(geometry.legendReservedHeight > 0f)
        assertTrue(geometry.radius > geometry.innerRadius)

        val secondSliceTap = geometry.center + degreeToOffset(225f, geometry.radius * 0.8f)
        val hit = hitTestPieSegment(secondSliceTap, geometry, segments)

        assertNotNull(hit)
        assertEquals("Beta", hit?.slice?.label)
        assertNull(hitTestPieSegment(geometry.center, geometry, segments))
        assertNull(hitTestPieSegment(geometry.center + Offset(geometry.radius + 8f, 0f), geometry, segments))
    }

    @Test
    fun labelPlacement_helpers_followPolicies() {
        val wideSegment = PieSegment(
            index = 0,
            slice = PieSlice("Wide", 70.0, 0xFF1E88E5.toInt()),
            ratio = 0.7f,
            start = 0f,
            sweep = 252f,
            mid = 126f,
        )
        val narrowSegment = wideSegment.copy(index = 1, sweep = 12f, mid = 6f)

        assertTrue(
            shouldPlacePieLabelInside(
                segment = wideSegment,
                labelPosition = PieLabelPosition.INSIDE,
                midRadius = 48f,
                textWidth = 200f,
            ),
        )
        assertFalse(
            shouldPlacePieLabelInside(
                segment = wideSegment,
                labelPosition = PieLabelPosition.OUTSIDE,
                midRadius = 48f,
                textWidth = 10f,
            ),
        )
        assertTrue(
            shouldPlacePieLabelInside(
                segment = wideSegment,
                labelPosition = PieLabelPosition.AUTO,
                midRadius = 48f,
                textWidth = 40f,
            ),
        )
        assertFalse(
            shouldPlacePieLabelInside(
                segment = narrowSegment,
                labelPosition = PieLabelPosition.AUTO,
                midRadius = 48f,
                textWidth = 40f,
            ),
        )
        assertTrue(isPieLabelOnRightSide(10f))
        assertFalse(isPieLabelOnRightSide(180f))
    }
}
