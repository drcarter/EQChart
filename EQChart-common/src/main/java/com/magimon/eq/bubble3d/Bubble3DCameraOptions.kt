package com.magimon.eq.bubble3d

/**
 * Camera defaults for orbit-style 3D navigation around the chart origin.
 */
data class Bubble3DCameraOptions(
    /** Horizontal orbit angle around the chart volume in degrees. */
    val yawDegrees: Float = -36f,
    /** Vertical orbit angle around the chart volume in degrees. */
    val pitchDegrees: Float = 24f,
    /** Distance from the camera eye to the chart origin. */
    val distance: Float = 4.6f,
    /** Minimum allowed zoom distance. */
    val minDistance: Float = 2.2f,
    /** Maximum allowed zoom distance. */
    val maxDistance: Float = 10f,
    /** Perspective field of view in degrees. */
    val fovDegrees: Float = 45f,
)
