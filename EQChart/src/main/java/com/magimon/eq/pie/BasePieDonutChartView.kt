package com.magimon.eq.pie

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Shared rendering view for pie and donut charts.
 *
 * Public API covers data/style/presentation configuration and slice click callbacks.
 * The actual shape (pie vs donut) is determined by the inner-radius ratio set by subclasses.
 */
abstract class BasePieDonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private data class LabelRender(
        val text: String,
        val x: Float,
        val y: Float,
        val align: Paint.Align,
        val lineFromX: Float? = null,
        val lineFromY: Float? = null,
        val lineMidX: Float? = null,
        val lineMidY: Float? = null,
        val lineToX: Float? = null,
        val lineToY: Float? = null,
    )

    private data class SliceOffset(
        val dx: Float,
        val dy: Float,
    )

    private val density = resources.displayMetrics.density

    private var styleOptions = PieDonutStyleOptions()
    private var presentationOptions = PieDonutPresentationOptions()
    private var rawSlices: List<PieSlice> = emptyList()
    private var segments: List<PieDonutChartMath.SliceSegment> = emptyList()

    private var innerRadiusRatio: Float = 0f

    private var onSliceClickListener: ((Int, PieSlice, Any?) -> Unit)? = null
    private var selectedSliceIndex: Int? = null
    private var selectionFromIndex: Int? = null
    private var selectionToIndex: Int? = null
    private var selectionProgress: Float = 1f

    private var renderProgress: Float = 1f
    private var animator: ValueAnimator? = null
    private var selectionAnimator: ValueAnimator? = null

    private var chartRect = RectF()
    private val translatedOuterRect = RectF()
    private val translatedInnerRect = RectF()
    private var centerX = 0f
    private var centerY = 0f
    private var outerRadius = 0f
    private var innerRadius = 0f

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val labelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }
    private val labelLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val legendTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.LEFT
    }
    private val legendMarkerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val centerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }
    private val centerSubTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }
    private val emptyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }

    private val slicePath = Path()

    init {
        applyStyle()
    }

    /**
     * Sets the slice data list.
     *
     * Only values that satisfy (`value > 0` and finite) are renderable.
     */
    fun setData(items: List<PieSlice>) {
        rawSlices = items
        selectionAnimator?.cancel()
        selectedSliceIndex = null
        selectionFromIndex = null
        selectionToIndex = null
        selectionProgress = 1f
        recomputeSegments()
        if (presentationOptions.animateOnDataChange && segments.isNotEmpty()) {
            playEnterAnimation()
        } else {
            renderProgress = 1f
            invalidate()
        }
    }

    /**
     * Sets data by mapping arbitrary source items to [PieSlice].
     */
    fun <T> setData(items: List<T>, mapper: (T) -> PieSlice) {
        setData(items.map(mapper))
    }

    /**
     * Sets rendering style options.
     */
    fun setStyleOptions(options: PieDonutStyleOptions) {
        styleOptions = options
        applyStyle()
        invalidate()
    }

    /**
     * Sets presentation and animation options.
     */
    fun setPresentationOptions(options: PieDonutPresentationOptions) {
        presentationOptions = options
        if (!presentationOptions.enableSelectionExpand) {
            selectionAnimator?.cancel()
            selectionFromIndex = null
            selectionToIndex = null
            selectionProgress = 1f
        }
        recomputeSegments()
        if (presentationOptions.animateOnDataChange && segments.isNotEmpty()) {
            playEnterAnimation()
        } else {
            renderProgress = 1f
            invalidate()
        }
    }

    /**
     * Sets the slice click callback.
     *
     * Callback arguments are ordered as `(sliceIndex, slice, payload)`.
     */
    fun setOnSliceClickListener(listener: (sliceIndex: Int, slice: PieSlice, payload: Any?) -> Unit) {
        onSliceClickListener = listener
    }

    /**
     * Manually plays the enter animation.
     */
    fun playEnterAnimation() {
        if (segments.isEmpty()) return
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

    /**
     * Sets the inner-hole radius ratio.
     *
     * Use `0` for pie charts and values greater than `0` for donut charts.
     */
    protected fun setInnerRadiusRatio(ratio: Float) {
        innerRadiusRatio = ratio.coerceIn(0f, 0.92f)
        invalidate()
    }

    /**
     * Returns the current inner-hole radius ratio.
     */
    protected fun currentInnerRadiusRatio(): Float = innerRadiusRatio

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        selectionAnimator?.cancel()
        selectionAnimator = null
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = (780f * density).toInt()
        val desiredHeight = (560f * density).toInt()
        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(styleOptions.backgroundColor)

        if (width <= 0 || height <= 0) return
        applyStyle()

        val legendReserved = resolveLegendReservedHeight(width.toFloat())
        val contentPadding = dp(styleOptions.contentPaddingDp)
        chartRect.set(
            paddingLeft + contentPadding,
            paddingTop + contentPadding,
            width.toFloat() - paddingRight - contentPadding,
            height.toFloat() - paddingBottom - contentPadding - legendReserved,
        )

        if (chartRect.width() <= 0f || chartRect.height() <= 0f) return

        centerX = chartRect.centerX()
        centerY = chartRect.centerY()
        outerRadius = (min(chartRect.width(), chartRect.height()) * 0.5f).coerceAtLeast(0f)
        innerRadius = outerRadius * innerRadiusRatio

        if (segments.isEmpty() || outerRadius <= 0f) {
            drawEmptyState(canvas)
            return
        }

        drawSlices(canvas)
        drawLabels(canvas)
        drawCenterTexts(canvas)
        drawLegend(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (segments.isEmpty()) return super.onTouchEvent(event)

        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> true
            MotionEvent.ACTION_UP -> {
                val hit = PieDonutChartMath.hitTest(
                    touchX = event.x,
                    touchY = event.y,
                    centerX = centerX,
                    centerY = centerY,
                    innerRadius = innerRadius,
                    outerRadius = outerRadius,
                    segments = segments,
                )
                if (hit != null) {
                    updateSelection(hit.index)
                    val segment = segments[hit.index]
                    onSliceClickListener?.invoke(segment.index, segment.slice, segment.slice.payload)
                    performClick()
                    true
                } else {
                    updateSelection(null)
                    false
                }
            }
            else -> super.onTouchEvent(event)
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun drawSlices(canvas: Canvas) {
        val progress = renderProgress.coerceIn(0f, 1f)
        val baseStroke = dp(styleOptions.sliceStrokeWidthDp)

        segments.forEachIndexed { index, segment ->
            val sweep = segment.sweepAngleDeg * progress
            if (abs(sweep) < 0.01f) return@forEachIndexed

            val offset = resolveSliceOffset(index, segment)
            val localCenterX = centerX + offset.dx
            val localCenterY = centerY + offset.dy

            translatedOuterRect.set(
                localCenterX - outerRadius,
                localCenterY - outerRadius,
                localCenterX + outerRadius,
                localCenterY + outerRadius,
            )
            translatedInnerRect.set(
                localCenterX - innerRadius,
                localCenterY - innerRadius,
                localCenterX + innerRadius,
                localCenterY + innerRadius,
            )

            slicePath.reset()
            val start = segment.startAngleDeg
            if (innerRadius <= 0f) {
                slicePath.moveTo(localCenterX, localCenterY)
                slicePath.arcTo(translatedOuterRect, start, sweep)
                slicePath.close()
            } else {
                val outerStart = PieDonutChartMath.pointOnCircle(localCenterX, localCenterY, outerRadius, start)
                slicePath.moveTo(outerStart.x, outerStart.y)
                slicePath.arcTo(translatedOuterRect, start, sweep)

                val endAngle = start + sweep
                val innerEnd = PieDonutChartMath.pointOnCircle(localCenterX, localCenterY, innerRadius, endAngle)
                slicePath.lineTo(innerEnd.x, innerEnd.y)
                slicePath.arcTo(translatedInnerRect, endAngle, -sweep)
                slicePath.close()
            }

            fillPaint.color = segment.slice.color
            canvas.drawPath(slicePath, fillPaint)

            val selected = isSliceHighlighted(index)
            strokePaint.color = styleOptions.sliceStrokeColor
            strokePaint.strokeWidth = if (selected) baseStroke * 2f else baseStroke
            canvas.drawPath(slicePath, strokePaint)
        }
    }

    private fun drawLabels(canvas: Canvas) {
        if (!presentationOptions.showLabels) return

        val fm = labelTextPaint.fontMetrics
        val baselineShift = -((fm.ascent + fm.descent) * 0.5f)
        val outsideOffset = max(dp(12f), outerRadius * 0.08f)
        val lineGap = dp(8f)

        val labels = buildList {
            segments.forEachIndexed { index, segment ->
                val text = buildSliceLabel(segment)
                if (text.isBlank()) return@forEachIndexed

                val sweepAbs = abs(segment.sweepAngleDeg)
                if (sweepAbs <= 0.4f) return@forEachIndexed

                val offset = resolveSliceOffset(index, segment)
                val localCenterX = centerX + offset.dx
                val localCenterY = centerY + offset.dy
                val mid = segment.midAngleDeg
                val textWidth = labelTextPaint.measureText(text)
                val midRadius = if (innerRadius > 0f) {
                    (innerRadius + outerRadius) * 0.5f
                } else {
                    outerRadius * 0.6f
                }
                val availableArc = Math.toRadians(sweepAbs.toDouble()).toFloat() * max(midRadius, 1f)

                val insideAllowed = when (presentationOptions.labelPosition) {
                    PieLabelPosition.INSIDE -> true
                    PieLabelPosition.OUTSIDE -> false
                    PieLabelPosition.AUTO -> sweepAbs >= 20f && textWidth <= availableArc * 0.95f
                }

                if (insideAllowed) {
                    val point = PieDonutChartMath.pointOnCircle(localCenterX, localCenterY, midRadius, mid)
                    add(
                        LabelRender(
                            text = text,
                            x = point.x,
                            y = point.y + baselineShift,
                            align = Paint.Align.CENTER,
                        ),
                    )
                } else {
                    val anchor = PieDonutChartMath.pointOnCircle(localCenterX, localCenterY, outerRadius, mid)
                    val elbow = PieDonutChartMath.pointOnCircle(localCenterX, localCenterY, outerRadius + outsideOffset, mid)
                    val rightSide = PieDonutChartMath.normalizeAngle(mid) <= 90f || PieDonutChartMath.normalizeAngle(mid) >= 270f
                    val endX = if (rightSide) elbow.x + lineGap else elbow.x - lineGap
                    val textX = if (rightSide) endX + dp(2f) else endX - dp(2f)
                    add(
                        LabelRender(
                            text = text,
                            x = textX,
                            y = elbow.y + baselineShift,
                            align = if (rightSide) Paint.Align.LEFT else Paint.Align.RIGHT,
                            lineFromX = anchor.x,
                            lineFromY = anchor.y,
                            lineMidX = elbow.x,
                            lineMidY = elbow.y,
                            lineToX = endX,
                            lineToY = elbow.y,
                        ),
                    )
                }
            }
        }

        labels.forEach { label ->
            if (label.lineFromX != null && label.lineFromY != null && label.lineMidX != null && label.lineMidY != null && label.lineToX != null && label.lineToY != null) {
                canvas.drawLine(label.lineFromX, label.lineFromY, label.lineMidX, label.lineMidY, labelLinePaint)
                canvas.drawLine(label.lineMidX, label.lineMidY, label.lineToX, label.lineToY, labelLinePaint)
            }
            labelTextPaint.textAlign = label.align
            canvas.drawText(label.text, label.x, label.y, labelTextPaint)
        }
    }

    private fun drawCenterTexts(canvas: Canvas) {
        if (innerRadius <= 0f) return

        val title = presentationOptions.centerText?.trim().orEmpty()
        val sub = presentationOptions.centerSubText?.trim().orEmpty()
        if (title.isEmpty() && sub.isEmpty()) return

        if (title.isNotEmpty() && sub.isNotEmpty()) {
            val gap = dp(4f)
            val titleY = centerY - gap
            val subY = centerY + centerSubTextPaint.fontSpacing * 0.6f
            canvas.drawText(title, centerX, titleY, centerTextPaint)
            canvas.drawText(sub, centerX, subY, centerSubTextPaint)
            return
        }

        if (title.isNotEmpty()) {
            canvas.drawText(title, centerX, centerY - (centerTextPaint.ascent() + centerTextPaint.descent()) * 0.5f, centerTextPaint)
        } else {
            canvas.drawText(sub, centerX, centerY - (centerSubTextPaint.ascent() + centerSubTextPaint.descent()) * 0.5f, centerSubTextPaint)
        }
    }

    private fun drawLegend(canvas: Canvas) {
        if (!presentationOptions.showLegend || segments.isEmpty()) return

        val markerSize = dp(styleOptions.legendMarkerSizeDp)
        val markerTextGap = dp(styleOptions.legendMarkerTextGapDp)
        val itemGap = dp(styleOptions.legendItemSpacingDp)
        val rowGap = dp(styleOptions.legendRowSpacingDp)
        val startX = paddingLeft + dp(styleOptions.contentPaddingDp) + dp(presentationOptions.legendLeftMarginDp)
        val maxX = width - paddingRight - dp(styleOptions.contentPaddingDp)

        var cursorX = startX
        val rowHeight = max(markerSize, legendTextPaint.fontSpacing)
        var baselineY = chartRect.bottom + dp(presentationOptions.legendTopMarginDp) + abs(legendTextPaint.fontMetrics.ascent)

        segments.forEach { segment ->
            val text = segment.slice.label.ifBlank { "Slice" }
            val textWidth = legendTextPaint.measureText(text)
            val itemWidth = markerSize + markerTextGap + textWidth

            if (cursorX + itemWidth > maxX && cursorX > startX) {
                cursorX = startX
                baselineY += rowHeight + rowGap
            }

            val markerTop = baselineY - abs(legendTextPaint.fontMetrics.ascent) + (rowHeight - markerSize) * 0.5f
            legendMarkerPaint.color = segment.slice.color
            canvas.drawRect(cursorX, markerTop, cursorX + markerSize, markerTop + markerSize, legendMarkerPaint)
            canvas.drawText(text, cursorX + markerSize + markerTextGap, baselineY, legendTextPaint)

            cursorX += itemWidth + itemGap
        }
    }

    private fun drawEmptyState(canvas: Canvas) {
        val text = presentationOptions.emptyText?.trim().orEmpty()
        if (text.isEmpty()) return
        val x = width * 0.5f
        val y = height * 0.5f - (emptyTextPaint.ascent() + emptyTextPaint.descent()) * 0.5f
        canvas.drawText(text, x, y, emptyTextPaint)
    }

    private fun buildSliceLabel(segment: PieDonutChartMath.SliceSegment): String {
        val name = segment.slice.label.trim()
        val percent = (segment.ratio * 100f).roundToInt().coerceIn(0, 100)
        if (name.isEmpty()) return "$percent%"
        return "$name $percent%"
    }

    private fun recomputeSegments() {
        segments = PieDonutChartMath.buildSegments(
            slices = rawSlices,
            startAngleDeg = presentationOptions.startAngleDeg,
            clockwise = presentationOptions.clockwise,
        )
        val selected = selectedSliceIndex
        if (selected != null && selected !in segments.indices) {
            selectedSliceIndex = null
        }
        val from = selectionFromIndex
        if (from != null && from !in segments.indices) {
            selectionFromIndex = null
        }
        val to = selectionToIndex
        if (to != null && to !in segments.indices) {
            selectionToIndex = null
        }
    }

    private fun updateSelection(newIndex: Int?) {
        val oldIndex = selectedSliceIndex
        if (oldIndex == newIndex && selectionFromIndex == null && selectionToIndex == null) {
            return
        }

        val allowExpand = presentationOptions.enableSelectionExpand && presentationOptions.selectedSliceExpandDp > 0f
        if (!allowExpand) {
            selectionAnimator?.cancel()
            selectionAnimator = null
            selectionFromIndex = null
            selectionToIndex = null
            selectionProgress = 1f
            selectedSliceIndex = newIndex
            invalidate()
            return
        }

        selectionAnimator?.cancel()
        selectionAnimator = null

        if (oldIndex == newIndex) {
            selectionFromIndex = null
            selectionToIndex = null
            selectionProgress = 1f
            invalidate()
            return
        }

        selectionFromIndex = oldIndex
        selectionToIndex = newIndex
        selectionProgress = 0f

        val duration = presentationOptions.selectedSliceExpandAnimMs.coerceAtLeast(0L)
        if (duration == 0L) {
            selectedSliceIndex = newIndex
            selectionFromIndex = null
            selectionToIndex = null
            selectionProgress = 1f
            invalidate()
            return
        }

        selectionAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                selectionProgress = (it.animatedValue as Float).coerceIn(0f, 1f)
                invalidate()
            }
            addListener(
                object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                    selectedSliceIndex = newIndex
                    selectionFromIndex = null
                    selectionToIndex = null
                    selectionProgress = 1f
                    selectionAnimator = null
                    invalidate()
                    }

                    override fun onAnimationCancel(animation: android.animation.Animator) {
                        selectionAnimator = null
                    }
                },
            )
            start()
        }
    }

    private fun resolveSliceOffset(index: Int, segment: PieDonutChartMath.SliceSegment): SliceOffset {
        if (!presentationOptions.enableSelectionExpand) return SliceOffset(0f, 0f)
        val expandDistance = dp(presentationOptions.selectedSliceExpandDp).coerceAtLeast(0f)
        if (expandDistance <= 0f) return SliceOffset(0f, 0f)

        val factor = when {
            selectionFromIndex != null || selectionToIndex != null -> {
                val leaving = if (index == selectionFromIndex) 1f - selectionProgress else 0f
                val entering = if (index == selectionToIndex) selectionProgress else 0f
                max(leaving, entering)
            }
            selectedSliceIndex == index -> 1f
            else -> 0f
        }

        if (factor <= 0f) return SliceOffset(0f, 0f)

        val distance = expandDistance * factor
        val angleRad = Math.toRadians(segment.midAngleDeg.toDouble())
        return SliceOffset(
            dx = (cos(angleRad) * distance).toFloat(),
            dy = (sin(angleRad) * distance).toFloat(),
        )
    }

    private fun isSliceHighlighted(index: Int): Boolean {
        return selectedSliceIndex == index || selectionFromIndex == index || selectionToIndex == index
    }

    private fun resolveLegendReservedHeight(availableWidth: Float): Float {
        if (!presentationOptions.showLegend || segments.isEmpty()) return 0f

        val markerSize = dp(styleOptions.legendMarkerSizeDp)
        val markerTextGap = dp(styleOptions.legendMarkerTextGapDp)
        val itemGap = dp(styleOptions.legendItemSpacingDp)
        val rowGap = dp(styleOptions.legendRowSpacingDp)
        val rowHeight = max(markerSize, legendTextPaint.fontSpacing)

        val startX = paddingLeft + dp(styleOptions.contentPaddingDp) + dp(presentationOptions.legendLeftMarginDp)
        val maxX = availableWidth - paddingRight - dp(styleOptions.contentPaddingDp)

        var cursorX = startX
        var rowCount = 1

        segments.forEach { segment ->
            val text = segment.slice.label.ifBlank { "Slice" }
            val textWidth = legendTextPaint.measureText(text)
            val itemWidth = markerSize + markerTextGap + textWidth
            if (cursorX + itemWidth > maxX && cursorX > startX) {
                rowCount += 1
                cursorX = startX
            }
            cursorX += itemWidth + itemGap
        }

        return dp(presentationOptions.legendTopMarginDp) +
            (rowCount * rowHeight) +
            ((rowCount - 1).coerceAtLeast(0) * rowGap) +
            dp(presentationOptions.legendBottomMarginDp)
    }

    private fun applyStyle() {
        strokePaint.color = styleOptions.sliceStrokeColor
        strokePaint.strokeWidth = dp(styleOptions.sliceStrokeWidthDp)

        labelTextPaint.color = styleOptions.labelTextColor
        labelTextPaint.textSize = sp(styleOptions.labelTextSizeSp)

        labelLinePaint.color = styleOptions.labelLineColor
        labelLinePaint.strokeWidth = dp(1f)

        legendTextPaint.color = styleOptions.legendTextColor
        legendTextPaint.textSize = sp(styleOptions.legendTextSizeSp)

        centerTextPaint.color = styleOptions.centerTextColor
        centerTextPaint.textSize = sp(styleOptions.centerTextSizeSp)

        centerSubTextPaint.color = styleOptions.centerSubTextColor
        centerSubTextPaint.textSize = sp(styleOptions.centerSubTextSizeSp)

        emptyTextPaint.color = styleOptions.centerSubTextColor
        emptyTextPaint.textSize = sp(13f)
    }

    private fun dp(value: Float): Float = value * density

    private fun sp(value: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        value,
        resources.displayMetrics,
    )
}
