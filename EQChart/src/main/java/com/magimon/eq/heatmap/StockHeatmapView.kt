package com.magimon.eq.heatmap

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.magimon.eq.treemap.TreemapChartLayoutResult
import com.magimon.eq.treemap.TreemapChartPresentationOptions
import com.magimon.eq.treemap.TreemapChartStyleOptions
import com.magimon.eq.treemap.TreemapGroup
import com.magimon.eq.treemap.TreemapItem
import com.magimon.eq.treemap.buildTreemapViewLayout
import com.magimon.eq.treemap.drawTreemapScene
import com.magimon.eq.treemap.findTreemapHit

/**
 * Stock heatmap view inspired by TradingView-style layouts.
 *
 * - Block color: mapped from [StockHeatmapItem.changePct]
 * - Block area: prefers `sizeRatio`, otherwise uses `marketCap`
 * - Group layout: two-stage squarified treemap
 */
class StockHeatmapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity

    private val sections = mutableListOf<StockHeatmapSection>()
    private val blocks = mutableListOf<BlockRect>()
    private val sectionLayouts = mutableListOf<SectionLayout>()

    private var treemapLayout = TreemapChartLayoutResult(emptyList(), emptyList(), isRenderable = false)
    private var itemClickListener: ((StockHeatmapItem) -> Unit)? = null

    private data class BlockRect(
        val item: StockHeatmapItem,
        val rect: RectF,
    )

    private data class SectionGroup(
        val name: String,
        val color: Int,
        val totalWeight: Float,
        val items: List<StockHeatmapItem>,
    )

    private data class SectionLayout(
        val group: SectionGroup,
        val headerRect: RectF,
    )

    /**
     * Sets the click listener for stock blocks.
     */
    fun setOnItemClickListener(listener: (StockHeatmapItem) -> Unit) {
        itemClickListener = listener
    }

    /**
     * Backward-compatible API.
     *
     * Accepts a flat stock list and auto-groups by `item.sector`.
     */
    fun setData(data: List<StockHeatmapItem>) {
        val grouped = data.groupBy { it.sector }
        val mapped = grouped.map { (sector, stocks) ->
            StockHeatmapSection(
                name = sector,
                color = StockHeatmapHelper.mapSectorToColor(sector),
                stocks = stocks,
            )
        }
        setSections(mapped)
    }

    /**
     * Recommended API.
     *
     * Passes section name/color/item list explicitly.
     */
    fun setSections(data: List<StockHeatmapSection>) {
        sections.clear()
        data.forEach { section ->
            val validStocks = section.stocks.filter { itemWeight(it) > 0f }
            if (section.name.isNotBlank() && validStocks.isNotEmpty()) {
                sections.add(section.copy(stocks = validStocks))
            }
        }
        recomputeLayout(width, height)
        requestLayout()
        invalidate()
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
            layout = treemapLayout,
            density = density,
            scaledDensity = scaledDensity,
            styleOptions = TreemapChartStyleOptions(),
            presentationOptions = TreemapChartPresentationOptions(),
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_UP -> {
                val hit = findTreemapHit(treemapLayout, event)
                val original = hit?.item?.payload as? StockHeatmapItem
                if (original != null) {
                    itemClickListener?.invoke(original)
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

    /**
     * Calculates the area weight for an item.
     *
     * Uses `sizeRatio` when valid; otherwise falls back to `marketCap`.
     */
    private fun itemWeight(item: StockHeatmapItem): Float {
        val ratio = item.sizeRatio ?: 0.0
        return if (ratio > 0.0) ratio.toFloat() else item.marketCap.toFloat().coerceAtLeast(0f)
    }

    /**
     * Recomputes the full treemap layout from current size and section data.
     */
    private fun recomputeLayout(w: Int, h: Int) {
        treemapLayout = buildTreemapViewLayout(
            groups = heatmapSectionsToTreemapGroups(sections),
            widthPx = w,
            heightPx = h,
            density = density,
            styleOptions = TreemapChartStyleOptions(),
            presentationOptions = TreemapChartPresentationOptions(),
        )
        blocks.clear()
        blocks.addAll(
            treemapLayout.itemLayouts.mapNotNull { layout ->
                val item = layout.item.payload as? StockHeatmapItem ?: return@mapNotNull null
                BlockRect(
                    item = item,
                    rect = RectF(layout.rect.left, layout.rect.top, layout.rect.right, layout.rect.bottom),
                )
            },
        )
        sectionLayouts.clear()
        sectionLayouts.addAll(
            treemapLayout.groupHeaders.map { header ->
                val section = sections.getOrNull(header.groupIndex)
                val stocks = section?.stocks.orEmpty()
                SectionLayout(
                    group = SectionGroup(
                        name = header.group.label,
                        color = header.fillColor,
                        totalWeight = stocks.sumOf { itemWeight(it).toDouble() }.toFloat(),
                        items = stocks,
                    ),
                    headerRect = RectF(header.rect.left, header.rect.top, header.rect.right, header.rect.bottom),
                )
            },
        )
    }

    private fun heatmapSectionsToTreemapGroups(data: List<StockHeatmapSection>): List<TreemapGroup> {
        return data.map { section ->
            TreemapGroup(
                label = section.name,
                color = section.color,
                items = section.stocks.map { item ->
                    TreemapItem(
                        label = item.symbol,
                        value = itemWeight(item).toDouble(),
                        color = StockHeatmapHelper.mapChangeToColor(item.changePct),
                        supportingText = StockHeatmapHelper.formatChange(item.changePct),
                        payload = item,
                    )
                },
            )
        }
    }
}
