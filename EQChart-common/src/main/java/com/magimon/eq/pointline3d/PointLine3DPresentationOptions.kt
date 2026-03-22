package com.magimon.eq.pointline3d

import android.graphics.Color

/**
 * Presentation and lighting options for a true 3D point-line scene.
 */
data class PointLine3DPresentationOptions(
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
    /** Whether to render connecting line geometry for each series. */
    val showLines: Boolean = true,
    /** Requested line width for the rendered series paths. */
    val lineWidth: Float = 3f,
    /** Alpha applied to rendered line geometry. */
    val lineAlpha: Float = 0.92f,
    /** Whether to render round point markers on top of the line path. */
    val showPointMarkers: Boolean = true,
    /** Requested point size for the rendered markers. */
    val pointSize: Float = 11f,
    /** Alpha applied to rendered point markers. */
    val pointAlpha: Float = 1f,
    /** Scale multiplier applied to the currently selected marker. */
    val selectedPointScale: Float = 1.35f,
)
