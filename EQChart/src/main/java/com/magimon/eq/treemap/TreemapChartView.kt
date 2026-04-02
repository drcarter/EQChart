package com.magimon.eq.treemap

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Android View renderer for grouped Treemap charts.
 *
 * The view renders a two-level `group -> item` treemap using the shared
 * [resolveTreemapChartLayout] engine so its geometry matches the Compose implementation.
 *
 * @see TreemapGroup
 * @see TreemapItem
 * @see TreemapChartStyleOptions
 * @see TreemapChartPresentationOptions
 */
class TreemapChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity

    private var groups: List<TreemapGroup> = emptyList()
    private var styleOptions = TreemapChartStyleOptions()
    private var presentationOptions = TreemapChartPresentationOptions()
    private var layout = TreemapChartLayoutResult(emptyList(), emptyList(), isRenderable = false)
    private var onItemClickListener: ((TreemapItem) -> Unit)? = null

    /**
     * Replaces the grouped data rendered by this view.
     */
    fun setGroups(data: List<TreemapGroup>) {
        groups = data
        recomputeLayout(width, height)
        invalidate()
    }

    /**
     * Applies updated visual styling.
     */
    fun setStyleOptions(options: TreemapChartStyleOptions) {
        styleOptions = options
        recomputeLayout(width, height)
        invalidate()
    }

    /**
     * Applies updated label and empty-state behavior.
     */
    fun setPresentationOptions(options: TreemapChartPresentationOptions) {
        presentationOptions = options
        recomputeLayout(width, height)
        invalidate()
    }

    /**
     * Registers a click callback for rendered tiles.
     */
    fun setOnItemClickListener(listener: (TreemapItem) -> Unit) {
        onItemClickListener = listener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = (800 * density).toInt()
        val desiredHeight = (1200 * density).toInt()
        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec),
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recomputeLayout(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawTreemapScene(
            canvas = canvas,
            widthPx = width,
            heightPx = height,
            layout = layout,
            density = density,
            scaledDensity = scaledDensity,
            styleOptions = styleOptions,
            presentationOptions = presentationOptions,
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_UP -> {
                val hit = findTreemapHit(layout, event)
                if (hit != null) {
                    onItemClickListener?.invoke(hit.item)
                    performClick()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun recomputeLayout(w: Int, h: Int) {
        layout = buildTreemapViewLayout(
            groups = groups,
            widthPx = w,
            heightPx = h,
            density = density,
            styleOptions = styleOptions,
            presentationOptions = presentationOptions,
        )
    }
}
