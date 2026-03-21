package com.magimon.eq.funnel

/**
 * One stage in a vertical funnel chart.
 *
 * @property label Stage label shown inside the funnel block.
 * @property value Stage magnitude used to determine relative funnel width.
 * @property color Optional fill color override.
 * @property payload Optional source object returned in click callbacks.
 */
data class FunnelStage(
    val label: String,
    val value: Double,
    val color: Int? = null,
    val payload: Any? = null,
)
