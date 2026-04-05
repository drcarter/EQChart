package com.magimon.eq.gantt

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.max

/**
 * Gantt chart renderer for Android Views.
 */
class GanttChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity

    private var styleOptions = GanttChartStyleOptions()
    private var presentationOptions = GanttChartPresentationOptions()
    private var layout = resolveGanttChartLayout(emptyList(), emptyList(), styleOptions, presentationOptions.xTickCount)
    private var placement = resolveGanttChartPlacement(
        layout = layout,
        config = GanttChartLayoutConfig(
            widthPx = 0f,
            heightPx = 0f,
            contentPaddingPx = 0f,
            rowSpacingPx = 0f,
            minTaskWidthPx = 0f,
            rowLabelReservedWidthPx = 0f,
            xAxisLabelReservedHeightPx = 0f,
        ),
        style = styleOptions,
        presentation = presentationOptions,
    )

    private val sourceTasks = mutableListOf<GanttTask>()
    private val sourceDependencies = mutableListOf<GanttDependency>()
    private val placedTasks = mutableListOf<GanttPlacedTask>()

    private var animator: ValueAnimator? = null
    private var renderProgress = 1f
    private var selectedTaskIndex: Int? = null
    private var onTaskClickListener: ((Int, GanttTask) -> Unit)? = null

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val dependencyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val taskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val todayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val milestoneStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val axisLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }
    private val rowLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.RIGHT
    }
    private val taskLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.LEFT
    }
    private val emptyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }

    init {
        applyStyle()
    }

    fun setTasks(items: List<GanttTask>) {
        sourceTasks.clear()
        sourceTasks.addAll(items)
        selectedTaskIndex = null
        refreshAndRender()
    }

    fun setDependencies(items: List<GanttDependency>) {
        sourceDependencies.clear()
        sourceDependencies.addAll(items)
        refreshAndRender()
    }

    fun setStyleOptions(options: GanttChartStyleOptions) {
        styleOptions = options
        applyStyle()
        refreshAndRender()
    }

    fun setPresentationOptions(options: GanttChartPresentationOptions) {
        presentationOptions = options
        applyStyle()
        refreshAndRender()
    }

    fun setOnTaskClickListener(listener: (index: Int, task: GanttTask) -> Unit) {
        onTaskClickListener = listener
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_UP -> {
                val hit = hitTestGanttTask(placement, event.x, event.y)
                selectedTaskIndex = hit?.task?.index
                if (hit != null) {
                    onTaskClickListener?.invoke(hit.task.index, sourceTasks[hit.task.index])
                    performClick()
                }
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        rebuildPlacement()

        if (layout.tasks.isEmpty()) {
            val x = width * 0.5f
            val y = height * 0.5f - (emptyTextPaint.descent() + emptyTextPaint.ascent()) * 0.5f
            canvas.drawText(presentationOptions.emptyText, x, y, emptyTextPaint)
            return
        }

        if (presentationOptions.showGrid) drawGrid(canvas)
        if (presentationOptions.showTodayIndicator) drawTodayIndicator(canvas)
        if (presentationOptions.showAxes) drawAxes(canvas)
        if (presentationOptions.showDependencies) drawDependencies(canvas)
        drawTasks(canvas)
        drawRowLabels(canvas)
        drawXAxisLabels(canvas)
    }

    private fun Float.dpToPx(): Float = this * density

    private fun Float.spToPx(): Float = this * scaledDensity

    private fun applyStyle() {
        backgroundPaint.color = styleOptions.backgroundColor
        gridPaint.color = styleOptions.gridColor
        gridPaint.strokeWidth = 1f * density
        axisPaint.color = styleOptions.axisColor
        axisPaint.strokeWidth = 1.1f * density
        dependencyPaint.color = styleOptions.dependencyLineColor
        dependencyPaint.strokeWidth = styleOptions.dependencyStrokeWidthDp.dpToPx()
        todayPaint.color = styleOptions.todayIndicatorColor
        todayPaint.strokeWidth = 1.5f * density
        selectedPaint.color = styleOptions.selectedTaskStrokeColor
        selectedPaint.strokeWidth = 2f * density
        milestoneStrokePaint.color = styleOptions.milestoneStrokeColor
        milestoneStrokePaint.strokeWidth = 1.25f * density
        axisLabelPaint.color = styleOptions.axisLabelColor
        axisLabelPaint.textSize = presentationOptions.axisLabelTextSizeSp.spToPx()
        rowLabelPaint.color = styleOptions.axisLabelColor
        rowLabelPaint.textSize = presentationOptions.axisLabelTextSizeSp.spToPx()
        taskLabelPaint.color = styleOptions.taskLabelTextColor
        taskLabelPaint.textSize = presentationOptions.taskLabelTextSizeSp.spToPx()
        emptyTextPaint.color = styleOptions.axisLabelColor
        emptyTextPaint.textSize = 14f.spToPx()
    }

    private fun refreshAndRender() {
        layout = resolveGanttChartLayout(
            tasks = sourceTasks,
            dependencies = sourceDependencies,
            style = styleOptions,
            tickCount = presentationOptions.xTickCount,
        )
        if (presentationOptions.animateOnDataChange && layout.tasks.isNotEmpty()) {
            playEnterAnimation()
        } else {
            animator?.cancel()
            renderProgress = 1f
            invalidate()
        }
    }

    private fun playEnterAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = presentationOptions.enterAnimationDurationMs
            startDelay = presentationOptions.enterAnimationDelayMs
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                renderProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun rebuildPlacement() {
        val paddingPx = styleOptions.contentPaddingDp.dpToPx()
        val rowLabelReservedWidth = (layout.tasks.maxOfOrNull {
            rowLabelPaint.measureText(presentationOptions.rowLabelFormatter(it.label))
        } ?: 0f) + 16f * density
        val xAxisReserve = axisLabelPaint.fontSpacing + 18f * density
        placement = resolveGanttChartPlacement(
            layout = layout,
            config = GanttChartLayoutConfig(
                widthPx = width.toFloat(),
                heightPx = height.toFloat(),
                contentPaddingPx = paddingPx,
                rowSpacingPx = styleOptions.rowSpacingDp.dpToPx(),
                minTaskWidthPx = styleOptions.minTaskWidthDp.dpToPx(),
                rowLabelReservedWidthPx = rowLabelReservedWidth,
                xAxisLabelReservedHeightPx = xAxisReserve,
                taskHeightRatio = styleOptions.taskHeightRatio,
            ),
            style = styleOptions.copy(
                milestoneSizeDp = styleOptions.milestoneSizeDp.dpToPx(),
                dependencyArrowSizeDp = styleOptions.dependencyArrowSizeDp.dpToPx(),
                dependencyStrokeWidthDp = styleOptions.dependencyStrokeWidthDp.dpToPx(),
            ),
            presentation = presentationOptions,
        )
        placedTasks.clear()
        placedTasks.addAll(placement.tasks)
    }

    private fun drawGrid(canvas: Canvas) {
        placement.ticks.forEach { tick ->
            canvas.drawLine(tick.xPx, placement.plotTopPx, tick.xPx, placement.plotBottomPx, gridPaint)
        }
    }

    private fun drawTodayIndicator(canvas: Canvas) {
        val x = placement.todayIndicatorXPx ?: return
        todayPaint.alpha = (255f * renderProgress).toInt().coerceIn(0, 255)
        canvas.drawLine(x, placement.plotTopPx, x, placement.plotBottomPx, todayPaint)
    }

    private fun drawAxes(canvas: Canvas) {
        canvas.drawLine(placement.plotLeftPx, placement.plotBottomPx, placement.plotRightPx, placement.plotBottomPx, axisPaint)
        canvas.drawLine(placement.plotLeftPx, placement.plotTopPx, placement.plotLeftPx, placement.plotBottomPx, axisPaint)
    }

    private fun drawDependencies(canvas: Canvas) {
        val alpha = (255f * renderProgress).toInt().coerceIn(0, 255)
        placement.dependencies.forEach { dependency ->
            dependencyPaint.color = dependency.dependency.color
            dependencyPaint.alpha = alpha
            for (index in 0 until dependency.points.lastIndex) {
                val start = dependency.points[index]
                val end = dependency.points[index + 1]
                canvas.drawLine(start.xPx, start.yPx, end.xPx, end.yPx, dependencyPaint)
            }
            canvas.drawLine(
                dependency.arrowTip.xPx,
                dependency.arrowTip.yPx,
                dependency.arrowWingA.xPx,
                dependency.arrowWingA.yPx,
                dependencyPaint,
            )
            canvas.drawLine(
                dependency.arrowTip.xPx,
                dependency.arrowTip.yPx,
                dependency.arrowWingB.xPx,
                dependency.arrowWingB.yPx,
                dependencyPaint,
            )
        }
    }

    private fun drawTasks(canvas: Canvas) {
        val radius = styleOptions.taskCornerRadiusDp.dpToPx()
        val selectedPadding = styleOptions.selectedTaskPaddingDp.dpToPx()
        val alpha = (255f * renderProgress).toInt().coerceIn(0, 255)

        placedTasks.forEach { task ->
            taskPaint.color = task.task.color
            taskPaint.alpha = alpha
            progressPaint.color = styleOptions.progressBarColor
            progressPaint.alpha = (alpha * 0.9f).toInt().coerceIn(0, 255)
            milestoneStrokePaint.alpha = alpha
            selectedPaint.alpha = alpha

            if (task.task.isMilestone) {
                val path = diamondPath(task)
                canvas.drawPath(path, taskPaint)
                canvas.drawPath(path, milestoneStrokePaint)
                if (selectedTaskIndex == task.task.index) {
                    canvas.drawPath(path, selectedPaint)
                }
            } else {
                val rect = RectF(task.leftPx, task.topPx, task.rightPx, task.bottomPx)
                canvas.drawRoundRect(rect, radius, radius, taskPaint)
                if (presentationOptions.showProgress && task.task.progress > 0f) {
                    val progressRect = RectF(task.leftPx, task.topPx, task.progressRightPx, task.bottomPx)
                    canvas.drawRoundRect(progressRect, radius, radius, progressPaint)
                }
                if (selectedTaskIndex == task.task.index) {
                    val outline = RectF(rect)
                    outline.inset(-selectedPadding, -selectedPadding)
                    canvas.drawRoundRect(outline, radius, radius, selectedPaint)
                }
            }

            if (presentationOptions.showTaskLabels) {
                drawTaskLabel(canvas, task)
            }
        }
    }

    private fun drawTaskLabel(canvas: Canvas, task: GanttPlacedTask) {
        val label = task.labelText
        val baseline = task.centerYPx - (taskLabelPaint.descent() + taskLabelPaint.ascent()) * 0.5f
        val insidePadding = 8f * density
        val textWidth = taskLabelPaint.measureText(label)
        if (!task.task.isMilestone && (task.rightPx - task.leftPx) >= textWidth + insidePadding * 2f) {
            taskLabelPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(label, task.leftPx + insidePadding, baseline, taskLabelPaint)
        } else {
            taskLabelPaint.textAlign = Paint.Align.LEFT
            val x = minOf(task.rightPx + 8f * density, width - styleOptions.contentPaddingDp.dpToPx())
            canvas.drawText(label, x, baseline, taskLabelPaint)
        }
    }

    private fun drawRowLabels(canvas: Canvas) {
        val labelX = placement.plotLeftPx - 10f * density
        placement.rowLabels.forEach { label ->
            val baseline = label.centerYPx - (rowLabelPaint.descent() + rowLabelPaint.ascent()) * 0.5f
            canvas.drawText(label.label, labelX, baseline, rowLabelPaint)
        }
    }

    private fun drawXAxisLabels(canvas: Canvas) {
        placement.ticks.forEach { tick ->
            canvas.drawText(
                tick.label,
                tick.xPx,
                placement.plotBottomPx + axisLabelPaint.fontSpacing,
                axisLabelPaint,
            )
        }
    }

    private fun diamondPath(task: GanttPlacedTask): Path {
        val halfWidth = max(1f, (task.rightPx - task.leftPx) * 0.5f)
        val halfHeight = max(1f, (task.bottomPx - task.topPx) * 0.5f)
        return Path().apply {
            moveTo(task.startXPx, task.topPx)
            lineTo(task.startXPx + halfWidth, task.centerYPx)
            lineTo(task.startXPx, task.bottomPx)
            lineTo(task.startXPx - halfWidth, task.centerYPx)
            close()
        }
    }
}
