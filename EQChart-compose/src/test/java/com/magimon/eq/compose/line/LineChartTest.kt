package com.magimon.eq.compose.line

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import com.magimon.eq.line.LineChartPresentationOptions
import com.magimon.eq.line.LineChartStyleOptions
import com.magimon.eq.line.LineDatum
import com.magimon.eq.line.LineSeries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LineChartTest {

    @Test
    fun resolveAxisRange_andTicks_expandDegenerateInputs() {
        val range = resolveAxisRange(5.0, 5.0)
        val ticks = buildTicks(range.first, range.second, 4)

        assertTrue(range.first < 5.0)
        assertTrue(range.second > 5.0)
        assertEquals(4, ticks.size)
        assertEquals(0.0, baselineValue(-2.0, 3.0), 0.0)
    }

    @Test
    fun computeLineChart_filtersInvalidPoints_andBuildsLegendReserve() {
        val layout = computeLineChart(
            widthPx = 320f,
            heightPx = 220f,
            series = listOf(
                LineSeries("alpha", 1, listOf(LineDatum(2.0, 4.0), LineDatum(Double.NaN, 1.0), LineDatum(0.0, 2.0))),
                LineSeries("beta", 2, listOf(LineDatum(1.0, 3.0), LineDatum(3.0, 6.0))),
            ),
            style = LineChartStyleOptions(),
            presentation = LineChartPresentationOptions(showLegend = true, xTickCount = 4, yTickCount = 5),
            density = 1f,
            scaledDensity = 1f,
        )

        requireNotNull(layout)
        assertEquals(2, layout.seriesPoints.size)
        assertEquals(4, layout.xTicks.size)
        assertEquals(5, layout.yTicks.size)
        assertTrue(layout.legendTop > layout.chartRect.bottom)
    }

    @Test
    fun mappingAndPathHelpers_coverPartialAnimation() {
        val rect = Rect(10f, 20f, 110f, 120f)
        val points = listOf(
            RenderLinePoint(10f, 120f, 0.0, 0.0, 0, 0, LineDatum(0.0, 0.0)),
            RenderLinePoint(60f, 70f, 1.0, 1.0, 0, 1, LineDatum(1.0, 1.0)),
            RenderLinePoint(110f, 20f, 2.0, 2.0, 0, 2, LineDatum(2.0, 2.0)),
        )
        val path = Path()

        buildAnimatedLinePath(points, 0.5f, path)
        assertEquals(10f, mapX(0.0, 0.0, 2.0, rect), 0.001f)
        assertEquals(120f, mapY(0.0, 0.0, 2.0, rect), 0.001f)
        assertTrue(visiblePointCount(3, 0.5f) in 2..3)
        assertNotEquals(0, withAlpha(0x00FF0000, 128))
    }

    @Test
    fun computeLineChart_returnsNullWhenCanvasOrDataIsInvalid() {
        assertEquals(
            null,
            computeLineChart(
                widthPx = 0f,
                heightPx = 200f,
                series = emptyList(),
                style = LineChartStyleOptions(),
                presentation = LineChartPresentationOptions(),
                density = 1f,
                scaledDensity = 1f,
            ),
        )

        val onlyInvalid = computeLineChart(
            widthPx = 200f,
            heightPx = 120f,
            series = listOf(LineSeries("bad", 1, listOf(LineDatum(Double.NaN, Double.NaN)))),
            style = LineChartStyleOptions(),
            presentation = LineChartPresentationOptions(),
            density = 1f,
            scaledDensity = 1f,
        )

        assertEquals(null, onlyInvalid)
        assertEquals(0, visiblePointCount(0, 0.4f))
    }
}
