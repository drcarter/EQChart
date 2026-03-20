package com.magimon.eq.compose.bar

import android.graphics.Paint
import androidx.compose.ui.geometry.Rect
import com.magimon.eq.bar.BarChartPresentationOptions
import com.magimon.eq.bar.BarChartStyleOptions
import com.magimon.eq.bar.BarDatum
import com.magimon.eq.bar.BarLayoutMode
import com.magimon.eq.bar.BarOrientation
import com.magimon.eq.bar.BarSeries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun computeBarLayout_coversGroupedAndStackedBranches() {
        val series = listOf(
            BarSeries("A", 1, listOf(BarDatum("Jan", 10.0, "a1"), BarDatum("Feb", -4.0, "a2"))),
            BarSeries("B", 2, listOf(BarDatum("Jan", 8.0, "b1"), BarDatum("Feb", 6.0, "b2"))),
        )

        val groupedVertical = computeBarLayout(
            widthPx = 320f,
            heightPx = 220f,
            series = series,
            style = BarChartStyleOptions(),
            presentation = BarChartPresentationOptions(
                layoutMode = BarLayoutMode.GROUPED,
                orientation = BarOrientation.VERTICAL,
                animateOnDataChange = false,
                showLegend = false,
            ),
            progress = 1f,
            density = 1f,
            scaledDensity = 1f,
        )
        val stackedHorizontal = computeBarLayout(
            widthPx = 320f,
            heightPx = 220f,
            series = series,
            style = BarChartStyleOptions(),
            presentation = BarChartPresentationOptions(
                layoutMode = BarLayoutMode.STACKED,
                orientation = BarOrientation.HORIZONTAL,
                animateOnDataChange = false,
                showLegend = false,
            ),
            progress = 0.5f,
            density = 1f,
            scaledDensity = 1f,
        )

        assertEquals(4, groupedVertical.bars.size)
        assertEquals(4, stackedHorizontal.bars.size)
        assertEquals(listOf("Jan", "Feb"), groupedVertical.categories)
        assertTrue(stackedHorizontal.legendTop > stackedHorizontal.chartRect.bottom)
    }

    @Test
    fun buildBars_respectsOrientationAndAnimationDirection() {
        val series = listOf(
            BarSeries("A", 1, listOf(BarDatum("Jan", 10.0, "a1"))),
            BarSeries("B", 2, listOf(BarDatum("Jan", 5.0, "b1"))),
        )
        val chartRect = Rect(0f, 0f, 200f, 100f)

        val animated = buildBars(
            series = series,
            categories = listOf("Jan"),
            style = BarChartStyleOptions(),
            presentation = BarChartPresentationOptions(
                layoutMode = BarLayoutMode.GROUPED,
                orientation = BarOrientation.HORIZONTAL,
                animationDirection = true,
            ),
            styleMin = 0.0,
            styleMax = 10.0,
            progress = 0.5f,
            chartRect = chartRect,
            density = 1f,
        )
        val full = buildBars(
            series = series,
            categories = listOf("Jan"),
            style = BarChartStyleOptions(),
            presentation = BarChartPresentationOptions(
                layoutMode = BarLayoutMode.GROUPED,
                orientation = BarOrientation.HORIZONTAL,
                animationDirection = false,
            ),
            styleMin = 0.0,
            styleMax = 10.0,
            progress = 0.5f,
            chartRect = chartRect,
            density = 1f,
        )

        assertEquals(2, animated.size)
        assertEquals(2, full.size)
        assertTrue(animated.first().right < full.first().right)
    }

    @Test
    fun resolveLegendReserveHeight_wrapsRowsWhenNeeded() {
        val rows = resolveLegendRowCount(
            widthPx = 120f,
            contentPadding = 8f,
            itemWidths = listOf(70f, 72f, 78f),
            itemSpacing = 8f,
            density = 1f,
        )

        assertTrue(rows > 1)
        assertFalse(axisTicks(0.0, 10.0, 5).isEmpty())
        assertEquals(0.0, baselineValue(-5.0, 3.0), 0.0)
    }

    @Test
    fun axisMappingHelpers_coverHorizontalAndVerticalRanges() {
        val rect = Rect(10f, 20f, 110f, 220f)

        assertEquals(220f, valueToY(rect, 0.0, 0.0, 10.0), 0.001f)
        assertEquals(60f, valueToX(rect, 5.0, 0.0, 10.0), 0.001f)
        assertEquals(listOf(0.0, 10.0), axisTicks(0.0, 10.0, 1))
    }
}
