package com.magimon.eq.compose

import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import com.magimon.eq.heatmap.StockHeatmapItem
import com.magimon.eq.heatmap.StockHeatmapSection
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private data class HeatmapBlockRect(
    val item: StockHeatmapItem,
    val rect: RectF,
)

private data class HeatmapSectionGroup(
    val name: String,
    val color: Int,
    val totalWeight: Float,
    val items: List<StockHeatmapItem>,
)

private data class HeatmapSectionLayout(
    val group: HeatmapSectionGroup,
    val headerRect: RectF,
)

private data class HeatmapWeightedItem<T>(
    val item: T,
    val weight: Float,
)

private data class HeatmapLayoutBlock<T>(
    val item: T,
    val rect: RectF,
)

private data class HeatmapComputed(
    val blocks: List<HeatmapBlockRect>,
    val sectionHeaders: List<HeatmapSectionLayout>,
)

@Composable
fun StockHeatmapChart(
    sections: List<StockHeatmapSection>,
    modifier: Modifier = Modifier,
    onItemClick: ((StockHeatmapItem) -> Unit)? = null,
) {
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val normalized = remember(sections) {
            sections.mapNotNull { section ->
                val valid = section.stocks.filter { heatmapItemWeight(it) > 0f }
                if (section.name.isBlank() || valid.isEmpty()) {
                    null
                } else {
                    section.copy(stocks = valid)
                }
            }
        }

        val computed = remember(normalized, widthPx, heightPx) {
            computeHeatmapLayout(normalized, widthPx, heightPx, density.density)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(computed.blocks) {
                    detectTapGestures { tap ->
                        val hit = computed.blocks.firstOrNull { it.rect.contains(tap.x, tap.y) }
                        if (hit != null) {
                            onItemClick?.invoke(hit.item)
                        }
                    }
                },
        ) {
            drawRect(color = Color.parseColor("#111316").toComposeColor())

            computed.blocks.forEach { block ->
                drawRect(
                    color = heatmapMapChangeToColor(block.item.changePct).toComposeColor(),
                    topLeft = Offset(block.rect.left, block.rect.top),
                    size = Size(block.rect.width(), block.rect.height()),
                )
                drawRect(
                    color = 0x55000000.toInt().toComposeColor(),
                    topLeft = Offset(block.rect.left, block.rect.top),
                    size = Size(block.rect.width(), block.rect.height()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f),
                )

                drawHeatmapBlockText(block.item, block.rect, density.density, density.fontScale * density.density)
            }

            val titlePaint = newTextPaint(
                color = 0xFFE9EEF5.toInt(),
                textSizePx = with(density) { 11.sp.toPx() },
                align = Paint.Align.LEFT,
                bold = true,
            )

            computed.sectionHeaders.forEach { section ->
                drawRect(
                    color = heatmapWithAlpha(section.group.color, 170).toComposeColor(),
                    topLeft = Offset(section.headerRect.left, section.headerRect.top),
                    size = Size(section.headerRect.width(), section.headerRect.height()),
                )
                drawRect(
                    color = 0x55000000.toInt().toComposeColor(),
                    topLeft = Offset(section.headerRect.left, section.headerRect.top),
                    size = Size(section.headerRect.width(), section.headerRect.height()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f),
                )

                drawContext.canvas.nativeCanvas.drawText(
                    section.group.name,
                    section.headerRect.left + (4f * density.density),
                    section.headerRect.top + titlePaint.textSize + (2f * density.density),
                    titlePaint,
                )
            }
        }
    }
}

