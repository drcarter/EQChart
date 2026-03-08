package com.magimon.eq.radar

/**
 * Radar chart series data.
 *
 * @property name Series name shown in the legend and interaction events
 * @property color Render color for polygon and points
 * @property values Axis-ordered values. The size must match the number of [RadarAxis] entries.
 * @property payload Source data delivered to click callbacks
 */
data class RadarSeries(
    val name: String,
    val color: Int,
    val values: List<Double>,
    val payload: Any? = null,
)
