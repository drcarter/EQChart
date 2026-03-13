package com.magimon.eq.radar

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Custom view that renders a multi-series radar chart.
 *
 * - Renders polygon geometry from axis labels ([RadarAxis]) and series ([RadarSeries]).
 * - Grid/axis/legend/point styles are controlled by [RadarChartStyleOptions].
 * - Display and animation behavior is controlled by [RadarChartPresentationOptions].
 * - Point selection is delivered via [setOnPointClickListener].
 */
class RadarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private data class Geometry(
        val centerX: Float,
        val centerY: Float,
        val radius: Float,
        val legendBottomY: Float,
    )

    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity

    private var axes: List<RadarAxis> = emptyList()
    private var series: List<RadarSeries> = emptyList()
    private var valueMax: Double = 100.0
    private var styleOptions = RadarChartStyleOptions()
    private var presentationOptions = RadarChartPresentationOptions()
    private var onPointClickListener: ((Int, Int, Double, Any?) -> Unit)? = null
    private var selectedPoint: RadarChartMath.HitResult? = null
    private var renderProgress = 1f
    private var animator: ValueAnimator? = null

    private var cachedPointsBySeries: List<List<RadarChartMath.Vec2>> = emptyList()
    private var cachedRenderableSeries: List<RadarSeries> = emptyList()

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val axisLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val polygonFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val polygonStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val pointCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val selectedRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
    }
    private val legendTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val legendMarkerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val polygonPath = Path()
    private val seriesPath = Path()
    private val chartAreaRect = RectF()

    init {
        applyStyleAndPresentation()
    }

    /**
     * Sets the axis label list.
     *
     * Blank labels are removed automatically.
     *
     * @param items Axis definitions for the chart
     */
    fun setAxes(items: List<RadarAxis>) {
        axes = items.filter { it.label.isNotBlank() }
        selectedPoint = null
        refreshDataAndRender()
    }

    /**
     * Sets the series list.
     *
     * Only series whose `values` size matches the axis count are renderable.
     *
     * @param items Series list
     */
    fun setSeries(items: List<RadarSeries>) {
        series = items
        selectedPoint = null
        refreshDataAndRender()
    }

    /**
     * Sets the max value used for normalization.
     *
     * Invalid input falls back to `100.0`.
     *
     * @param maxValue Normalization max value
     */
    fun setValueMax(maxValue: Double) {
        valueMax = if (maxValue.isFinite() && maxValue > 0.0) maxValue else 100.0
        invalidate()
    }

    /**
     * Sets chart style options.
     *
     * @param options Rendering style options
     */
    fun setStyleOptions(options: RadarChartStyleOptions) {
        styleOptions = options
        applyStyleAndPresentation()
        invalidate()
    }

    /**
     * Sets chart presentation/animation options.
     *
     * @param options Presentation/animation options
     */
    fun setPresentationOptions(options: RadarChartPresentationOptions) {
        presentationOptions = options
        applyStyleAndPresentation()
        invalidate()
    }

    /**
     * Sets the point click listener.
     *
     * @param listener Callback in `(seriesIndex, axisIndex, value, payload)` format
     */
    fun setOnPointClickListener(listener: (seriesIndex: Int, axisIndex: Int, value: Double, payload: Any?) -> Unit) {
        onPointClickListener = listener
    }

    /**
     * Plays the enter animation.
     *
     * Does nothing when there are fewer than 3 axes or no renderable series.
     */
    fun playEnterAnimation() {
        if (axes.size < 3 || renderableSeries().isEmpty()) return
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

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = (860f * density).toInt()
        val desiredHeight = (680f * density).toInt()
        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        if (width <= 0 || height <= 0) return

        val visibleSeries = renderableSeries()
        cachedRenderableSeries = visibleSeries
        val geometry = computeGeometry(canvas, visibleSeries)
        if (geometry.radius <= 0f || axes.size < 3 || visibleSeries.isEmpty()) {
            cachedPointsBySeries = emptyList()
            return
        }

        drawGridAndAxes(canvas, geometry)

        val pointsBySeries = buildSeriesPoints(
            seriesList = visibleSeries,
            geometry = geometry,
            progress = renderProgress,
        )
        cachedPointsBySeries = pointsBySeries

        drawSeries(canvas, visibleSeries, pointsBySeries)
        if (presentationOptions.showAxisLabels) {
            drawAxisLabels(canvas, geometry)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (cachedRenderableSeries.isEmpty() || cachedPointsBySeries.isEmpty()) return super.onTouchEvent(event)

        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> true
            MotionEvent.ACTION_UP -> {
                val hit = RadarChartMath.nearestPoint(
                    touchX = event.x,
                    touchY = event.y,
                    pointsBySeries = cachedPointsBySeries,
                    hitRadiusPx = styleOptions.touchHitRadiusDp * density,
                )
                selectedPoint = hit
                invalidate()

                if (hit != null) {
                    val seriesItem = cachedRenderableSeries.getOrNull(hit.seriesIndex)
                    val value = seriesItem?.values?.getOrNull(hit.axisIndex)
                    if (seriesItem != null && value != null) {
                        onPointClickListener?.invoke(hit.seriesIndex, hit.axisIndex, value, seriesItem.payload)
                    }
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

    private fun refreshDataAndRender() {
        if (presentationOptions.animateOnDataChange) {
            playEnterAnimation()
        } else {
            animator?.cancel()
            animator = null
            renderProgress = 1f
            invalidate()
        }
    }

    /**
     * Returns only series that are renderable with the current axis/series setup.
     *
     * - Excludes series whose value count differs from axis count
     * - Excludes series where all values are non-finite (`NaN`/`Infinity`)
     */
    private fun renderableSeries(): List<RadarSeries> {
        if (axes.isEmpty()) return emptyList()
        val axisCount = axes.size
        return series.filter { radarSeries ->
            if (radarSeries.values.size != axisCount) return@filter false
            radarSeries.values.any { value -> value.isFinite() }
        }
    }

    private fun applyStyleAndPresentation() {
        backgroundPaint.color = styleOptions.backgroundColor

        gridPaint.color = styleOptions.gridColor
        gridPaint.strokeWidth = styleOptions.gridStrokeWidthDp.coerceAtLeast(0.5f) * density

        axisPaint.color = styleOptions.axisColor
        axisPaint.strokeWidth = styleOptions.axisStrokeWidthDp.coerceAtLeast(0.5f) * density
        axisPaint.pathEffect = DashPathEffect(
            floatArrayOf(
                styleOptions.axisDashLengthDp.coerceAtLeast(1f) * density,
                styleOptions.axisDashGapDp.coerceAtLeast(1f) * density,
            ),
            0f,
        )

        axisLabelPaint.color = styleOptions.axisLabelColor
        axisLabelPaint.textSize = max(8f, presentationOptions.axisLabelTextSizeSp) * scaledDensity

        polygonStrokePaint.strokeWidth = styleOptions.polygonStrokeWidthDp.coerceAtLeast(0.5f) * density
        selectedRingPaint.strokeWidth = styleOptions.selectedStrokeWidthDp.coerceAtLeast(1f) * density

        legendTextPaint.color = styleOptions.legendTextColor
        legendTextPaint.textSize = max(8f, presentationOptions.legendTextSizeSp) * scaledDensity
    }

    /**
     * Computes chart center/radius/legend area.
     *
     * Adjusts final radius via [resolveRadiusToFitAxisLabels] to avoid label clipping.
     */
    private fun computeGeometry(canvas: Canvas, visibleSeries: List<RadarSeries>): Geometry {
        val contentPadding = styleOptions.contentPaddingDp * density

        val legendBottom = if (presentationOptions.showLegend) {
            drawLegend(canvas, visibleSeries)
        } else {
            paddingTop.toFloat()
        }

        chartAreaRect.set(
            paddingLeft + contentPadding,
            legendBottom + contentPadding,
            width.toFloat() - paddingRight - contentPadding,
            height.toFloat() - paddingBottom - contentPadding,
        )

        val availableHalfWidth = chartAreaRect.width() * 0.5f
        val availableHalfHeight = chartAreaRect.height() * 0.5f
        val maxRadius = min(availableHalfWidth, availableHalfHeight).coerceAtLeast(0f)
        val radius = resolveRadiusToFitAxisLabels(
            centerX = chartAreaRect.centerX(),
            centerY = chartAreaRect.centerY(),
            maxRadius = maxRadius,
        )

        return Geometry(
            centerX = chartAreaRect.centerX(),
            centerY = chartAreaRect.centerY(),
            radius = radius,
            legendBottomY = legendBottom,
        )
    }

    /**
     * Renders the legend and returns its bottom Y coordinate.
     *
     * Used to compute the start position of the chart body.
     */
    private fun drawLegend(canvas: Canvas, visibleSeries: List<RadarSeries>): Float {
        if (visibleSeries.isEmpty()) {
            return paddingTop.toFloat()
        }

        val markerSize = styleOptions.legendMarkerSizeDp * density
        val itemGap = styleOptions.legendItemGapDp * density
        val rowGap = styleOptions.legendRowGapDp * density
        val startX = paddingLeft + (styleOptions.contentPaddingDp + presentationOptions.legendLeftMarginDp) * density
        val maxX = width.toFloat() - paddingRight - styleOptions.contentPaddingDp * density
        val baselineOffset = abs(legendTextPaint.fontMetrics.ascent)
        val rowHeight = max(markerSize, legendTextPaint.fontMetrics.bottom - legendTextPaint.fontMetrics.top)

        var cursorX = startX
        var baselineY = paddingTop + (styleOptions.contentPaddingDp + presentationOptions.legendTopMarginDp) * density + baselineOffset

        visibleSeries.forEach { item ->
            val textWidth = legendTextPaint.measureText(item.name)
            val itemWidth = markerSize + 6f * density + textWidth + itemGap

            if (cursorX + itemWidth > maxX && cursorX > startX) {
                cursorX = startX
                baselineY += rowHeight + rowGap
            }

            val markerTop = baselineY - baselineOffset + (rowHeight - markerSize) * 0.5f
            legendMarkerPaint.color = item.color
            canvas.drawRect(cursorX, markerTop, cursorX + markerSize, markerTop + markerSize, legendMarkerPaint)
            canvas.drawText(item.name, cursorX + markerSize + 6f * density, baselineY, legendTextPaint)

            cursorX += itemWidth
        }

        return baselineY + styleOptions.legendBottomGapDp * density
    }

    /**
     * Renders polygon grid levels and radial axis lines.
     */
    private fun drawGridAndAxes(canvas: Canvas, geometry: Geometry) {
        val axisCount = axes.size
        val levels = presentationOptions.gridLevels.coerceAtLeast(1)
        val radius = geometry.radius

        for (level in 1..levels) {
            val levelRadius = radius * (level.toFloat() / levels.toFloat())
            polygonPath.reset()
            for (axisIndex in 0 until axisCount) {
                val point = RadarChartMath.vertex(
                    centerX = geometry.centerX,
                    centerY = geometry.centerY,
                    radius = levelRadius,
                    axisIndex = axisIndex,
                    axisCount = axisCount,
                    startAngleDeg = presentationOptions.startAngleDeg,
                )
                if (axisIndex == 0) {
                    polygonPath.moveTo(point.x, point.y)
                } else {
                    polygonPath.lineTo(point.x, point.y)
                }
            }
            polygonPath.close()
            canvas.drawPath(polygonPath, gridPaint)
        }

        for (axisIndex in 0 until axisCount) {
            val outer = RadarChartMath.vertex(
                centerX = geometry.centerX,
                centerY = geometry.centerY,
                radius = radius,
                axisIndex = axisIndex,
                axisCount = axisCount,
                startAngleDeg = presentationOptions.startAngleDeg,
            )
            canvas.drawLine(geometry.centerX, geometry.centerY, outer.x, outer.y, axisPaint)
        }
    }

    /**
     * Renders axis labels around the chart perimeter.
     */
    private fun drawAxisLabels(canvas: Canvas, geometry: Geometry) {
        val axisCount = axes.size
        val offset = styleOptions.axisLabelOffsetDp * density

        for (axisIndex in 0 until axisCount) {
            val point = RadarChartMath.vertex(
                centerX = geometry.centerX,
                centerY = geometry.centerY,
                radius = geometry.radius + offset,
                axisIndex = axisIndex,
                axisCount = axisCount,
                startAngleDeg = presentationOptions.startAngleDeg,
            )
            val dx = point.x - geometry.centerX
            axisLabelPaint.textAlign = resolveAxisLabelAlign(dx)
            val baselineY = resolveAxisLabelBaseline(point.y)
            canvas.drawText(axes[axisIndex].label, point.x, baselineY, axisLabelPaint)
        }
    }

    /**
     * Shrinks radius until axis labels fit without clipping and returns the final radius.
     *
     * @param centerX Chart center X
     * @param centerY Chart center Y
     * @param maxRadius Maximum allowed radius in the current layout
     */
    private fun resolveRadiusToFitAxisLabels(
        centerX: Float,
        centerY: Float,
        maxRadius: Float,
    ): Float {
        if (!presentationOptions.showAxisLabels || axes.isEmpty()) return maxRadius

        val minRadius = 24f * density
        val step = max(1f, 1.5f * density)
        var radius = maxRadius
        while (radius > minRadius && !axisLabelsFitInChartArea(centerX, centerY, radius)) {
            radius -= step
        }
        return radius.coerceAtLeast(0f)
    }

    /**
     * Checks whether all axis labels stay inside chart bounds for the current radius.
     */
    private fun axisLabelsFitInChartArea(
        centerX: Float,
        centerY: Float,
        radius: Float,
    ): Boolean {
        val axisCount = axes.size
        if (axisCount <= 0) return true

        val offset = styleOptions.axisLabelOffsetDp * density
        val fm = axisLabelPaint.fontMetrics

        for (axisIndex in 0 until axisCount) {
            val point = RadarChartMath.vertex(
                centerX = centerX,
                centerY = centerY,
                radius = radius + offset,
                axisIndex = axisIndex,
                axisCount = axisCount,
                startAngleDeg = presentationOptions.startAngleDeg,
            )
            val label = axes[axisIndex].label
            val labelWidth = axisLabelPaint.measureText(label)
            val align = resolveAxisLabelAlign(point.x - centerX)
            val baselineY = resolveAxisLabelBaseline(point.y)

            val left = when (align) {
                Paint.Align.LEFT -> point.x
                Paint.Align.CENTER -> point.x - (labelWidth * 0.5f)
                Paint.Align.RIGHT -> point.x - labelWidth
            }
            val right = when (align) {
                Paint.Align.LEFT -> point.x + labelWidth
                Paint.Align.CENTER -> point.x + (labelWidth * 0.5f)
                Paint.Align.RIGHT -> point.x
            }
            val top = baselineY + fm.ascent
            val bottom = baselineY + fm.descent

            if (left < chartAreaRect.left || right > chartAreaRect.right || top < chartAreaRect.top || bottom > chartAreaRect.bottom) {
                return false
            }
        }
        return true
    }

    /**
     * Returns horizontal alignment for an axis label.
     *
     * Uses LEFT on the right side, RIGHT on the left side, and CENTER near vertical axes.
     */
    private fun resolveAxisLabelAlign(dx: Float): Paint.Align {
        return when {
            abs(dx) < 8f * density -> Paint.Align.CENTER
            dx > 0f -> Paint.Align.LEFT
            else -> Paint.Align.RIGHT
        }
    }

    /**
     * Converts a label anchor Y position to a text baseline Y position.
     */
    private fun resolveAxisLabelBaseline(anchorY: Float): Float {
        return anchorY + (axisLabelPaint.textSize * 0.35f)
    }

    /**
     * Converts current series into screen-space point lists.
     */
    private fun buildSeriesPoints(
        seriesList: List<RadarSeries>,
        geometry: Geometry,
        progress: Float,
    ): List<List<RadarChartMath.Vec2>> {
        val axisCount = axes.size
        return seriesList.map { item ->
            RadarChartMath.polygonPoints(
                values = item.values,
                maxValue = valueMax,
                centerX = geometry.centerX,
                centerY = geometry.centerY,
                radius = geometry.radius,
                axisCount = axisCount,
                progress = progress,
                startAngleDeg = presentationOptions.startAngleDeg,
            )
        }
    }

    /**
     * Renders series polygons (fill/stroke) and points.
     */
    private fun drawSeries(
        canvas: Canvas,
        visibleSeries: List<RadarSeries>,
        pointsBySeries: List<List<RadarChartMath.Vec2>>,
    ) {
        pointsBySeries.forEachIndexed { seriesIndex, points ->
            if (points.isEmpty()) return@forEachIndexed
            val seriesItem = visibleSeries[seriesIndex]

            seriesPath.reset()
            points.forEachIndexed { pointIndex, point ->
                if (pointIndex == 0) {
                    seriesPath.moveTo(point.x, point.y)
                } else {
                    seriesPath.lineTo(point.x, point.y)
                }
            }
            seriesPath.close()

            polygonFillPaint.color = applyAlpha(seriesItem.color, styleOptions.fillAlpha)
            polygonStrokePaint.color = seriesItem.color
            polygonStrokePaint.strokeWidth = if (selectedPoint?.seriesIndex == seriesIndex) {
                styleOptions.selectedStrokeWidthDp.coerceAtLeast(1f) * density
            } else {
                styleOptions.polygonStrokeWidthDp.coerceAtLeast(0.5f) * density
            }

            canvas.drawPath(seriesPath, polygonFillPaint)
            canvas.drawPath(seriesPath, polygonStrokePaint)

            if (presentationOptions.showPoints) {
                drawSeriesPoints(canvas, seriesIndex, seriesItem.color, points)
            }
        }
    }

    /**
     * Renders axis points for a single series.
     *
     * Selected points are emphasized with an extra outer ring.
     */
    private fun drawSeriesPoints(
        canvas: Canvas,
        seriesIndex: Int,
        color: Int,
        points: List<RadarChartMath.Vec2>,
    ) {
        val pointRadius = styleOptions.pointRadiusDp * density
        val coreRadius = styleOptions.pointCoreRadiusDp * density
        val glowRadius = styleOptions.pointGlowRadiusDp * density
        val selectedRadius = styleOptions.selectedPointRadiusDp * density

        points.forEachIndexed { axisIndex, point ->
            glowPaint.color = applyAlpha(color, styleOptions.pointGlowAlpha)
            pointPaint.color = color

            canvas.drawCircle(point.x, point.y, glowRadius, glowPaint)
            canvas.drawCircle(point.x, point.y, pointRadius, pointPaint)
            canvas.drawCircle(point.x, point.y, coreRadius, pointCorePaint)

            if (selectedPoint?.seriesIndex == seriesIndex && selectedPoint?.axisIndex == axisIndex) {
                canvas.drawCircle(point.x, point.y, selectedRadius, selectedRingPaint)
            }
        }
    }

    /**
     * Returns a color with overridden alpha.
     */
    private fun applyAlpha(color: Int, alpha: Int): Int {
        return Color.argb(
            alpha.coerceIn(0, 255),
            Color.red(color),
            Color.green(color),
            Color.blue(color),
        )
    }
}
