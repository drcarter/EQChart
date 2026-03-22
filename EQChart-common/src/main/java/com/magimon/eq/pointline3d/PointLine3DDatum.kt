package com.magimon.eq.pointline3d

/**
 * Single point in a true 3D point-line series.
 *
 * Points are rendered in list order and connected sequentially by the series
 * line renderer. Non-finite coordinates are filtered out by the chart host.
 */
data class PointLine3DDatum(
    val x: Double,
    val y: Double,
    val z: Double,
    val color: Int? = null,
    val label: String? = null,
    val payload: Any? = null,
)
