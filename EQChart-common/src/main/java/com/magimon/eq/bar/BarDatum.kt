package com.magimon.eq.bar

/**
 * Single bar value for one series.
 *
 * @property category Category key for x-axis grouping.
 * @property value Numeric bar value.
 * @property payload Optional source object passed in bar click callbacks.
 */
data class BarDatum(
    val category: String,
    val value: Double,
    val payload: Any? = null,
)

