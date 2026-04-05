package com.magimon.eq.calendarheatmap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarHeatmapChartLayoutEngineTest {

    private fun config() = CalendarHeatmapChartLayoutConfig(
        widthPx = 530f,
        heightPx = 180f,
        contentPaddingPx = 12f,
        monthLabelHeightPx = 16f,
        weekdayLabelWidthPx = 24f,
        labelGapPx = 6f,
        cellGapPx = 2f,
    )

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
            config = config(),
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
            config = config(),
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
            config = config(),
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
            config = config(),
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

    @Test
    fun resolveCalendarHeatmapChartLayout_rendersAll366CellsInLeapYear() {
        val layout = resolveCalendarHeatmapChartLayout(
            data = CalendarHeatmapData(year = 2024, days = emptyList()),
            config = config(),
        )

        assertTrue(layout.isRenderable)
        assertEquals(366, layout.dayCells.size)
        assertTrue(layout.dayCells.any { it.month == 2 && it.dayOfMonth == 29 })
    }

    @Test
    fun resolveCalendarHeatmapChartLayout_mapsPositiveBucketsAtThresholdBoundaries() {
        val style = CalendarHeatmapChartStyleOptions(
            level1Color = 0xFFCCEEDD.toInt(),
            level2Color = 0xFF99DD99.toInt(),
            level3Color = 0xFF55BB66.toInt(),
            level4Color = 0xFF228844.toInt(),
        )
        val layout = resolveCalendarHeatmapChartLayout(
            data = CalendarHeatmapData(
                year = 2025,
                days = listOf(
                    CalendarHeatmapDay(1, 1, 1.0, payload = "min"),
                    CalendarHeatmapDay(1, 2, 2.0, payload = "level2-boundary"),
                    CalendarHeatmapDay(1, 3, 3.0, payload = "level3-boundary"),
                    CalendarHeatmapDay(1, 4, 4.0, payload = "level4-boundary"),
                    CalendarHeatmapDay(1, 5, 5.0, payload = "max"),
                ),
            ),
            config = config(),
            style = style,
        )

        val jan1 = layout.dayCells.first { it.month == 1 && it.dayOfMonth == 1 }
        val jan2 = layout.dayCells.first { it.month == 1 && it.dayOfMonth == 2 }
        val jan3 = layout.dayCells.first { it.month == 1 && it.dayOfMonth == 3 }
        val jan4 = layout.dayCells.first { it.month == 1 && it.dayOfMonth == 4 }
        val jan5 = layout.dayCells.first { it.month == 1 && it.dayOfMonth == 5 }

        assertEquals(style.level1Color, jan1.fillColor)
        assertEquals(style.level2Color, jan2.fillColor)
        assertEquals(style.level3Color, jan3.fillColor)
        assertEquals(style.level4Color, jan4.fillColor)
        assertEquals(style.level4Color, jan5.fillColor)
    }
}
