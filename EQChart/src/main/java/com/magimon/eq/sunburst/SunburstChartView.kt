package com.magimon.eq.sunburst

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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * Android View-based Sunburst chart.
 *
 * The view renders hierarchical [SunburstNode] trees as concentric rings whose geometry is
 * resolved by [SunburstChartLayoutEngine]. Shared style and presentation options keep the View
 * and Compose implementations aligned.
 *
 * @see SunburstNode
 * @see SunburstChartStyleOptions
 * @see SunburstChartPresentationOptions
 * @see SunburstChartLayoutEngine
 */
class SunburstChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density

    private var styleOptions = SunburstChartStyleOptions()
    private var presentationOptions = SunburstChartPresentationOptions()
    private var nodes: List<SunburstNode> = emptyList()
    private var layout = SunburstChartLayoutResult(
        segments = emptyList(),
        depthCount = 0,
        totalValue = 0.0,
        isRenderable = false,
        emptyReason = "no data",
    )

    private var selectedPath: List<Int>? = null
    private var renderProgress = 1f
    private var animator: ValueAnimator? = null
    private var onNodeClickListener: ((segmentIndex: Int, node: SunburstNode, payload: Any?) -> Unit)? = null

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }
    private val centerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val centerSubTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }
    private val emptyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }

    private val segmentPath = Path()
    private val outerRect = RectF()
    private val innerRect = RectF()

    init {
        applyStyle()
        recomputeLayout()
    }

    /**
     * Replaces the root nodes rendered by the chart.
     *
     * @param items Root [SunburstNode] items that define the hierarchy.
     * @see SunburstNode
     */
    fun setNodes(items: List<SunburstNode>) {
        nodes = items
        selectedPath = null
        recomputeLayout()
        if (presentationOptions.animateOnDataChange && layout.isRenderable) {
            playEnterAnimation()
        } else {
            renderProgress = 1f
            invalidate()
        }
    }

    /**
     * Applies shared visual styling for the chart.
     *
     * @param options Updated styling values.
     * @see SunburstChartStyleOptions
     */
    fun setStyleOptions(options: SunburstChartStyleOptions) {
        styleOptions = options
        applyStyle()
        recomputeLayout()
        invalidate()
    }

    /**
     * Applies shared presentation and animation options for the chart.
     *
     * @param options Updated presentation values.
     * @see SunburstChartPresentationOptions
     */
    fun setPresentationOptions(options: SunburstChartPresentationOptions) {
        presentationOptions = options
        selectedPath = null
        recomputeLayout()
        if (presentationOptions.animateOnDataChange && layout.isRenderable) {
            playEnterAnimation()
        } else {
            renderProgress = 1f
            invalidate()
        }
    }

    /**
     * Registers a callback for segment taps.
     *
     * @param listener Listener invoked with the segment index, resolved node, and node payload.
     * @see SunburstNode
     */
    fun setOnNodeClickListener(listener: (segmentIndex: Int, node: SunburstNode, payload: Any?) -> Unit) {
        onNodeClickListener = listener
    }

    /**
     * Starts the enter animation using the current [SunburstChartPresentationOptions] timing.
     *
     * @see SunburstChartPresentationOptions
     */
    fun playEnterAnimation() {
        if (!layout.isRenderable) return
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

        val centerX = width * 0.5f
        val centerY = height * 0.5f
        val outerRadius = (
            min(
                width - paddingLeft - paddingRight,
                height - paddingTop - paddingBottom,
            ) * 0.5f - dp(styleOptions.contentPaddingDp)
            ).coerceAtLeast(0f)

        if (!layout.isRenderable || outerRadius <= 0f) {
            drawEmptyState(canvas, centerX, centerY)
            return
        }

        val strokeWidth = dp(styleOptions.segmentStrokeWidthDp).coerceAtLeast(1f)
        layout.segments.forEach { segment ->
            val innerRadius = outerRadius * segment.innerRadiusRatio
            val outerRingRadius = outerRadius * segment.outerRadiusRatio
            val sweep = segment.sweepAngleDeg * renderProgress
            if (abs(sweep) < 0.01f) return@forEach

            buildSegmentPath(
                centerX = centerX,
                centerY = centerY,
                innerRadius = innerRadius,
                outerRadius = outerRingRadius,
                startAngleDeg = segment.startAngleDeg,
                sweepAngleDeg = sweep,
            )

            fillPaint.color = segment.color
            canvas.drawPath(segmentPath, fillPaint)

            strokePaint.strokeWidth = strokeWidth
            strokePaint.color = if (selectedPath == segment.path) {
                styleOptions.selectedSegmentStrokeColor
            } else {
                styleOptions.segmentStrokeColor
            }
            canvas.drawPath(segmentPath, strokePaint)

            drawSegmentLabel(
                canvas = canvas,
                centerX = centerX,
                centerY = centerY,
                outerRadius = outerRadius,
                segment = segment,
                sweep = sweep,
            )
        }

        drawCenterText(canvas, centerX, centerY, outerRadius)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!layout.isRenderable) return super.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_UP -> {
                val centerX = width * 0.5f
                val centerY = height * 0.5f
                val outerRadius = (
                    min(
                        width - paddingLeft - paddingRight,
                        height - paddingTop - paddingBottom,
                    ) * 0.5f - dp(styleOptions.contentPaddingDp)
                    ).coerceAtLeast(0f)
                if (outerRadius <= 0f) return false

                val dx = event.x - centerX
                val dy = event.y - centerY
                val normalizedRadius = hypot(dx, dy) / outerRadius
                val angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                val hit = SunburstChartLayoutEngine.hitTestSegment(layout, normalizedRadius, angleDeg)
                selectedPath = hit?.path
                invalidate()
                if (hit != null) {
                    findNode(hit.path)?.let { node ->
                        val index = layout.segments.indexOfFirst { it.path == hit.path }
                        onNodeClickListener?.invoke(index, node, node.payload)
                    }
                }
                performClick()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    private fun recomputeLayout() {
        layout = SunburstChartLayoutEngine.compute(
            nodes = nodes,
            styleOptions = styleOptions,
            presentationOptions = presentationOptions,
        )
    }

    private fun applyStyle() {
        labelPaint.color = styleOptions.labelTextColor
        labelPaint.textSize = sp(styleOptions.labelTextSizeSp)
        centerTextPaint.color = styleOptions.centerTextColor
        centerTextPaint.textSize = sp(styleOptions.centerTextSizeSp)
        centerSubTextPaint.color = styleOptions.centerSubTextColor
        centerSubTextPaint.textSize = sp(styleOptions.centerSubTextSizeSp)
        emptyTextPaint.color = styleOptions.centerSubTextColor
        emptyTextPaint.textSize = sp(13f)
    }

    private fun drawSegmentLabel(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        outerRadius: Float,
        segment: SunburstSegmentLayout,
        sweep: Float,
    ) {
        if (!presentationOptions.showLabels || abs(sweep) < presentationOptions.minLabelSweepDeg) return

        val midAngle = segment.startAngleDeg + (sweep * 0.5f)
        val innerRadius = outerRadius * segment.innerRadiusRatio
        val outerRingRadius = outerRadius * segment.outerRadiusRatio
        val radius = (innerRadius + outerRingRadius) * 0.5f
        val offset = degreeToPoint(midAngle, radius)
        val baseline = centerY + offset.second - ((labelPaint.descent() + labelPaint.ascent()) * 0.5f)
        canvas.drawText(segment.label, centerX + offset.first, baseline, labelPaint)
    }

    private fun drawCenterText(canvas: Canvas, centerX: Float, centerY: Float, outerRadius: Float) {
        val innerHoleRadius = outerRadius * presentationOptions.innerHoleRatio
        if (innerHoleRadius <= 0f) return

        val centerText = presentationOptions.centerText?.trim().orEmpty()
        val centerSubText = presentationOptions.centerSubText?.trim().orEmpty()
        if (centerText.isEmpty() && centerSubText.isEmpty()) return

        if (centerText.isNotEmpty() && centerSubText.isNotEmpty()) {
            canvas.drawText(centerText, centerX, centerY - dp(6f), centerTextPaint)
            canvas.drawText(centerSubText, centerX, centerY + dp(12f), centerSubTextPaint)
        } else if (centerText.isNotEmpty()) {
            canvas.drawText(centerText, centerX, centerY - ((centerTextPaint.descent() + centerTextPaint.ascent()) * 0.5f), centerTextPaint)
        } else {
            canvas.drawText(centerSubText, centerX, centerY - ((centerSubTextPaint.descent() + centerSubTextPaint.ascent()) * 0.5f), centerSubTextPaint)
        }
    }

    private fun drawEmptyState(canvas: Canvas, centerX: Float, centerY: Float) {
        val message = presentationOptions.emptyText?.trim().orEmpty()
        if (message.isNotEmpty()) {
            canvas.drawText(
                message,
                centerX,
                centerY - ((emptyTextPaint.descent() + emptyTextPaint.ascent()) * 0.5f),
                emptyTextPaint,
            )
        }
    }

    private fun buildSegmentPath(
        centerX: Float,
        centerY: Float,
        innerRadius: Float,
        outerRadius: Float,
        startAngleDeg: Float,
        sweepAngleDeg: Float,
    ) {
        outerRect.set(centerX - outerRadius, centerY - outerRadius, centerX + outerRadius, centerY + outerRadius)
        innerRect.set(centerX - innerRadius, centerY - innerRadius, centerX + innerRadius, centerY + innerRadius)

        val outerStart = degreeToPoint(startAngleDeg, outerRadius)
        segmentPath.reset()
        segmentPath.moveTo(centerX + outerStart.first, centerY + outerStart.second)
        segmentPath.arcTo(outerRect, startAngleDeg, sweepAngleDeg, false)

        if (innerRadius > 0f) {
            val innerEnd = degreeToPoint(startAngleDeg + sweepAngleDeg, innerRadius)
            segmentPath.lineTo(centerX + innerEnd.first, centerY + innerEnd.second)
            segmentPath.arcTo(innerRect, startAngleDeg + sweepAngleDeg, -sweepAngleDeg, false)
        } else {
            segmentPath.lineTo(centerX, centerY)
        }
        segmentPath.close()
    }

    private fun findNode(path: List<Int>): SunburstNode? {
        var current: SunburstNode? = null
        var levelNodes = nodes
        path.forEach { index ->
            current = levelNodes.getOrNull(index) ?: return null
            levelNodes = current?.children ?: emptyList()
        }
        return current
    }

    private fun degreeToPoint(angleDeg: Float, radius: Float): Pair<Float, Float> {
        val radians = angleDeg * (PI.toFloat() / 180f)
        return (cos(radians) * radius) to (sin(radians) * radius)
    }

    private fun dp(value: Float): Float = value * density

    private fun sp(value: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            value,
            resources.displayMetrics,
        )
    }
}
