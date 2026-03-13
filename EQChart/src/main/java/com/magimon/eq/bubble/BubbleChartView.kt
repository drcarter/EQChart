package com.magimon.eq.bubble

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Custom view that renders a bubble chart.
 *
 * - Supports `SCATTER` and `PACKED` layout modes.
 * - Axis/grid behavior is controlled by [BubbleAxisOptions].
 * - Title/legend behavior is controlled by [BubblePresentationOptions].
 */
class BubbleChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private data class MutableBubbleNode(
        val datum: BubbleDatum,
        val radius: Float,
        var x: Float,
        var y: Float,
    )

    private data class BubbleLayout(
        val datum: BubbleDatum,
        val centerX: Float,
        val centerY: Float,
        val radius: Float,
    )

    private val density = resources.displayMetrics.density

    private val outerPadding = 12f * density
    private val axisGutterLeft = 48f * density
    private val axisGutterBottom = 36f * density
    private val axisGutterTop = 12f * density
    private val axisGutterRight = 12f * density

    private var axisOptions = BubbleAxisOptions()
    private var presentationOptions = BubblePresentationOptions()
    private var scaleOverride: BubbleScaleOverride? = null
    private var layoutMode = BubbleLayoutMode.SCATTER
    private var onBubbleClickListener: ((BubbleDatum) -> Unit)? = null

    private val rawData = mutableListOf<BubbleDatum>()
    private val bubbleLayouts = mutableListOf<BubbleLayout>()
    private var explicitLegendItems: List<BubbleLegendItem> = emptyList()
    private var resolvedLegendItems: List<BubbleLegendItem> = emptyList()

    private var selectedBubble: BubbleLayout? = null
    private var plotRect = RectF()

    private var xRange = BubbleChartMath.NumericRange(0.0, 1.0)
    private var yRange = BubbleChartMath.NumericRange(0.0, 1.0)
    private var sizeRange = BubbleChartMath.NumericRange(0.0, 1.0)
    private var xTicks: List<Double> = emptyList()
    private var yTicks: List<Double> = emptyList()

    private var titleReservedHeight = 0f
    private var legendReservedHeight = 0f

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#101418")
    }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8D9AA8")
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 10f * resources.displayMetrics.scaledDensity
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C6D2DE")
        textSize = 10f * resources.displayMetrics.scaledDensity
    }
    private val selectedStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2A2A2A")
        textAlign = Paint.Align.LEFT
        textSize = 20f * resources.displayMetrics.scaledDensity
    }
    private val legendTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2A2A2A")
        textAlign = Paint.Align.LEFT
        textSize = 12f * resources.displayMetrics.scaledDensity
    }
    private val legendMarkerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    /**
     * Sets axis and grid display options.
     */
    fun setAxisOptions(options: BubbleAxisOptions) {
        axisOptions = options
        recomputeLayout(width, height)
        invalidate()
    }

    /**
     * Sets presentation options such as title and legend.
     */
    fun setPresentationOptions(options: BubblePresentationOptions) {
        presentationOptions = options
        recomputeLayout(width, height)
        invalidate()
    }

    /**
     * Sets explicit legend items.
     *
     * Whether these items are used depends on [BubblePresentationOptions.legendMode].
     */
    fun setLegendItems(items: List<BubbleLegendItem>) {
        explicitLegendItems = items
        recomputeLayout(width, height)
        invalidate()
    }

    /**
     * Sets manual axis/size scale overrides.
     */
    fun setScaleOverride(override: BubbleScaleOverride?) {
        scaleOverride = override
        recomputeLayout(width, height)
        invalidate()
    }

    /**
     * Sets the bubble layout mode.
     */
    fun setLayoutMode(mode: BubbleLayoutMode) {
        layoutMode = mode
        recomputeLayout(width, height)
        invalidate()
    }

    /**
     * Sets the chart background color.
     */
    fun setChartBackgroundColor(color: Int) {
        backgroundPaint.color = color
        invalidate()
    }

    /**
     * Sets the bubble dataset.
     *
     * Non-finite `x/y/size` values are filtered out automatically.
     */
    fun setData(data: List<BubbleDatum>) {
        rawData.clear()
        rawData.addAll(data.filter { it.size.isFinite() && it.x.isFinite() && it.y.isFinite() })
        selectedBubble = null
        recomputeLayout(width, height)
        invalidate()
    }

    /**
     * Sets data by mapping an arbitrary source type into [BubbleDatum].
     */
    fun <T> setData(items: List<T>, mapper: (T) -> BubbleDatum) {
        setData(items.map(mapper))
    }

    /**
     * Sets the bubble click listener.
     */
    fun setOnBubbleClickListener(listener: (BubbleDatum) -> Unit) {
        onBubbleClickListener = listener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = (800f * density).toInt()
        val desiredHeight = (520f * density).toInt()
        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec),
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recomputeLayout(w, h)
    }

    /**
     * Recomputes bubble coordinates and plot bounds from current data/options/view size.
     */
    private fun recomputeLayout(width: Int, height: Int) {
        bubbleLayouts.clear()
        resolvedLegendItems = resolveLegendItems()

        if (width <= 0 || height <= 0) {
            plotRect = RectF()
            return
        }

        applyPresentationStyles()
        titleReservedHeight = resolveTitleReservedHeight()
        legendReservedHeight = resolveLegendReservedHeight()

        val baseRect = RectF(
            outerPadding,
            outerPadding + titleReservedHeight,
            width.toFloat() - outerPadding,
            height.toFloat() - outerPadding - legendReservedHeight,
        )

        val useAxisSpace = axisOptions.showAxes || axisOptions.showTicks
        plotRect = if (useAxisSpace) {
            RectF(
                baseRect.left + axisGutterLeft,
                baseRect.top + axisGutterTop,
                baseRect.right - axisGutterRight,
                baseRect.bottom - axisGutterBottom,
            )
        } else {
            baseRect
        }

        if (plotRect.width() <= 0f || plotRect.height() <= 0f || rawData.isEmpty()) {
            xTicks = emptyList()
            yTicks = emptyList()
            return
        }

        val minRadius = (4f * density).coerceAtLeast(min(plotRect.width(), plotRect.height()) * 0.01f)
        val maxRadius = min(plotRect.width(), plotRect.height()) * 0.12f

        val layouts = if (layoutMode == BubbleLayoutMode.PACKED) {
            xTicks = emptyList()
            yTicks = emptyList()
            buildPackedLayouts(minRadius, maxRadius)
        } else {
            val override = scaleOverride
            xRange = BubbleChartMath.resolveRange(rawData.map { it.x }, override?.xMin, override?.xMax)
            yRange = BubbleChartMath.resolveRange(rawData.map { it.y }, override?.yMin, override?.yMax)
            sizeRange = BubbleChartMath.resolveRange(rawData.map { it.size }, override?.sizeMin, override?.sizeMax)

            xTicks = BubbleChartMath.tickValues(xRange, 5)
            yTicks = BubbleChartMath.tickValues(yRange, 5)

            rawData.map { datum ->
                val radius = BubbleChartMath.mapRadius(datum.size, sizeRange, minRadius, maxRadius)
                var cx = BubbleChartMath.mapX(datum.x, xRange, plotRect)
                var cy = BubbleChartMath.mapY(datum.y, yRange, plotRect)
                cx = cx.coerceIn(plotRect.left + radius, plotRect.right - radius)
                cy = cy.coerceIn(plotRect.top + radius, plotRect.bottom - radius)
                BubbleLayout(
                    datum = datum,
                    centerX = cx,
                    centerY = cy,
                    radius = radius,
                )
            }
        }

        bubbleLayouts.addAll(layouts.sortedByDescending { it.radius })
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        drawTitle(canvas)

        if (plotRect.width() > 0f && plotRect.height() > 0f) {
            if (axisOptions.showGrid) drawGrid(canvas)
            if (axisOptions.showAxes) drawAxes(canvas)
            if (axisOptions.showTicks) drawTicks(canvas)

            bubbleLayouts.forEach { bubble ->
                bubblePaint.color = bubble.datum.color
                canvas.drawCircle(bubble.centerX, bubble.centerY, bubble.radius, bubblePaint)
                drawBubbleLabel(canvas, bubble)
            }

            selectedBubble?.let { selected ->
                canvas.drawCircle(selected.centerX, selected.centerY, selected.radius, selectedStrokePaint)
            }
        }

        drawLegend(canvas)
    }

    /**
     * Renders the title at the top-left area.
     */
    private fun drawTitle(canvas: Canvas) {
        val title = presentationOptions.title?.trim().orEmpty()
        if (title.isEmpty()) return

        val x = outerPadding
        val y = outerPadding - titlePaint.ascent()
        canvas.drawText(title, x, y, titlePaint)
    }

    /**
     * Renders legend items near the bottom-left area.
     */
    private fun drawLegend(canvas: Canvas) {
        if (!presentationOptions.showLegend || resolvedLegendItems.isEmpty() || plotRect.height() <= 0f) return

        val markerSize = dp(presentationOptions.legendMarkerSizeDp)
        val markerTextGap = dp(presentationOptions.legendMarkerTextGapDp)
        val itemSpacing = dp(presentationOptions.legendItemSpacingDp)
        val lineHeight = max(legendTextPaint.fontSpacing, markerSize)
        val startX = outerPadding + dp(presentationOptions.legendLeftMarginDp)
        var rowTop = plotRect.bottom + dp(presentationOptions.legendSectionTopPaddingDp)

        resolvedLegendItems.forEach { item ->
            val markerTop = rowTop + (lineHeight - markerSize) * 0.5f
            legendMarkerPaint.color = item.color
            canvas.drawRect(startX, markerTop, startX + markerSize, markerTop + markerSize, legendMarkerPaint)

            val baseline = rowTop + (lineHeight - (legendTextPaint.descent() + legendTextPaint.ascent())) * 0.5f
            canvas.drawText(item.label, startX + markerSize + markerTextGap, baseline, legendTextPaint)

            rowTop += lineHeight + itemSpacing
        }
    }

    private fun drawGrid(canvas: Canvas) {
        xTicks.forEach { tick ->
            val x = BubbleChartMath.mapX(tick, xRange, plotRect)
            canvas.drawLine(x, plotRect.top, x, plotRect.bottom, gridPaint)
        }
        yTicks.forEach { tick ->
            val y = BubbleChartMath.mapY(tick, yRange, plotRect)
            canvas.drawLine(plotRect.left, y, plotRect.right, y, gridPaint)
        }
    }

    private fun drawAxes(canvas: Canvas) {
        canvas.drawLine(plotRect.left, plotRect.top, plotRect.left, plotRect.bottom, axisPaint)
        canvas.drawLine(plotRect.left, plotRect.bottom, plotRect.right, plotRect.bottom, axisPaint)
    }

    private fun drawTicks(canvas: Canvas) {
        val xLabelY = plotRect.bottom + 16f * density
        xTicks.forEach { tick ->
            val x = BubbleChartMath.mapX(tick, xRange, plotRect)
            val label = axisOptions.xLabelFormatter(tick)
            tickPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(label, x, xLabelY, tickPaint)
        }

        yTicks.forEach { tick ->
            val y = BubbleChartMath.mapY(tick, yRange, plotRect)
            val label = axisOptions.yLabelFormatter(tick)
            tickPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(label, plotRect.left - 8f * density, y + 4f * density, tickPaint)
        }
    }

    /**
     * Renders in-bubble labels with a contrast-aware text color.
     */
    private fun drawBubbleLabel(canvas: Canvas, bubble: BubbleLayout) {
        val lines = bubble.datum.label
            ?.split('\n')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        if (lines.isEmpty()) return
        if (bubble.radius < 18f * density) return

        labelPaint.color = contentColorForBubble(bubble.datum.color)
        val lineHeight = labelPaint.fontSpacing * 0.9f
        val totalHeight = lineHeight * (lines.size - 1)
        var y = bubble.centerY - totalHeight / 2f - (labelPaint.descent() + labelPaint.ascent()) / 2f
        lines.forEach { line ->
            canvas.drawText(line, bubble.centerX, y, labelPaint)
            y += lineHeight
        }
    }

    private fun contentColorForBubble(color: Int): Int {
        val luminance = (0.299f * Color.red(color)) + (0.587f * Color.green(color)) + (0.114f * Color.blue(color))
        return if (luminance > 170f) Color.parseColor("#111111") else Color.WHITE
    }

    /**
     * Applies paint styles from presentation options.
     */
    private fun applyPresentationStyles() {
        titlePaint.color = presentationOptions.titleColor
        titlePaint.textSize = sp(presentationOptions.titleTextSizeSp)

        legendTextPaint.color = presentationOptions.legendTextColor
        legendTextPaint.textSize = sp(presentationOptions.legendTextSizeSp)
    }

    /**
     * Computes reserved height for the title area.
     */
    private fun resolveTitleReservedHeight(): Float {
        val title = presentationOptions.title?.trim().orEmpty()
        if (title.isEmpty()) return 0f
        return titlePaint.fontSpacing + dp(presentationOptions.titleBottomSpacingDp)
    }

    /**
     * Computes reserved height for the legend area.
     */
    private fun resolveLegendReservedHeight(): Float {
        if (!presentationOptions.showLegend || resolvedLegendItems.isEmpty()) return 0f

        val markerSize = dp(presentationOptions.legendMarkerSizeDp)
        val itemSpacing = dp(presentationOptions.legendItemSpacingDp)
        val lineHeight = max(legendTextPaint.fontSpacing, markerSize)
        val itemCount = resolvedLegendItems.size
        val itemsHeight = (lineHeight * itemCount) + (itemSpacing * (itemCount - 1).coerceAtLeast(0))

        return dp(presentationOptions.legendSectionTopPaddingDp) +
            itemsHeight +
            dp(presentationOptions.legendBottomMarginDp)
    }

    /**
     * Resolves final legend items from mode and current data.
     */
    private fun resolveLegendItems(): List<BubbleLegendItem> {
        return BubbleLegendResolver.resolve(
            data = rawData,
            mode = presentationOptions.legendMode,
            explicitItems = explicitLegendItems,
        )
    }

    /**
     * Computes bubble layout for `PACKED` mode.
     *
     * Starts from a spiral seed, resolves overlaps iteratively, then scales to fit plot bounds.
     */
    private fun buildPackedLayouts(minRadius: Float, maxRadius: Float): List<BubbleLayout> {
        sizeRange = BubbleChartMath.resolveRange(rawData.map { it.size }, null, null)
        val centerX = plotRect.centerX()
        val centerY = plotRect.centerY()
        val spiralStep = maxRadius * 1.45f
        val gap = 2f * density
        val gravity = 0.045f
        val iterations = 220

        val nodes = rawData
            .map { datum ->
                MutableBubbleNode(
                    datum = datum,
                    radius = BubbleChartMath.mapRadius(datum.size, sizeRange, minRadius, maxRadius),
                    x = centerX,
                    y = centerY,
                )
            }
            .sortedByDescending { it.radius }

        nodes.forEachIndexed { index, node ->
            if (index == 0) {
                node.x = centerX
                node.y = centerY
            } else {
                val angle = GOLDEN_ANGLE * index
                val distance = sqrt(index.toFloat()) * spiralStep
                node.x = centerX + (cos(angle) * distance)
                node.y = centerY + (sin(angle) * distance)
            }
        }

        repeat(iterations) {
            nodes.forEach { node ->
                node.x += (centerX - node.x) * gravity
                node.y += (centerY - node.y) * gravity
            }

            for (i in 0 until nodes.lastIndex) {
                val a = nodes[i]
                for (j in i + 1 until nodes.size) {
                    val b = nodes[j]
                    var dx = b.x - a.x
                    var dy = b.y - a.y
                    var distance = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                    val minDistance = a.radius + b.radius + gap

                    if (distance < minDistance) {
                        if (distance < 0.001f) {
                            val seedAngle = ((i * 73 + j * 37) % 360) * (PI.toFloat() / 180f)
                            dx = cos(seedAngle)
                            dy = sin(seedAngle)
                            distance = 1f
                        }

                        val overlap = (minDistance - distance) * 0.5f
                        val ux = dx / distance
                        val uy = dy / distance

                        a.x -= ux * overlap
                        a.y -= uy * overlap
                        b.x += ux * overlap
                        b.y += uy * overlap
                    }
                }
            }
        }

        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY

        nodes.forEach { node ->
            minX = min(minX, node.x - node.radius)
            maxX = maxOf(maxX, node.x + node.radius)
            minY = min(minY, node.y - node.radius)
            maxY = maxOf(maxY, node.y + node.radius)
        }

        val clusterWidth = (maxX - minX).coerceAtLeast(1f)
        val clusterHeight = (maxY - minY).coerceAtLeast(1f)
        val scale = min(
            (plotRect.width() * 0.94f) / clusterWidth,
            (plotRect.height() * 0.94f) / clusterHeight,
        )

        val clusterCenterX = (minX + maxX) * 0.5f
        val clusterCenterY = (minY + maxY) * 0.5f

        return nodes.map { node ->
            val transformedX = centerX + (node.x - clusterCenterX) * scale
            val transformedY = centerY + (node.y - clusterCenterY) * scale
            val transformedRadius = node.radius * scale
            BubbleLayout(
                datum = node.datum,
                centerX = transformedX.coerceIn(plotRect.left + transformedRadius, plotRect.right - transformedRadius),
                centerY = transformedY.coerceIn(plotRect.top + transformedRadius, plotRect.bottom - transformedRadius),
                radius = transformedRadius,
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val hit = bubbleLayouts.asReversed().firstOrNull { bubble ->
                val dx = event.x - bubble.centerX
                val dy = event.y - bubble.centerY
                (dx * dx + dy * dy) <= (bubble.radius * bubble.radius)
            }

            if (hit != null) {
                selectedBubble = hit
                onBubbleClickListener?.invoke(hit.datum)
                invalidate()
            }
        }
        return true
    }

    private fun dp(value: Float): Float = value * density

    private fun sp(value: Float): Float = value * resources.displayMetrics.scaledDensity

    private companion object {
        const val GOLDEN_ANGLE = 2.3999633f
    }
}
