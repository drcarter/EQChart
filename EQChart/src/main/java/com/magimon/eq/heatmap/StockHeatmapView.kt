package com.magimon.eq.heatmap

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * A custom View that displays a stock heatmap similar to TradingView
 * - Block size represents market cap
 * - Block color represents price change (green for positive, red for negative)
 * - Stocks are grouped by sector
 */
class StockHeatmapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var sectorGroups: List<SectorGroup> = emptyList()
    private var selectedItem: StockHeatmapItem? = null
    private var onItemClickListener: ((StockHeatmapItem) -> Unit)? = null

    // Drawing components
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sectorLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Layout parameters
    private var padding = 16f
    private var sectorSpacing = 24f
    private var blockSpacing = 4f
    private var minBlockSize = 40f
    private var maxBlockSize = 120f
    private var sectorLabelHeight = 32f

    // Color configuration
    private val positiveColor = Color.parseColor("#26A69A") // Green
    private val negativeColor = Color.parseColor("#EF5350") // Red
    private val neutralColor = Color.parseColor("#78909C") // Gray
    private val backgroundColor = Color.parseColor("#FFFFFF")
    private val textColor = Color.parseColor("#FFFFFF")
    private val sectorLabelColor = Color.parseColor("#424242")
    private val selectedStrokeColor = Color.parseColor("#FFC107")
    private val selectedStrokeWidth = 4f

    // Layout data
    private data class BlockLayout(
        val item: StockHeatmapItem,
        val rect: RectF,
        val sector: String,
        val blockSize: Float
    )

    private var blockLayouts: MutableList<BlockLayout> = mutableListOf()
    private var sectorLabelRects: Map<String, RectF> = emptyMap()

    init {
        setupPaints()
    }

    private fun setupPaints() {
        textPaint.apply {
            color = textColor
            textSize = 12f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        sectorLabelPaint.apply {
            color = sectorLabelColor
            textSize = 14f
            textAlign = Paint.Align.LEFT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        selectedPaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = selectedStrokeWidth
            color = selectedStrokeColor
        }
    }

    /**
     * Set the stock data to display
     */
    fun setData(items: List<StockHeatmapItem>) {
        sectorGroups = items
            .groupBy { it.sector }
            .map { (sector, items) -> SectorGroup(sector, items) }
            .sortedByDescending { it.totalMarketCap }

        requestLayout()
        invalidate()
    }

    /**
     * Set click listener for stock items
     */
    fun setOnItemClickListener(listener: (StockHeatmapItem) -> Unit) {
        onItemClickListener = listener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = calculateHeight(width)
        setMeasuredDimension(width, height)
    }

    private fun calculateHeight(width: Int): Int {
        if (sectorGroups.isEmpty()) {
            return (padding * 2).toInt()
        }

        var currentY = padding + sectorLabelHeight
        val availableWidth = width - padding * 2

        sectorGroups.forEach { group ->
            currentY += blockSpacing
            
            // Simulate layout to calculate actual height
            val sortedItems = group.items.sortedByDescending { it.marketCap }
            val maxMarketCap = sortedItems.firstOrNull()?.marketCap ?: 0L
            val minMarketCap = sortedItems.lastOrNull()?.marketCap ?: 0L
            val avgBlockSize = calculateAverageBlockSize(sortedItems, maxMarketCap, minMarketCap)
            
            var x = padding
            var y = currentY
            var maxRowHeight = 0f

            sortedItems.forEach { item ->
                val itemBlockSize = calculateItemBlockSize(item.marketCap, maxMarketCap, minMarketCap)
                val rowHeight = itemBlockSize + blockSpacing
                
                if (x + itemBlockSize > width - padding && x > padding) {
                    x = padding
                    y += maxRowHeight
                    maxRowHeight = rowHeight
                } else {
                    maxRowHeight = max(maxRowHeight, rowHeight)
                }

                x += itemBlockSize + blockSpacing
            }

            currentY = y + maxRowHeight + sectorSpacing
        }

        return (currentY + padding).toInt()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            calculateLayouts()
        }
    }

    private fun calculateLayouts() {
        if (sectorGroups.isEmpty()) return

        blockLayouts.clear()
        val sectorLabelRectsMap = mutableMapOf<String, RectF>()

        var currentY = padding + sectorLabelHeight
        val availableWidth = width - padding * 2

        sectorGroups.forEach { group ->
            // Sector label
            val labelRect = RectF(
                padding,
                currentY - sectorLabelHeight,
                width - padding,
                currentY
            )
            sectorLabelRectsMap[group.sector] = labelRect
            currentY += blockSpacing

            // Calculate block layout with individual sizes
            val sortedItems = group.items.sortedByDescending { it.marketCap }
            val maxMarketCap = sortedItems.firstOrNull()?.marketCap ?: 0L
            val minMarketCap = sortedItems.lastOrNull()?.marketCap ?: 0L
            
            // Calculate average block size for row calculation
            val avgBlockSize = calculateAverageBlockSize(sortedItems, maxMarketCap, minMarketCap)
            val blocksPerRow = calculateBlocksPerRow(availableWidth, avgBlockSize)
            
            var x = padding
            var y = currentY
            var column = 0
            var maxRowHeight = 0f

            sortedItems.forEach { item ->
                val itemBlockSize = calculateItemBlockSize(item.marketCap, maxMarketCap, minMarketCap)
                val rowHeight = itemBlockSize + blockSpacing
                
                // Check if we need to wrap to next row
                if (column > 0 && x + itemBlockSize > width - padding) {
                    column = 0
                    x = padding
                    y += maxRowHeight
                    maxRowHeight = rowHeight
                } else {
                    maxRowHeight = max(maxRowHeight, rowHeight)
                }

                val rect = RectF(
                    x,
                    y,
                    x + itemBlockSize,
                    y + itemBlockSize
                )
                blockLayouts.add(BlockLayout(item, rect, group.sector, itemBlockSize))

                column++
                x += itemBlockSize + blockSpacing
            }

            currentY = y + maxRowHeight + sectorSpacing
        }

        sectorLabelRects = sectorLabelRectsMap
    }

    private fun calculateBlocksPerRow(availableWidth: Float, avgBlockSize: Float): Int {
        val blocksPerRow = ((availableWidth + blockSpacing) / (avgBlockSize + blockSpacing)).toInt()
        return max(1, blocksPerRow)
    }

    private fun calculateAverageBlockSize(items: List<StockHeatmapItem>, maxMarketCap: Long, minMarketCap: Long): Float {
        if (items.isEmpty()) return (minBlockSize + maxBlockSize) / 2
        if (maxMarketCap == minMarketCap) return (minBlockSize + maxBlockSize) / 2
        
        // Calculate average market cap
        val avgMarketCap = items.map { it.marketCap }.average().toLong()
        return calculateItemBlockSize(avgMarketCap, maxMarketCap, minMarketCap)
    }

    private fun calculateItemBlockSize(marketCap: Long, maxMarketCap: Long, minMarketCap: Long): Float {
        if (maxMarketCap == minMarketCap) return (minBlockSize + maxBlockSize) / 2
        
        // Use logarithmic scaling for better visual distribution
        // This ensures larger market caps get proportionally larger blocks
        val logMax = kotlin.math.ln(maxMarketCap.toDouble())
        val logMin = kotlin.math.ln(minMarketCap.toDouble())
        val logCurrent = kotlin.math.ln(marketCap.toDouble())
        
        val normalized = if (logMax > logMin) {
            ((logCurrent - logMin) / (logMax - logMin)).toFloat()
        } else {
            0.5f
        }
        
        val sizeRange = maxBlockSize - minBlockSize
        return minBlockSize + (sizeRange * normalized)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background
        canvas.drawColor(backgroundColor)

        if (sectorGroups.isEmpty()) return

        // Draw sector labels
        sectorLabelRects.forEach { (sector, rect) ->
            val group = sectorGroups.find { it.sector == sector }
            val text = "$sector (${group?.items?.size ?: 0})"
            val y = rect.centerY() + (sectorLabelPaint.textSize / 3)
            canvas.drawText(text, rect.left, y, sectorLabelPaint)
        }

        // Draw blocks
        blockLayouts.forEach { blockLayout ->
            val item = blockLayout.item
            val rect = blockLayout.rect

            // Draw block with color based on change
            paint.color = getColorForChange(item.change)
            canvas.drawRoundRect(rect, 4f, 4f, paint)

            // Draw selected border
            if (selectedItem?.symbol == item.symbol) {
                canvas.drawRoundRect(
                    rect.left - selectedStrokeWidth / 2,
                    rect.top - selectedStrokeWidth / 2,
                    rect.right + selectedStrokeWidth / 2,
                    rect.bottom + selectedStrokeWidth / 2,
                    4f,
                    4f,
                    selectedPaint
                )
            }

            // Draw symbol text
            if (rect.width() > 30) {
                val textY = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
                canvas.drawText(item.symbol, rect.centerX(), textY, textPaint)
            }
        }
    }

    private fun getColorForChange(change: Float): Int {
        return when {
            change > 0 -> interpolateColor(neutralColor, positiveColor, min(change / 5f, 1f))
            change < 0 -> interpolateColor(neutralColor, negativeColor, min(-change / 5f, 1f))
            else -> neutralColor
        }
    }

    private fun interpolateColor(color1: Int, color2: Int, factor: Float): Int {
        val clampedFactor = max(0f, min(1f, factor))
        val r1 = Color.red(color1)
        val g1 = Color.green(color1)
        val b1 = Color.blue(color1)
        val r2 = Color.red(color2)
        val g2 = Color.green(color2)
        val b2 = Color.blue(color2)

        return Color.rgb(
            (r1 + (r2 - r1) * clampedFactor).toInt(),
            (g1 + (g2 - g1) * clampedFactor).toInt(),
            (b1 + (b2 - b1) * clampedFactor).toInt()
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val clickedItem = findItemAt(event.x, event.y)
                if (clickedItem != null) {
                    selectedItem = clickedItem
                    invalidate()
                    onItemClickListener?.invoke(clickedItem)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun findItemAt(x: Float, y: Float): StockHeatmapItem? {
        return blockLayouts.find { it.rect.contains(x, y) }?.item
    }

    /**
     * Configuration methods
     */
    fun setPadding(padding: Float) {
        this.padding = padding
        requestLayout()
        invalidate()
    }

    fun setBlockSpacing(spacing: Float) {
        this.blockSpacing = spacing
        requestLayout()
        invalidate()
    }

    fun setSectorSpacing(spacing: Float) {
        this.sectorSpacing = spacing
        requestLayout()
        invalidate()
    }

    fun setBlockSizeRange(min: Float, max: Float) {
        this.minBlockSize = min
        this.maxBlockSize = max
        requestLayout()
        invalidate()
    }

    fun setPositiveColor(color: Int) {
        // This would require updating the color calculation
        invalidate()
    }

    fun setNegativeColor(color: Int) {
        // This would require updating the color calculation
        invalidate()
    }
}

