package com.magimon.eq.boxplot

/**
 * Behavioral options for box plot charts.
 */
data class BoxPlotChartPresentationOptions(
    val showGrid: Boolean = true,
    val showAxes: Boolean = true,
    val showValueLabels: Boolean = true,
    val animateOnDataChange: Boolean = true,
    val enterAnimationDurationMs: Long = 680L,
    val enterAnimationDelayMs: Long = 30L,
    val animationDirection: Boolean = true,
    val emptyText: String = "No data",
    val axisLabelTextSizeSp: Float = 11.5f,
    val valueLabelTextSizeSp: Float = 11.5f,
    val yLabelFormatter: (Double) -> String = ::formatBoxPlotAxisValue,
    val valueLabelFormatter: (BoxPlotLayoutEntry) -> String = { entry ->
        entry.title ?: formatBoxPlotAxisValue(entry.median)
    },
    val xLabelFormatter: (BoxPlotLayoutEntry) -> String = { entry -> entry.label },
    val yTickCount: Int = 6,
)
