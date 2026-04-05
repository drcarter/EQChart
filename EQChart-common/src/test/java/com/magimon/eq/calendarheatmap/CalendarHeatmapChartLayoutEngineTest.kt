package com.magimon.eq.calendarheatmap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarHeatmapChartLayoutEngineTest {

    @Test
    fun resolveCalendarHeatmapChartLayout_mapsDaysIntoSundayFirstWeeks() {
        val layout = resolveCalendarHeatmapChartLayout(
            data = CalendarHeatmapData(
                year = 2026,
                days = listOf(
                    CalendarHeatmapDay(month = 1, dayOfMonth = 1, value = 1.0, payload = "jan-1"),
                    CalendarHeatmapDay(month = 1, dayOfMonth = 4, value = 2.0, payload = "jan-4"),
                ),
            ),
            config = CalendarHeatmapChartLayoutConfig(
                widthPx = 530f,
                heightPx = 180f,
                contentPaddingPx = 12f,
                monthLabelHeightPx = 16f,
                weekdayLabelWidthPx = 24f,
                labelGapPx = 6f,
                cellGapPx = 2f,
            ),
        )

        assertTrue(layout.isRenderable)
        val jan1 = layout.dayCells.first { it.month == 1 && it.dayOfMonth == 1 }
        val jan4 = layout.dayCells.first { it.month == 1 && it.dayOfMonth == 4 }
        assertEquals(0, jan1.weekIndex)
        assertEquals(4, jan1.weekdayIndex)
        assertEquals(1, jan4.weekIndex)
        assertEquals(0, jan4.weekdayIndex)
    }

    @Test
    fun resolveCalendarHeatmapChartLayout_dropsInvalidDatesAndUsesLastDuplicate() {
        val layout = resolveCalendarHeatmapChartLayout(
            data = CalendarHeatmapData(
                year = 2024,
                days = listOf(
                    CalendarHeatmapDay(2, 29, 3.0, payload = "keep"),
                    CalendarHeatmapDay(2, 29, 7.0, payload = "replace"),
                    CalendarHeatmapDay(2, 30, 5.0, payload = "invalid"),
                    CalendarHeatmapDay(4, 1, -1.0, payload = "negative"),
                ),
            ),
            config = CalendarHeatmapChartLayoutConfig(
                widthPx = 530f,
                heightPx = 180f,
                contentPaddingPx = 12f,
                monthLabelHeightPx = 16f,
                weekdayLabelWidthPx = 24f,
                labelGapPx = 6f,
                cellGapPx = 2f,
            ),
        )

        val leap = layout.dayCells.first { it.month == 2 && it.dayOfMonth == 29 }
        val aprilFirst = layout.dayCells.first { it.month == 4 && it.dayOfMonth == 1 }

        assertEquals(7.0, leap.value!!, 0.0)
        assertEquals("replace", leap.day?.payload)
        assertNull(aprilFirst.value)
        assertNull(aprilFirst.day)
    }

    @Test
    fun resolveCalendarHeatmapChartLayout_rendersWholeYearEvenWhenDataIsEmpty() {
        val layout = resolveCalendarHeatmapChartLayout(
            data = CalendarHeatmapData(year = 2025, days = emptyList()),
            config = CalendarHeatmapChartLayoutConfig(
                widthPx = 530f,
                heightPx = 180f,
                contentPaddingPx = 12f,
                monthLabelHeightPx = 16f,
                weekdayLabelWidthPx = 24f,
                labelGapPx = 6f,
                cellGapPx = 2f,
            ),
        )

        assertTrue(layout.isRenderable)
        assertEquals(365, layout.dayCells.size)
        assertTrue(layout.dayCells.all { it.value == null })
    }

    @Test
    fun findCalendarHeatmapHit_ignoresEmptyCells() {
        val layout = resolveCalendarHeatmapChartLayout(
            data = CalendarHeatmapData(
                year = 2026,
                days = listOf(CalendarHeatmapDay(1, 1, 4.0, payload = "one")),
            ),
            config = CalendarHeatmapChartLayoutConfig(
                widthPx = 530f,
                heightPx = 180f,
                contentPaddingPx = 12f,
                monthLabelHeightPx = 16f,
                weekdayLabelWidthPx = 24f,
                labelGapPx = 6f,
                cellGapPx = 2f,
            ),
        )

        val emptyCell = layout.dayCells.first { it.month == 1 && it.dayOfMonth == 2 }
        val hit = findCalendarHeatmapHit(
            layout,
            emptyCell.rect.left + emptyCell.rect.width * 0.5f,
            emptyCell.rect.top + emptyCell.rect.height * 0.5f,
        )

        assertFalse(layout.dayCells.first { it.month == 1 && it.dayOfMonth == 1 }.day == null)
        assertNull(hit)
    }
}
