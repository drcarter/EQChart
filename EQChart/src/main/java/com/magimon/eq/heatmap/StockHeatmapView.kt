package com.magimon.eq.heatmap

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

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

    private val sections = mutableListOf<StockHeatmapSection>()

    private data class BlockRect(
        val item: StockHeatmapItem,
        val rect: RectF,
    )

    private val blocks = mutableListOf<BlockRect>()

    private val density: Float = resources.displayMetrics.density
    private val outerPadding = 6f * density
    private val sectionGap = 3f * density
    private val blockGap = 2f * density
    private val sectionHeaderHeight: Float = 20f * density

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#111316")
    }

    private val rectPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = 0x55000000
    }

    private val symbolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.LEFT
        textSize = 12f * resources.displayMetrics.scaledDensity
        isFakeBoldText = true
    }

    private val changePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.LEFT
        textSize = 10f * resources.displayMetrics.scaledDensity
    }

    private data class SectionLayout(
        val group: SectionGroup,
        val headerRect: RectF,
    )

    private val sectionLayouts = mutableListOf<SectionLayout>()

    private val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE9EEF5.toInt()
        textAlign = Paint.Align.LEFT
        textSize = 11f * resources.displayMetrics.scaledDensity
        isFakeBoldText = true
    }

    private val sectionHeaderBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var itemClickListener: ((StockHeatmapItem) -> Unit)? = null

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
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = 800 * density
        val desiredHeight = 1200 * density

        val width = resolveSize(desiredWidth.toInt(), widthMeasureSpec)
        val height = resolveSize(desiredHeight.toInt(), heightMeasureSpec)

        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recomputeLayout(w, h)
    }

    /**
     * Recomputes the full treemap layout from current size and section data.
     */
    private fun recomputeLayout(width: Int, height: Int) {
        blocks.clear()
        sectionLayouts.clear()
        if (width <= 0 || height <= 0 || sections.isEmpty()) return

        val rootRect = RectF(
            outerPadding,
            outerPadding,
            width.toFloat() - outerPadding,
            height.toFloat() - outerPadding,
        )

        if (sections.size <= 1) {
            val flatItems = sections.first().stocks
            val flatBlocks = layoutSquarified(
                flatItems.map { WeightedItem(it, itemWeight(it)) },
                rootRect,
            )
            blocks.addAll(
                flatBlocks.mapNotNull {
                    val inset = insetRect(it.rect, blockGap)
                    if (inset.width() > 0f && inset.height() > 0f) BlockRect(it.item, inset) else null
                },
            )
            return
        }

        val sectionWeighted = sections.map { section ->
            val total = section.stocks.sumOf { itemWeight(it).toDouble() }.toFloat().coerceAtLeast(0.01f)
            SectionGroup(section.name, section.color, total, section.stocks)
        }

        val sectionRects = layoutSquarified(
            sectionWeighted.map { WeightedItem(it, it.totalWeight) },
            rootRect,
        )

        sectionRects.forEach { sectionBlock ->
            val group = sectionBlock.item
            val fullRect = insetRect(sectionBlock.rect, sectionGap)
            if (fullRect.width() <= 0f || fullRect.height() <= 0f) return@forEach

            if (fullRect.height() <= sectionHeaderHeight * 1.5f) {
                val itemRects = layoutSquarified(
                    group.items.map { WeightedItem(it, itemWeight(it)) },
                    fullRect,
                )
                itemRects.forEach { itemBlock ->
                    val inset = insetRect(itemBlock.rect, blockGap)
                    if (inset.width() > 0f && inset.height() > 0f) {
                        blocks.add(BlockRect(itemBlock.item, inset))
                    }
                }
            } else {
                val headerRect = RectF(
                    fullRect.left,
                    fullRect.top,
                    fullRect.right,
                    fullRect.top + sectionHeaderHeight,
                )
                val contentRect = RectF(
                    fullRect.left,
                    headerRect.bottom,
                    fullRect.right,
                    fullRect.bottom,
                )

                sectionLayouts.add(
                    SectionLayout(
                        group = group,
                        headerRect = headerRect,
                    ),
                )

                val itemRects = layoutSquarified(
                    group.items.map { WeightedItem(it, itemWeight(it)) },
                    contentRect,
                )
                itemRects.forEach { itemBlock ->
                    val inset = insetRect(itemBlock.rect, blockGap)
                    if (inset.width() > 0f && inset.height() > 0f) {
                        blocks.add(BlockRect(itemBlock.item, inset))
                    }
                }
            }
        }
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
     * Returns a new rect inset by the same margin on all sides.
     */
    private fun insetRect(src: RectF, inset: Float): RectF {
        return RectF(src.left + inset, src.top + inset, src.right - inset, src.bottom - inset)
    }

    /**
     * Returns a color with overridden alpha based on an existing color.
     */
    private fun withAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
    }

    private data class WeightedItem<T>(
        val item: T,
        val weight: Float,
    )

    private data class SectionGroup(
        val name: String,
        val color: Int,
        val totalWeight: Float,
        val items: List<StockHeatmapItem>,
    )

    private data class LayoutBlock<T>(
        val item: T,
        val rect: RectF,
    )

    /**
     * Lays out weighted items into a rectangle using the squarified treemap algorithm.
     */
    private fun <T> layoutSquarified(
        items: List<WeightedItem<T>>,
        bounds: RectF,
    ): List<LayoutBlock<T>> {
        if (items.isEmpty()) return emptyList()

        val totalWeight = items.sumOf { it.weight.toDouble() }.toFloat()
        if (totalWeight <= 0f || bounds.width() <= 0f || bounds.height() <= 0f) {
            return emptyList()
        }
        val area = bounds.width() * bounds.height()
        val scale = area / totalWeight

        val scaled = items
            .filter { it.weight > 0f }
            .sortedByDescending { it.weight }
            .map { it.copy(weight = max(it.weight * scale, 1f)) }

        if (scaled.isEmpty()) return emptyList()

        val result = mutableListOf<LayoutBlock<T>>()
        squarify(
            remaining = scaled.toMutableList(),
            currentRow = mutableListOf(),
            rect = RectF(bounds),
            outBlocks = result,
        )
        return result
    }

    /**
     * Adds items sequentially while keeping row aspect ratio from getting worse.
     */
    private fun <T> squarify(
        remaining: MutableList<WeightedItem<T>>,
        currentRow: MutableList<WeightedItem<T>>,
        rect: RectF,
        outBlocks: MutableList<LayoutBlock<T>>,
    ) {
        if (remaining.isEmpty()) {
            if (currentRow.isNotEmpty()) {
                layoutRow(currentRow, rect, outBlocks)
            }
            return
        }

        val item = remaining.removeAt(0)
        if (currentRow.isEmpty()) {
            currentRow.add(item)
            squarify(remaining, currentRow, rect, outBlocks)
            return
        }

        val w = min(rect.width(), rect.height())
        val currentWorst = worstAspectRatio(currentRow, w)
        val newRow = ArrayList(currentRow)
        newRow.add(item)
        val newWorst = worstAspectRatio(newRow, w)

        if (newWorst <= currentWorst) {
            currentRow.add(item)
            squarify(remaining, currentRow, rect, outBlocks)
        } else {
            val newRect = layoutRow(currentRow, rect, outBlocks)
            currentRow.clear()
            currentRow.add(item)
            squarify(remaining, currentRow, newRect, outBlocks)
        }
    }

    /**
     * Finalizes placement for the current row and returns the remaining rect.
     */
    private fun <T> layoutRow(
        row: List<WeightedItem<T>>,
        rect: RectF,
        outBlocks: MutableList<LayoutBlock<T>>,
    ): RectF {
        val totalArea = row.sumOf { it.weight.toDouble() }.toFloat()
        val isHorizontal = rect.width() >= rect.height()

        return if (isHorizontal) {
            val rowHeight = totalArea / rect.width()
            var x = rect.left
            val y = rect.top
            row.forEach { item ->
                val itemWidth = item.weight / rowHeight
                val blockRect = RectF(
                    x,
                    y,
                    min(x + itemWidth, rect.right),
                    min(y + rowHeight, rect.bottom),
                )
                outBlocks.add(LayoutBlock(item.item, blockRect))
                x += itemWidth
            }
            RectF(rect.left, y + rowHeight, rect.right, rect.bottom)
        } else {
            val columnWidth = totalArea / rect.height()
            val x = rect.left
            var y = rect.top
            row.forEach { item ->
                val itemHeight = item.weight / columnWidth
                val blockRect = RectF(
                    x,
                    y,
                    min(x + columnWidth, rect.right),
                    min(y + itemHeight, rect.bottom),
                )
                outBlocks.add(LayoutBlock(item.item, blockRect))
                y += itemHeight
            }
            RectF(x + columnWidth, rect.top, rect.right, rect.bottom)
        }
    }

    /**
     * Computes the worst aspect ratio among items in the row.
     *
     * Smaller values indicate a layout closer to square blocks.
     */
    private fun worstAspectRatio(
        row: List<WeightedItem<*>>,
        w: Float,
    ): Float {
        if (row.isEmpty() || w <= 0f) return Float.MAX_VALUE

        var worst = 0f
        val sumArea = row.sumOf { it.weight.toDouble() }.toFloat()
        val sideShort = min(w, sumArea / w)

        row.forEach { item ->
            val area = item.weight
            if (area <= 0f) return@forEach
            val side1 = area / sideShort
            val side2 = sideShort
            val ratio = max(side1 / side2, side2 / side1)
            worst = max(worst, ratio)
        }
        return if (worst == 0f) Float.MAX_VALUE else worst
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        blocks.forEach { block ->
            val color = StockHeatmapHelper.mapChangeToColor(block.item.changePct)
            rectPaint.color = color
            canvas.drawRect(block.rect, rectPaint)
            canvas.drawRect(block.rect, borderPaint)

            drawItemText(canvas, block)
        }

        sectionLayouts.forEach { sectionLayout ->
            val header = sectionLayout.headerRect
            val group = sectionLayout.group

            sectionHeaderBgPaint.color = withAlpha(group.color, 170)
            canvas.drawRect(header, sectionHeaderBgPaint)
            canvas.drawRect(header, borderPaint)

            val padding = 4f * density
            val x = header.left + padding
            val y = header.top + sectionTitlePaint.textSize + padding / 2f
            canvas.drawText(group.name, x, y, sectionTitlePaint)
        }
    }

    /**
     * Conditionally renders ticker/change text based on block size.
     */
    private fun drawItemText(canvas: Canvas, block: BlockRect) {
        val rect = block.rect
        val w = rect.width()
        val h = rect.height()

        val minWidthForSymbol = 42f * density
        val minHeightForSymbol = 22f * density
        if (w < minWidthForSymbol || h < minHeightForSymbol) return

        val padding = 4f * density
        val textScale = (min(w, h) / (58f * density)).coerceIn(0.85f, 1.45f)
        symbolPaint.textSize = 12f * resources.displayMetrics.scaledDensity * textScale
        changePaint.textSize = 10f * resources.displayMetrics.scaledDensity * textScale

        val symbolX = rect.left + padding
        val symbolY = rect.top + padding + symbolPaint.textSize
        canvas.drawText(block.item.symbol, symbolX, symbolY, symbolPaint)

        val minHeightForChange = 36f * density
        if (h >= minHeightForChange) {
            val changeText = StockHeatmapHelper.formatChange(block.item.changePct)
            val changeY = symbolY + changePaint.textSize + (2f * density)
            canvas.drawText(changeText, symbolX, changeY, changePaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_UP -> {
                val x = event.x
                val y = event.y
                val hit = blocks.firstOrNull { it.rect.contains(x, y) }
                if (hit != null) {
                    itemClickListener?.invoke(hit.item)
                    return true
                }
            }
        }
        return true
    }
}
