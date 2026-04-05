package com.magimon.eq.app.ui.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarHeatmapSampleDataTest {

    @Test
    fun yearActivityData_returnsExpectedYearAndSampleDays() {
        val data = CalendarHeatmapSampleData.yearActivityData()

        assertEquals(2026, data.year)
        assertTrue(data.days.isNotEmpty())
        assertTrue(data.days.any { it.month == 8 && it.dayOfMonth == 21 })
        assertTrue(data.days.none { it.value < 0.0 })
    }
}
