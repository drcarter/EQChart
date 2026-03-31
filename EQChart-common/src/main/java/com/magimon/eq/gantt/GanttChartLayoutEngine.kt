package com.magimon.eq.gantt

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Render-ready task produced by [resolveGanttChartLayout].
 */
data class GanttLayoutTask(
    val index: Int,
    val id: String,
    val label: String,
    val title: String?,
    val rawStartValue: Double,
    val rawEndValue: Double,
    val startValue: Double,
    val endValue: Double,
    val duration: Double,
    val progress: Float,
    val color: Int,
    val isMilestone: Boolean,
    val payload: Any?,
)

/**
 * Render-ready dependency produced by [resolveGanttChartLayout].
 */
data class GanttLayoutDependency(
    val index: Int,
    val fromTaskId: String,
    val toTaskId: String,
    val type: GanttDependencyType,
    val color: Int,
    val payload: Any?,
)

/**
 * Shared numeric layout for Gantt charts.
 */
data class GanttChartLayout(
    val tasks: List<GanttLayoutTask>,
    val dependencies: List<GanttLayoutDependency>,
    val minValue: Double,
    val maxValue: Double,
    val ticks: List<Double>,
)

/**
 * Pixel layout configuration consumed by [resolveGanttChartPlacement].
 */
data class GanttChartLayoutConfig(
    val widthPx: Float,
    val heightPx: Float,
    val contentPaddingPx: Float,
    val rowSpacingPx: Float,
    val minTaskWidthPx: Float,
    val rowLabelReservedWidthPx: Float,
    val xAxisLabelReservedHeightPx: Float,
    val taskHeightRatio: Float = 0.58f,
)

/**
 * One formatted X-axis tick positioned in pixel space.
 */
data class GanttAxisTick(
    val value: Double,
    val xPx: Float,
    val label: String,
)

/**
 * One formatted row label positioned in pixel space.
 */
data class GanttRowLabel(
    val index: Int,
    val label: String,
    val centerYPx: Float,
)

/**
 * One task placed inside the plot area.
 */
data class GanttPlacedTask(
    val task: GanttLayoutTask,
    val leftPx: Float,
    val topPx: Float,
    val rightPx: Float,
    val bottomPx: Float,
    val startXPx: Float,
    val endXPx: Float,
    val labelText: String,
) {
    val centerXPx: Float
        get() = (leftPx + rightPx) / 2f

    val centerYPx: Float
        get() = (topPx + bottomPx) / 2f

    val progressRightPx: Float
        get() = leftPx + (rightPx - leftPx) * task.progress.coerceIn(0f, 1f)
}

/**
 * One routed dependency edge in pixel space.
 */
data class GanttPlacedDependency(
    val dependency: GanttLayoutDependency,
    val points: List<GanttPoint>,
    val arrowTip: GanttPoint,
    val arrowWingA: GanttPoint,
    val arrowWingB: GanttPoint,
)

/**
 * One point in pixel space.
 */
data class GanttPoint(
    val xPx: Float,
    val yPx: Float,
)

/**
 * Shared placement result for View and Compose Gantt renderers.
 */
data class GanttChartPlacement(
    val tasks: List<GanttPlacedTask>,
    val dependencies: List<GanttPlacedDependency>,
    val ticks: List<GanttAxisTick>,
    val rowLabels: List<GanttRowLabel>,
    val plotLeftPx: Float,
    val plotTopPx: Float,
    val plotRightPx: Float,
    val plotBottomPx: Float,
    val rowHeightPx: Float,
    val todayIndicatorXPx: Float?,
)

private fun emptyGanttPlacement(
    plotLeft: Float,
    plotTop: Float,
    plotRight: Float,
    plotBottom: Float,
): GanttChartPlacement {
    return GanttChartPlacement(
        tasks = emptyList(),
        dependencies = emptyList(),
        ticks = emptyList(),
        rowLabels = emptyList(),
        plotLeftPx = plotLeft,
        plotTopPx = plotTop,
        plotRightPx = plotRight,
        plotBottomPx = plotBottom,
        rowHeightPx = 0f,
        todayIndicatorXPx = null,
    )
}

/**
 * Resolves sanitized tasks/dependencies and the shared numeric axis for Gantt charts.
 */
