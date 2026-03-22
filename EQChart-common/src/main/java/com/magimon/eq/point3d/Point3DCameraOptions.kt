package com.magimon.eq.point3d

/**
 * Orbit camera options shared by true 3D point-series charts.
 */
data class Point3DCameraOptions(
    /** Horizontal orbit rotation around the scene origin in degrees. */
    val yawDegrees: Float = -36f,
    /** Vertical orbit rotation around the scene origin in degrees. */
    val pitchDegrees: Float = 24f,
    /** Camera distance from the scene origin. */
    val distance: Float = 4.6f,
    /** Minimum zoom distance allowed during pinch gestures. */
    val minDistance: Float = 2.2f,
    /** Maximum zoom distance allowed during pinch gestures. */
    val maxDistance: Float = 10f,
    /** Perspective field of view in degrees. */
    val fovDegrees: Float = 45f,
)
