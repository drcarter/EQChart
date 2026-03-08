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
 * 다중 시리즈 레이더 차트를 렌더링하는 커스텀 View.
 *
 * - 축 라벨([RadarAxis])과 시리즈([RadarSeries])를 받아 다각형 차트를 렌더링한다.
 * - 그리드/축/범례/포인트 스타일은 [RadarChartStyleOptions]로 제어한다.
 * - 애니메이션/표시 정책은 [RadarChartPresentationOptions]로 제어한다.
 * - 포인트 클릭 시 [setOnPointClickListener]를 통해 선택 정보를 전달한다.
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
     * 축 라벨 목록을 설정한다.
     *
     * 빈 라벨은 자동으로 제거된다.
     *
     * @param items 차트 축 정의 목록
     */
    fun setAxes(items: List<RadarAxis>) {
        axes = items.filter { it.label.isNotBlank() }
        selectedPoint = null
        refreshDataAndRender()
    }

    /**
     * 시리즈 목록을 설정한다.
     *
     * `values` 개수가 축 개수와 일치하는 시리즈만 렌더링 대상이 된다.
     *
     * @param items 시리즈 목록
     */
    fun setSeries(items: List<RadarSeries>) {
        series = items
        selectedPoint = null
        refreshDataAndRender()
    }

    /**
     * 값 스케일 최대값을 설정한다.
     *
     * 잘못된 입력은 기본값(100.0)으로 보정된다.
     *
     * @param maxValue 값 정규화 기준 최대값
     */
    fun setValueMax(maxValue: Double) {
        valueMax = if (maxValue.isFinite() && maxValue > 0.0) maxValue else 100.0
        invalidate()
    }

    /**
     * 차트 스타일 옵션을 설정한다.
     *
     * @param options 렌더링 스타일 옵션
     */
    fun setStyleOptions(options: RadarChartStyleOptions) {
        styleOptions = options
        applyStyleAndPresentation()
        invalidate()
    }

    /**
     * 차트 표시/애니메이션 옵션을 설정한다.
     *
     * @param options 표시/애니메이션 옵션
     */
    fun setPresentationOptions(options: RadarChartPresentationOptions) {
        presentationOptions = options
        applyStyleAndPresentation()
        invalidate()
    }

    /**
     * 포인트 클릭 리스너를 설정한다.
     *
     * @param listener `(seriesIndex, axisIndex, value, payload)` 형태 콜백
     */
    fun setOnPointClickListener(listener: (seriesIndex: Int, axisIndex: Int, value: Double, payload: Any?) -> Unit) {
        onPointClickListener = listener
    }

    /**
     * 초기 등장 애니메이션을 재생한다.
     *
     * 축이 3개 미만이거나 유효 시리즈가 없으면 재생하지 않는다.
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
     * 현재 축/시리즈 조합에서 렌더링 가능한 시리즈만 반환한다.
     *
     * - 값 개수가 축 개수와 다르면 제외
     * - 모든 값이 비유한값(`NaN`/`Infinity`)이면 제외
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
     * 차트 중심/반지름/범례 영역을 계산한다.
     *
     * 레이블이 클리핑되지 않도록 [resolveRadiusToFitAxisLabels]를 통해 최종 반지름을 보정한다.
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
     * 범례를 렌더링하고, 범례의 하단 Y 좌표를 반환한다.
     *
     * 차트 본문 시작 지점을 계산할 때 사용한다.
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
     * 다각형 그리드와 방사형 축 라인을 렌더링한다.
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
     * 축 라벨을 차트 외곽에 렌더링한다.
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
     * 축 라벨이 잘리지 않는 범위까지 반지름을 줄여 최종 반지름을 반환한다.
     *
     * @param centerX 차트 중심 X
     * @param centerY 차트 중심 Y
     * @param maxRadius 현재 레이아웃에서 허용 가능한 최대 반지름
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
     * 현재 반지름에서 모든 축 라벨이 차트 영역 내부에 들어오는지 검사한다.
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
     * 축 라벨 수평 정렬을 반환한다.
     *
     * 중심점 기준 우측은 LEFT, 좌측은 RIGHT, 거의 수직축은 CENTER를 사용한다.
     */
    private fun resolveAxisLabelAlign(dx: Float): Paint.Align {
        return when {
            abs(dx) < 8f * density -> Paint.Align.CENTER
            dx > 0f -> Paint.Align.LEFT
            else -> Paint.Align.RIGHT
        }
    }

    /**
     * 라벨 앵커 좌표를 텍스트 baseline 좌표로 변환한다.
     */
    private fun resolveAxisLabelBaseline(anchorY: Float): Float {
        return anchorY + (axisLabelPaint.textSize * 0.35f)
    }

    /**
     * 현재 시리즈들을 화면 좌표 포인트 목록으로 변환한다.
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
     * 시리즈 폴리곤(채움/외곽선)과 포인트를 렌더링한다.
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
     * 시리즈의 각 축 포인트를 렌더링한다.
     *
     * 선택된 포인트는 외곽 링을 추가해 강조한다.
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
     * 색상에 alpha를 덮어쓴 값을 반환한다.
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
