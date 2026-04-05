package com.magimon.eq.violin

/**
 * One categorical violin plot series backed by raw numeric samples.
 */
data class ViolinPlotSeries(
    val label: String,
    val samples: List<Double>,
    val color: Int? = null,
    val payload: Any? = null,
)
