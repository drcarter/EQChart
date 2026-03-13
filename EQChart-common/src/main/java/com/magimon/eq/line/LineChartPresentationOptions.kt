package com.magimon.eq.line

/**
 * Presentation behavior for line and area charts.
 *
 * @property showLegend Whether to render the legend.
 * @property showGrid Whether to render the grid lines.
 * @property showAxes Whether to render axes.
 * @property showPoints Whether to render point markers.
 * @property showAreaFill Whether to fill area below the line. Used by [AreaChartView].
 * @property animateOnDataChange Whether to animate on data updates.
 * @property enterAnimationDurationMs Duration in milliseconds for enter animation.
 * @property enterAnimationDelayMs Delay in milliseconds before enter animation starts.
 * @property legendTextSizeSp Legend text size in sp.
 * @property axisLabelTextSizeSp Axis label text size in sp.
 * @property legendLeftMarginDp Legend left margin in dp.
 * @property legendTopMarginDp Legend top margin in dp.
 * @property legendBottomMarginDp Legend bottom margin in dp.
 * @property emptyText Empty state message.
 * @property xLabelFormatter Converts x values to axis labels.
 * @property yLabelFormatter Converts y values to axis labels.
 * @property xTickCount Number of x-axis ticks to render.
 * @property yTickCount Number of y-axis ticks to render.
 */
data class LineChartPresentationOptions(
    val showLegend: Boolean = true,
    val showGrid: Boolean = true,
    val showAxes: Boolean = true,
    val showPoints: Boolean = true,
    val showAreaFill: Boolean = false,
    val animateOnDataChange: Boolean = true,
    val enterAnimationDurationMs: Long = 700L,
    val enterAnimationDelayMs: Long = 30L,
    val legendTextSizeSp: Float = 12f,
    val axisLabelTextSizeSp: Float = 12f,
    val legendLeftMarginDp: Float = 4f,
    val legendTopMarginDp: Float = 4f,
    val legendBottomMarginDp: Float = 10f,
    val emptyText: String = "No data",
    val xLabelFormatter: (Double) -> String = { value ->
        if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            (kotlin.math.round(value * 10.0) / 10.0).toString()
        }
    },
    val yLabelFormatter: (Double) -> String = { value ->
        if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format("%.1f", value)
        }
    },
    val xTickCount: Int = 6,
    val yTickCount: Int = 6,
)