fun resolveGanttChartLayout(
    tasks: List<GanttTask>,
    dependencies: List<GanttDependency> = emptyList(),
    style: GanttChartStyleOptions = GanttChartStyleOptions(),
    tickCount: Int = 6,
): GanttChartLayout {
    val sanitizedTasks = tasks.mapIndexedNotNull { index, task ->
        val id = task.id.trim()
        if (id.isEmpty()) return@mapIndexedNotNull null
        if (task.label.isBlank()) return@mapIndexedNotNull null
        if (!task.start.isFinite()) return@mapIndexedNotNull null
        if (!task.end.isFinite()) return@mapIndexedNotNull null

        val startValue = min(task.start, task.end)
        val endValue = max(task.start, task.end)
        GanttLayoutTask(
            index = index,
            id = id,
            label = task.label,
            title = task.title?.takeIf { it.isNotBlank() },
            rawStartValue = task.start,
            rawEndValue = task.end,
            startValue = startValue,
            endValue = endValue,
            duration = endValue - startValue,
            progress = if (task.progress.isFinite()) task.progress.coerceIn(0f, 1f) else 0f,
            color = task.color ?: style.defaultTaskColor,
            isMilestone = task.isMilestone || abs(endValue - startValue) <= 1e-12,
            payload = task.payload,
        )
    }

    if (sanitizedTasks.isEmpty()) {
        return GanttChartLayout(
            tasks = emptyList(),
            dependencies = emptyList(),
            minValue = -1.0,
            maxValue = 1.0,
            ticks = ganttAxisTicks(-1.0, 1.0, tickCount),
        )
    }

    var rawMin = sanitizedTasks.first().startValue
    var rawMax = sanitizedTasks.first().endValue
    for (task in sanitizedTasks) {
        if (task.startValue < rawMin) rawMin = task.startValue
        if (task.endValue > rawMax) rawMax = task.endValue
    }
    val minValue: Double
    val maxValue: Double
    if (abs(rawMax - rawMin) <= 1e-12) {
        minValue = rawMin - 1.0
        maxValue = rawMax + 1.0
    } else {
        minValue = rawMin
        maxValue = rawMax
    }

    val taskById = sanitizedTasks.associateBy { it.id }
    val sanitizedDependencies = dependencies.mapIndexedNotNull { index, dependency ->
        val fromTaskId = dependency.fromTaskId.trim()
        val toTaskId = dependency.toTaskId.trim()
        if (fromTaskId.isEmpty() || toTaskId.isEmpty()) return@mapIndexedNotNull null
        if (fromTaskId == toTaskId) return@mapIndexedNotNull null
        if (taskById[fromTaskId] == null || taskById[toTaskId] == null) return@mapIndexedNotNull null

        GanttLayoutDependency(
            index = index,
            fromTaskId = fromTaskId,
            toTaskId = toTaskId,
            type = dependency.type,
            color = dependency.color ?: style.dependencyLineColor,
            payload = dependency.payload,
        )
    }

    return GanttChartLayout(
        tasks = sanitizedTasks,
        dependencies = sanitizedDependencies,
        minValue = minValue,
        maxValue = maxValue,
        ticks = ganttAxisTicks(minValue, maxValue, tickCount),
    )
}

/**
 * Resolves pixel placement for a [GanttChartLayout].
 */
