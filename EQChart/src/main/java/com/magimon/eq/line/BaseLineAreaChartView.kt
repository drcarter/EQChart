package com.magimon.eq.line

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Shared renderer for line and area charts.
 *
 * - Manages series data and sanitization
 * - Resolves axis ranges and coordinate mapping
 * - Draws grid/axis/legend
 * - Performs click hit-test on points
 * - Handles enter animation lifecycle
 */
abstract class BaseLineAreaChartView(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private data class RenderPoint(
        val x: Float,
        val y: Float,
        val valueX: Double,
        val valueY: Double,
        val rawPoint: LineDatum,
        val seriesIndex: Int,
        val pointIndex: Int,
    )

    private data class SeriesContainer(
        val name: String,
        val color: Int,
        val areaFillColor: Int?,
        val points: List<LineDatum>,
        val payload: Any?,
    )

    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity

    private var styleOptions = LineChartStyleOptions()
    private var presentationOptions = LineChartPresentationOptions()
    private val seriesList = mutableListOf<SeriesContainer>()
    private val cachedPoints = mutableListOf<List<RenderPoint>>()
    private var selectedPoint: RenderPoint? = null
    private var animator: ValueAnimator? = null
    private var renderProgress = 1f

    private var onPointClickListener: ((Int, Int, LineDatum, Any?) -> Unit)? = null

    private val chartArea = RectF()
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val areaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val legendMarkerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val emptyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }
    private val axisLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.RIGHT
    }
    private val xAxisLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }
    private val legendTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.LEFT
    }

    private val linePath = Path()
    private val areaPath = Path()

    private val contentPaddingPx: Float
        get() = styleOptions.contentPaddingDp * density

    /**
     * Whether this chart instance should render the area fill under the line.
     */
    protected abstract fun enableAreaFill(): Boolean

    /**
     * Replaces all current line series.
     *
     * Invalid data points are filtered out automatically. Series points are rendered in x-order.
     */
    fun setSeries(items: List<LineSeries>) {
        seriesList.clear()
        seriesList.addAll(
            items
                .map { item ->
                    SeriesContainer(
                        name = item.name,
                        color = item.color,
                        areaFillColor = item.areaFillColor,
                        points = item.points
                            .filter { it.x.isFinite() && it.y.isFinite() }
                            .sortedBy { it.x },
                        payload = item.payload,
                    )
                }
                .filter { it.points.isNotEmpty() },
        )
        selectedPoint = null
        refreshWithAnimation()
    }

    /**
     * Replaces data by mapping arbitrary objects to [LineSeries].
     */
    fun <T> setSeries(items: List<T>, mapper: (T) -> LineSeries) {
        setSeries(items.map(mapper))
    }

    /**
     * Sets rendering style options.
     */
    fun setStyleOptions(options: LineChartStyleOptions) {
        styleOptions = options
        applyStyle()
        invalidate()
    }

    /**
     * Sets presentation options.
     */
    fun setPresentationOptions(options: LineChartPresentationOptions) {
        presentationOptions = options
        if (options.animateOnDataChange && seriesList.isNotEmpty()) {
            playEnterAnimation()
        } else {
            renderProgress = 1f
            invalidate()
        }
    }

    /**
     * Sets a callback to receive point interaction events.
     *
     * Callback format: `(seriesIndex, pointIndex, point, payload)`.
     * If a point has no payload, series payload is returned.
     */
    fun setOnPointClickListener(listener: (Int, Int, LineDatum, Any?) -> Unit) {
        onPointClickListener = listener
    }

    /**
     * Plays enter animation.
     */
    fun playEnterAnimation() {
        if (seriesList.isEmpty()) return
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

    private fun refreshWithAnimation() {
        if (seriesList.isNotEmpty() && presentationOptions.animateOnDataChange) {
            playEnterAnimation()
        } else {
            renderProgress = 1f
            invalidate()
        }
    }

    private fun applyStyle() {
        backgroundPaint.color = styleOptions.backgroundColor
        gridPaint.color = styleOptions.gridColor
        axisPaint.color = styleOptions.axisColor
        linePaint.color = styleOptions.axisColor
        markerPaint.color = styleOptions.axisColor
        emptyTextPaint.color = styleOptions.axisLabelColor
        emptyTextPaint.textSize = styleOptions.pointLabelTextSizeSp * scaledDensity
        axisLabelPaint.color = styleOptions.axisLabelColor
        axisLabelPaint.textSize = presentationOptions.axisLabelTextSizeSp * scaledDensity
        xAxisLabelPaint.color = styleOptions.axisLabelColor
        xAxisLabelPaint.textSize = presentationOptions.axisLabelTextSizeSp * scaledDensity
        legendTextPaint.color = styleOptions.legendTextColor
        legendTextPaint.textSize = styleOptions.legendTextSizeSp * scaledDensity
        gridPaint.strokeWidth = 1f * density
        axisPaint.strokeWidth = 1.1f * density
        linePaint.strokeWidth = styleOptions.lineStrokeWidthDp * density
    }

    private fun resolveAxisRange(minValue: Double?, maxValue: Double?): Pair<Double, Double> {
        val min = minValue ?: 0.0
        val max = maxValue ?: 0.0
        if (!min.isFinite() || !max.isFinite()) return 0.0 to 0.0
        val delta = max - min
        return if (abs(delta) <= 1e-12) {
            (min - 1.0) to (max + 1.0)
        } else {
            min to max
        }
    }

    private fun baselineValue(min: Double, max: Double): Double {
        return if (min <= 0.0 && max >= 0.0) 0.0 else min
    }

    private fun mapX(value: Double, min: Double, max: Double): Float {
        val ratio = if (max == min) 0.5f else ((value - min) / (max - min)).toFloat()
        return chartArea.left + ratio * chartArea.width()
    }

    private fun mapY(value: Double, min: Double, max: Double): Float {
        val ratio = if (max == min) 0.5f else ((value - min) / (max - min)).toFloat()
        return chartArea.bottom - ratio * chartArea.height()
    }

    private fun buildTicks(min: Double, max: Double, count: Int, reverseY: Boolean): List<Double> {
        val safeCount = count.coerceAtLeast(2)
        return if (reverseY) {
            List(safeCount) { index ->
                max - (max - min) * index.toDouble() / (safeCount - 1).toDouble()
            }
        } else {
            List(safeCount) { index ->
                min + (max - min) * index.toDouble() / (safeCount - 1).toDouble()
            }
        }
    }

    private fun buildCachedPoints(minX: Double, maxX: Double, minY: Double, maxY: Double): List<List<RenderPoint>> {
        cachedPoints.clear()
        cachedPoints.addAll(
            seriesList.mapIndexed { seriesIndex, series ->
                series.points.mapIndexed { pointIndex, point ->
                    RenderPoint(
                        x = mapX(point.x, minX, maxX),
                        y = mapY(point.y, minY, maxY),
                        valueX = point.x,
                        valueY = point.y,
                        rawPoint = point,
                        seriesIndex = seriesIndex,
                        pointIndex = pointIndex,
                    )
                }
            },
        )
        return cachedPoints
    }

    private fun buildPath(points: List<RenderPoint>, progress: Float, path: Path) {
        path.reset()
        if (points.isEmpty()) return

        val progressPoints = ((points.size - 1).coerceAtLeast(0) * progress).coerceIn(0f, (points.size - 1).toFloat())
        val lastFull = progressPoints.toInt()
        val fraction = progressPoints - lastFull

        path.moveTo(points[0].x, points[0].y)
        for (index in 1..min(lastFull, points.lastIndex)) {
            path.lineTo(points[index].x, points[index].y)
        }

        if (lastFull < points.lastIndex && progress > 0f) {
            val from = points[lastFull]
            val to = points[lastFull + 1]
            val x = from.x + (to.x - from.x) * fraction
            val y = from.y + (to.y - from.y) * fraction
            path.lineTo(x, y)
        }
    }

    private fun visiblePointCount(size: Int, progress: Float): Int {
        if (size <= 0) return 0
        if (progress >= 1f) return size
        val segmentCount = (size - 1).coerceAtLeast(1)
        return (segmentCount * progress).toInt() + 1
    }

    private fun findHit(x: Float, y: Float): RenderPoint? {
        val hitRadius = styleOptions.touchHitRadiusDp * density
        var best: RenderPoint? = null
        var bestDistance = Float.MAX_VALUE

        for (seriesPoints in cachedPoints) {
            for (point in seriesPoints) {
                val distance = hypot((point.x - x).toDouble(), (point.y - y).toDouble()).toFloat()
                if (distance <= hitRadius && distance < bestDistance) {
                    bestDistance = distance
                    best = point
                }
            }
        }
        return best
    }

    private fun areaFillColorForSeries(series: SeriesContainer): Int {
        val base = series.areaFillColor ?: series.color
        val alpha = styleOptions.areaFillAlpha.coerceIn(0, 255)
        val red = (base shr 16) and 0xFF
        val green = (base shr 8) and 0xFF
        val blue = base and 0xFF
        return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
    }

    private fun legendHeight(): Float {
        if (!presentationOptions.showLegend || seriesList.isEmpty()) return 0f
        return legendTextPaint.fontSpacing + presentationOptions.legendBottomMarginDp * density
    }

    private fun xLabelAreaHeight(): Float {
        return styleOptions.pointLabelTextSizeSp * scaledDensity * 1.25f + 8f * density
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

        if (width <= 0 || height <= 0) return
        if (seriesList.isEmpty()) {
            if (presentationOptions.emptyText.isNotBlank()) {
                canvas.drawText(
                    presentationOptions.emptyText,
                    width * 0.5f,
                    height * 0.5f,
                    emptyTextPaint.apply { textAlign = Paint.Align.CENTER },
                )
            }
            return
        }

        val allPoints = seriesList.flatMap { it.points }
        if (allPoints.isEmpty()) {
            if (presentationOptions.emptyText.isNotBlank()) {
                canvas.drawText(
                    presentationOptions.emptyText,
                    width * 0.5f,
                    height * 0.5f,
                    emptyTextPaint.apply { textAlign = Paint.Align.CENTER },
                )
            }
            return
        }

        val (minXRaw, maxXRaw) = resolveAxisRange(
            allPoints.minOfOrNull { it.x },
            allPoints.maxOfOrNull { it.x },
        )
        val (minYRaw, maxYRaw) = resolveAxisRange(
            allPoints.minOfOrNull { it.y },
            allPoints.maxOfOrNull { it.y },
        )

        val reservedBottom = legendHeight() + xLabelAreaHeight()
        chartArea.set(
            contentPaddingPx,
            contentPaddingPx,
            width.toFloat() - contentPaddingPx,
            height.toFloat() - contentPaddingPx - reservedBottom,
        )

        if (chartArea.width() <= 0f || chartArea.height() <= 0f) return

        val tickCountX = presentationOptions.xTickCount.coerceAtLeast(2)
        val tickCountY = presentationOptions.yTickCount.coerceAtLeast(2)
        val xTicks = buildTicks(minXRaw, maxXRaw, tickCountX, reverseY = false)
        val yTicks = buildTicks(minYRaw, maxYRaw, tickCountY, reverseY = true)

        val pointsBySeries = buildCachedPoints(minXRaw, maxXRaw, minYRaw, maxYRaw)
        val progress = renderProgress.coerceIn(0f, 1f)
        val baselineRaw = baselineValue(minYRaw, maxYRaw)
        val baselineY = mapY(baselineRaw, minYRaw, maxYRaw)

        if (presentationOptions.showGrid) {
            yTicks.forEach { tick ->
                val y = mapY(tick, minYRaw, maxYRaw)
                canvas.drawLine(chartArea.left, y, chartArea.right, y, gridPaint)
                canvas.drawText(
                    presentationOptions.yLabelFormatter(tick),
                    chartArea.left - 8f * density,
                    y + (axisLabelPaint.textSize * 0.35f),
                    axisLabelPaint,
                )
            }
            xTicks.forEach { tick ->
                val x = mapX(tick, minXRaw, maxXRaw)
                canvas.drawLine(x, chartArea.top, x, chartArea.bottom, gridPaint)
                canvas.drawText(
                    presentationOptions.xLabelFormatter(tick),
                    x,
                    chartArea.bottom + axisLabelPaint.textSize + 8f * density,
                    xAxisLabelPaint,
                )
            }
        }

        if (presentationOptions.showAxes) {
            canvas.drawLine(chartArea.left, chartArea.top, chartArea.left, chartArea.bottom, axisPaint)
            canvas.drawLine(chartArea.left, chartArea.bottom, chartArea.right, chartArea.bottom, axisPaint)
            canvas.drawLine(chartArea.right, chartArea.top, chartArea.right, chartArea.bottom, axisPaint)
        }

        for ((seriesIndex, series) in seriesList.withIndex()) {
            val points = pointsBySeries.getOrNull(seriesIndex).orEmpty()
            if (points.isEmpty()) continue

            linePaint.color = series.color
            markerPaint.color = series.color

            if (enableAreaFill() && presentationOptions.showAreaFill && points.size >= 2) {
                areaPath.reset()
                buildPath(points, progress, areaPath)
                areaPath.lineTo(points.last().x, baselineY)
                areaPath.lineTo(points[0].x, baselineY)
                areaPath.close()
                areaPaint.color = areaFillColorForSeries(series)
                canvas.drawPath(areaPath, areaPaint)
            }

            linePath.reset()
            buildPath(points, progress, linePath)
            canvas.drawPath(linePath, linePaint)

            if (presentationOptions.showPoints) {
                val drawn = visiblePointCount(points.size, progress)
                for (pointIndex in points.indices) {
                    if (pointIndex >= drawn) break
                    val point = points[pointIndex]
                    val isSelected = selectedPoint == point
                    markerPaint.color = if (isSelected) Color.WHITE else series.color
                    val radius = if (isSelected) {
                        styleOptions.selectedPointRadiusDp * density
                    } else {
                        styleOptions.pointRadiusDp * density
                    }
                    canvas.drawCircle(point.x, point.y, radius, markerPaint)
                }
            }
        }

        if (presentationOptions.showLegend) {
            drawLegend(canvas)
        }
    }

    private fun drawLegend(canvas: Canvas) {
        if (seriesList.isEmpty()) return
        val startX = chartArea.left + presentationOptions.legendLeftMarginDp * density
        val startY = chartArea.bottom + presentationOptions.legendTopMarginDp * density
        var x = startX
        var y = startY + legendTextPaint.textSize
        val markerSize = styleOptions.legendMarkerSizeDp * density

        seriesList.forEach { series ->
            val markerRight = x + markerSize
            val textX = markerRight + styleOptions.legendMarkerTextGapDp * density
            val measure = legendTextPaint.measureText(series.name)
            if (x + markerSize + measure > width - contentPaddingPx) {
                x = startX
                y += legendTextPaint.textSize + styleOptions.legendRowSpacingDp * density
            }
            legendMarkerPaint.color = series.color
            canvas.drawRect(x, y - markerSize, markerRight, y, legendMarkerPaint)
            canvas.drawText(series.name, textX, y, legendTextPaint)
            x = textX + measure + styleOptions.legendItemSpacingDp * density
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (width <= 0 || height <= 0 || seriesList.isEmpty()) return super.onTouchEvent(event)
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> true
            MotionEvent.ACTION_UP -> {
                val hit = findHit(event.x, event.y)
                selectedPoint = hit
                val selected = hit
                if (selected == null) {
                    return@onTouchEvent(true)
                }
                val series = seriesList.getOrNull(selected.seriesIndex)
                val payload = selected.rawPoint.payload ?: series?.payload
                onPointClickListener?.invoke(selected.seriesIndex, selected.pointIndex, selected.rawPoint, payload)
                invalidate()
                performClick()
                true
            }
            else -> super.onTouchEvent(event)
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
