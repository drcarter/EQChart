package com.magimon.eq.line

/**
 * Series definition for line and area charts.
 *
 * @property name Series label used in legend and optional callback payload.
 * @property color Series line and marker color (ARGB int).
 * @property points Data points in chart order.
 * @property payload Optional source object passed through point click callbacks.
 * @property areaFillColor Optional override for area fill color.
 *   If omitted, [LineChartStyleOptions.areaFillAlpha] is applied to `color`.
 */
data class LineSeries(
    val name: String,
    val color: Int,
    val points: List<LineDatum>,
    val payload: Any? = null,
    val areaFillColor: Int? = null,
)