fun resolveGanttChartPlacement(
    layout: GanttChartLayout,
    config: GanttChartLayoutConfig,
    style: GanttChartStyleOptions = GanttChartStyleOptions(),
    presentation: GanttChartPresentationOptions = GanttChartPresentationOptions(),
): GanttChartPlacement {
    val plotLeft = config.contentPaddingPx + config.rowLabelReservedWidthPx
    val plotTop = config.contentPaddingPx
    val plotRight = config.widthPx - config.contentPaddingPx
    val plotBottom = config.heightPx - config.contentPaddingPx - config.xAxisLabelReservedHeightPx
    val plotWidth = plotRight - plotLeft
    val plotHeight = plotBottom - plotTop

    if (layout.tasks.isEmpty()) {
        return emptyGanttPlacement(plotLeft, plotTop, max(plotLeft, plotRight), max(plotTop, plotBottom))
    }
    if (plotWidth <= 0f || plotHeight <= 0f) {
        return emptyGanttPlacement(plotLeft, plotTop, max(plotLeft, plotRight), max(plotTop, plotBottom))
    }

    val rowCount = layout.tasks.size
    val safeRowSpacing = max(0f, config.rowSpacingPx)
    val availableHeight = max(0f, plotHeight - safeRowSpacing * (rowCount - 1))
    val rowHeight = availableHeight / rowCount
    val taskHeight = rowHeight * config.taskHeightRatio.coerceIn(0.2f, 1f)

    fun mapValueToX(value: Double): Float {
        val ratio = if (abs(layout.maxValue - layout.minValue) <= 1e-12) {
            0f
        } else {
            ((value - layout.minValue) / (layout.maxValue - layout.minValue)).toFloat().coerceIn(0f, 1f)
        }
        return plotLeft + plotWidth * ratio
    }

    val milestoneSizePx = max(1f, style.milestoneSizeDp)
    val placedTasks = layout.tasks.mapIndexed { rowIndex, task ->
        val rowTop = plotTop + rowIndex * (rowHeight + safeRowSpacing)
        val top = rowTop + (rowHeight - taskHeight) / 2f
        val bottom = top + taskHeight
        val startXPx = mapValueToX(task.startValue)
        val endXPx = mapValueToX(task.endValue)
        val leftPx: Float
        val rightPx: Float
        if (task.isMilestone) {
            leftPx = startXPx - milestoneSizePx * 0.5f
            rightPx = startXPx + milestoneSizePx * 0.5f
        } else {
            leftPx = min(startXPx, endXPx)
            rightPx = max(startXPx + config.minTaskWidthPx, max(startXPx, endXPx))
        }
        GanttPlacedTask(
            task = task,
            leftPx = leftPx,
            topPx = top,
            rightPx = min(rightPx, plotRight),
            bottomPx = bottom,
            startXPx = startXPx,
            endXPx = endXPx,
            labelText = presentation.taskLabelFormatter(task),
        )
    }

    val placedDependencies = layout.dependencies.mapNotNull { dependency ->
        if (dependency.type != GanttDependencyType.FINISH_TO_START) return@mapNotNull null
        val fromTask = placedTasks.firstOrNull { it.task.id == dependency.fromTaskId } ?: return@mapNotNull null
        val toTask = placedTasks.firstOrNull { it.task.id == dependency.toTaskId } ?: return@mapNotNull null

        val startPoint = GanttPoint(
            xPx = if (fromTask.task.isMilestone) fromTask.startXPx else fromTask.endXPx,
            yPx = fromTask.centerYPx,
        )
        val endPoint = GanttPoint(
            xPx = toTask.startXPx,
            yPx = toTask.centerYPx,
        )
        val direction = if (endPoint.xPx >= startPoint.xPx) 1f else -1f
        val bendOffsetPx = max(8f, style.contentPaddingDp * 1.2f)
        val elbowX = if (direction >= 0f) {
            max(startPoint.xPx + bendOffsetPx, (startPoint.xPx + endPoint.xPx) * 0.5f)
        } else {
            min(startPoint.xPx - bendOffsetPx, (startPoint.xPx + endPoint.xPx) * 0.5f)
        }
        val arrowSizePx = max(4f, style.dependencyArrowSizeDp)
        val arrowBaseX = endPoint.xPx - direction * arrowSizePx
        GanttPlacedDependency(
            dependency = dependency,
            points = listOf(
                startPoint,
                GanttPoint(elbowX, startPoint.yPx),
                GanttPoint(elbowX, endPoint.yPx),
                endPoint,
            ),
            arrowTip = endPoint,
            arrowWingA = GanttPoint(arrowBaseX, endPoint.yPx - arrowSizePx * 0.55f),
            arrowWingB = GanttPoint(arrowBaseX, endPoint.yPx + arrowSizePx * 0.55f),
        )
    }

    val ticks = layout.ticks.map { value ->
        GanttAxisTick(
            value = value,
            xPx = mapValueToX(value),
            label = presentation.xLabelFormatter(value),
        )
    }
    val rowLabels = placedTasks.mapIndexed { index, task ->
        GanttRowLabel(
            index = index,
            label = presentation.rowLabelFormatter(task.task.label),
            centerYPx = task.centerYPx,
        )
    }
    val todayIndicatorXPx = presentation.todayValue
        ?.takeIf { presentation.showTodayIndicator && it.isFinite() }
        ?.let(::mapValueToX)

    return GanttChartPlacement(
        tasks = placedTasks,
        dependencies = placedDependencies,
        ticks = ticks,
        rowLabels = rowLabels,
        plotLeftPx = plotLeft,
        plotTopPx = plotTop,
        plotRightPx = plotRight,
        plotBottomPx = plotBottom,
        rowHeightPx = rowHeight,
        todayIndicatorXPx = todayIndicatorXPx,
    )
}

/**
 * Returns the topmost placed task that contains the provided point.
 */
fun hitTestGanttTask(
    placement: GanttChartPlacement,
    xPx: Float,
    yPx: Float,
): GanttPlacedTask? {
    for (index in placement.tasks.indices.reversed()) {
        val task = placement.tasks[index]
        if (xPx < task.leftPx || xPx > task.rightPx) continue
        if (yPx < task.topPx || yPx > task.bottomPx) continue
        return task
    }
    return null
}

/**
 * Produces evenly spaced X-axis ticks for the resolved range.
 */
fun ganttAxisTicks(minValue: Double, maxValue: Double, count: Int): List<Double> {
    val safeCount = max(2, count)
    if (safeCount == 2) return listOf(minValue, maxValue)
    return List(safeCount) { index ->
        minValue + (maxValue - minValue) * index.toDouble() / (safeCount - 1).toDouble()
    }
}

/**
 * Default X-axis formatter used by [GanttChartPresentationOptions].
 */
fun formatGanttAxisValue(value: Double): String {
    if (!value.isFinite()) return "0"
    val absolute = abs(value)
    return when {
        absolute >= 1_000_000.0 -> "${trimGanttAxisValue(value / 1_000_000.0)}M"
        absolute >= 1_000.0 -> "${trimGanttAxisValue(value / 1_000.0)}K"
        abs(value - value.toInt().toDouble()) <= 1e-9 -> value.toInt().toString()
        else -> trimGanttAxisValue(value)
    }
}

private fun trimGanttAxisValue(value: Double): String {
    val rounded = ((value * 10.0).toInt()) / 10.0
    return if (abs(rounded - rounded.toInt().toDouble()) <= 1e-9) {
        rounded.toInt().toString()
    } else {
        rounded.toString()
    }
}
