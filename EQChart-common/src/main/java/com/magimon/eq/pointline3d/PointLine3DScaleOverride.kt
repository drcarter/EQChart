package com.magimon.eq.pointline3d

/**
 * Manual X/Y/Z range overrides for the true 3D point-line chart.
 */
data class PointLine3DScaleOverride(
    val xMin: Double? = null,
    val xMax: Double? = null,
    val yMin: Double? = null,
    val yMax: Double? = null,
    val zMin: Double? = null,
    val zMax: Double? = null,
)
