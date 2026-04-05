package com.magimon.eq.calendarheatmap

/**
 * Input data for a single-year GitHub-style calendar heatmap.
 *
 * @property year Gregorian calendar year rendered by the chart.
 * @property days Sparse day entries keyed by [CalendarHeatmapDay.month] and
 * [CalendarHeatmapDay.dayOfMonth].
 */
data class CalendarHeatmapData(
    val year: Int,
    val days: List<CalendarHeatmapDay>,
)
