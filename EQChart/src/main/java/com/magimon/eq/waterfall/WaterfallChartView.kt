package com.magimon.eq.waterfall

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
 * Cartesian waterfall chart renderer.
 *
 * Supports cumulative delta bars, subtotal/total summary bars, connector lines, and click callbacks.
 */
class WaterfallChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private data class RenderBar(
        val rect: RectF,
        val entry: WaterfallLayoutEntry,
    )

    private data class RenderConnector(
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
    )

    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity

    private var styleOptions = WaterfallChartStyleOptions()
    private var presentationOptions = WaterfallChartPresentationOptions()
    private var layout = resolveWaterfallChartLayout(emptyList(), styleOptions, presentationOptions.yTickCount)

    private val sourceEntries = mutableListOf<WaterfallEntry>()
    private val bars = mutableListOf<RenderBar>()
    private val connectors = mutableListOf<RenderConnector>()

    private var animator: ValueAnimator? = null
    private var renderProgress = 1f
    private var selectedBar: RenderBar? = null
    private var onEntryClickListener: ((Int, WaterfallEntry, Double) -> Unit)? = null

    private val chartArea = RectF()
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val connectorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
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
     * Replaces all waterfall entries.
     */
    fun setEntries(items: List<WaterfallEntry>) {
        sourceEntries.clear()
        sourceEntries.addAll(items)
        selectedBar = null
        refreshAndRender()
    }

    /**
     * Replaces entries by mapping arbitrary objects.
     */
    fun <T> setEntries(items: List<T>, mapper: (T) -> WaterfallEntry) {
        setEntries(items.map(mapper))
    }

    /**
     * Sets render style options.
     */
    fun setStyleOptions(options: WaterfallChartStyleOptions) {
        styleOptions = options
        applyStyle()
        refreshAndRender()
    }

    /**
     * Sets presentation behavior.
     */
    fun setPresentationOptions(options: WaterfallChartPresentationOptions) {
        presentationOptions = options
        applyStyle()
        refreshAndRender()
    }

    /**
     * Sets entry click callback.
     *
     * Callback format: `(index, entry, cumulativeTotal)`.
     */
    fun setOnEntryClickListener(listener: (Int, WaterfallEntry, Double) -> Unit) {
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
                    val entry = WaterfallEntry(
                        label = hit.entry.label,
                        value = hit.entry.rawValue,
                        color = hit.entry.color,
                        kind = hit.entry.kind,
                        payload = hit.entry.payload,
                    )
                    onEntryClickListener?.invoke(hit.entry.index, entry, hit.entry.endValue)
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
        connectorPaint.color = styleOptions.connectorColor
        connectorPaint.strokeWidth = styleOptions.connectorStrokeWidthDp.dpToPx()
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
        layout = resolveWaterfallChartLayout(
            entries = sourceEntries,
            style = styleOptions,
            tickCount = presentationOptions.yTickCount,
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

    private fun animatedEndValue(entry: WaterfallLayoutEntry): Double {
        if (!presentationOptions.animationDirection) return entry.endValue
        return entry.startValue + (entry.endValue - entry.startValue) * renderProgress.toDouble()
    }

    private fun displayedConnectorValue(entry: WaterfallLayoutEntry): Double {
        return if (entry.kind == WaterfallEntryKind.DELTA) entry.startValue else animatedEndValue(entry)
    }

    private fun buildRenderCache() {
        bars.clear()
        connectors.clear()
        if (layout.entries.isEmpty() || chartArea.width() <= 0f || chartArea.height() <= 0f) return

        val slotWidth = chartArea.width() / layout.entries.size.toFloat()
        val categoryGap = styleOptions.categorySpacingDp.dpToPx()
        val usableSlot = (slotWidth - categoryGap).coerceAtLeast(1f)
        val barWidth = (usableSlot * styleOptions.barWidthRatio.coerceIn(0.1f, 1f)).coerceAtLeast(1f)
        val cornerRadius = styleOptions.barCornerRadiusDp.dpToPx()

        layout.entries.forEachIndexed { index, entry ->
            val left = chartArea.left + index * slotWidth + (slotWidth - barWidth) * 0.5f
            val right = left + barWidth
            val startY = valueToY(entry.startValue)
            val endY = valueToY(animatedEndValue(entry))
            val rect = RectF(
                left,
                min(startY, endY),
                right,
                max(startY, endY),
            )
            if (rect.height() < cornerRadius) {
                rect.bottom = max(rect.bottom, rect.top + 1f)
            }
            bars.add(RenderBar(rect = rect, entry = entry))
        }

        if (!presentationOptions.showConnectorLines || bars.size < 2) return

        bars.zipWithNext { previous, current ->
            val previousX = previous.rect.right
            val currentX = current.rect.left
            val previousY = valueToY(animatedEndValue(previous.entry))
            val currentY = valueToY(displayedConnectorValue(current.entry))
            connectors.add(
                RenderConnector(
                    startX = previousX,
                    startY = previousY,
                    endX = currentX,
                    endY = currentY,
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

    private fun drawConnectors(canvas: Canvas) {
        connectors.forEach { connector ->
            canvas.drawLine(
                connector.startX,
                connector.startY,
                connector.endX,
                connector.endY,
                connectorPaint,
            )
        }
    }

    private fun drawBars(canvas: Canvas) {
        val radius = styleOptions.barCornerRadiusDp.dpToPx()
        bars.forEach { bar ->
            barPaint.color = bar.entry.color
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
            val label = presentationOptions.valueLabelFormatter(bar.entry.displayValue)
            val isPositive = bar.entry.endValue >= bar.entry.startValue
            val y = if (isPositive) {
                bar.rect.top - 6f * density
            } else {
                bar.rect.bottom + valueLabelPaint.textSize + 2f * density
            }
            canvas.drawText(label, bar.rect.centerX(), y, valueLabelPaint)
        }
    }

    private fun drawCategoryLabels(canvas: Canvas) {
        if (bars.isEmpty()) return

        bars.forEach { bar ->
            canvas.drawText(
                bar.entry.label,
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
        if (layout.entries.isEmpty() || chartArea.width() <= 0f || chartArea.height() <= 0f) {
            drawEmptyState(canvas)
            return
        }

        buildRenderCache()
        drawGridAndAxes(canvas)
        drawConnectors(canvas)
        drawBars(canvas)
        drawValueLabels(canvas)
        drawCategoryLabels(canvas)
    }
}
