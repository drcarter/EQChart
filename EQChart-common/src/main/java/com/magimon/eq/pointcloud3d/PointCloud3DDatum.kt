package com.magimon.eq.pointcloud3d

/**
 * Single point in a true 3D point-cloud chart.
 *
 * Each point can provide its own color and size value so the chart can encode
 * additional dimensions beyond X/Y/Z position.
 */
data class PointCloud3DDatum(
    val x: Double,
    val y: Double,
    val z: Double,
    val size: Double = 1.0,
    val color: Int,
    val label: String? = null,
    val payload: Any? = null,
)
