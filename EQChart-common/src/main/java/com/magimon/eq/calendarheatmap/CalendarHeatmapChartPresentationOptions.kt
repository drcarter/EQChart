package com.magimon.eq.calendarheatmap

/**
 * Behavioral options for calendar heatmap charts.
 */
data class CalendarHeatmapChartPresentationOptions(
    val showMonthLabels: Boolean = true,
    val showWeekdayLabels: Boolean = true,
    val emptyText: String = "No data",
    val monthLabelTextSizeSp: Float = 11.5f,
    val weekdayLabelTextSizeSp: Float = 10.5f,
)
