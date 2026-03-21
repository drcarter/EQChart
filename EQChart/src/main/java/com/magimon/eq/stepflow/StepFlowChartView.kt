package com.magimon.eq.stepflow

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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * View-based step flow infographic chart with step and hub tap callbacks.
 *
 * The view consumes shared step-flow contracts from `EQChart-common`, delegates deterministic
 * geometry to [StepFlowChartLayoutEngine], and renders the resulting hub, spine, badges, and
 * pill cards with a thin Android View wrapper.
 */
class StepFlowChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity

    private var hubContent: StepFlowHubContent? = null
    private var steps: List<StepFlowStep> = emptyList()
    private var styleOptions = StepFlowChartStyleOptions()
    private var presentationOptions = StepFlowChartPresentationOptions()
    private var layoutResult = StepFlowChartLayoutResult(
        hubLayout = null,
        hubRingSegments = emptyList(),
        spineSegments = emptyList(),
        tailDots = emptyList(),
        stepLayouts = emptyList(),
        isRenderable = false,
        emptyReason = "empty",
    )

    private var selectedStepIndex: Int? = null
    private var hubSelected: Boolean = false
    private var renderProgress: Float = 1f
    private var animator: ValueAnimator? = null

    private var onStepClickListener: ((Int, StepFlowStep, Any?) -> Unit)? = null
    private var onHubClickListener: ((StepFlowHubContent, Any?) -> Unit)? = null

    private val hubFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val hubStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val hubRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val hubEyebrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }
    private val hubTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }
    private val hubBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }
    private val spinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val tailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val connectorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val badgeFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val badgeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }
    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val cardStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val cardTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val cardBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val iconCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val iconTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }
    private val emptyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }

    private val spinePath = Path()
    private val cardRect = RectF()
    private val ringRect = RectF()

    init {
        applyStyle()
    }

    fun setHubContent(content: StepFlowHubContent?) {
        hubContent = content
        hubSelected = false
        resolveLayout(width, height)
        playEnterAnimationIfNeeded()
        invalidate()
    }

    fun setSteps(items: List<StepFlowStep>) {
        steps = items
        selectedStepIndex = null
        hubSelected = false
        resolveLayout(width, height)
        playEnterAnimationIfNeeded()
        invalidate()
    }

    fun setStyleOptions(options: StepFlowChartStyleOptions) {
        styleOptions = options
        applyStyle()
        resolveLayout(width, height)
        invalidate()
    }

    fun setPresentationOptions(options: StepFlowChartPresentationOptions) {
        presentationOptions = options
        resolveLayout(width, height)
        playEnterAnimationIfNeeded()
        invalidate()
    }

    fun setOnStepClickListener(listener: (stepIndex: Int, step: StepFlowStep, payload: Any?) -> Unit) {
        onStepClickListener = listener
    }

    fun setOnHubClickListener(listener: (content: StepFlowHubContent, payload: Any?) -> Unit) {
        onHubClickListener = listener
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = (900f * density).roundToInt()
        val desiredHeight = (540f * density).roundToInt()
        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec),
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        resolveLayout(w, h)
        playEnterAnimationIfNeeded()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(styleOptions.backgroundColor)

        if (width <= 0 || height <= 0) return
        if (!layoutResult.isRenderable) {
            drawEmptyState(canvas)
            return
        }

        drawHub(canvas)
        drawSpine(canvas)
        layoutResult.stepLayouts.forEach { drawStep(canvas, it) }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!layoutResult.isRenderable) return false
        if (event.actionMasked == MotionEvent.ACTION_DOWN) return true
        if (event.actionMasked != MotionEvent.ACTION_UP) return super.onTouchEvent(event)

        val stepHit = StepFlowChartLayoutEngine.hitTestStep(
            layout = layoutResult,
            x = event.x,
            y = event.y,
            tolerancePx = max(dp(8f), styleOptions.connectorWidthDp * density),
        )
        if (stepHit != null) {
            selectedStepIndex = stepHit
            hubSelected = false
            steps.getOrNull(stepHit)?.let { step ->
                onStepClickListener?.invoke(stepHit, step, step.payload)
            }
            invalidate()
            performClick()
            return true
        }

        if (StepFlowChartLayoutEngine.hitTestHub(layoutResult, event.x, event.y)) {
            hubSelected = true
            selectedStepIndex = null
            hubContent?.let { content ->
                onHubClickListener?.invoke(content, content.payload)
            }
            invalidate()
            performClick()
            return true
        }

        if (selectedStepIndex != null || hubSelected) {
            selectedStepIndex = null
            hubSelected = false
            invalidate()
        }
        return true
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    private fun resolveLayout(w: Int, h: Int) {
        layoutResult = if (w > 0 && h > 0) {
            val config = StepFlowChartLayoutConfig(
                widthPx = w.toFloat(),
                heightPx = h.toFloat(),
                contentPaddingPx = dp(styleOptions.contentPaddingDp),
                hubRadiusPx = dp(styleOptions.hubRadiusDp),
                hubRingThicknessPx = dp(styleOptions.hubRingThicknessDp),
                spineWidthPx = dp(styleOptions.spineWidthDp),
                spineDotRadiusPx = dp(styleOptions.spineDotRadiusDp),
                tailDotRadiusPx = dp(styleOptions.tailDotRadiusDp),
                badgeRadiusPx = dp(styleOptions.badgeRadiusDp),
                cardWidthPx = dp(styleOptions.cardWidthDp),
                cardHeightPx = dp(styleOptions.cardHeightDp),
                cardCornerRadiusPx = dp(styleOptions.cardCornerRadiusDp),
                connectorWidthPx = dp(styleOptions.connectorWidthDp),
                iconCircleRadiusPx = dp(styleOptions.iconCircleRadiusDp),
                cardGapPx = dp(styleOptions.badgeRadiusDp * 0.72f),
                badgeOverlapPx = dp(styleOptions.badgeRadiusDp * 0.42f),
                topBottomInsetPx = dp(max(styleOptions.badgeRadiusDp * 1.8f, styleOptions.cardHeightDp * 0.65f)),
            ).scaleToFit(steps.size)

            StepFlowChartLayoutEngine.compute(
                hubContent = hubContent,
                steps = steps,
                config = config,
                styleOptions = styleOptions,
                presentationOptions = presentationOptions,
            )
        } else {
            layoutResult.copy(isRenderable = false, emptyReason = "empty")
        }
    }

    private fun playEnterAnimationIfNeeded() {
        animator?.cancel()
        if (!layoutResult.isRenderable || !presentationOptions.animateOnDataChange) {
            renderProgress = 1f
            return
        }
        renderProgress = 0f
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = presentationOptions.animationDurationMs.coerceAtLeast(0L)
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                renderProgress = (it.animatedValue as Float)
                invalidate()
            }
            start()
        }
    }

    private fun drawHub(canvas: Canvas) {
        val hub = layoutResult.hubLayout ?: return
        val alpha = selectionAlpha(stepIndex = null)

        hubFillPaint.color = applyAlpha(styleOptions.hubFillColor, alpha)
        hubStrokePaint.color = applyAlpha(
            if (hubSelected) styleOptions.selectedStrokeColor else styleOptions.hubStrokeColor,
            alpha,
        )
        hubStrokePaint.strokeWidth = if (hubSelected) {
            dp(styleOptions.selectedStrokeWidthDp)
        } else {
            dp(styleOptions.hubStrokeWidthDp)
        }

        canvas.drawCircle(hub.centerX, hub.centerY, hub.radius, hubFillPaint)
        canvas.drawCircle(hub.centerX, hub.centerY, hub.radius, hubStrokePaint)

        ringRect.set(
            hub.left - (hub.ringThickness * 0.5f),
            hub.top - (hub.ringThickness * 0.5f),
            hub.right + (hub.ringThickness * 0.5f),
            hub.bottom + (hub.ringThickness * 0.5f),
        )
        hubRingPaint.strokeWidth = hub.ringThickness
        layoutResult.hubRingSegments.forEach { segment ->
            hubRingPaint.color = applyAlpha(segment.color, alpha * renderProgress)
            canvas.drawArc(ringRect, segment.startAngleDeg, segment.sweepAngleDeg, false, hubRingPaint)
        }

        hubContent?.eyebrow?.takeIf { it.isNotBlank() }?.let { eyebrow ->
            hubEyebrowPaint.color = applyAlpha(styleOptions.hubEyebrowTextColor, alpha)
            canvas.drawText(eyebrow, hub.eyebrowCenterX, hub.eyebrowBaselineY, hubEyebrowPaint)
        }
        canvas.drawText(
            hub.content.title,
            hub.titleCenterX,
            hub.titleBaselineY,
            hubTitlePaint.apply { color = applyAlpha(styleOptions.hubTitleTextColor, alpha) },
        )
        if (presentationOptions.showHubDescription) {
            hub.content.description?.takeIf { it.isNotBlank() }?.let { description ->
                drawCenteredTextBlock(
                    canvas = canvas,
                    text = description,
                    centerX = hub.descriptionCenterX,
                    baselineY = hub.descriptionBaselineY,
                    maxWidth = hub.radius * 1.55f,
                    paint = hubBodyPaint.apply { color = applyAlpha(styleOptions.hubBodyTextColor, alpha) },
                    maxLines = 3,
                )
            }
        }
    }

    private fun drawSpine(canvas: Canvas) {
        val alpha = selectionAlpha(stepIndex = null)
        spinePaint.color = applyAlpha(styleOptions.spineColor, alpha * renderProgress)
        spinePaint.strokeWidth = dp(styleOptions.spineWidthDp)
        connectorPaint.color = applyAlpha(styleOptions.connectorColor, alpha * renderProgress)
        connectorPaint.strokeWidth = dp(styleOptions.connectorWidthDp)
        tailPaint.color = applyAlpha(styleOptions.spineColor, alpha * renderProgress)
        dotPaint.color = applyAlpha(styleOptions.spineColor, alpha * renderProgress)

        spinePath.reset()
        layoutResult.spineSegments.forEachIndexed { index, segment ->
            if (index == 0) {
                spinePath.moveTo(segment.startX, segment.startY)
            }
            spinePath.quadTo(segment.controlX, segment.controlY, segment.endX, segment.endY)
        }
        canvas.drawPath(spinePath, spinePaint)

        layoutResult.tailDots.forEach { tail ->
            canvas.drawCircle(tail.centerX, tail.centerY, tail.radius, tailPaint)
        }
    }

    private fun drawStep(canvas: Canvas, stepLayout: StepFlowStepLayout) {
        val selected = selectedStepIndex == stepLayout.originalIndex
        val alpha = selectionAlpha(stepIndex = stepLayout.originalIndex)
        val strokeColor = if (selected) styleOptions.selectedStrokeColor else styleOptions.badgeStrokeColor
        val cardStrokeColor = if (selected) styleOptions.selectedStrokeColor else stepLayout.step.accentColor

        dotPaint.color = applyAlpha(stepLayout.step.accentColor, alpha * renderProgress)
        canvas.drawCircle(
            stepLayout.spineDotCenterX,
            stepLayout.spineDotCenterY,
            stepLayout.spineDotRadius,
            dotPaint,
        )

        connectorPaint.color = applyAlpha(styleOptions.connectorColor, alpha * renderProgress)
        canvas.drawLine(
            stepLayout.connectorStartX,
            stepLayout.connectorStartY,
            stepLayout.connectorEndX,
            stepLayout.connectorEndY,
            connectorPaint,
        )

        cardRect.set(stepLayout.cardLeft, stepLayout.cardTop, stepLayout.cardRight, stepLayout.cardBottom)
        cardPaint.color = applyAlpha(stepLayout.step.accentColor, alpha * renderProgress)
        cardStrokePaint.color = applyAlpha(cardStrokeColor, alpha)
        cardStrokePaint.strokeWidth = if (selected) dp(styleOptions.selectedStrokeWidthDp) else dp(1f)
        canvas.drawRoundRect(cardRect, stepLayout.cardCornerRadius, stepLayout.cardCornerRadius, cardPaint)
        canvas.drawRoundRect(cardRect, stepLayout.cardCornerRadius, stepLayout.cardCornerRadius, cardStrokePaint)

        badgeFillPaint.color = applyAlpha(styleOptions.badgeFillColor, alpha)
        badgeStrokePaint.color = applyAlpha(strokeColor, alpha)
        badgeStrokePaint.strokeWidth = if (selected) dp(styleOptions.selectedStrokeWidthDp) else dp(styleOptions.badgeStrokeWidthDp)
        canvas.drawCircle(stepLayout.badgeCenterX, stepLayout.badgeCenterY, stepLayout.badgeRadius, badgeFillPaint)
        canvas.drawCircle(stepLayout.badgeCenterX, stepLayout.badgeCenterY, stepLayout.badgeRadius, badgeStrokePaint)

        drawCenteredTextBlock(
            canvas = canvas,
            text = formatBadgeLabel(stepLayout.step.badgeLabel),
            centerX = stepLayout.badgeCenterX,
            baselineY = stepLayout.badgeCenterY - (badgeTextPaint.descent() + badgeTextPaint.ascent()) * 0.15f,
            maxWidth = stepLayout.badgeRadius * 1.5f,
            paint = badgeTextPaint.apply { color = applyAlpha(styleOptions.badgeTextColor, alpha) },
            maxLines = 2,
            centerLines = true,
        )

        cardTitlePaint.color = applyAlpha(styleOptions.cardTitleTextColor, alpha)
        canvas.drawText(
            stepLayout.step.title,
            stepLayout.titleX,
            stepLayout.titleBaselineY,
            cardTitlePaint,
        )
        if (presentationOptions.showStepDescriptions) {
            stepLayout.step.description?.takeIf { it.isNotBlank() }?.let { description ->
                drawLeftAlignedTextBlock(
                    canvas = canvas,
                    text = description,
                    startX = stepLayout.bodyX,
                    baselineY = stepLayout.bodyBaselineY,
                    maxWidth = max(1f, stepLayout.textRight - stepLayout.bodyX),
                    paint = cardBodyPaint.apply { color = applyAlpha(styleOptions.cardBodyTextColor, alpha) },
                    maxLines = 2,
                )
            }
        }

        iconCirclePaint.color = applyAlpha(styleOptions.iconCircleFillColor, alpha)
        canvas.drawCircle(stepLayout.iconCenterX, stepLayout.iconCenterY, stepLayout.iconRadius, iconCirclePaint)
        stepLayout.step.iconText?.takeIf { it.isNotBlank() }?.let { iconText ->
            iconTextPaint.color = applyAlpha(styleOptions.iconTextColor, alpha)
            val baseline = stepLayout.iconCenterY - ((iconTextPaint.descent() + iconTextPaint.ascent()) * 0.5f)
            canvas.drawText(iconText.take(2), stepLayout.iconCenterX, baseline, iconTextPaint)
        }
    }

    private fun drawEmptyState(canvas: Canvas) {
        val text = presentationOptions.emptyText?.trim().orEmpty()
        if (text.isEmpty()) return
        canvas.drawText(
            text,
            width * 0.5f,
            (height * 0.5f) - ((emptyTextPaint.descent() + emptyTextPaint.ascent()) * 0.5f),
            emptyTextPaint,
        )
    }

    private fun drawCenteredTextBlock(
        canvas: Canvas,
        text: String,
        centerX: Float,
        baselineY: Float,
        maxWidth: Float,
        paint: Paint,
        maxLines: Int,
        centerLines: Boolean = true,
    ) {
        val lines = wrapText(text, maxWidth, paint, maxLines)
        val lineHeight = (paint.textSize * 1.15f)
        val firstBaseline = baselineY - ((lines.lastIndex) * lineHeight * 0.5f)
        lines.forEachIndexed { index, line ->
            paint.textAlign = if (centerLines) Paint.Align.CENTER else Paint.Align.LEFT
            canvas.drawText(line, centerX, firstBaseline + (index * lineHeight), paint)
        }
    }

    private fun drawLeftAlignedTextBlock(
        canvas: Canvas,
        text: String,
        startX: Float,
        baselineY: Float,
        maxWidth: Float,
        paint: Paint,
        maxLines: Int,
    ) {
        val lines = wrapText(text, maxWidth, paint, maxLines)
        val lineHeight = paint.textSize * 1.1f
        lines.forEachIndexed { index, line ->
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(line, startX, baselineY + (index * lineHeight), paint)
        }
    }

    private fun wrapText(text: String, maxWidth: Float, paint: Paint, maxLines: Int): List<String> {
        if (text.isBlank()) return emptyList()
        val forcedLines = text.split('\n').flatMap { line ->
            val words = line.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            if (words.isEmpty()) return@flatMap listOf("")

            val lines = mutableListOf<String>()
            var current = words.first()
            for (word in words.drop(1)) {
                val candidate = "$current $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    current = candidate
                } else {
                    lines += current
                    current = word
                }
            }
            lines += current
            lines
        }
        if (forcedLines.size <= maxLines) return forcedLines
        val trimmed = forcedLines.take(maxLines).toMutableList()
        val lastIndex = trimmed.lastIndex
        var last = trimmed[lastIndex]
        while (last.isNotEmpty() && paint.measureText("$last...") > maxWidth) {
            last = last.dropLast(1)
        }
        trimmed[lastIndex] = if (last.isEmpty()) "..." else "$last..."
        return trimmed
    }

    private fun formatBadgeLabel(value: String): String {
        val trimmed = value.trim()
        val firstSpace = trimmed.indexOf(' ')
        return if (firstSpace > 0) {
            trimmed.substring(0, firstSpace) + "\n" + trimmed.substring(firstSpace + 1)
        } else {
            trimmed
        }
    }

    private fun applyStyle() {
        hubEyebrowPaint.textSize = sp(styleOptions.hubEyebrowTextSizeSp)
        hubTitlePaint.textSize = sp(styleOptions.hubTitleTextSizeSp)
        hubBodyPaint.textSize = sp(styleOptions.hubBodyTextSizeSp)
        badgeTextPaint.textSize = sp(styleOptions.badgeTextSizeSp)
        cardTitlePaint.textSize = sp(styleOptions.cardTitleTextSizeSp)
        cardBodyPaint.textSize = sp(styleOptions.cardBodyTextSizeSp)
        iconTextPaint.textSize = sp(styleOptions.iconTextSizeSp)
        emptyTextPaint.textSize = sp(13f)
        emptyTextPaint.color = styleOptions.hubFillColor
    }

    private fun selectionAlpha(stepIndex: Int?): Float {
        val baseProgress = renderProgress.coerceIn(0f, 1f)
        return when {
            hubSelected && stepIndex != null -> 0.42f * baseProgress
            selectedStepIndex == null && !hubSelected -> 1f * baseProgress
            stepIndex == selectedStepIndex -> 1f * baseProgress
            stepIndex == null && hubSelected -> 1f * baseProgress
            else -> 0.34f * baseProgress
        }
    }

    private fun applyAlpha(color: Int, alpha: Float): Int {
        val clamped = (alpha.coerceIn(0f, 1f) * 255f).roundToInt()
        return Color.argb(clamped, Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun dp(value: Float): Float = value * density

    private fun sp(value: Float): Float = value * scaledDensity
}
