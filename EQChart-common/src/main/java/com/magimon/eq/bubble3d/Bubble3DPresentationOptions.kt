package com.magimon.eq.bubble3d

import android.graphics.Color

/**
 * Presentation and lighting options for a 3D bubble scene.
 */
data class Bubble3DPresentationOptions(
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
    /** Minimum ambient light contribution applied to bubble shading. */
    val ambientLight: Float = 0.32f,
    /** Lower bound for the world-space bubble radius after size mapping. */
    val minBubbleRadius: Float = 0.08f,
    /** Upper bound for the world-space bubble radius after size mapping. */
    val maxBubbleRadius: Float = 0.26f,
)
