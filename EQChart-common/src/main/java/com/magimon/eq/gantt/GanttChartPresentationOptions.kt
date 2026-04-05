package com.magimon.eq.gantt

/**
 * Behavioral options for Gantt charts.
 */
data class GanttChartPresentationOptions(
    val showGrid: Boolean = true,
    val showAxes: Boolean = true,
    val showTaskLabels: Boolean = true,
    val showProgress: Boolean = true,
    val showDependencies: Boolean = true,
    val showTodayIndicator: Boolean = false,
    val todayValue: Double? = null,
    val animateOnDataChange: Boolean = true,
    val enterAnimationDurationMs: Long = 680L,
    val enterAnimationDelayMs: Long = 30L,
    val emptyText: String = "No data",
    val axisLabelTextSizeSp: Float = 11.5f,
    val taskLabelTextSizeSp: Float = 11.5f,
    val xLabelFormatter: (Double) -> String = ::formatGanttAxisValue,
    val rowLabelFormatter: (String) -> String = { it },
    val taskLabelFormatter: (GanttLayoutTask) -> String = { task ->
        task.title ?: "${task.label}: ${formatGanttAxisValue(task.startValue)}-${formatGanttAxisValue(task.endValue)}"
    },
    val xTickCount: Int = 6,
)
