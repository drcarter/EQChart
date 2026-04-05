package com.magimon.eq.boxplot

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.max
import kotlin.math.min

/**
 * Cartesian box plot chart renderer.
 */
class BoxPlotChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private data class RenderEntry(
        val touchRect: RectF,
        val boxRect: RectF,
        val centerX: Float,
        val minY: Float,
        val maxY: Float,
        val medianY: Float,
        val capHalfWidth: Float,
        val outliers: List<PointF>,
        val entry: BoxPlotLayoutEntry,
    )

    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity

    private var styleOptions = BoxPlotChartStyleOptions()
    private var presentationOptions = BoxPlotChartPresentationOptions()
    private var layout = resolveBoxPlotChartLayout(emptyList(), styleOptions, presentationOptions)

    private val sourceEntries = mutableListOf<BoxPlotEntry>()
    private val renderEntries = mutableListOf<RenderEntry>()

    private var animator: ValueAnimator? = null
    private var renderProgress = 1f
    private var selectedEntry: RenderEntry? = null
    private var onEntryClickListener: ((Int, BoxPlotEntry) -> Unit)? = null

    private val chartArea = RectF()
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val whiskerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val medianPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val outlierPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
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

    fun setEntries(items: List<BoxPlotEntry>) {
        sourceEntries.clear()
        sourceEntries.addAll(items)
        selectedEntry = null
        refreshAndRender()
    }

    fun <T> setEntries(items: List<T>, mapper: (T) -> BoxPlotEntry) {
        setEntries(items.map(mapper))
    }

    fun setStyleOptions(options: BoxPlotChartStyleOptions) {
        styleOptions = options
        applyStyle()
        refreshAndRender()
    }

    fun setPresentationOptions(options: BoxPlotChartPresentationOptions) {
        presentationOptions = options
        applyStyle()
        refreshAndRender()
    }

    fun setOnEntryClickListener(listener: (Int, BoxPlotEntry) -> Unit) {
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
                val hit = renderEntries.lastOrNull { it.touchRect.contains(event.x, event.y) }
                selectedEntry = hit
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
        whiskerPaint.color = styleOptions.axisColor
        whiskerPaint.strokeWidth = styleOptions.whiskerStrokeWidthDp.dpToPx()
        medianPaint.color = styleOptions.medianLineColor
        medianPaint.strokeWidth = max(1.4f * density, styleOptions.whiskerStrokeWidthDp.dpToPx())
        outlierPaint.color = styleOptions.outlierColor
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
        layout = resolveBoxPlotChartLayout(
            entries = sourceEntries,
            style = styleOptions,
            presentation = presentationOptions,
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

    private fun animatedValue(value: Double): Double {
        if (!presentationOptions.animationDirection) return value
        return layout.minValue + (value - layout.minValue) * renderProgress.toDouble()
    }

    private fun buildRenderCache() {
        renderEntries.clear()
        if (layout.entries.isEmpty() || chartArea.width() <= 0f || chartArea.height() <= 0f) return

        val categoryGap = styleOptions.categorySpacingDp.dpToPx()
        val slotWidth = chartArea.width() / layout.entries.size.toFloat()
        val usableSlot = (slotWidth - categoryGap).coerceAtLeast(1f)
        val boxWidth = (usableSlot * styleOptions.boxWidthRatio.coerceIn(0.2f, 0.9f)).coerceAtLeast(8f)
        val capHalfWidth = boxWidth * 0.35f

        layout.entries.forEachIndexed { index, entry ->
            val slotLeft = chartArea.left + slotWidth * index
            val slotRight = slotLeft + slotWidth
            val centerX = (slotLeft + slotRight) * 0.5f
            val minY = valueToY(animatedValue(entry.min))
            val q1Y = valueToY(animatedValue(entry.q1))
            val medianY = valueToY(animatedValue(entry.median))
            val q3Y = valueToY(animatedValue(entry.q3))
            val maxY = valueToY(animatedValue(entry.max))
            val boxRect = RectF(
                centerX - boxWidth * 0.5f,
                min(q3Y, q1Y),
                centerX + boxWidth * 0.5f,
                max(q3Y, q1Y),
            )
            val touchRect = RectF(
                slotLeft,
                min(maxY, boxRect.top),
                slotRight,
                max(minY, boxRect.bottom),
            )
            val outliers = entry.outliers.map { outlier ->
                PointF(centerX, valueToY(animatedValue(outlier)))
            }
            renderEntries += RenderEntry(
                touchRect = touchRect,
                boxRect = boxRect,
                centerX = centerX,
                minY = minY,
                maxY = maxY,
                medianY = medianY,
                capHalfWidth = capHalfWidth,
                outliers = outliers,
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
        val boxRadius = styleOptions.boxCornerRadiusDp.dpToPx()
        val outlierRadius = styleOptions.outlierRadiusDp.dpToPx()
        val selectedPadding = styleOptions.selectedPaddingDp.dpToPx()

        renderEntries.forEach { renderEntry ->
            boxPaint.color = renderEntry.entry.color

            canvas.drawLine(
                renderEntry.centerX,
                renderEntry.maxY,
                renderEntry.centerX,
                renderEntry.boxRect.top,
                whiskerPaint,
            )
            canvas.drawLine(
                renderEntry.centerX,
                renderEntry.boxRect.bottom,
                renderEntry.centerX,
                renderEntry.minY,
                whiskerPaint,
            )
            canvas.drawLine(
                renderEntry.centerX - renderEntry.capHalfWidth,
                renderEntry.maxY,
                renderEntry.centerX + renderEntry.capHalfWidth,
                renderEntry.maxY,
                whiskerPaint,
            )
            canvas.drawLine(
                renderEntry.centerX - renderEntry.capHalfWidth,
                renderEntry.minY,
                renderEntry.centerX + renderEntry.capHalfWidth,
                renderEntry.minY,
                whiskerPaint,
            )

            canvas.drawRoundRect(renderEntry.boxRect, boxRadius, boxRadius, boxPaint)
            canvas.drawLine(
                renderEntry.boxRect.left,
                renderEntry.medianY,
                renderEntry.boxRect.right,
                renderEntry.medianY,
                medianPaint,
            )

            renderEntry.outliers.forEach { outlier ->
                canvas.drawCircle(outlier.x, outlier.y, outlierRadius, outlierPaint)
            }

            if (selectedEntry === renderEntry) {
                val outline = RectF(
                    renderEntry.boxRect.left,
                    min(renderEntry.maxY, renderEntry.boxRect.top),
                    renderEntry.boxRect.right,
                    max(renderEntry.minY, renderEntry.boxRect.bottom),
                )
                outline.inset(-selectedPadding, -selectedPadding)
                canvas.drawRoundRect(outline, boxRadius, boxRadius, selectedPaint)
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
            val y = min(renderEntry.maxY, renderEntry.boxRect.top) - 6f * density
            canvas.drawText(
                presentationOptions.valueLabelFormatter(renderEntry.entry),
                renderEntry.centerX,
                y,
                valueLabelPaint,
            )
        }
    }
}
