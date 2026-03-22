package com.magimon.eq.rangebar

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.max
import kotlin.math.min

/**
 * Horizontal range bar and timeline chart renderer for Android Views.
 *
 * Each row draws one [RangeBarEntry] across a shared numeric X axis, making
 * the chart suitable for timelines, schedules, roadmaps, and duration ranges.
 */
class RangeBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private data class RenderBar(
        val rect: RectF,
        val entry: RangeBarLayoutEntry,
    )

    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity

    private var styleOptions = RangeBarChartStyleOptions()
    private var presentationOptions = RangeBarChartPresentationOptions()
    private var layout = resolveRangeBarChartLayout(emptyList(), styleOptions, presentationOptions.xTickCount)

    private val sourceEntries = mutableListOf<RangeBarEntry>()
    private val bars = mutableListOf<RenderBar>()

    private var animator: ValueAnimator? = null
    private var renderProgress = 1f
    private var selectedBar: RenderBar? = null
    private var onEntryClickListener: ((Int, RangeBarEntry) -> Unit)? = null

    private val chartArea = RectF()
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val selectedBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val axisLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }
    private val rowLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.RIGHT
    }
    private val rangeLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
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

    /**
     * Replaces all rendered entries.
     */
    fun setEntries(items: List<RangeBarEntry>) {
        sourceEntries.clear()
        sourceEntries.addAll(items)
        selectedBar = null
        refreshAndRender()
    }

    /**
     * Replaces entries by mapping arbitrary source objects.
     */
    fun <T> setEntries(items: List<T>, mapper: (T) -> RangeBarEntry) {
        setEntries(items.map(mapper))
    }

    /**
     * Sets chart style options.
     */
    fun setStyleOptions(options: RangeBarChartStyleOptions) {
        styleOptions = options
        applyStyle()
        refreshAndRender()
    }

    /**
     * Sets chart presentation options.
     */
    fun setPresentationOptions(options: RangeBarChartPresentationOptions) {
        presentationOptions = options
        applyStyle()
        refreshAndRender()
    }

    /**
     * Sets the interval click callback.
     *
     * Callback format: `(index, entry)`.
     */
    fun setOnEntryClickListener(listener: (index: Int, entry: RangeBarEntry) -> Unit) {
        onEntryClickListener = listener
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
                val hit = bars.lastOrNull { it.rect.contains(event.x, event.y) }
                selectedBar = hit
                if (hit != null) {
                    onEntryClickListener?.invoke(hit.entry.index, sourceEntries[hit.entry.index])
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

        if (layout.entries.isEmpty()) {
            val x = width * 0.5f
            val y = height * 0.5f - (emptyTextPaint.descent() + emptyTextPaint.ascent()) * 0.5f
            canvas.drawText(presentationOptions.emptyText, x, y, emptyTextPaint)
            return
        }

        if (presentationOptions.showGrid) drawGrid(canvas)
        if (presentationOptions.showAxes) drawAxes(canvas)
        drawBars(canvas)
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
        selectedBarPaint.color = styleOptions.selectedBarStrokeColor
        selectedBarPaint.strokeWidth = 2f * density
        axisLabelPaint.color = styleOptions.axisLabelColor
        axisLabelPaint.textSize = presentationOptions.axisLabelTextSizeSp.spToPx()
        rowLabelPaint.color = styleOptions.axisLabelColor
        rowLabelPaint.textSize = presentationOptions.axisLabelTextSizeSp.spToPx()
        rangeLabelPaint.color = styleOptions.barLabelTextColor
        rangeLabelPaint.textSize = presentationOptions.barLabelTextSizeSp.spToPx()
        emptyTextPaint.color = styleOptions.axisLabelColor
        emptyTextPaint.textSize = 14f.spToPx()
    }

    private fun refreshAndRender() {
        layout = resolveRangeBarChartLayout(
            entries = sourceEntries,
            style = styleOptions,
            tickCount = presentationOptions.xTickCount,
        )
        if (presentationOptions.animateOnDataChange && layout.entries.isNotEmpty()) {
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
        val maxRowLabelWidth = layout.entries.maxOfOrNull {
            rowLabelPaint.measureText(presentationOptions.rowLabelFormatter(it.label))
        } ?: 0f
        val bottomReserve = padding + axisLabelPaint.fontSpacing + 18f * density
        val topReserve = padding + if (presentationOptions.showBarLabels) rangeLabelPaint.fontSpacing + 8f * density else 8f * density
        val leftReserve = padding + maxRowLabelWidth + 16f * density
        chartArea.set(
            leftReserve,
            topReserve,
            width - padding,
            height - bottomReserve,
        )
    }

    private fun valueToX(value: Double): Float {
        val minValue = layout.minValue
        val maxValue = layout.maxValue
        val ratio = if (maxValue == minValue) 0.5f else ((value - minValue) / (maxValue - minValue)).toFloat()
        return chartArea.left + ratio * chartArea.width()
    }

    private fun animatedEndValue(entry: RangeBarLayoutEntry): Double {
        if (!presentationOptions.animationDirection) return entry.endValue
        return entry.startValue + (entry.endValue - entry.startValue) * renderProgress.toDouble()
    }

    private fun buildRenderCache() {
        bars.clear()
        if (layout.entries.isEmpty() || chartArea.width() <= 0f || chartArea.height() <= 0f) return

        val rowGap = styleOptions.rowSpacingDp.dpToPx()
        val slotHeight = chartArea.height() / layout.entries.size.toFloat()
        val barHeight = ((slotHeight - rowGap).coerceAtLeast(1f) * styleOptions.barHeightRatio.coerceIn(0.2f, 1f)).coerceAtLeast(1f)

        layout.entries.forEachIndexed { rowIndex, entry ->
            val top = chartArea.top + rowIndex * slotHeight + (slotHeight - barHeight) * 0.5f
            val bottom = top + barHeight
            val startX = valueToX(entry.startValue)
            val endX = valueToX(animatedEndValue(entry))
            bars += RenderBar(
                rect = RectF(
                    min(startX, endX),
                    top,
                    max(startX + styleOptions.minBarWidthDp.dpToPx(), max(startX, endX)),
                    bottom,
                ),
                entry = entry,
            )
        }
    }

    private fun drawGrid(canvas: Canvas) {
        layout.ticks.forEach { tick ->
            val x = valueToX(tick)
            canvas.drawLine(x, chartArea.top, x, chartArea.bottom, gridPaint)
        }
    }

    private fun drawAxes(canvas: Canvas) {
        canvas.drawLine(chartArea.left, chartArea.bottom, chartArea.right, chartArea.bottom, axisPaint)
        canvas.drawLine(chartArea.left, chartArea.top, chartArea.left, chartArea.bottom, axisPaint)
    }

    private fun drawBars(canvas: Canvas) {
        val radius = styleOptions.barCornerRadiusDp.dpToPx()
        val selectedPadding = styleOptions.selectedBarPaddingDp.dpToPx()
        bars.forEach { bar ->
            barPaint.color = bar.entry.color
            canvas.drawRoundRect(bar.rect, radius, radius, barPaint)
            if (selectedBar === bar) {
                val outlineRect = RectF(bar.rect)
                outlineRect.inset(-selectedPadding, -selectedPadding)
                canvas.drawRoundRect(outlineRect, radius, radius, selectedBarPaint)
            }
            if (presentationOptions.showBarLabels) {
                drawRangeLabel(canvas, bar)
            }
        }
    }

    private fun drawRangeLabel(canvas: Canvas, bar: RenderBar) {
        val label = presentationOptions.barLabelFormatter(bar.entry)
        val baseline = bar.rect.centerY() - (rangeLabelPaint.descent() + rangeLabelPaint.ascent()) * 0.5f
        val textWidth = rangeLabelPaint.measureText(label)
        val insidePadding = 8f * density
        if (bar.rect.width() >= textWidth + (insidePadding * 2f)) {
            rangeLabelPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(label, bar.rect.left + insidePadding, baseline, rangeLabelPaint)
        } else {
            rangeLabelPaint.textAlign = Paint.Align.LEFT
            val x = (bar.rect.right + 6f * density).coerceAtMost(width - styleOptions.contentPaddingDp.dpToPx())
            canvas.drawText(label, x, baseline, rangeLabelPaint)
        }
    }

    private fun drawRowLabels(canvas: Canvas) {
        val labelX = chartArea.left - 10f * density
        bars.forEach { bar ->
            val baseline = bar.rect.centerY() - (rowLabelPaint.descent() + rowLabelPaint.ascent()) * 0.5f
            canvas.drawText(presentationOptions.rowLabelFormatter(bar.entry.label), labelX, baseline, rowLabelPaint)
        }
    }

    private fun drawXAxisLabels(canvas: Canvas) {
        val baseline = chartArea.bottom + axisLabelPaint.fontSpacing
        layout.ticks.forEach { tick ->
            canvas.drawText(
                presentationOptions.xLabelFormatter(tick),
                valueToX(tick),
                baseline,
                axisLabelPaint,
            )
        }
    }
}
