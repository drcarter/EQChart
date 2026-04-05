package com.magimon.eq.app.ui.compose

import com.magimon.eq.calendarheatmap.CalendarHeatmapData
import com.magimon.eq.calendarheatmap.CalendarHeatmapDay

object CalendarHeatmapSampleData {

    fun yearActivityData(): CalendarHeatmapData {
        return CalendarHeatmapData(
            year = 2026,
            days = listOf(
                CalendarHeatmapDay(1, 3, 2.0, payload = "Kickoff"),
                CalendarHeatmapDay(1, 7, 5.0, payload = "Spec review"),
                CalendarHeatmapDay(1, 9, 8.0, payload = "Launch prep"),
                CalendarHeatmapDay(2, 14, 3.0, payload = "Valentine campaign"),
                CalendarHeatmapDay(2, 27, 7.0, payload = "Milestone close"),
                CalendarHeatmapDay(3, 4, 1.0, payload = "Quiet day"),
                CalendarHeatmapDay(3, 18, 9.0, payload = "Peak traffic"),
                CalendarHeatmapDay(4, 2, 4.0, payload = "Beta release"),
                CalendarHeatmapDay(4, 22, 6.0, payload = "Earth Day push"),
                CalendarHeatmapDay(5, 11, 2.0, payload = "Planning"),
                CalendarHeatmapDay(5, 29, 8.0, payload = "Promo week"),
                CalendarHeatmapDay(6, 8, 5.0, payload = "Mid-year review"),
                CalendarHeatmapDay(6, 30, 7.0, payload = "Quarter close"),
                CalendarHeatmapDay(7, 5, 1.0, payload = "Holiday spillover"),
                CalendarHeatmapDay(7, 19, 6.0, payload = "Feature spike"),
                CalendarHeatmapDay(8, 6, 3.0, payload = "UX polish"),
                CalendarHeatmapDay(8, 21, 9.0, payload = "Release day"),
                CalendarHeatmapDay(9, 10, 4.0, payload = "Backlog burn"),
                CalendarHeatmapDay(9, 28, 6.0, payload = "Conference"),
                CalendarHeatmapDay(10, 8, 2.0, payload = "Maintenance"),
                CalendarHeatmapDay(10, 31, 7.0, payload = "Halloween sale"),
                CalendarHeatmapDay(11, 17, 8.0, payload = "Black Friday prep"),
                CalendarHeatmapDay(12, 5, 4.0, payload = "Year-end push"),
                CalendarHeatmapDay(12, 24, 9.0, payload = "Holiday peak"),
            ),
        )
    }
}
