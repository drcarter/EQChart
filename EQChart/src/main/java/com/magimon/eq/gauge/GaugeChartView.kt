package com.magimon.eq.gauge

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * View-based semi-circular single-value gauge chart.
 *
 * This component renders a base track, optional threshold ranges, a progress arc for the current
 * value, tick marks, min/max labels, and a simple needle indicator.
 *
 * The view is intentionally non-interactive in v1. Its public API is centered on updating the
 * current [GaugeValue], supplying optional [GaugeRange] bands, and styling the presentation.
 *
 * Invalid gauge input does not throw. Instead, the view renders
 * [GaugeChartPresentationOptions.emptyText].
 */
class GaugeChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density

    private var styleOptions = GaugeChartStyleOptions()
    private var presentationOptions = GaugeChartPresentationOptions()
    private var gaugeValue: GaugeValue? = null
    private var ranges: List<GaugeRange> = emptyList()

    private var resolvedValue: GaugeChartMath.ResolvedGaugeValue? = null
    private var resolvedRanges: List<GaugeChartMath.ResolvedGaugeRange> = emptyList()

    private var renderedProgress: Float = 0f
    private var centerX: Float = 0f
    private var centerY: Float = 0f
    private var arcRadius: Float = 0f
    private val arcRect = RectF()
    private var animator: ValueAnimator? = null

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val hubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val valueTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }
    private val labelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }
    private val minMaxTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
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
     * Sets the current gauge value.
     *
     * The value is validated and clamped into its domain before rendering. When
     * [GaugeChartPresentationOptions.animateOnValueChange] is enabled, the visible progress
     * transitions from the previous value to the new one.
     */
    fun setValue(value: GaugeValue) {
        gaugeValue = value
        resolvedValue = GaugeChartMath.resolveValue(value)
        resolveRanges()
        val targetProgress = resolvedValue?.progress ?: 0f
        if (presentationOptions.animateOnValueChange && resolvedValue != null) {
            animateProgressTo(targetProgress)
        } else {
            animator?.cancel()
            renderedProgress = targetProgress
            invalidate()
        }
    }

    /**
     * Sets optional colored threshold bands shown behind the current value.
     *
     * Invalid bands are ignored during rendering rather than rejected eagerly.
     */
    fun setRanges(items: List<GaugeRange>) {
        ranges = items
        resolveRanges()
        invalidate()
    }

    /**
     * Replaces the current visual styling for the gauge.
     */
    fun setStyleOptions(options: GaugeChartStyleOptions) {
        styleOptions = options
        applyStyle()
        invalidate()
    }

    /**
     * Replaces the current presentation and animation options.
     *
     * This affects layout-oriented choices such as tick visibility, labels, and animation behavior.
     */
    fun setPresentationOptions(options: GaugeChartPresentationOptions) {
        presentationOptions = options
        val targetProgress = resolvedValue?.progress ?: 0f
        if (!options.animateOnValueChange) {
            animator?.cancel()
            renderedProgress = targetProgress
        }
        invalidate()
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = (780f * density).toInt()
        val desiredHeight = (460f * density).toInt()
        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(styleOptions.backgroundColor)
        applyStyle()

        if (width <= 0 || height <= 0) return

        val resolved = resolvedValue
        if (resolved == null) {
            drawEmptyState(canvas)
            return
        }

        updateGeometry()
        if (arcRadius <= 0f) return

        drawTrack(canvas)
        drawRanges(canvas)
        drawProgress(canvas)
        drawTicks(canvas, resolved)
        drawIndicator(canvas)
        drawCenterTexts(canvas, resolved)
        drawMinMaxLabels(canvas, resolved)
    }

    private fun resolveRanges() {
        val resolved = resolvedValue
        resolvedRanges = if (resolved == null) {
            emptyList()
        } else {
            GaugeChartMath.resolveRanges(ranges, resolved.minValue, resolved.maxValue)
        }
    }

    private fun animateProgressTo(targetProgress: Float) {
        animator?.cancel()
        val startProgress = renderedProgress
        animator = ValueAnimator.ofFloat(startProgress, targetProgress.coerceIn(0f, 1f)).apply {
            duration = presentationOptions.animationDurationMs.coerceAtLeast(0L)
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                renderedProgress = (it.animatedValue as Float).coerceIn(0f, 1f)
                invalidate()
            }
            start()
        }
    }

    private fun updateGeometry() {
        val padding = dp(styleOptions.contentPaddingDp)
        val thickness = dp(styleOptions.progressThicknessDp)
        val valueTextBlock = sp(styleOptions.valueTextSizeSp) + sp(styleOptions.labelTextSizeSp) + dp(20f)
        val minMaxReserve = if (presentationOptions.showMinMaxLabels) {
            sp(styleOptions.minMaxTextSizeSp) + dp(14f)
        } else {
            dp(8f)
        }

        val availableWidth = width - paddingLeft - paddingRight - (padding * 2f)
        val availableHeight = height - paddingTop - paddingBottom - (padding * 2f)
        val centerCandidateY = height - paddingBottom - padding - minMaxReserve
        val maxRadiusByWidth = (availableWidth * 0.5f) - (thickness * 0.5f)
        val maxRadiusByHeight = centerCandidateY - paddingTop - padding - (thickness * 0.5f)
        val targetRadius = min(maxRadiusByWidth, maxRadiusByHeight)
        val minCenterGap = valueTextBlock.coerceAtLeast(0f)

        arcRadius = max(targetRadius, 0f)
        centerX = width * 0.5f
        centerY = max(
            paddingTop + padding + thickness,
            height - paddingBottom - padding - minMaxReserve - minCenterGap,
        )

        val maxAllowedRadius = min(
            (availableWidth * 0.5f) - (thickness * 0.5f),
            centerY - paddingTop - padding - (thickness * 0.5f),
        ).coerceAtLeast(0f)
        arcRadius = min(arcRadius, maxAllowedRadius)

        arcRect.set(
            centerX - arcRadius,
            centerY - arcRadius,
            centerX + arcRadius,
            centerY + arcRadius,
        )
    }

    private fun drawTrack(canvas: Canvas) {
        arcPaint.color = styleOptions.trackColor
        canvas.drawArc(
            arcRect,
            presentationOptions.startAngleDeg,
            presentationOptions.sweepAngleDeg,
            false,
            arcPaint,
        )
    }

    private fun drawRanges(canvas: Canvas) {
        resolvedRanges.forEach { range ->
            arcPaint.color = range.color
            val start = GaugeChartMath.valueToAngle(range.startRatio, presentationOptions.startAngleDeg, presentationOptions.sweepAngleDeg)
            val sweep = presentationOptions.sweepAngleDeg * (range.endRatio - range.startRatio)
            if (sweep > 0f) {
                canvas.drawArc(arcRect, start, sweep, false, arcPaint)
            }
        }
    }

    private fun drawProgress(canvas: Canvas) {
        if (renderedProgress <= 0f) return
        arcPaint.color = styleOptions.progressColor
        canvas.drawArc(
            arcRect,
            presentationOptions.startAngleDeg,
            presentationOptions.sweepAngleDeg * renderedProgress.coerceIn(0f, 1f),
            false,
            arcPaint,
        )
    }

    private fun drawTicks(canvas: Canvas, resolved: GaugeChartMath.ResolvedGaugeValue) {
        if (!presentationOptions.showTicks) return

        val tickCount = presentationOptions.tickCount.coerceAtLeast(1)
        val outerRadius = arcRadius + (dp(styleOptions.progressThicknessDp) * 0.15f)
        val innerRadius = outerRadius - dp(styleOptions.tickLengthDp)

        for (index in 0..tickCount) {
            val fraction = index.toFloat() / tickCount.toFloat()
            val angle = GaugeChartMath.valueToAngle(fraction, presentationOptions.startAngleDeg, presentationOptions.sweepAngleDeg)
            val start = GaugeChartMath.pointOnCircle(centerX, centerY, outerRadius, angle)
            val end = GaugeChartMath.pointOnCircle(centerX, centerY, innerRadius, angle)
            canvas.drawLine(start.x, start.y, end.x, end.y, tickPaint)
        }
    }

    private fun drawIndicator(canvas: Canvas) {
        val angle = GaugeChartMath.valueToAngle(renderedProgress, presentationOptions.startAngleDeg, presentationOptions.sweepAngleDeg)
        val end = GaugeChartMath.pointOnCircle(
            centerX,
            centerY,
            (arcRadius - dp(styleOptions.progressThicknessDp) * 0.75f).coerceAtLeast(0f),
            angle,
        )
        canvas.drawLine(centerX, centerY, end.x, end.y, indicatorPaint)
        canvas.drawCircle(centerX, centerY, dp(styleOptions.indicatorCenterRadiusDp), hubPaint)
    }

    private fun drawCenterTexts(canvas: Canvas, resolved: GaugeChartMath.ResolvedGaugeValue) {
        val valueY = centerY - (arcRadius * 0.18f)
        if (presentationOptions.showValueText) {
            val valueText = formatGaugeNumber(resolved.clampedValue)
            canvas.drawText(valueText, centerX, valueY, valueTextPaint)
        }

        if (presentationOptions.showCenterLabel) {
            val label = resolved.label?.takeIf { it.isNotBlank() } ?: return
            val labelY = valueY + sp(styleOptions.labelTextSizeSp) + dp(10f)
            canvas.drawText(label, centerX, labelY, labelTextPaint)
        }
    }

    private fun drawMinMaxLabels(canvas: Canvas, resolved: GaugeChartMath.ResolvedGaugeValue) {
        if (!presentationOptions.showMinMaxLabels) return

        val radius = arcRadius + dp(styleOptions.progressThicknessDp) * 0.7f
        val startPoint = GaugeChartMath.pointOnCircle(centerX, centerY, radius, presentationOptions.startAngleDeg)
        val endPoint = GaugeChartMath.pointOnCircle(
            centerX,
            centerY,
            radius,
            presentationOptions.startAngleDeg + presentationOptions.sweepAngleDeg,
        )
        val baselineOffset = sp(styleOptions.minMaxTextSizeSp) + dp(4f)

        canvas.drawText(
            formatGaugeNumber(resolved.minValue),
            startPoint.x,
            startPoint.y + baselineOffset,
            minMaxTextPaint,
        )
        canvas.drawText(
            formatGaugeNumber(resolved.maxValue),
            endPoint.x,
            endPoint.y + baselineOffset,
            minMaxTextPaint,
        )
    }

    private fun drawEmptyState(canvas: Canvas) {
        val text = presentationOptions.emptyText?.trim().orEmpty()
        if (text.isEmpty()) return
        canvas.drawText(
            text,
            width * 0.5f,
            height * 0.5f,
            emptyTextPaint,
        )
    }

    private fun applyStyle() {
        arcPaint.strokeWidth = dp(styleOptions.progressThicknessDp)
        tickPaint.color = styleOptions.tickColor
        tickPaint.strokeWidth = dp(styleOptions.tickThicknessDp)
        indicatorPaint.color = styleOptions.indicatorColor
        indicatorPaint.strokeWidth = dp(styleOptions.indicatorThicknessDp)
        hubPaint.color = styleOptions.indicatorCenterColor

        valueTextPaint.color = styleOptions.valueTextColor
        valueTextPaint.textSize = sp(styleOptions.valueTextSizeSp)
        labelTextPaint.color = styleOptions.labelTextColor
        labelTextPaint.textSize = sp(styleOptions.labelTextSizeSp)
        minMaxTextPaint.color = styleOptions.minMaxTextColor
        minMaxTextPaint.textSize = sp(styleOptions.minMaxTextSizeSp)
        emptyTextPaint.color = styleOptions.labelTextColor
        emptyTextPaint.textSize = sp(13f)
    }

    private fun dp(value: Float): Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)

    private fun sp(value: Float): Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)

    private fun formatGaugeNumber(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.roundToInt().toString()
        } else {
            String.format("%.1f", value)
        }
    }
}
