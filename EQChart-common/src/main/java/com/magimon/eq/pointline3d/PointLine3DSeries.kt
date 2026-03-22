package com.magimon.eq.pointline3d

/**
 * Ordered 3D series definition for the point-line chart.
 *
 * The chart connects [points] in list order. Line and point colors can be
 * configured separately so the path and markers remain readable on dark scenes.
 */
data class PointLine3DSeries(
    val name: String,
    val lineColor: Int,
    val points: List<PointLine3DDatum>,
    val pointColor: Int = lineColor,
    val lineWidthPx: Float = 3f,
    val pointRadiusScale: Float = 1f,
    val payload: Any? = null,
)
