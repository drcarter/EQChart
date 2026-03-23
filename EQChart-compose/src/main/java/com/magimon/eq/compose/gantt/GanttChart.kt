package com.magimon.eq.compose.gantt

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import com.magimon.eq.compose.internal.newTextPaint
import com.magimon.eq.compose.internal.toComposeColor
import com.magimon.eq.gantt.GanttChartPresentationOptions
import com.magimon.eq.gantt.GanttChartLayoutConfig
import com.magimon.eq.gantt.GanttChartStyleOptions
import com.magimon.eq.gantt.GanttDependency
import com.magimon.eq.gantt.GanttPlacedTask
import com.magimon.eq.gantt.GanttTask
import com.magimon.eq.gantt.hitTestGanttTask
import com.magimon.eq.gantt.resolveGanttChartLayout
import com.magimon.eq.gantt.resolveGanttChartPlacement

private fun dpToPx(value: Float, density: Float): Float = value * density

/**
 * Canvas-based Gantt chart composable.
 */
@Composable
fun GanttChart(
    tasks: List<GanttTask>,
    modifier: Modifier = Modifier,
    dependencies: List<GanttDependency> = emptyList(),
    styleOptions: GanttChartStyleOptions = GanttChartStyleOptions(),
    presentationOptions: GanttChartPresentationOptions = GanttChartPresentationOptions(),
    onTaskClick: ((index: Int, task: GanttTask) -> Unit)? = null,
) {
    val density = LocalDensity.current
    val densityPx = density.density
    val scaledDensity = density.fontScale * densityPx
    val renderProgress = remember { Animatable(1f) }
    var selectedTaskIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(tasks, dependencies, presentationOptions.animateOnDataChange, presentationOptions.enterAnimationDurationMs, presentationOptions.enterAnimationDelayMs) {
        if (presentationOptions.animateOnDataChange && tasks.isNotEmpty()) {
            renderProgress.snapTo(0f)
            renderProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = presentationOptions.enterAnimationDurationMs.toInt().coerceAtLeast(0),
                    delayMillis = presentationOptions.enterAnimationDelayMs.toInt().coerceAtLeast(0),
                ),
            )
        } else {
            renderProgress.snapTo(1f)
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val layout = remember(tasks, dependencies, styleOptions, presentationOptions.xTickCount) {
            resolveGanttChartLayout(
                tasks = tasks,
                dependencies = dependencies,
                style = styleOptions,
                tickCount = presentationOptions.xTickCount,
            )
        }

        val rowLabelPaint = newTextPaint(
            color = styleOptions.axisLabelColor,
            textSizePx = presentationOptions.axisLabelTextSizeSp * scaledDensity,
            align = Paint.Align.RIGHT,
        )
        val axisLabelPaint = newTextPaint(
            color = styleOptions.axisLabelColor,
            textSizePx = presentationOptions.axisLabelTextSizeSp * scaledDensity,
            align = Paint.Align.CENTER,
        )
        val taskLabelPaint = newTextPaint(
            color = styleOptions.taskLabelTextColor,
            textSizePx = presentationOptions.taskLabelTextSizeSp * scaledDensity,
            align = Paint.Align.LEFT,
        )
        val emptyTextPaint = newTextPaint(
            color = styleOptions.axisLabelColor,
            textSizePx = 14f * scaledDensity,
            align = Paint.Align.CENTER,
        )

        val placement = remember(
            layout,
            widthPx,
            heightPx,
            styleOptions,
            presentationOptions,
            densityPx,
        ) {
            val rowLabelReservedWidth = (layout.tasks.maxOfOrNull {
                rowLabelPaint.measureText(presentationOptions.rowLabelFormatter(it.label))
            } ?: 0f) + 16f * densityPx
            resolveGanttChartPlacement(
                layout = layout,
                config = GanttChartLayoutConfig(
                    widthPx = widthPx,
                    heightPx = heightPx,
                    contentPaddingPx = dpToPx(styleOptions.contentPaddingDp, densityPx),
                    rowSpacingPx = dpToPx(styleOptions.rowSpacingDp, densityPx),
                    minTaskWidthPx = dpToPx(styleOptions.minTaskWidthDp, densityPx),
                    rowLabelReservedWidthPx = rowLabelReservedWidth,
                    xAxisLabelReservedHeightPx = axisLabelPaint.fontSpacing + 18f * densityPx,
                    taskHeightRatio = styleOptions.taskHeightRatio,
                ),
                style = styleOptions.copy(
                    milestoneSizeDp = dpToPx(styleOptions.milestoneSizeDp, densityPx),
                    dependencyArrowSizeDp = dpToPx(styleOptions.dependencyArrowSizeDp, densityPx),
                    dependencyStrokeWidthDp = dpToPx(styleOptions.dependencyStrokeWidthDp, densityPx),
                ),
                presentation = presentationOptions,
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(placement, tasks, onTaskClick) {
                    detectTapGestures { tap ->
                        val hit = hitTestGanttTask(placement, tap.x, tap.y)
                        selectedTaskIndex = hit?.task?.index
                        hit?.let {
                            val sourceTask = tasks.getOrNull(it.task.index) ?: return@let
                            onTaskClick?.invoke(it.task.index, sourceTask)
                        }
                    }
                },
        ) {
            val alpha = renderProgress.value.coerceIn(0f, 1f)
            drawRect(color = styleOptions.backgroundColor.toComposeColor())

            if (layout.tasks.isEmpty()) {
                drawContext.canvas.nativeCanvas.drawText(
                    presentationOptions.emptyText,
                    size.width * 0.5f,
                    size.height * 0.5f,
                    emptyTextPaint,
                )
                return@Canvas
            }

            if (presentationOptions.showGrid) {
                placement.ticks.forEach { tick ->
                    drawLine(
                        color = styleOptions.gridColor.toComposeColor(),
                        start = Offset(tick.xPx, placement.plotTopPx),
                        end = Offset(tick.xPx, placement.plotBottomPx),
                        strokeWidth = dpToPx(1f, densityPx),
                    )
                }
            }

            if (presentationOptions.showTodayIndicator) {
                placement.todayIndicatorXPx?.let { x ->
                    drawLine(
                        color = styleOptions.todayIndicatorColor.toComposeColor().copy(alpha = alpha),
                        start = Offset(x, placement.plotTopPx),
                        end = Offset(x, placement.plotBottomPx),
                        strokeWidth = dpToPx(1.5f, densityPx),
                    )
                }
            }

            if (presentationOptions.showAxes) {
                drawLine(
                    color = styleOptions.axisColor.toComposeColor(),
                    start = Offset(placement.plotLeftPx, placement.plotBottomPx),
                    end = Offset(placement.plotRightPx, placement.plotBottomPx),
                    strokeWidth = dpToPx(1.2f, densityPx),
                )
                drawLine(
                    color = styleOptions.axisColor.toComposeColor(),
                    start = Offset(placement.plotLeftPx, placement.plotTopPx),
                    end = Offset(placement.plotLeftPx, placement.plotBottomPx),
                    strokeWidth = dpToPx(1.2f, densityPx),
                )
            }

            if (presentationOptions.showDependencies) {
                placement.dependencies.forEach { dependency ->
                    for (index in 0 until dependency.points.lastIndex) {
                        val start = dependency.points[index]
                        val end = dependency.points[index + 1]
                        drawLine(
                            color = dependency.dependency.color.toComposeColor().copy(alpha = alpha),
                            start = Offset(start.xPx, start.yPx),
                            end = Offset(end.xPx, end.yPx),
                            strokeWidth = dpToPx(styleOptions.dependencyStrokeWidthDp, densityPx),
                        )
                    }
                    drawLine(
                        color = dependency.dependency.color.toComposeColor().copy(alpha = alpha),
                        start = Offset(dependency.arrowTip.xPx, dependency.arrowTip.yPx),
                        end = Offset(dependency.arrowWingA.xPx, dependency.arrowWingA.yPx),
                        strokeWidth = dpToPx(styleOptions.dependencyStrokeWidthDp, densityPx),
                    )
                    drawLine(
                        color = dependency.dependency.color.toComposeColor().copy(alpha = alpha),
                        start = Offset(dependency.arrowTip.xPx, dependency.arrowTip.yPx),
                        end = Offset(dependency.arrowWingB.xPx, dependency.arrowWingB.yPx),
                        strokeWidth = dpToPx(styleOptions.dependencyStrokeWidthDp, densityPx),
                    )
                }
            }

            placement.tasks.forEach { task ->
                if (task.task.isMilestone) {
                    drawPath(
                        path = milestonePath(task),
                        color = task.task.color.toComposeColor().copy(alpha = alpha),
                    )
                    drawPath(
                        path = milestonePath(task),
                        color = styleOptions.milestoneStrokeColor.toComposeColor().copy(alpha = alpha),
                        style = Stroke(width = dpToPx(1.25f, densityPx)),
                    )
                    if (selectedTaskIndex == task.task.index) {
                        drawPath(
                            path = milestonePath(task),
                            color = styleOptions.selectedTaskStrokeColor.toComposeColor().copy(alpha = alpha),
                            style = Stroke(width = dpToPx(2f, densityPx)),
                        )
                    }
                } else {
                    drawRoundRect(
                        color = task.task.color.toComposeColor().copy(alpha = alpha),
                        topLeft = Offset(task.leftPx, task.topPx),
                        size = Size(task.rightPx - task.leftPx, task.bottomPx - task.topPx),
                        cornerRadius = CornerRadius(
                            dpToPx(styleOptions.taskCornerRadiusDp, densityPx),
                            dpToPx(styleOptions.taskCornerRadiusDp, densityPx),
                        ),
                    )
                    if (presentationOptions.showProgress && task.task.progress > 0f) {
                        drawRoundRect(
                            color = styleOptions.progressBarColor.toComposeColor().copy(alpha = alpha),
                            topLeft = Offset(task.leftPx, task.topPx),
                            size = Size((task.progressRightPx - task.leftPx).coerceAtLeast(1f), task.bottomPx - task.topPx),
                            cornerRadius = CornerRadius(
                                dpToPx(styleOptions.taskCornerRadiusDp, densityPx),
                                dpToPx(styleOptions.taskCornerRadiusDp, densityPx),
                            ),
                        )
                    }
                    if (selectedTaskIndex == task.task.index) {
                        drawRoundRect(
                            color = styleOptions.selectedTaskStrokeColor.toComposeColor().copy(alpha = alpha),
                            topLeft = Offset(
                                task.leftPx - dpToPx(styleOptions.selectedTaskPaddingDp, densityPx),
                                task.topPx - dpToPx(styleOptions.selectedTaskPaddingDp, densityPx),
                            ),
                            size = Size(
                                (task.rightPx - task.leftPx) + dpToPx(styleOptions.selectedTaskPaddingDp, densityPx) * 2f,
                                (task.bottomPx - task.topPx) + dpToPx(styleOptions.selectedTaskPaddingDp, densityPx) * 2f,
                            ),
                            cornerRadius = CornerRadius(
                                dpToPx(styleOptions.taskCornerRadiusDp, densityPx),
                                dpToPx(styleOptions.taskCornerRadiusDp, densityPx),
                            ),
                            style = Stroke(width = dpToPx(1.8f, densityPx)),
                        )
                    }
                }
            }

            drawContext.canvas.nativeCanvas.apply {
                placement.rowLabels.forEach { label ->
                    drawText(
                        label.label,
                        placement.plotLeftPx - 10f * densityPx,
                        label.centerYPx - (rowLabelPaint.descent() + rowLabelPaint.ascent()) * 0.5f,
                        rowLabelPaint,
                    )
                }

                if (presentationOptions.showTaskLabels) {
                    placement.tasks.forEach { task ->
                        val baseline = task.centerYPx - (taskLabelPaint.descent() + taskLabelPaint.ascent()) * 0.5f
                        val label = task.labelText
                        val textWidth = taskLabelPaint.measureText(label)
                        if (!task.task.isMilestone && (task.rightPx - task.leftPx) >= textWidth + 16f * densityPx) {
                            drawText(label, task.leftPx + 8f * densityPx, baseline, taskLabelPaint)
                        } else {
                            drawText(
                                label,
                                minOf(task.rightPx + 8f * densityPx, size.width - 4f * densityPx),
                                baseline,
                                taskLabelPaint,
                            )
                        }
                    }
                }

                placement.ticks.forEach { tick ->
                    drawText(
                        tick.label,
                        tick.xPx,
                        placement.plotBottomPx + axisLabelPaint.fontSpacing,
                        axisLabelPaint,
                    )
                }
            }
        }
    }
}

private fun milestonePath(task: GanttPlacedTask): Path {
    return Path().apply {
        moveTo(task.startXPx, task.topPx)
        lineTo(task.rightPx, task.centerYPx)
        lineTo(task.startXPx, task.bottomPx)
        lineTo(task.leftPx, task.centerYPx)
        close()
    }
}
