package com.magimon.eq.violin

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.max
import kotlin.math.min

/**
 * Cartesian violin plot renderer.
 */
class ViolinPlotChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private data class RenderEntry(
        val touchRect: RectF,
        val centerX: Float,
        val maxHalfWidth: Float,
        val topY: Float,
        val bottomY: Float,
        val q1Y: Float,
        val medianY: Float,
        val q3Y: Float,
        val outlinePoints: List<PointF>,
        val entry: ViolinPlotLayoutSeries,
    )

    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity

    private var styleOptions = ViolinPlotChartStyleOptions()
    private var presentationOptions = ViolinPlotChartPresentationOptions()
    private var layout = resolveViolinPlotChartLayout(emptyList(), styleOptions, presentationOptions)

    private val sourceSeries = mutableListOf<ViolinPlotSeries>()
    private val renderEntries = mutableListOf<RenderEntry>()

    private var animator: ValueAnimator? = null
    private var renderProgress = 1f
    private var selectedEntry: RenderEntry? = null
    private var onSeriesClickListener: ((Int, ViolinPlotSeries) -> Unit)? = null

    private val chartArea = RectF()
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val violinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val quartileBandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val medianPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val axisLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.RIGHT
    }
    private val categoryLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }
    private val valueLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }
    private val emptyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }

    init {
        applyStyle()
    }

    fun setSeries(items: List<ViolinPlotSeries>) {
        sourceSeries.clear()
        sourceSeries.addAll(items)
        selectedEntry = null
        refreshAndRender()
    }

    fun <T> setSeries(items: List<T>, mapper: (T) -> ViolinPlotSeries) {
        setSeries(items.map(mapper))
    }

    fun setStyleOptions(options: ViolinPlotChartStyleOptions) {
        styleOptions = options
        applyStyle()
        refreshAndRender()
    }

    fun setPresentationOptions(options: ViolinPlotChartPresentationOptions) {
        presentationOptions = options
        applyStyle()
        refreshAndRender()
    }

    fun setOnSeriesClickListener(listener: (Int, ViolinPlotSeries) -> Unit) {
        onSeriesClickListener = listener
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
                val hit = renderEntries.lastOrNull { it.touchRect.contains(event.x, event.y) }
                selectedEntry = hit
                if (hit != null) {
                    onSeriesClickListener?.invoke(hit.entry.index, sourceSeries[hit.entry.index])
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
        updateChartArea()
        buildRenderCache()

        if (layout.series.isEmpty()) {
            val x = width * 0.5f
            val y = height * 0.5f - (emptyTextPaint.descent() + emptyTextPaint.ascent()) * 0.5f
            canvas.drawText(presentationOptions.emptyText, x, y, emptyTextPaint)
            return
        }

        if (presentationOptions.showGrid) drawGrid(canvas)
        if (presentationOptions.showAxes) drawAxes(canvas)
        drawEntries(canvas)
        drawCategoryLabels(canvas)
        drawValueLabels(canvas)
    }

    private fun Float.dpToPx(): Float = this * density

    private fun Float.spToPx(): Float = this * scaledDensity

    private fun applyStyle() {
        backgroundPaint.color = styleOptions.backgroundColor
        gridPaint.color = styleOptions.gridColor
        gridPaint.strokeWidth = 1f * density
        axisPaint.color = styleOptions.axisColor
        axisPaint.strokeWidth = 1.1f * density
        quartileBandPaint.color = styleOptions.quartileBandColor
        medianPaint.color = styleOptions.medianLineColor
        medianPaint.strokeWidth = max(1.4f * density, styleOptions.violinStrokeWidthDp.dpToPx())
        selectedPaint.color = styleOptions.selectedOutlineColor
        selectedPaint.strokeWidth = 2f * density
        axisLabelPaint.color = styleOptions.axisLabelColor
        axisLabelPaint.textSize = presentationOptions.axisLabelTextSizeSp.spToPx()
        categoryLabelPaint.color = styleOptions.axisLabelColor
        categoryLabelPaint.textSize = presentationOptions.axisLabelTextSizeSp.spToPx()
        valueLabelPaint.color = styleOptions.valueLabelTextColor
        valueLabelPaint.textSize = presentationOptions.valueLabelTextSizeSp.spToPx()
        emptyTextPaint.color = styleOptions.axisLabelColor
        emptyTextPaint.textSize = 14f.spToPx()
    }

    private fun refreshAndRender() {
        layout = resolveViolinPlotChartLayout(
            series = sourceSeries,
            style = styleOptions,
            presentation = presentationOptions,
        )
        if (presentationOptions.animateOnDataChange && layout.series.isNotEmpty()) {
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

    private fun updateChartArea() {
        val padding = styleOptions.contentPaddingDp.dpToPx()
        val maxTickWidth = layout.ticks.maxOfOrNull {
            axisLabelPaint.measureText(presentationOptions.yLabelFormatter(it))
        } ?: 0f
        val leftReserve = padding + maxTickWidth + 12f * density
        val bottomReserve = padding + categoryLabelPaint.fontSpacing + 18f * density
        val topReserve = padding + if (presentationOptions.showValueLabels) valueLabelPaint.fontSpacing + 8f * density else 8f * density
        chartArea.set(
            leftReserve,
            topReserve,
            width - padding,
            height - bottomReserve,
        )
    }

    private fun valueToY(value: Double): Float {
        val ratio = if (layout.maxValue == layout.minValue) {
            0.5f
        } else {
            ((value - layout.minValue) / (layout.maxValue - layout.minValue)).toFloat()
        }
        return chartArea.bottom - ratio * chartArea.height()
    }

    private fun buildRenderCache() {
        renderEntries.clear()
        if (layout.series.isEmpty() || chartArea.width() <= 0f || chartArea.height() <= 0f) return

        val categoryGap = styleOptions.categorySpacingDp.dpToPx()
        val slotWidth = chartArea.width() / layout.series.size.toFloat()
        val usableSlot = (slotWidth - categoryGap).coerceAtLeast(1f)
        val maxHalfWidth = (usableSlot * styleOptions.violinWidthRatio.coerceIn(0.2f, 0.95f) * 0.5f).coerceAtLeast(6f)

        layout.series.forEachIndexed { index, entry ->
            val slotLeft = chartArea.left + slotWidth * index
            val slotRight = slotLeft + slotWidth
            val centerX = (slotLeft + slotRight) * 0.5f
            val outlinePoints = entry.densityPoints.map { densityPoint ->
                PointF(
                    centerX + maxHalfWidth * densityPoint.widthRatio * renderProgress,
                    valueToY(densityPoint.value),
                )
            }
            val topY = outlinePoints.minOfOrNull { it.y } ?: chartArea.top
            val bottomY = outlinePoints.maxOfOrNull { it.y } ?: chartArea.bottom
            renderEntries += RenderEntry(
                touchRect = RectF(slotLeft, topY, slotRight, bottomY),
                centerX = centerX,
                maxHalfWidth = maxHalfWidth * renderProgress,
                topY = topY,
                bottomY = bottomY,
                q1Y = valueToY(entry.q1),
                medianY = valueToY(entry.median),
                q3Y = valueToY(entry.q3),
                outlinePoints = outlinePoints,
                entry = entry,
            )
        }
    }

    private fun drawGrid(canvas: Canvas) {
        layout.ticks.forEach { tick ->
            val y = valueToY(tick)
            canvas.drawLine(chartArea.left, y, chartArea.right, y, gridPaint)
            canvas.drawText(
                presentationOptions.yLabelFormatter(tick),
                chartArea.left - 8f * density,
                y + axisLabelPaint.textSize * 0.35f,
                axisLabelPaint,
            )
        }
    }

    private fun drawAxes(canvas: Canvas) {
        canvas.drawLine(chartArea.left, chartArea.bottom, chartArea.right, chartArea.bottom, axisPaint)
        canvas.drawLine(chartArea.left, chartArea.top, chartArea.left, chartArea.bottom, axisPaint)
    }

    private fun drawEntries(canvas: Canvas) {
        val path = Path()
        val selectedPadding = styleOptions.selectedPaddingDp.dpToPx()
        renderEntries.forEach { renderEntry ->
            violinPaint.color = renderEntry.entry.color
            buildViolinPath(path, renderEntry)
            canvas.drawPath(path, violinPaint)

            if (presentationOptions.showQuartileBand) {
                val bandHalfWidth = renderEntry.maxHalfWidth * 0.22f
                canvas.drawRoundRect(
                    renderEntry.centerX - bandHalfWidth,
                    min(renderEntry.q3Y, renderEntry.q1Y),
                    renderEntry.centerX + bandHalfWidth,
                    max(renderEntry.q3Y, renderEntry.q1Y),
                    bandHalfWidth,
                    bandHalfWidth,
                    quartileBandPaint,
                )
            }

            if (presentationOptions.showMedianLine) {
                val medianHalfWidth = renderEntry.maxHalfWidth * 0.68f
                canvas.drawLine(
                    renderEntry.centerX - medianHalfWidth,
                    renderEntry.medianY,
                    renderEntry.centerX + medianHalfWidth,
                    renderEntry.medianY,
                    medianPaint,
                )
            }

            if (selectedEntry === renderEntry) {
                val outline = RectF(renderEntry.touchRect)
                outline.inset(-selectedPadding, -selectedPadding)
                canvas.drawRoundRect(outline, selectedPadding, selectedPadding, selectedPaint)
            }
        }
    }

    private fun drawCategoryLabels(canvas: Canvas) {
        renderEntries.forEach { renderEntry ->
            canvas.drawText(
                presentationOptions.xLabelFormatter(renderEntry.entry),
                renderEntry.centerX,
                chartArea.bottom + categoryLabelPaint.fontSpacing,
                categoryLabelPaint,
            )
        }
    }

    private fun drawValueLabels(canvas: Canvas) {
        if (!presentationOptions.showValueLabels) return
        renderEntries.forEach { renderEntry ->
            canvas.drawText(
                presentationOptions.valueLabelFormatter(renderEntry.entry),
                renderEntry.centerX,
                renderEntry.topY - 6f * density,
                valueLabelPaint,
            )
        }
    }

    private fun buildViolinPath(path: Path, renderEntry: RenderEntry) {
        path.reset()
        if (renderEntry.outlinePoints.isEmpty()) return
        val first = renderEntry.outlinePoints.first()
        path.moveTo(renderEntry.centerX, first.y)
        renderEntry.outlinePoints.forEach { point ->
            path.lineTo(point.x, point.y)
        }
        for (index in renderEntry.outlinePoints.lastIndex downTo 0) {
            val point = renderEntry.outlinePoints[index]
            val mirroredX = renderEntry.centerX - (point.x - renderEntry.centerX)
            path.lineTo(mirroredX, point.y)
        }
        path.close()
    }
}
