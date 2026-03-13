package com.magimon.eq.line

/**
 * Single point in a line or area chart.
 *
 * @property x Horizontal value on the chart axis.
 * @property y Vertical value on the chart axis.
 * @property payload Optional source object passed back in click callbacks.
 *
 * Notes:
 * - Only finite points are rendered.
 * - If values are sparse, gaps are not interpolated unless line smoothing is enabled.
 */
data class LineDatum(
    val x: Double,
    val y: Double,
    val payload: Any? = null,
)