@Composable
fun StockHeatmapChartFromItems(
    items: List<StockHeatmapItem>,
    modifier: Modifier = Modifier,
    onItemClick: ((StockHeatmapItem) -> Unit)? = null,
) {
    val sections = remember(items) {
        items
            .groupBy { it.sector }
            .map { (sector, stocks) ->
                StockHeatmapSection(
                    name = sector,
                    color = heatmapMapSectorToColor(sector),
                    stocks = stocks,
                )
            }
    }

    StockHeatmapChart(
        sections = sections,
        modifier = modifier,
        onItemClick = onItemClick,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHeatmapBlockText(
    item: StockHeatmapItem,
    rect: RectF,
    density: Float,
    scaledDensity: Float,
) {
    val width = rect.width()
    val height = rect.height()

    val minWidthForSymbol = 42f * density
    val minHeightForSymbol = 22f * density
    if (width < minWidthForSymbol || height < minHeightForSymbol) return

    val textScale = (min(width, height) / (58f * density)).coerceIn(0.85f, 1.45f)
    val symbolPaint = newTextPaint(
        color = Color.WHITE,
        textSizePx = 12f * scaledDensity * textScale,
        align = Paint.Align.LEFT,
        bold = true,
    )
    val changePaint = newTextPaint(
        color = Color.WHITE,
        textSizePx = 10f * scaledDensity * textScale,
        align = Paint.Align.LEFT,
    )

    val padding = 4f * density
    val symbolX = rect.left + padding
    val symbolY = rect.top + padding + symbolPaint.textSize
    drawContext.canvas.nativeCanvas.drawText(item.symbol, symbolX, symbolY, symbolPaint)

    val minHeightForChange = 36f * density
    if (height >= minHeightForChange) {
        val changeY = symbolY + changePaint.textSize + (2f * density)
        drawContext.canvas.nativeCanvas.drawText(
            heatmapFormatChange(item.changePct),
            symbolX,
            changeY,
            changePaint,
        )
    }
}

private fun computeHeatmapLayout(
    sections: List<StockHeatmapSection>,
    width: Float,
    height: Float,
    density: Float,
): HeatmapComputed {
    val blocks = mutableListOf<HeatmapBlockRect>()
    val sectionLayouts = mutableListOf<HeatmapSectionLayout>()

    if (width <= 0f || height <= 0f || sections.isEmpty()) {
        return HeatmapComputed(blocks = blocks, sectionHeaders = sectionLayouts)
    }

    val outerPadding = 6f * density
    val sectionGap = 3f * density
    val blockGap = 2f * density
    val sectionHeaderHeight = 20f * density

    val rootRect = RectF(
        outerPadding,
        outerPadding,
        width - outerPadding,
        height - outerPadding,
    )

    if (sections.size <= 1) {
        val flatItems = sections.firstOrNull()?.stocks.orEmpty()
        val flatBlocks = heatmapLayoutSquarified(
            flatItems.map { HeatmapWeightedItem(it, heatmapItemWeight(it)) },
            rootRect,
        )
        flatBlocks.forEach { block ->
            val inset = heatmapInsetRect(block.rect, blockGap)
            if (inset.width() > 0f && inset.height() > 0f) {
                blocks.add(HeatmapBlockRect(item = block.item, rect = inset))
            }
        }
        return HeatmapComputed(blocks = blocks, sectionHeaders = sectionLayouts)
    }

    val sectionWeighted = sections.map { section ->
        val total = section.stocks.sumOf { heatmapItemWeight(it).toDouble() }.toFloat().coerceAtLeast(0.01f)
        HeatmapSectionGroup(
            name = section.name,
            color = section.color,
            totalWeight = total,
            items = section.stocks,
        )
    }

    val sectionRects = heatmapLayoutSquarified(
        sectionWeighted.map { HeatmapWeightedItem(it, it.totalWeight) },
        rootRect,
    )

    sectionRects.forEach { sectionBlock ->
        val group = sectionBlock.item
        val fullRect = heatmapInsetRect(sectionBlock.rect, sectionGap)
        if (fullRect.width() <= 0f || fullRect.height() <= 0f) return@forEach

        if (fullRect.height() <= sectionHeaderHeight * 1.5f) {
            val itemRects = heatmapLayoutSquarified(
                group.items.map { HeatmapWeightedItem(it, heatmapItemWeight(it)) },
                fullRect,
            )
            itemRects.forEach { itemBlock ->
                val inset = heatmapInsetRect(itemBlock.rect, blockGap)
                if (inset.width() > 0f && inset.height() > 0f) {
                    blocks.add(HeatmapBlockRect(item = itemBlock.item, rect = inset))
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

            sectionLayouts.add(HeatmapSectionLayout(group = group, headerRect = headerRect))

            val itemRects = heatmapLayoutSquarified(
                group.items.map { HeatmapWeightedItem(it, heatmapItemWeight(it)) },
                contentRect,
            )
            itemRects.forEach { itemBlock ->
                val inset = heatmapInsetRect(itemBlock.rect, blockGap)
                if (inset.width() > 0f && inset.height() > 0f) {
                    blocks.add(HeatmapBlockRect(item = itemBlock.item, rect = inset))
                }
            }
        }
    }

    return HeatmapComputed(blocks = blocks, sectionHeaders = sectionLayouts)
}

private fun heatmapInsetRect(src: RectF, inset: Float): RectF {
    return RectF(src.left + inset, src.top + inset, src.right - inset, src.bottom - inset)
}

private fun heatmapItemWeight(item: StockHeatmapItem): Float {
    val ratio = item.sizeRatio ?: 0.0
    return if (ratio > 0.0) {
        ratio.toFloat()
    } else {
        item.marketCap.toFloat().coerceAtLeast(0f)
    }
}

private fun <T> heatmapLayoutSquarified(
    items: List<HeatmapWeightedItem<T>>,
    bounds: RectF,
): List<HeatmapLayoutBlock<T>> {
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

    val result = mutableListOf<HeatmapLayoutBlock<T>>()
    heatmapSquarify(
        remaining = scaled.toMutableList(),
        currentRow = mutableListOf(),
        rect = RectF(bounds),
        outBlocks = result,
    )
    return result
}

private fun <T> heatmapSquarify(
    remaining: MutableList<HeatmapWeightedItem<T>>,
    currentRow: MutableList<HeatmapWeightedItem<T>>,
    rect: RectF,
    outBlocks: MutableList<HeatmapLayoutBlock<T>>,
) {
    if (remaining.isEmpty()) {
        if (currentRow.isNotEmpty()) {
            heatmapLayoutRow(currentRow, rect, outBlocks)
        }
        return
    }

    val item = remaining.removeAt(0)
    if (currentRow.isEmpty()) {
        currentRow.add(item)
        heatmapSquarify(remaining, currentRow, rect, outBlocks)
        return
    }

    val w = min(rect.width(), rect.height())
    val currentWorst = heatmapWorstAspectRatio(currentRow, w)
    val newRow = ArrayList(currentRow)
    newRow.add(item)
    val newWorst = heatmapWorstAspectRatio(newRow, w)

    if (newWorst <= currentWorst) {
        currentRow.add(item)
        heatmapSquarify(remaining, currentRow, rect, outBlocks)
    } else {
        val newRect = heatmapLayoutRow(currentRow, rect, outBlocks)
        currentRow.clear()
        currentRow.add(item)
        heatmapSquarify(remaining, currentRow, newRect, outBlocks)
    }
}

private fun <T> heatmapLayoutRow(
    row: List<HeatmapWeightedItem<T>>,
    rect: RectF,
    outBlocks: MutableList<HeatmapLayoutBlock<T>>,
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
            outBlocks.add(HeatmapLayoutBlock(item.item, blockRect))
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
            outBlocks.add(HeatmapLayoutBlock(item.item, blockRect))
            y += itemHeight
        }
        RectF(x + columnWidth, rect.top, rect.right, rect.bottom)
    }
}

private fun heatmapWorstAspectRatio(
    row: List<HeatmapWeightedItem<*>>,
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

private fun heatmapWithAlpha(color: Int, alpha: Int): Int {
    return Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
}

private fun heatmapMapSectorToColor(sector: String): Int {
    val palette = listOf(
        Color.parseColor("#1E88E5"),
        Color.parseColor("#00897B"),
        Color.parseColor("#F4511E"),
        Color.parseColor("#6D4C41"),
        Color.parseColor("#8E24AA"),
        Color.parseColor("#3949AB"),
        Color.parseColor("#43A047"),
        Color.parseColor("#FB8C00"),
    )
    return palette[abs(sector.hashCode()) % palette.size]
}

private fun heatmapMapChangeToColor(changePct: Double): Int {
    val maxAbs = 6.0
    val clamped = max(-maxAbs, min(maxAbs, changePct))
    val ratio = (clamped / maxAbs).toFloat()

    val neutral = Color.parseColor("#2A2F36")
    val up = Color.parseColor("#1F8F55")
    val down = Color.parseColor("#B23A3A")

    return when {
        ratio > 0f -> heatmapInterpolateColor(neutral, up, ratio)
        ratio < 0f -> heatmapInterpolateColor(neutral, down, abs(ratio))
        else -> neutral
    }
}

private fun heatmapInterpolateColor(from: Int, to: Int, t: Float): Int {
    val clamped = t.coerceIn(0f, 1f)
    val a = (Color.alpha(from) + (Color.alpha(to) - Color.alpha(from)) * clamped).toInt()
    val r = (Color.red(from) + (Color.red(to) - Color.red(from)) * clamped).toInt()
    val g = (Color.green(from) + (Color.green(to) - Color.green(from)) * clamped).toInt()
    val b = (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * clamped).toInt()
    return Color.argb(a, r, g, b)
}

private fun heatmapFormatChange(changePct: Double): String = String.format("%+.2f%%", changePct)
