package com.magimon.eq.sankey

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * View-based Sankey chart with node/link tap selection.
 *
 * The view consumes explicit node/link lists, delegates graph layout to
 * [SankeyChartLayoutEngine], and renders a flow diagram with stage inference, link ribbons,
 * optional labels, and selection highlighting.
 *
 * Selection behavior:
 * - tapping a node highlights that node and its connected links
 * - tapping a link highlights that link and its endpoint nodes
 * - tapping empty space clears the current selection
 *
 * Invalid graphs such as cycles or backward stage assignments render
 * [SankeyChartPresentationOptions.emptyText].
 */
class SankeyChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density

    private var nodes: List<SankeyNode> = emptyList()
    private var links: List<SankeyLink> = emptyList()
    private var styleOptions = SankeyChartStyleOptions()
    private var presentationOptions = SankeyChartPresentationOptions()
    private var layoutResult = SankeyChartLayoutResult(emptyList(), emptyList(), 0, false, "empty")

    private var selectedNodeIndex: Int? = null
    private var selectedLinkIndex: Int? = null
    private var renderProgress: Float = 1f
    private var animator: ValueAnimator? = null

    private var onNodeClickListener: ((Int, SankeyNode, Any?) -> Unit)? = null
    private var onLinkClickListener: ((Int, SankeyLink, Any?) -> Unit)? = null

    private val linkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val nodeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val nodeLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val linkValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }
    private val emptyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }

    private val nodeRect = RectF()
    private val linkPath = Path()

    init {
        applyStyle()
    }

    /**
     * Sets the full node list used by the chart.
     *
     * Nodes are matched by [SankeyNode.id]. Changing the node list clears current selection and
     * recomputes the layout.
     */
    fun setNodes(items: List<SankeyNode>) {
        nodes = items
        selectedNodeIndex = null
        selectedLinkIndex = null
        resolveLayout(width, height)
        playEnterAnimationIfNeeded()
        invalidate()
    }

    /**
     * Sets the full link list used by the chart.
     *
     * Links are validated against the current node set. Changing the link list clears current
     * selection and recomputes the layout.
     */
    fun setLinks(items: List<SankeyLink>) {
        links = items
        selectedNodeIndex = null
        selectedLinkIndex = null
        resolveLayout(width, height)
        playEnterAnimationIfNeeded()
        invalidate()
    }

    /**
     * Replaces the visual styling for nodes, links, labels, and selection states.
     */
    fun setStyleOptions(options: SankeyChartStyleOptions) {
        styleOptions = options
        applyStyle()
        resolveLayout(width, height)
        invalidate()
    }

    /**
     * Replaces the current behavioral and layout presentation options.
     */
    fun setPresentationOptions(options: SankeyChartPresentationOptions) {
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
    fun setOnNodeClickListener(listener: (nodeIndex: Int, node: SankeyNode, payload: Any?) -> Unit) {
        onNodeClickListener = listener
    }

    /**
     * Registers a callback invoked when a link is tapped.
     *
     * Callback arguments are ordered as `(linkIndex, link, payload)`.
     */
    fun setOnLinkClickListener(listener: (linkIndex: Int, link: SankeyLink, payload: Any?) -> Unit) {
        onLinkClickListener = listener
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = (840f * density).toInt()
        val desiredHeight = (520f * density).toInt()
        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec),
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        resolveLayout(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(styleOptions.backgroundColor)

        if (width <= 0 || height <= 0) return
        if (layoutResult.nodeLayouts.isEmpty() && (nodes.isNotEmpty() || links.isNotEmpty())) {
            resolveLayout(width, height)
        }
        if (!layoutResult.isRenderable) {
            drawEmptyState(canvas)
            return
        }

        layoutResult.linkLayouts.forEach { link ->
            linkPaint.color = resolveLinkColor(link)
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
        if (presentationOptions.showLinkValues) {
            layoutResult.linkLayouts.forEach { link ->
                drawLinkValue(canvas, link)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!layoutResult.isRenderable) return false
        if (event.action != MotionEvent.ACTION_UP) return true

        val nodeHit = SankeyChartLayoutEngine.hitTestNode(layoutResult, event.x, event.y)
        if (nodeHit != null) {
            selectedNodeIndex = nodeHit
            selectedLinkIndex = null
            val node = nodes.getOrNull(nodeHit)
            if (node != null) {
                onNodeClickListener?.invoke(nodeHit, node, node.payload)
            }
            invalidate()
            performClick()
            return true
        }

        val linkHit = SankeyChartLayoutEngine.hitTestLink(layoutResult, event.x, event.y, dp(8f))
        if (linkHit != null) {
            selectedLinkIndex = linkHit
            selectedNodeIndex = null
            val link = links.getOrNull(linkHit)
            if (link != null) {
                onLinkClickListener?.invoke(linkHit, link, link.payload)
            }
            invalidate()
            performClick()
            return true
        }

        if (selectedNodeIndex != null || selectedLinkIndex != null) {
            selectedNodeIndex = null
            selectedLinkIndex = null
            invalidate()
            performClick()
        }
        return true
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    private fun resolveLayout(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        layoutResult = SankeyChartLayoutEngine.compute(
            nodes = nodes,
            links = links,
            config = SankeyChartLayoutConfig(
                widthPx = w.toFloat(),
                heightPx = h.toFloat(),
                contentPaddingPx = dp(styleOptions.contentPaddingDp),
                nodeWidthPx = dp(styleOptions.nodeWidthDp),
                nodeMinHeightPx = dp(styleOptions.nodeMinHeightDp),
                nodeGapPx = dp(presentationOptions.nodeGapDp),
                columnGapPx = dp(presentationOptions.columnGapDp),
            ),
            styleOptions = styleOptions,
        )
    }

    private fun playEnterAnimationIfNeeded() {
        animator?.cancel()
        if (!presentationOptions.animateOnDataChange || !layoutResult.isRenderable) {
            renderProgress = 1f
            return
        }
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
    }

    private fun drawLink(canvas: Canvas, link: SankeyLinkLayout) {
        val startX = link.sourceRight
        val endX = link.targetLeft
        val controlOffset = (endX - startX) * 0.35f

        linkPath.reset()
        linkPath.moveTo(startX, link.sourceTop)
        linkPath.cubicTo(
            startX + controlOffset,
            link.sourceTop,
            endX - controlOffset,
            link.targetTop,
            endX,
            link.targetTop,
        )
        linkPath.lineTo(endX, link.targetBottom)
        linkPath.cubicTo(
            endX - controlOffset,
            link.targetBottom,
            startX + controlOffset,
            link.sourceBottom,
            startX,
            link.sourceBottom,
        )
        linkPath.close()
        canvas.drawPath(linkPath, linkPaint)
    }

    private fun drawNode(canvas: Canvas, node: SankeyNodeLayout) {
        nodePaint.color = withAlpha(node.node.color, resolveNodeFillAlpha(node))
        nodeRect.set(node.left, node.top, node.right, node.bottom)
        canvas.drawRoundRect(
            nodeRect,
            dp(styleOptions.nodeCornerRadiusDp),
            dp(styleOptions.nodeCornerRadiusDp),
            nodePaint,
        )

        val shouldStroke = isNodeHighlighted(node.originalIndex)
        if (shouldStroke) {
            nodeStrokePaint.strokeWidth = dp(styleOptions.selectedStrokeWidthDp)
            nodeStrokePaint.color = styleOptions.selectedStrokeColor
            canvas.drawRoundRect(
                nodeRect,
                dp(styleOptions.nodeCornerRadiusDp),
                dp(styleOptions.nodeCornerRadiusDp),
                nodeStrokePaint,
            )
        } else if (styleOptions.nodeStrokeWidthDp > 0f) {
            nodeStrokePaint.strokeWidth = dp(styleOptions.nodeStrokeWidthDp)
            nodeStrokePaint.color = withAlpha(styleOptions.nodeStrokeColor, renderProgress)
            canvas.drawRoundRect(
                nodeRect,
                dp(styleOptions.nodeCornerRadiusDp),
                dp(styleOptions.nodeCornerRadiusDp),
                nodeStrokePaint,
            )
        }
    }

    private fun drawNodeLabel(canvas: Canvas, node: SankeyNodeLayout) {
        val lastStage = max(0, layoutResult.stageCount - 1)
        val alignRight = node.stage == lastStage
        nodeLabelPaint.textAlign = if (alignRight) Paint.Align.RIGHT else Paint.Align.LEFT
        nodeLabelPaint.color = withAlpha(styleOptions.nodeLabelTextColor, resolveNodeLabelAlpha(node))
        val x = if (alignRight) node.left - dp(8f) else node.right + dp(8f)
        val y = node.centerY - ((nodeLabelPaint.descent() + nodeLabelPaint.ascent()) * 0.5f)
        canvas.drawText(node.node.label, x, y, nodeLabelPaint)
    }

    private fun drawLinkValue(canvas: Canvas, link: SankeyLinkLayout) {
        if (link.thickness < sp(styleOptions.linkValueTextSizeSp)) return
        linkValuePaint.color = withAlpha(styleOptions.linkValueTextColor, resolveLinkValueAlpha(link))
        val x = (link.sourceRight + link.targetLeft) * 0.5f
        val y = ((link.sourceCenterY + link.targetCenterY) * 0.5f) -
            ((linkValuePaint.descent() + linkValuePaint.ascent()) * 0.5f)
        val text = link.link.label?.takeIf { it.isNotBlank() } ?: formatValue(link.link.value)
        canvas.drawText(text, x, y, linkValuePaint)
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

    private fun resolveLinkColor(link: SankeyLinkLayout): Int {
        val hasSelection = selectedNodeIndex != null || selectedLinkIndex != null
        val alpha = when {
            isLinkHighlighted(link.originalIndex) -> 0.92f * renderProgress
            hasSelection -> 0.16f * renderProgress
            else -> presentationOptions.linkAlpha.coerceIn(0f, 1f) * renderProgress
        }
        return withAlpha(link.color, alpha)
    }

    private fun resolveNodeFillAlpha(node: SankeyNodeLayout): Float {
        val hasSelection = selectedNodeIndex != null || selectedLinkIndex != null
        return when {
            isNodeHighlighted(node.originalIndex) -> renderProgress
            hasSelection -> 0.36f * renderProgress
            else -> renderProgress
        }
    }

    private fun resolveNodeLabelAlpha(node: SankeyNodeLayout): Float {
        val hasSelection = selectedNodeIndex != null || selectedLinkIndex != null
        return when {
            isNodeHighlighted(node.originalIndex) -> renderProgress
            hasSelection -> 0.4f * renderProgress
            else -> renderProgress
        }
    }

    private fun resolveLinkValueAlpha(link: SankeyLinkLayout): Float {
        val hasSelection = selectedNodeIndex != null || selectedLinkIndex != null
        return when {
            isLinkHighlighted(link.originalIndex) -> renderProgress
            hasSelection -> 0.24f * renderProgress
            else -> 0.72f * renderProgress
        }
    }

    private fun isNodeHighlighted(nodeOriginalIndex: Int): Boolean {
        val selectedNode = selectedNodeIndex
        if (selectedNode != null) return nodeOriginalIndex == selectedNode

        val selectedLink = selectedLinkIndex ?: return false
        val link = layoutResult.linkLayouts.firstOrNull { it.originalIndex == selectedLink } ?: return false
        return link.sourceNodeOriginalIndex == nodeOriginalIndex || link.targetNodeOriginalIndex == nodeOriginalIndex
    }

    private fun isLinkHighlighted(linkOriginalIndex: Int): Boolean {
        val selectedNode = selectedNodeIndex
        if (selectedNode != null) {
            return layoutResult.linkLayouts.any {
                it.originalIndex == linkOriginalIndex &&
                    (it.sourceNodeOriginalIndex == selectedNode || it.targetNodeOriginalIndex == selectedNode)
            }
        }
        return selectedLinkIndex == linkOriginalIndex
    }

    private fun applyStyle() {
        nodeStrokePaint.strokeWidth = dp(styleOptions.nodeStrokeWidthDp)
        nodeLabelPaint.color = styleOptions.nodeLabelTextColor
        nodeLabelPaint.textSize = sp(styleOptions.nodeLabelTextSizeSp)
        linkValuePaint.color = styleOptions.linkValueTextColor
        linkValuePaint.textSize = sp(styleOptions.linkValueTextSizeSp)
        emptyTextPaint.color = styleOptions.nodeLabelTextColor
        emptyTextPaint.textSize = sp(13f)
    }

    private fun withAlpha(color: Int, alphaFraction: Float): Int {
        val fraction = alphaFraction.coerceIn(0f, 1f)
        val alpha = (Color.alpha(color) * fraction).roundToInt().coerceIn(0, 255)
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun formatValue(value: Double): String {
        return if (value % 1.0 == 0.0) value.roundToInt().toString() else String.format("%.1f", value)
    }

    private fun dp(value: Float): Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)

    private fun sp(value: Float): Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)
}
