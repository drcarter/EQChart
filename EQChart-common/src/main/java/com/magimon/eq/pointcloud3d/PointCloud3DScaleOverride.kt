package com.magimon.eq.pointcloud3d

/**
 * Manual X/Y/Z/size range overrides for the true 3D point-cloud chart.
 */
data class PointCloud3DScaleOverride(
    val xMin: Double? = null,
    val xMax: Double? = null,
    val yMin: Double? = null,
    val yMax: Double? = null,
    val zMin: Double? = null,
    val zMax: Double? = null,
    val sizeMin: Double? = null,
    val sizeMax: Double? = null,
)
