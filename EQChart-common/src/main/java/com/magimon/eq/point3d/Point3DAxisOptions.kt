package com.magimon.eq.point3d

import kotlin.math.abs

/**
 * Axis, grid, and tick rendering options shared by true 3D point-series charts.
 */
data class Point3DAxisOptions(
    /** Whether to render the three primary axis lines. */
    val showAxes: Boolean = true,
    /** Whether to render supporting grid planes behind the point volume. */
    val showGridPlanes: Boolean = true,
    /** Whether to render short tick marks along the axes. */
    val showTicks: Boolean = true,
    /** Number of divisions used when generating grid lines and tick marks. */
    val gridDivisions: Int = 4,
    /** Optional title rendered near the positive X axis endpoint. */
    val xAxisTitle: String? = null,
    /** Optional title rendered near the positive Y axis endpoint. */
    val yAxisTitle: String? = null,
    /** Optional title rendered near the positive Z axis endpoint. */
    val zAxisTitle: String? = null,
    /** Formats values on the X axis. */
    val xLabelFormatter: (Double) -> String = { defaultPoint3DNumberFormat(it) },
    /** Formats values on the Y axis. */
    val yLabelFormatter: (Double) -> String = { defaultPoint3DNumberFormat(it) },
    /** Formats values on the Z axis. */
    val zLabelFormatter: (Double) -> String = { defaultPoint3DNumberFormat(it) },
)

/**
 * Default compact formatter shared by the axis label callbacks.
 */
private fun defaultPoint3DNumberFormat(value: Double): String {
    val absValue = abs(value)
    return when {
        absValue >= 1_000_000_000.0 -> String.format("%.1fB", value / 1_000_000_000.0)
        absValue >= 1_000_000.0 -> String.format("%.1fM", value / 1_000_000.0)
        absValue >= 1_000.0 -> String.format("%.1fK", value / 1_000.0)
        absValue >= 100.0 -> String.format("%.0f", value)
        absValue >= 10.0 -> String.format("%.1f", value)
        else -> String.format("%.2f", value)
    }
}
