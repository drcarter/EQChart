package com.magimon.eq.compose.calendarheatmap

import com.magimon.eq.calendarheatmap.CalendarHeatmapChartPresentationOptions
import com.magimon.eq.calendarheatmap.CalendarHeatmapChartStyleOptions
import com.magimon.eq.calendarheatmap.CalendarHeatmapData
import com.magimon.eq.calendarheatmap.CalendarHeatmapDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CalendarHeatmapChartTest {

    @Test
    fun computeCalendarHeatmapLayout_buildsYearGridAndLabels() {
        val computed = computeCalendarHeatmapLayout(
            widthPx = 760f,
            heightPx = 220f,
            data = CalendarHeatmapData(
                year = 2025,
                days = listOf(
                    CalendarHeatmapDay(month = 1, dayOfMonth = 1, value = 3.0),
                    CalendarHeatmapDay(month = 4, dayOfMonth = 7, value = 9.0),
                ),
            ),
            style = CalendarHeatmapChartStyleOptions(),
            presentation = CalendarHeatmapChartPresentationOptions(),
            density = 1f,
            scaledDensity = 1f,
        )

        assertTrue(computed.isRenderable)
        assertEquals(365, computed.dayCells.size)
        assertTrue(computed.dayCells.any { it.value == 9.0 })
    }

    @Test
    fun computeCalendarHeatmapLayout_returnsNonRenderableForInvalidBounds() {
        val computed = computeCalendarHeatmapLayout(
            widthPx = 0f,
            heightPx = 220f,
            data = CalendarHeatmapData(
                year = 2025,
                days = listOf(CalendarHeatmapDay(month = 1, dayOfMonth = 1, value = 3.0)),
            ),
            style = CalendarHeatmapChartStyleOptions(),
            presentation = CalendarHeatmapChartPresentationOptions(),
            density = 1f,
            scaledDensity = 1f,
        )

        assertFalse(computed.isRenderable)
        assertTrue(computed.dayCells.isEmpty())
    }
}
