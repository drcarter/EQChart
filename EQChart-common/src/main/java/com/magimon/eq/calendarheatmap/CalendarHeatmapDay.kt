package com.magimon.eq.calendarheatmap

/**
 * A single day entry inside a [CalendarHeatmapData] year.
 *
 * @property month 1-based calendar month (`1..12`).
 * @property dayOfMonth 1-based day of month.
 * @property value Non-negative intensity value. Zero is rendered as an empty day.
 * @property label Optional short label used by samples or future overlays.
 * @property payload Optional caller-owned object delivered on click callbacks.
 */
data class CalendarHeatmapDay(
    val month: Int,
    val dayOfMonth: Int,
    val value: Double,
    val label: String? = null,
    val payload: Any? = null,
)
