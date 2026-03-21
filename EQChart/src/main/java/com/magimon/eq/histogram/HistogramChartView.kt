package com.magimon.eq.histogram

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
 * Cartesian histogram chart renderer.
 *
 * Supports ordered bins, value labels, and click callbacks.
 */
class HistogramChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private data class RenderBar(
        val rect: RectF,
        val bin: HistogramLayoutBin,
    )

    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity

    private var styleOptions = HistogramChartStyleOptions()
    private var presentationOptions = HistogramChartPresentationOptions()
    private var layout = resolveHistogramChartLayout(emptyList(), styleOptions, presentationOptions)

    private val sourceBins = mutableListOf<HistogramBin>()
    private val bars = mutableListOf<RenderBar>()

    private var animator: ValueAnimator? = null
    private var renderProgress = 1f
    private var selectedBar: RenderBar? = null
    private var onBinClickListener: ((Int, HistogramBin, Double) -> Unit)? = null

    private val chartArea = RectF()
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val selectedBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
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

    /**
     * Replaces all histogram bins.
     */
    fun setBins(items: List<HistogramBin>) {
        sourceBins.clear()
        sourceBins.addAll(items)
        selectedBar = null
        refreshAndRender()
    }

    /**
     * Replaces bins by mapping arbitrary objects.
     */
    fun <T> setBins(items: List<T>, mapper: (T) -> HistogramBin) {
        setBins(items.map(mapper))
    }

    /**
     * Sets render style options.
     */
    fun setStyleOptions(options: HistogramChartStyleOptions) {
        styleOptions = options
        applyStyle()
        refreshAndRender()
    }

    /**
     * Sets presentation behavior.
     */
    fun setPresentationOptions(options: HistogramChartPresentationOptions) {
        presentationOptions = options
        applyStyle()
        refreshAndRender()
    }

    /**
     * Sets bin click callback.
     *
     * Callback format: `(index, bin, value)`.
     */
    fun setOnBinClickListener(listener: (Int, HistogramBin, Double) -> Unit) {
        onBinClickListener = listener
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
                    onBinClickListener?.invoke(
                        hit.bin.index,
                        HistogramBin(
                            start = hit.bin.start,
                            end = hit.bin.end,
                            value = hit.bin.value,
                            label = hit.bin.label,
                            color = hit.bin.color,
                            payload = hit.bin.payload,
                        ),
                        hit.bin.value,
                    )
                    performClick()
                }
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun Float.dpToPx(): Float = this * density

    private fun Float.spToPx(): Float = this * scaledDensity

    private fun applyStyle() {
        backgroundPaint.color = styleOptions.backgroundColor
        gridPaint.color = styleOptions.gridColor
        gridPaint.strokeWidth = 1f * density
        axisPaint.color = styleOptions.axisColor
        axisPaint.strokeWidth = 1.1f * density
        selectedBarPaint.color = styleOptions.axisColor
        selectedBarPaint.strokeWidth = 2f * density
        axisLabelPaint.color = styleOptions.axisLabelColor
        axisLabelPaint.textSize = presentationOptions.axisLabelTextSizeSp.spToPx()
        categoryLabelPaint.color = styleOptions.axisLabelColor
        categoryLabelPaint.textSize = presentationOptions.axisLabelTextSizeSp.spToPx()
        valueLabelPaint.color = styleOptions.barValueTextColor
        valueLabelPaint.textSize = presentationOptions.valueLabelTextSizeSp.spToPx()
        emptyTextPaint.color = styleOptions.axisLabelColor
        emptyTextPaint.textSize = 14f.spToPx()
    }

    private fun refreshAndRender() {
        layout = resolveHistogramChartLayout(
            bins = sourceBins,
            style = styleOptions,
            presentation = presentationOptions,
        )
        if (presentationOptions.animateOnDataChange && layout.bins.isNotEmpty()) {
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
        val topReserve = padding + if (presentationOptions.showBarLabels) valueLabelPaint.fontSpacing + 8f * density else 8f * density
        chartArea.set(
            leftReserve,
            topReserve,
            width - padding,
            height - bottomReserve,
        )
    }

    private fun valueToY(value: Double): Float {
        val minValue = layout.minValue
        val maxValue = layout.maxValue
        val ratio = if (maxValue == minValue) 0.5f else ((value - minValue) / (maxValue - minValue)).toFloat()
        return chartArea.bottom - ratio * chartArea.height()
    }

    private fun animatedValue(value: Double): Double {
        if (!presentationOptions.animationDirection) return value
        return layout.baselineValue + (value - layout.baselineValue) * renderProgress.toDouble()
    }

    private fun buildRenderCache() {
        bars.clear()
        if (layout.bins.isEmpty() || chartArea.width() <= 0f || chartArea.height() <= 0f) return

        val slotWidth = chartArea.width() / layout.bins.size.toFloat()
        val categoryGap = styleOptions.categorySpacingDp.dpToPx()
        val barWidth = (slotWidth - categoryGap).coerceAtLeast(1f)

        layout.bins.forEachIndexed { index, bin ->
            val left = chartArea.left + index * slotWidth + categoryGap * 0.5f
            val right = left + barWidth
            val startY = valueToY(layout.baselineValue)
            val endY = valueToY(animatedValue(bin.value))
            bars.add(
                RenderBar(
                    rect = RectF(
                        left,
                        min(startY, endY),
                        right,
                        max(startY, endY),
                    ),
                    bin = bin,
                ),
            )
        }
    }

    private fun drawGridAndAxes(canvas: Canvas) {
        if (presentationOptions.showGrid) {
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

        if (presentationOptions.showAxes) {
            val baselineY = valueToY(layout.baselineValue)
            canvas.drawLine(chartArea.left, baselineY, chartArea.right, baselineY, axisPaint)
            canvas.drawLine(chartArea.left, chartArea.top, chartArea.left, chartArea.bottom, axisPaint)
        }
    }

    private fun drawBars(canvas: Canvas) {
        val radius = styleOptions.barCornerRadiusDp.dpToPx()
        bars.forEach { bar ->
            barPaint.color = bar.bin.color
            canvas.drawRoundRect(bar.rect, radius, radius, barPaint)

            if (selectedBar === bar) {
                val padding = styleOptions.selectedBarPaddingDp.dpToPx()
                val selectedRect = RectF(bar.rect).apply { inset(-padding, -padding) }
                canvas.drawRoundRect(selectedRect, radius, radius, selectedBarPaint)
            }
        }
    }

    private fun drawValueLabels(canvas: Canvas) {
        if (!presentationOptions.showBarLabels) return

        bars.forEach { bar ->
            val y = if (bar.bin.value >= layout.baselineValue) {
                bar.rect.top - 6f * density
            } else {
                bar.rect.bottom + valueLabelPaint.textSize + 2f * density
            }
            canvas.drawText(
                presentationOptions.valueLabelFormatter(bar.bin.value),
                bar.rect.centerX(),
                y,
                valueLabelPaint,
            )
        }
    }

    private fun drawCategoryLabels(canvas: Canvas) {
        bars.forEach { bar ->
            canvas.drawText(
                bar.bin.label,
                bar.rect.centerX(),
                chartArea.bottom + categoryLabelPaint.textSize + 6f * density,
                categoryLabelPaint,
            )
        }
    }

    private fun drawEmptyState(canvas: Canvas) {
        canvas.drawText(
            presentationOptions.emptyText,
            width * 0.5f,
            height * 0.5f,
            emptyTextPaint,
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        updateChartArea()
        if (layout.bins.isEmpty() || chartArea.width() <= 0f || chartArea.height() <= 0f) {
            drawEmptyState(canvas)
            return
        }

        buildRenderCache()
        drawGridAndAxes(canvas)
        drawBars(canvas)
        drawValueLabels(canvas)
        drawCategoryLabels(canvas)
    }
}
