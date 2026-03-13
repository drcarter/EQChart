package com.magimon.eq.bar

/**
 * Series definition for bar charts.
 *
 * @property name Name shown in legend and click callbacks.
 * @property color Fill color of bars in this series.
 * @property points Data points keyed by category.
 * @property payload Optional payload returned by callbacks.
 */
data class BarSeries(
    val name: String,
    val color: Int,
    val points: List<BarDatum>,
    val payload: Any? = null,
)

