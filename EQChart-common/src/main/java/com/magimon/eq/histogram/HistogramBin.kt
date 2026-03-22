package com.magimon.eq.histogram

/**
 * One histogram bucket.
 *
 * @property start Inclusive lower bound of the bin.
 * @property end Exclusive upper bound of the bin.
 * @property value Count or frequency rendered for the bin.
 * @property label Optional display label override.
 * @property color Optional bar color override.
 * @property payload Optional source object returned in click callbacks.
 */
data class HistogramBin(
    val start: Double,
    val end: Double,
    val value: Double,
    val label: String? = null,
    val color: Int? = null,
    val payload: Any? = null,
)
