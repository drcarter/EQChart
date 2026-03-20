package com.magimon.eq.cycle

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * View-based cycle diagram chart with node/link tap selection.
 *
 * The view consumes explicit node/link lists, delegates ring layout and hit testing to
 * [CycleChartLayoutEngine], and renders curved directional links plus circular nodes.
 *
 * Selection behavior:
 * - tapping a node highlights that node and its connected links
 * - tapping a link highlights that link and its endpoint nodes
 * - tapping empty space clears the current selection
 *
 * Invalid or empty input renders [CycleChartPresentationOptions.emptyText].
 */
class CycleChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity

    private var nodes: List<CycleNode> = emptyList()
    private var links: List<CycleLink> = emptyList()
    private var styleOptions = CycleChartStyleOptions()
    private var presentationOptions = CycleChartPresentationOptions()
    private var layoutResult = CycleChartLayoutResult(
        nodeLayouts = emptyList(),
        linkLayouts = emptyList(),
        centerX = 0f,
        centerY = 0f,
        orbitRadius = 0f,
        isRenderable = false,
        emptyReason = "empty",
    )

    private var selectedNodeIndex: Int? = null
    private var selectedLinkIndex: Int? = null
    private var renderProgress: Float = 1f
    private var animator: ValueAnimator? = null

    private var onNodeClickListener: ((Int, CycleNode, Any?) -> Unit)? = null
    private var onLinkClickListener: ((Int, CycleLink, Any?) -> Unit)? = null

    private val linkPath = Path()
    private val arrowPath = Path()

    private val linkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val nodeFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val nodeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val nodeLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val linkLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
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
     * Sets the full node list used by the chart.
     *
     * Nodes are arranged in input order around the ring. Changing the list clears current
     * selection and recomputes the layout.
     */
    fun setNodes(items: List<CycleNode>) {
        nodes = items
        clearSelection()
        resolveLayout(width, height)
        playEnterAnimationIfNeeded()
        invalidate()
    }

    /**
     * Sets the full link list used by the chart.
     *
     * Links are validated against the current node set. Changing the list clears current selection
     * and recomputes the layout.
     */
    fun setLinks(items: List<CycleLink>) {
        links = items
        clearSelection()
        resolveLayout(width, height)
        playEnterAnimationIfNeeded()
        invalidate()
    }

    /**
     * Replaces the visual styling used for nodes, links, labels, and selection state.
     */
    fun setStyleOptions(options: CycleChartStyleOptions) {
        styleOptions = options
        applyStyle()
        resolveLayout(width, height)
        invalidate()
    }

    /**
     * Replaces the current behavioral and layout presentation options.
     */
    fun setPresentationOptions(options: CycleChartPresentationOptions) {
        presentationOptions = options
        resolveLayout(width, height)
        playEnterAnimationIfNeeded()
        invalidate()
    }

    /**
     * Registers a callback invoked when a node is tapped.
     *
     * Callback arguments are ordered as `(nodeIndex, node, payload)`.
     */
    fun setOnNodeClickListener(listener: (nodeIndex: Int, node: CycleNode, payload: Any?) -> Unit) {
        onNodeClickListener = listener
    }

    /**
     * Registers a callback invoked when a link is tapped.
     *
     * Callback arguments are ordered as `(linkIndex, link, payload)`.
     */
    fun setOnLinkClickListener(listener: (linkIndex: Int, link: CycleLink, payload: Any?) -> Unit) {
        onLinkClickListener = listener
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = (820f * density).toInt()
        val desiredHeight = (620f * density).toInt()
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

        layoutResult.linkLayouts.forEach { link ->
            drawLink(canvas, link)
        }
        layoutResult.nodeLayouts.forEach { node ->
            drawNode(canvas, node)
        }
        if (presentationOptions.showNodeLabels) {
            layoutResult.nodeLayouts.forEach { node ->
                drawNodeLabel(canvas, node)
            }
        }
        if (presentationOptions.showLinkLabels) {
            layoutResult.linkLayouts.forEach { link ->
                drawLinkLabel(canvas, link)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!layoutResult.isRenderable) return false
        if (event.actionMasked == MotionEvent.ACTION_DOWN) return true
        if (event.actionMasked != MotionEvent.ACTION_UP) return super.onTouchEvent(event)

        val nodeHit = CycleChartLayoutEngine.hitTestNode(layoutResult, event.x, event.y)
        if (nodeHit != null) {
            selectedNodeIndex = nodeHit
            selectedLinkIndex = null
            nodes.getOrNull(nodeHit)?.let { node ->
                onNodeClickListener?.invoke(nodeHit, node, node.payload)
            }
            invalidate()
            performClick()
            return true
        }

        val linkHit = CycleChartLayoutEngine.hitTestLink(
            layout = layoutResult,
            x = event.x,
            y = event.y,
            tolerancePx = max(dp(10f), styleOptions.linkMaxThicknessDp * density * 0.5f),
        )
        if (linkHit != null) {
            selectedNodeIndex = null
            selectedLinkIndex = linkHit
            links.getOrNull(linkHit)?.let { link ->
                onLinkClickListener?.invoke(linkHit, link, link.payload)
            }
            invalidate()
            performClick()
            return true
        }

        if (selectedNodeIndex != null || selectedLinkIndex != null) {
            clearSelection()
            invalidate()
        }
        return true
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    private fun resolveLayout(w: Int, h: Int) {
        layoutResult = if (w > 0 && h > 0) {
            CycleChartLayoutEngine.compute(
                nodes = nodes,
                links = links,
                config = CycleChartLayoutConfig(
                    widthPx = w.toFloat(),
                    heightPx = h.toFloat(),
                    contentPaddingPx = dp(styleOptions.contentPaddingDp),
                    nodeRadiusPx = dp(styleOptions.nodeRadiusDp),
                    linkMinThicknessPx = dp(styleOptions.linkMinThicknessDp),
                    linkMaxThicknessPx = dp(styleOptions.linkMaxThicknessDp),
                    linkInsetPx = dp(styleOptions.linkInsetDp),
                    startAngleDeg = presentationOptions.startAngleDeg,
                    clockwise = presentationOptions.clockwise,
                    linkInnerRadiusFactor = presentationOptions.linkInnerRadiusFactor,
                ),
                styleOptions = styleOptions,
            )
        } else {
            CycleChartLayoutResult(
                nodeLayouts = emptyList(),
                linkLayouts = emptyList(),
                centerX = 0f,
                centerY = 0f,
                orbitRadius = 0f,
                isRenderable = false,
                emptyReason = "empty",
            )
        }
    }

    private fun playEnterAnimationIfNeeded() {
        animator?.cancel()
        if (layoutResult.isRenderable && presentationOptions.animateOnDataChange) {
            renderProgress = 0f
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = presentationOptions.animationDurationMs.coerceAtLeast(0L)
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    renderProgress = (it.animatedValue as Float).coerceIn(0f, 1f)
                    invalidate()
                }
                start()
            }
        } else {
            renderProgress = 1f
        }
    }

    private fun clearSelection() {
        selectedNodeIndex = null
        selectedLinkIndex = null
    }

    private fun drawLink(canvas: Canvas, link: CycleLinkLayout) {
        val highlighted = isLinkHighlighted(link)
        val selectionActive = hasSelection()
        val alpha = when {
            highlighted -> renderProgress
            selectionActive -> 0.18f * renderProgress
            else -> presentationOptions.linkAlpha.coerceIn(0f, 1f) * renderProgress
        }
        val strokeWidth = if (highlighted) {
            max(link.thickness, dp(styleOptions.selectedStrokeWidthDp))
        } else {
            link.thickness
        }

        linkPath.reset()
        linkPath.moveTo(link.startX, link.startY)
        linkPath.quadTo(link.controlX, link.controlY, link.endX, link.endY)

        linkPaint.color = withAlpha(link.color, alpha)
        linkPaint.strokeWidth = strokeWidth
        canvas.drawPath(linkPath, linkPaint)

        val arrowColor = if (highlighted) styleOptions.selectedStrokeColor else link.color
        drawArrowHead(
            canvas = canvas,
            endX = link.endX,
            endY = link.endY,
            tangentX = link.endX - link.controlX,
            tangentY = link.endY - link.controlY,
            sizePx = dp(styleOptions.linkArrowSizeDp),
            color = withAlpha(arrowColor, alpha),
        )
    }

    private fun drawNode(canvas: Canvas, node: CycleNodeLayout) {
        val highlighted = isNodeHighlighted(node.originalIndex)
        val selectionActive = hasSelection()
        val fillAlpha = when {
            highlighted -> renderProgress
            selectionActive -> 0.34f
            else -> renderProgress
        }

        nodeFillPaint.color = withAlpha(node.node.color, fillAlpha)
        canvas.drawCircle(node.centerX, node.centerY, node.radius, nodeFillPaint)

        nodeStrokePaint.color = if (highlighted) styleOptions.selectedStrokeColor else styleOptions.nodeStrokeColor
        nodeStrokePaint.strokeWidth = if (highlighted) {
            dp(styleOptions.selectedStrokeWidthDp)
        } else {
            dp(styleOptions.nodeStrokeWidthDp)
        }
        canvas.drawCircle(node.centerX, node.centerY, node.radius, nodeStrokePaint)
    }

    private fun drawNodeLabel(canvas: Canvas, node: CycleNodeLayout) {
        val directionX = node.centerX - layoutResult.centerX
        val directionY = node.centerY - layoutResult.centerY
        val length = sqrt((directionX * directionX) + (directionY * directionY)).coerceAtLeast(1f)
        val unitX = directionX / length
        val unitY = directionY / length
        val offset = node.radius + dp(14f)
        val x = node.centerX + (unitX * offset)
        val y = node.centerY + (unitY * offset) - ((nodeLabelPaint.descent() + nodeLabelPaint.ascent()) * 0.5f)

        nodeLabelPaint.color = withAlpha(
            styleOptions.nodeLabelTextColor,
            when {
                isNodeHighlighted(node.originalIndex) -> 1f
                hasSelection() -> 0.45f
                else -> 1f
            },
        )
        nodeLabelPaint.textAlign = when {
            abs(unitX) < 0.18f -> Paint.Align.CENTER
            unitX > 0f -> Paint.Align.LEFT
            else -> Paint.Align.RIGHT
        }
        canvas.drawText(node.node.label, x, y, nodeLabelPaint)
    }

    private fun drawLinkLabel(canvas: Canvas, link: CycleLinkLayout) {
        val text = link.link.label?.trim().orEmpty().ifBlank { formatValue(link.link.value) }
        if (text.isBlank()) return

        linkLabelPaint.color = withAlpha(
            styleOptions.linkLabelTextColor,
            when {
                isLinkHighlighted(link) -> 1f
                hasSelection() -> 0.32f
                else -> 1f
            },
        )
        val baselineY = link.midY - ((linkLabelPaint.descent() + linkLabelPaint.ascent()) * 0.5f)
        canvas.drawText(text, link.midX, baselineY, linkLabelPaint)
    }

    private fun drawEmptyState(canvas: Canvas) {
        val text = presentationOptions.emptyText?.trim().orEmpty()
        if (text.isBlank()) return

        val x = width * 0.5f
        val y = (height * 0.5f) - ((emptyTextPaint.descent() + emptyTextPaint.ascent()) * 0.5f)
        canvas.drawText(text, x, y, emptyTextPaint)
    }

    private fun applyStyle() {
        nodeLabelPaint.color = styleOptions.nodeLabelTextColor
        nodeLabelPaint.textSize = sp(styleOptions.nodeLabelTextSizeSp)
        linkLabelPaint.color = styleOptions.linkLabelTextColor
        linkLabelPaint.textSize = sp(styleOptions.linkLabelTextSizeSp)
        emptyTextPaint.color = styleOptions.nodeLabelTextColor
        emptyTextPaint.textSize = sp(13f)
    }

    private fun hasSelection(): Boolean = selectedNodeIndex != null || selectedLinkIndex != null

    private fun isNodeHighlighted(nodeIndex: Int): Boolean {
        val selectedNode = selectedNodeIndex
        if (selectedNode != null) return selectedNode == nodeIndex

        val selectedLink = selectedLinkIndex ?: return false
        val link = layoutResult.linkLayouts.firstOrNull { it.originalIndex == selectedLink } ?: return false
        return link.sourceNodeOriginalIndex == nodeIndex || link.targetNodeOriginalIndex == nodeIndex
    }

    private fun isLinkHighlighted(link: CycleLinkLayout): Boolean {
        val selectedLink = selectedLinkIndex
        if (selectedLink != null) return selectedLink == link.originalIndex

        val selectedNode = selectedNodeIndex ?: return false
        return link.sourceNodeOriginalIndex == selectedNode || link.targetNodeOriginalIndex == selectedNode
    }

    private fun drawArrowHead(
        canvas: Canvas,
        endX: Float,
        endY: Float,
        tangentX: Float,
        tangentY: Float,
        sizePx: Float,
        color: Int,
    ) {
        val length = sqrt((tangentX * tangentX) + (tangentY * tangentY))
        if (length <= 1e-4f || sizePx <= 0f) return

        val dirX = tangentX / length
        val dirY = tangentY / length
        val perpX = -dirY
        val perpY = dirX
        val backX = endX - (dirX * sizePx)
        val backY = endY - (dirY * sizePx)
        val wing = sizePx * 0.55f

        arrowPath.reset()
        arrowPath.moveTo(endX, endY)
        arrowPath.lineTo(backX + (perpX * wing), backY + (perpY * wing))
        arrowPath.lineTo(backX - (perpX * wing), backY - (perpY * wing))
        arrowPath.close()

        arrowPaint.color = color
        canvas.drawPath(arrowPath, arrowPaint)
    }

    private fun formatValue(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.roundToInt().toString()
        } else {
            String.format("%.1f", value)
        }
    }

    private fun withAlpha(color: Int, alpha: Float): Int {
        val clamped = alpha.coerceIn(0f, 1f)
        val baseAlpha = Color.alpha(color) / 255f
        val outputAlpha = (baseAlpha * clamped * 255f).roundToInt().coerceIn(0, 255)
        return Color.argb(outputAlpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun dp(value: Float): Float = value * density

    private fun sp(value: Float): Float = value * scaledDensity
}
