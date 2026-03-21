package com.magimon.eq.funnel

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator

/**
 * Vertical funnel chart renderer.
 *
 * Supports tapered stages, centered labels, and click callbacks.
 */
class FunnelChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private data class RenderStage(
        val path: Path,
        val topLeft: PointF,
        val topRight: PointF,
        val bottomRight: PointF,
        val bottomLeft: PointF,
        val centerX: Float,
        val centerY: Float,
        val stage: FunnelLayoutStage,
    )

    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity

    private var styleOptions = FunnelChartStyleOptions()
    private var presentationOptions = FunnelChartPresentationOptions()
    private var layout = resolveFunnelChartLayout(emptyList(), styleOptions)

    private val sourceStages = mutableListOf<FunnelStage>()
    private val renderStages = mutableListOf<RenderStage>()

    private var animator: ValueAnimator? = null
    private var renderProgress = 1f
    private var selectedStage: RenderStage? = null
    private var onStageClickListener: ((Int, FunnelStage, Double) -> Unit)? = null

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stageStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val selectedStagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
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

    fun setStages(items: List<FunnelStage>) {
        sourceStages.clear()
        sourceStages.addAll(items)
        selectedStage = null
        refreshAndRender()
    }

    fun <T> setStages(items: List<T>, mapper: (T) -> FunnelStage) {
        setStages(items.map(mapper))
    }

    fun setStyleOptions(options: FunnelChartStyleOptions) {
        styleOptions = options
        applyStyle()
        refreshAndRender()
    }

    fun setPresentationOptions(options: FunnelChartPresentationOptions) {
        presentationOptions = options
        applyStyle()
        refreshAndRender()
    }

    /**
     * Callback format: `(index, stage, value)`.
     */
    fun setOnStageClickListener(listener: (Int, FunnelStage, Double) -> Unit) {
        onStageClickListener = listener
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
                val hit = renderStages.lastOrNull { stage -> pointInQuad(event.x, event.y, stage) }
                selectedStage = hit
                if (hit != null) {
                    onStageClickListener?.invoke(
                        hit.stage.index,
                        FunnelStage(
                            label = hit.stage.label,
                            value = hit.stage.value,
                            color = hit.stage.color,
                            payload = hit.stage.payload,
                        ),
                        hit.stage.value,
                    )
                    performClick()
                }
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun Float.spToPx(): Float = this * scaledDensity
    private fun Float.dpToPx(): Float = this * density

    private fun applyStyle() {
        backgroundPaint.color = styleOptions.backgroundColor
        stageStrokePaint.color = styleOptions.stageBorderColor
        stageStrokePaint.strokeWidth = 2f * density
        selectedStagePaint.color = styleOptions.selectedStageBorderColor
        selectedStagePaint.strokeWidth = 2.5f * density
        labelPaint.color = styleOptions.labelTextColor
        labelPaint.textSize = presentationOptions.labelTextSizeSp.spToPx()
        valuePaint.color = styleOptions.valueTextColor
        valuePaint.textSize = presentationOptions.valueTextSizeSp.spToPx()
        emptyTextPaint.color = styleOptions.labelTextColor
        emptyTextPaint.textSize = 14f.spToPx()
    }

    private fun refreshAndRender() {
        layout = resolveFunnelChartLayout(sourceStages, styleOptions)
        if (presentationOptions.animateOnDataChange && layout.stages.isNotEmpty()) {
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

    private fun buildRenderCache() {
        renderStages.clear()
        if (layout.stages.isEmpty()) return

        val padding = styleOptions.contentPaddingDp.dpToPx()
        val stageGap = styleOptions.stageGapDp.dpToPx()
        val availableHeight = (height - padding * 2f - stageGap * (layout.stages.size - 1)).coerceAtLeast(1f)
        val stageHeight = availableHeight / layout.stages.size.toFloat()
        val centerX = width * 0.5f
        val maxWidth = (width - padding * 2f).coerceAtLeast(1f)
        val progress = if (presentationOptions.animationDirection) renderProgress else 1f

        layout.stages.forEachIndexed { index, stage ->
            val topY = padding + index * (stageHeight + stageGap)
            val bottomY = topY + stageHeight
            val topWidth = maxWidth * stage.topWidthRatio * progress
            val bottomWidth = maxWidth * stage.bottomWidthRatio * progress
            val topLeft = PointF(centerX - topWidth * 0.5f, topY)
            val topRight = PointF(centerX + topWidth * 0.5f, topY)
            val bottomRight = PointF(centerX + bottomWidth * 0.5f, bottomY)
            val bottomLeft = PointF(centerX - bottomWidth * 0.5f, bottomY)
            val path = Path().apply {
                moveTo(topLeft.x, topLeft.y)
                lineTo(topRight.x, topRight.y)
                lineTo(bottomRight.x, bottomRight.y)
                lineTo(bottomLeft.x, bottomLeft.y)
                close()
            }
            renderStages.add(
                RenderStage(
                    path = path,
                    topLeft = topLeft,
                    topRight = topRight,
                    bottomRight = bottomRight,
                    bottomLeft = bottomLeft,
                    centerX = centerX,
                    centerY = (topY + bottomY) * 0.5f,
                    stage = stage,
                ),
            )
        }
    }

    private fun pointInQuad(x: Float, y: Float, stage: RenderStage): Boolean {
        val points = arrayOf(stage.topLeft, stage.topRight, stage.bottomRight, stage.bottomLeft)
        var inside = false
        var j = points.lastIndex
        for (i in points.indices) {
            val yi = points[i].y
            val yj = points[j].y
            val xi = points[i].x
            val xj = points[j].x
            val intersects = ((yi > y) != (yj > y)) &&
                (x < (xj - xi) * (y - yi) / ((yj - yi).takeIf { it != 0f } ?: 1f) + xi)
            if (intersects) inside = !inside
            j = i
        }
        return inside
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
        buildRenderCache()
        if (renderStages.isEmpty()) {
            drawEmptyState(canvas)
            return
        }

        renderStages.forEach { stage ->
            stagePaint.color = stage.stage.color
            canvas.drawPath(stage.path, stagePaint)
            canvas.drawPath(stage.path, stageStrokePaint)
            if (selectedStage === stage) {
                canvas.drawPath(stage.path, selectedStagePaint)
            }

            if (presentationOptions.showLabels) {
                val labelY = if (presentationOptions.showValues) {
                    stage.centerY - 2f * density
                } else {
                    stage.centerY + labelPaint.textSize * 0.35f
                }
                canvas.drawText(
                    presentationOptions.stageLabelFormatter(
                        FunnelStage(
                            label = stage.stage.label,
                            value = stage.stage.value,
                            color = stage.stage.color,
                            payload = stage.stage.payload,
                        ),
                    ),
                    stage.centerX,
                    labelY,
                    labelPaint,
                )
            }
            if (presentationOptions.showValues) {
                val valueY = if (presentationOptions.showLabels) {
                    stage.centerY + valuePaint.textSize + 2f * density
                } else {
                    stage.centerY + valuePaint.textSize * 0.35f
                }
                canvas.drawText(
                    presentationOptions.valueLabelFormatter(stage.stage.value),
                    stage.centerX,
                    valueY,
                    valuePaint,
                )
            }
        }
    }
}
