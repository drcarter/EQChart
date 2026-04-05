package com.magimon.eq.calendarheatmap

import com.magimon.eq.testutil.layoutAndDraw
import com.magimon.eq.testutil.readPrivate
import com.magimon.eq.testutil.touchUp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CalendarHeatmapChartViewTest {

    @Test
    fun setData_buildsLayoutAndDispatchesDayClick() {
        val context = RuntimeEnvironment.getApplication()
        val view = CalendarHeatmapChartView(context)
        var clickedPayload: Any? = null

        view.setOnDayClickListener { clickedPayload = it.payload }
        view.setData(
            CalendarHeatmapData(
                year = 2025,
                days = listOf(
                    CalendarHeatmapDay(month = 1, dayOfMonth = 1, value = 4.0, payload = "jan-1"),
                ),
            ),
        )

        layoutAndDraw(view, width = 760, height = 220)

        val layout = view.readPrivate<CalendarHeatmapChartLayoutResult>("layout")
        val day = layout.dayCells.first { it.value != null }
        touchUp(
            view,
            day.rect.left + day.rect.width * 0.5f,
            day.rect.top + day.rect.height * 0.5f,
        )

        assertEquals("jan-1", clickedPayload)
    }

    @Test
    fun emptyCells_doNotDispatchCallbacks() {
        val context = RuntimeEnvironment.getApplication()
        val view = CalendarHeatmapChartView(context)
        var clickedPayload: Any? = null

        view.setOnDayClickListener { clickedPayload = it.payload }
        view.setData(
            CalendarHeatmapData(
                year = 2025,
                days = listOf(
                    CalendarHeatmapDay(month = 1, dayOfMonth = 1, value = 4.0, payload = "jan-1"),
                ),
            ),
        )

        layoutAndDraw(view, width = 760, height = 220)

        val layout = view.readPrivate<CalendarHeatmapChartLayoutResult>("layout")
        val empty = layout.dayCells.first { it.value == null }
        touchUp(
            view,
            empty.rect.left + empty.rect.width * 0.5f,
            empty.rect.top + empty.rect.height * 0.5f,
        )

        assertNull(clickedPayload)
    }
}
