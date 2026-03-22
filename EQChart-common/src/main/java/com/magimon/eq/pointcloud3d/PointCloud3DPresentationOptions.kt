package com.magimon.eq.pointcloud3d

import android.graphics.Color

/**
 * Presentation options for a true 3D point-cloud scene.
 */
data class PointCloud3DPresentationOptions(
    /** Background clear color for the OpenGL surface. */
    val backgroundColor: Int = Color.parseColor("#09111B"),
    /** Shared grid and tick color. */
    val gridColor: Int = Color.parseColor("#334155"),
    /** Axis color for the X direction. */
    val xAxisColor: Int = Color.parseColor("#F97316"),
    /** Axis color for the Y direction. */
    val yAxisColor: Int = Color.parseColor("#22C55E"),
    /** Axis color for the Z direction. */
    val zAxisColor: Int = Color.parseColor("#38BDF8"),
    /** Lower bound for the mapped point-marker size. */
    val minPointSize: Float = 6f,
    /** Upper bound for the mapped point-marker size. */
    val maxPointSize: Float = 18f,
    /** Alpha applied to rendered points. */
    val pointAlpha: Float = 0.95f,
    /** Scale multiplier applied to the currently selected point. */
    val selectedPointScale: Float = 1.4f,
)
