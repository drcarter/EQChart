package com.magimon.eq.bar

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Cartesian bar chart renderer.
 *
 * Supports:
 * - vertical and horizontal orientation
 * - grouped and stacked layout
 * - click callbacks
 */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private data class RenderBar(
        val rect: RectF,
        val seriesIndex: Int,
        val categoryIndex: Int,
        val value: Double,
        val payload: Any?,
    )

    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity

    private var styleOptions = BarChartStyleOptions()
    private var presentationOptions = BarChartPresentationOptions()

    private val seriesList = mutableListOf<BarSeries>()
    private val categories = mutableListOf<String>()
    private val bars = mutableListOf<RenderBar>()

    private var animator: ValueAnimator? = null
    private var renderProgress = 1f
    private var selectedBar: RenderBar? = null
    private var onBarClickListener: ((Int, Int, Double, Any?) -> Unit)? = null

    private val chartArea = RectF()
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val selectedBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }
    private val axisLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.RIGHT
    }
    private val categoryLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }
    private val legendTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.LEFT
    }
    private val legendMarkerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    init {
        applyStyle()
    }

    /**
     * Replaces all bar series.
     */
    fun setSeries(items: List<BarSeries>) {
        seriesList.clear()
        categories.clear()

        val sanitized = items.map { item ->
            BarSeries(
                name = item.name,
                color = item.color,
                points = item.points
                    .filter { it.category.isNotBlank() && it.value.isFinite() },
                payload = item.payload,
            )
        }.filter { it.points.isNotEmpty() }

        sanitized.forEach { series ->
            series.points.forEach { point ->
                if (!categories.contains(point.category)) categories.add(point.category)
            }
        }

        seriesList.addAll(sanitized)
        selectedBar = null
        refreshAndRender()
    }

    /**
     * Replaces series data by mapping arbitrary objects.
     */
    fun <T> setSeries(items: List<T>, mapper: (T) -> BarSeries) {
        setSeries(items.map(mapper))
    }

    /**
     * Sets render style options.
     */
    fun setStyleOptions(options: BarChartStyleOptions) {
        styleOptions = options
        applyStyle()
        invalidate()
    }

    /**
     * Sets presentation behavior.
     */
    fun setPresentationOptions(options: BarChartPresentationOptions) {
        presentationOptions = options
        if (options.animateOnDataChange && !seriesList.isEmpty()) {
            playEnterAnimation()
        } else {
            renderProgress = 1f
            invalidate()
        }
    }

    /**
     * Sets bar click callback.
     *
     * Callback format: `(seriesIndex, categoryIndex, value, payload)`.
     */
    fun setOnBarClickListener(listener: (Int, Int, Double, Any?) -> Unit) {
        onBarClickListener = listener
    }

    private fun Float.dpToPx() = this * density
    private fun Float.spToPx() = this * scaledDensity

    private fun applyStyle() {
        backgroundPaint.color = styleOptions.backgroundColor
        gridPaint.color = styleOptions.gridColor
        axisPaint.color = styleOptions.axisColor
        axisPaint.strokeWidth = 1.1f * density
        gridPaint.strokeWidth = 1f * density
        barPaint.color = styleOptions.axisColor

        labelPaint.color = styleOptions.barValueTextColor
        axisLabelPaint.color = styleOptions.axisLabelColor
        categoryLabelPaint.color = styleOptions.axisLabelColor
        legendTextPaint.color = styleOptions.legendTextColor
        categoryLabelPaint.textSize = presentationOptions.axisLabelTextSizeSp.spToPx()
        axisLabelPaint.textSize = presentationOptions.axisLabelTextSizeSp.spToPx()
        legendTextPaint.textSize = presentationOptions.legendTextSizeSp.spToPx()
        labelPaint.textSize = presentationOptions.axisLabelTextSizeSp.spToPx()
    }

    private fun contentPaddingPx(): Float = styleOptions.contentPaddingDp * density

    private fun legendHeight(): Float {
        if (!presentationOptions.showLegend || seriesList.isEmpty()) return 0f
        return legendTextPaint.fontSpacing + 8f * density
    }

    private fun resolveValueRange(values: Collection<Double>): Pair<Double, Double> {
        if (values.isEmpty()) return -1.0 to 1.0
        val min = values.minOrNull() ?: -1.0
        val max = values.maxOrNull() ?: 1.0
        if (!min.isFinite() || !max.isFinite()) return -1.0 to 1.0
        return if (abs(max - min) <= 1e-12) (min - 1.0) to (max + 1.0) else min to max
    }

    private fun baselineValue(minValue: Double, maxValue: Double): Double {
        return if (minValue <= 0.0 && maxValue >= 0.0) 0.0 else minValue
    }

    private fun valueToY(value: Double, min: Double, max: Double): Float {
        val ratio = if (max == min) 0.5f else ((value - min) / (max - min)).toFloat()
        return chartArea.bottom - ratio * chartArea.height()
    }

    private fun valueToX(value: Double, min: Double, max: Double): Float {
        val ratio = if (max == min) 0.5f else ((value - min) / (max - min)).toFloat()
        return chartArea.left + ratio * chartArea.width()
    }

    private fun valueFor(seriesIndex: Int, category: String): Double? {
        return seriesList
            .getOrNull(seriesIndex)
            ?.points
            ?.firstOrNull { it.category == category }
            ?.value
    }

    private fun payloadFor(seriesIndex: Int, category: String): Any? {
        return seriesList
            .getOrNull(seriesIndex)
            ?.points
            ?.firstOrNull { it.category == category }
            ?.payload
    }

    private fun valueToScaled(value: Double, progress: Float): Double {
        return value * progress
    }

    private fun buildGroupedVertical(progress: Float, minValue: Double, maxValue: Double, baselineY: Float) {
        bars.clear()
        val categoryCount = categories.size
        if (categoryCount == 0 || seriesList.isEmpty()) return

        val slotWidth = chartArea.width() / categoryCount.toFloat()
        val categoryGap = styleOptions.categorySpacingDp.dpToPx()
        val usableSlot = (slotWidth - categoryGap).coerceAtLeast(1f)
        val barGap = styleOptions.barSpacingDp.dpToPx()
        val seriesCount = seriesList.size
        val barWidth = max(1f, (usableSlot - barGap * max(0, seriesCount - 1)) / seriesCount.toFloat())

        categories.forEachIndexed { categoryIndex, category ->
            val startX = chartArea.left + categoryIndex * slotWidth + categoryGap * 0.5f
            for (seriesIndex in 0 until seriesCount) {
                val value = valueFor(seriesIndex, category) ?: continue
                val scaledValue = valueToScaled(value, progress)
                val left = startX + seriesIndex * (barWidth + barGap)
                val right = left + barWidth
                bars.add(
                    RenderBar(
                        rect = RectF(
                            left,
                            min(valueToY(scaledValue, minValue, maxValue), baselineY),
                            right,
                            max(valueToY(scaledValue, minValue, maxValue), baselineY),
                        ),
                        seriesIndex = seriesIndex,
                        categoryIndex = categoryIndex,
                        value = value,
                        payload = payloadFor(seriesIndex, category),
                    ),
                )
            }
        }
    }

    private fun buildStackedVertical(progress: Float, minValue: Double, maxValue: Double, baseline: Double) {
        bars.clear()
        if (categories.isEmpty() || seriesList.isEmpty()) return

        val categoryCount = categories.size
        val slotWidth = chartArea.width() / categoryCount.toFloat()
        val categoryGap = styleOptions.categorySpacingDp.dpToPx()
        val usableSlot = (slotWidth - categoryGap).coerceAtLeast(1f)

        val seriesCount = seriesList.size
        categories.forEachIndexed { categoryIndex, category ->
            val left = chartArea.left + categoryIndex * slotWidth + categoryGap * 0.5f
            val right = left + usableSlot
            var positiveBase = baseline
            var negativeBase = baseline

            for (seriesIndex in 0 until seriesCount) {
                val value = valueFor(seriesIndex, category) ?: continue
                val scaledValue = valueToScaled(value, progress)
                val from = if (scaledValue >= 0.0) positiveBase else negativeBase
                val to = if (scaledValue >= 0.0) positiveBase + scaledValue else negativeBase + scaledValue
                bars.add(
                    RenderBar(
                        rect = RectF(
                            left,
                            min(valueToY(to, minValue, maxValue), valueToY(from, minValue, maxValue)),
                            right,
                            max(valueToY(to, minValue, maxValue), valueToY(from, minValue, maxValue)),
                        ),
                        seriesIndex = seriesIndex,
                        categoryIndex = categoryIndex,
                        value = value,
                        payload = payloadFor(seriesIndex, category),
                    ),
                )
                if (scaledValue >= 0.0) positiveBase = to else negativeBase = to
            }
        }
    }

    private fun buildGroupedHorizontal(progress: Float, minValue: Double, maxValue: Double, baselineX: Float) {
        bars.clear()
        val categoryCount = categories.size
        if (categoryCount == 0 || seriesList.isEmpty()) return

        val slotHeight = chartArea.height() / categoryCount.toFloat()
        val categoryGap = styleOptions.categorySpacingDp.dpToPx()
        val usableSlot = (slotHeight - categoryGap).coerceAtLeast(1f)
        val barGap = styleOptions.barSpacingDp.dpToPx()
        val seriesCount = seriesList.size
        val barHeight = max(1f, (usableSlot - barGap * max(0, seriesCount - 1)) / seriesCount.toFloat())

        categories.forEachIndexed { categoryIndex, category ->
            val topBase = chartArea.top + categoryIndex * slotHeight + categoryGap * 0.5f
            for (seriesIndex in 0 until seriesCount) {
                val value = valueFor(seriesIndex, category) ?: continue
                val scaledValue = valueToScaled(value, progress)
                val top = topBase + seriesIndex * (barHeight + barGap)
                val bottom = top + barHeight
                val x = valueToX(scaledValue, minValue, maxValue)
                bars.add(
                    RenderBar(
                        rect = RectF(
                            min(x, baselineX),
                            top,
                            max(x, baselineX),
                            bottom,
                        ),
                        seriesIndex = seriesIndex,
                        categoryIndex = categoryIndex,
                        value = value,
                        payload = payloadFor(seriesIndex, category),
                    ),
                )
            }
        }
    }

    private fun buildStackedHorizontal(progress: Float, minValue: Double, maxValue: Double, baseline: Double) {
        bars.clear()
        if (categories.isEmpty() || seriesList.isEmpty()) return

        val categoryCount = categories.size
        val slotHeight = chartArea.height() / categoryCount.toFloat()
        val categoryGap = styleOptions.categorySpacingDp.dpToPx()
        val usableSlot = (slotHeight - categoryGap).coerceAtLeast(1f)

        val seriesCount = seriesList.size
        categories.forEachIndexed { categoryIndex, category ->
            val topBase = chartArea.top + categoryIndex * slotHeight + categoryGap * 0.5f
            val bottom = topBase + usableSlot
            var positiveBase = baseline
            var negativeBase = baseline

            for (seriesIndex in 0 until seriesCount) {
                val value = valueFor(seriesIndex, category) ?: continue
                val scaledValue = valueToScaled(value, progress)
                val from = if (scaledValue >= 0.0) positiveBase else negativeBase
                val to = if (scaledValue >= 0.0) positiveBase + scaledValue else negativeBase + scaledValue
                bars.add(
                    RenderBar(
                        rect = RectF(
                            min(valueToX(from, minValue, maxValue), valueToX(to, minValue, maxValue)),
                            topBase,
                            max(valueToX(from, minValue, maxValue), valueToX(to, minValue, maxValue)),
                            bottom,
                        ),
                        seriesIndex = seriesIndex,
                        categoryIndex = categoryIndex,
                        value = value,
                        payload = payloadFor(seriesIndex, category),
                    ),
                )
                if (scaledValue >= 0.0) positiveBase = to else negativeBase = to
            }
        }
    }

    private fun rebuildBars(progress: Float, minValue: Double, maxValue: Double) {
        val baseline = baselineValue(minValue, maxValue)
        val isVertical = presentationOptions.orientation == BarOrientation.VERTICAL
        val baselinePixel = if (isVertical) valueToY(baseline, minValue, maxValue) else valueToX(baseline, minValue, maxValue)
        val animatedProgress = if (presentationOptions.animationDirection) progress else 1f

        when (presentationOptions.orientation) {
            BarOrientation.VERTICAL -> {
                when (presentationOptions.layoutMode) {
                    BarLayoutMode.GROUPED -> buildGroupedVertical(animatedProgress, minValue, maxValue, baselinePixel)
                    BarLayoutMode.STACKED -> buildStackedVertical(animatedProgress, minValue, maxValue, baseline)
                }
            }
            BarOrientation.HORIZONTAL -> {
                when (presentationOptions.layoutMode) {
                    BarLayoutMode.GROUPED -> buildGroupedHorizontal(animatedProgress, minValue, maxValue, baselinePixel)
                    BarLayoutMode.STACKED -> buildStackedHorizontal(animatedProgress, minValue, maxValue, baseline)
                }
            }
        }
    }

    private fun valueTickList(minValue: Double, maxValue: Double): List<Double> {
        val count = if (presentationOptions.orientation == BarOrientation.VERTICAL) {
            presentationOptions.yTickCount
        } else {
            presentationOptions.xTickCount
        }.coerceAtLeast(2)

        return List(count) { index ->
            minValue + (maxValue - minValue) * index.toDouble() / (count - 1).toDouble()
        }
    }

    private fun categoryCenters(isVertical: Boolean): List<Float> {
        if (categories.isEmpty()) return emptyList()
        return if (isVertical) {
            val slot = chartArea.width() / categories.size.toFloat()
            List(categories.size) { index -> chartArea.left + slot * (index + 0.5f) }
        } else {
            val slot = chartArea.height() / categories.size.toFloat()
            List(categories.size) { index -> chartArea.top + slot * (index + 0.5f) }
        }
    }

    private fun tickReserveBottom(isVertical: Boolean): Float {
        return if (isVertical) {
            categoryLabelPaint.textSize + 16f * density
        } else {
            axisLabelPaint.textSize + 16f * density
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = (860f * density).toInt()
        val desiredHeight = (560f * density).toInt()
        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        applyStyle()
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        if (seriesList.isEmpty() || categories.isEmpty()) {
            if (presentationOptions.emptyText.isNotBlank()) {
                canvas.drawText(
                    presentationOptions.emptyText,
                    width * 0.5f,
                    height * 0.5f,
                    axisLabelPaint.apply {
                        textAlign = Paint.Align.CENTER
                    },
                )
            }
            return
        }

        val allValues = seriesList.flatMap { it.points.map { point -> point.value } }
        val (minValue, maxValue) = resolveValueRange(allValues)
        val isVertical = presentationOptions.orientation == BarOrientation.VERTICAL

        chartArea.set(
            contentPaddingPx(),
            contentPaddingPx(),
            width.toFloat() - contentPaddingPx(),
            height.toFloat() - contentPaddingPx() - tickReserveBottom(isVertical) - legendHeight(),
        )

        if (chartArea.width() <= 0f || chartArea.height() <= 0f) return

        val ticks = valueTickList(minValue, maxValue)
        val centers = categoryCenters(isVertical)

        if (presentationOptions.showGrid) {
            if (isVertical) {
                ticks.forEach { value ->
                    val y = valueToY(value, minValue, maxValue)
                    canvas.drawLine(chartArea.left, y, chartArea.right, y, gridPaint)
                    axisLabelPaint.textAlign = Paint.Align.RIGHT
                    canvas.drawText(
                        presentationOptions.yLabelFormatter(value),
                        chartArea.left - 8f * density,
                        y + axisLabelPaint.textSize * 0.35f,
                        axisLabelPaint,
                    )
                }
                centers.forEach { x ->
                    canvas.drawLine(x, chartArea.top, x, chartArea.bottom, gridPaint)
                }
            } else {
                ticks.forEach { value ->
                    val x = valueToX(value, minValue, maxValue)
                    canvas.drawLine(x, chartArea.top, x, chartArea.bottom, gridPaint)
                    categoryLabelPaint.textAlign = Paint.Align.CENTER
                    canvas.drawText(
                        presentationOptions.yLabelFormatter(value),
                        x,
                        chartArea.bottom + 14f * density + categoryLabelPaint.textSize,
                        categoryLabelPaint,
                    )
                }
                centers.forEach { y ->
                    canvas.drawLine(chartArea.left, y, chartArea.right, y, gridPaint)
                }
            }
        }

        if (presentationOptions.showAxes) {
            if (isVertical) {
                val axisY = valueToY(baselineValue(minValue, maxValue), minValue, maxValue)
                canvas.drawLine(chartArea.left, axisY, chartArea.right, axisY, axisPaint)
                canvas.drawLine(chartArea.left, chartArea.top, chartArea.left, chartArea.bottom, axisPaint)
            } else {
                val axisX = valueToX(baselineValue(minValue, maxValue), minValue, maxValue)
                canvas.drawLine(axisX, chartArea.top, axisX, chartArea.bottom, axisPaint)
                canvas.drawLine(chartArea.left, chartArea.bottom, chartArea.right, chartArea.bottom, axisPaint)
            }
        }

        rebuildBars(renderProgress, minValue, maxValue)

        if (presentationOptions.showBars) {
            val corner = styleOptions.barCornerRadiusDp.dpToPx()
            for (bar in bars) {
                val series = seriesList.getOrNull(bar.seriesIndex) ?: continue
                barPaint.color = series.color
                canvas.drawRoundRect(bar.rect, corner, corner, barPaint)

                if (bar == selectedBar) {
                    selectedBarPaint.color = series.color
                    canvas.drawRoundRect(
                        RectF(
                            bar.rect.left - styleOptions.selectedBarPaddingDp.dpToPx(),
                            bar.rect.top - styleOptions.selectedBarPaddingDp.dpToPx(),
                            bar.rect.right + styleOptions.selectedBarPaddingDp.dpToPx(),
                            bar.rect.bottom + styleOptions.selectedBarPaddingDp.dpToPx(),
                        ),
                        corner,
                        corner,
                        selectedBarPaint,
                    )
                }

                if (presentationOptions.showBarLabels) {
                    val text = presentationOptions.yLabelFormatter(bar.value)
                    if (isVertical) {
                        canvas.drawText(
                            text,
                            (bar.rect.left + bar.rect.right) * 0.5f,
                            min(bar.rect.top, bar.rect.bottom) - 4f * density,
                            labelPaint,
                        )
                    } else {
                        val x = if (bar.rect.left <= bar.rect.right) bar.rect.right + 4f * density else bar.rect.left - 4f * density
                        val align = if (bar.value >= 0.0) Paint.Align.LEFT else Paint.Align.RIGHT
                        labelPaint.textAlign = align
                        canvas.drawText(
                            text,
                            if (bar.value >= 0.0) {
                                x
                            } else {
                                x
                            },
                            (bar.rect.top + bar.rect.bottom) * 0.5f + labelPaint.textSize * 0.35f,
                            labelPaint,
                        )
                        labelPaint.textAlign = Paint.Align.CENTER
                    }
                }
            }

            if (isVertical) {
                categoryLabelPaint.textAlign = Paint.Align.CENTER
                centers.forEachIndexed { index, centerX ->
                    canvas.drawText(
                        presentationOptions.xLabelFormatter(categories[index]),
                        centerX,
                        chartArea.bottom + categoryLabelPaint.textSize + 6f * density,
                        categoryLabelPaint,
                    )
                }
            } else {
                axisLabelPaint.textAlign = Paint.Align.RIGHT
                centers.forEachIndexed { index, centerY ->
                    canvas.drawText(
                        presentationOptions.xLabelFormatter(categories[index]),
                        chartArea.left - 6f * density,
                        centerY + axisLabelPaint.textSize * 0.35f,
                        axisLabelPaint,
                    )
                }
            }
        }

        if (presentationOptions.showLegend) {
            drawLegend(canvas)
        }
    }

    private fun drawLegend(canvas: Canvas) {
        if (seriesList.isEmpty()) return
        var x = chartArea.left + styleOptions.legendMarkerSizeDp.dpToPx()
        var y = chartArea.bottom + 4f * density + 4f * density
        val markerSize = styleOptions.legendMarkerSizeDp.dpToPx()
        val lineHeight = legendTextPaint.fontSpacing

        for (series in seriesList) {
            val measure = legendTextPaint.measureText(series.name)
            if (x + markerSize + measure > width - 12f * density) {
                x = chartArea.left + styleOptions.legendMarkerSizeDp.dpToPx()
                y += lineHeight + styleOptions.legendRowSpacingDp.dpToPx()
            }
            legendMarkerPaint.color = series.color
            canvas.drawRect(x, y - markerSize, x + markerSize, y, legendMarkerPaint)
            canvas.drawText(series.name, x + markerSize + styleOptions.legendMarkerTextGapDp.dpToPx(), y, legendTextPaint)
            x += markerSize + styleOptions.legendMarkerTextGapDp.dpToPx() + measure + styleOptions.legendItemSpacingDp.dpToPx()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!presentationOptions.showBars || bars.isEmpty()) return super.onTouchEvent(event)
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> true
            MotionEvent.ACTION_UP -> {
                selectedBar = bars.firstOrNull { it.rect.contains(event.x, event.y) }
                selectedBar?.let { bar ->
                    val payload = bar.payload ?: seriesList.getOrNull(bar.seriesIndex)?.payload
                    onBarClickListener?.invoke(bar.seriesIndex, bar.categoryIndex, bar.value, payload)
                    invalidate()
                    performClick()
                }
                true
            }
            else -> super.onTouchEvent(event)
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun refreshAndRender() {
        if (seriesList.isNotEmpty() && presentationOptions.animateOnDataChange) {
            playEnterAnimation()
        } else {
            renderProgress = 1f
            invalidate()
        }
    }

    private fun playEnterAnimation() {
        animator?.cancel()
        renderProgress = 0f
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = presentationOptions.enterAnimationDurationMs.coerceAtLeast(0L)
            startDelay = presentationOptions.enterAnimationDelayMs.coerceAtLeast(0L)
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                renderProgress = (it.animatedValue as Float).coerceIn(0f, 1f)
                invalidate()
            }
            start()
        }
    }
}
