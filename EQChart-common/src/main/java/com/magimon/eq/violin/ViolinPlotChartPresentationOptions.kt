package com.magimon.eq.violin

/**
 * Behavioral options for violin plot charts.
 */
data class ViolinPlotChartPresentationOptions(
    val showGrid: Boolean = true,
    val showAxes: Boolean = true,
    val showValueLabels: Boolean = true,
    val showMedianLine: Boolean = true,
    val showQuartileBand: Boolean = true,
    val animateOnDataChange: Boolean = true,
    val enterAnimationDurationMs: Long = 680L,
    val enterAnimationDelayMs: Long = 30L,
    val animationDirection: Boolean = true,
    val emptyText: String = "No data",
    val axisLabelTextSizeSp: Float = 11.5f,
    val valueLabelTextSizeSp: Float = 11.5f,
    val yLabelFormatter: (Double) -> String = ::formatViolinPlotAxisValue,
    val valueLabelFormatter: (ViolinPlotLayoutSeries) -> String = { entry ->
        "Median ${formatViolinPlotAxisValue(entry.median)}"
    },
    val xLabelFormatter: (ViolinPlotLayoutSeries) -> String = { entry -> entry.label },
    val yTickCount: Int = 6,
    val densityPointCount: Int = 48,
)
