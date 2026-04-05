package com.magimon.eq.rangebar

/**
 * One horizontal interval rendered by a range bar or timeline chart.
 *
 * Values are interpreted on a shared numeric X axis. Consumers can use
 * [RangeBarChartPresentationOptions.xLabelFormatter] to present those values
 * as months, dates, percentages, or any other domain-specific scale.
 *
 * @property label Row label rendered beside the interval.
 * @property start Inclusive interval start on the shared X axis.
 * @property end Inclusive interval end on the shared X axis.
 * @property color Optional bar color override.
 * @property title Optional text rendered inside or near the interval bar.
 * @property payload Optional source object returned in click callbacks.
 */
data class RangeBarEntry(
    val label: String,
    val start: Double,
    val end: Double,
    val color: Int? = null,
    val title: String? = null,
    val payload: Any? = null,
)
