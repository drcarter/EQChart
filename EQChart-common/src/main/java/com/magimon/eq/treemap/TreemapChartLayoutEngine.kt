package com.magimon.eq.treemap

import android.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Rectangular region expressed in pixels relative to the chart canvas.
 */
data class TreemapRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float
        get() = right - left

    val height: Float
        get() = bottom - top

    fun contains(x: Float, y: Float): Boolean = x in left..right && y in top..bottom
}

/**
 * Pixel-based layout inputs consumed by [resolveTreemapChartLayout].
 */
data class TreemapChartLayoutConfig(
    val widthPx: Float,
    val heightPx: Float,
    val contentPaddingPx: Float,
    val groupGapPx: Float,
    val tileGapPx: Float,
    val headerHeightPx: Float,
)

/**
 * Render-ready tile placement produced by [resolveTreemapChartLayout].
 */
data class TreemapItemLayout(
    val groupIndex: Int,
    val itemIndex: Int,
    val item: TreemapItem,
    val rect: TreemapRect,
    val fillColor: Int,
)

/**
 * Render-ready group header placement produced by [resolveTreemapChartLayout].
 */
data class TreemapGroupHeaderLayout(
    val groupIndex: Int,
    val group: TreemapGroup,
    val rect: TreemapRect,
    val fillColor: Int,
)

/**
 * Shared layout result for View and Compose Treemap renderers.
 */
data class TreemapChartLayoutResult(
    val itemLayouts: List<TreemapItemLayout>,
    val groupHeaders: List<TreemapGroupHeaderLayout>,
    val isRenderable: Boolean,
    val emptyReason: String? = null,
)

private data class WeightedItem<T>(
    val item: T,
    val weight: Float,
)

private data class LayoutBlock<T>(
    val item: T,
    val rect: TreemapRect,
)

private data class SanitizedGroup(
    val originalIndex: Int,
    val group: TreemapGroup,
    val resolvedGroupColor: Int,
    val totalWeight: Float,
    val items: List<SanitizedItem>,
)

private data class SanitizedItem(
    val originalIndex: Int,
    val item: TreemapItem,
    val resolvedColor: Int,
)

private const val HEADER_VISIBILITY_HEIGHT_MULTIPLIER = 1.5f

/**
 * Resolves validated item placements and optional group headers for a two-level Treemap chart.
 *
 * Items with blank labels, non-finite values, or non-positive values are dropped before layout.
 * Groups with blank labels or without at least one renderable item are also dropped.
 */
fun resolveTreemapChartLayout(
    groups: List<TreemapGroup>,
    config: TreemapChartLayoutConfig,
    style: TreemapChartStyleOptions = TreemapChartStyleOptions(),
    presentation: TreemapChartPresentationOptions = TreemapChartPresentationOptions(),
): TreemapChartLayoutResult {
    if (config.widthPx <= 0f || config.heightPx <= 0f) {
        return TreemapChartLayoutResult(
            itemLayouts = emptyList(),
            groupHeaders = emptyList(),
            isRenderable = false,
            emptyReason = "non-positive size",
        )
    }

    val sanitized = groups.mapIndexedNotNull { groupIndex, group ->
        val label = group.label.trim()
        if (label.isEmpty()) return@mapIndexedNotNull null

        val resolvedGroupColor = group.color ?: treemapFallbackColor(
            key = label,
            palette = style.fallbackItemColors,
        )
        val validItems = group.items.mapIndexedNotNull { itemIndex, item ->
            if (item.label.isBlank()) return@mapIndexedNotNull null
            if (!item.value.isFinite() || item.value <= 0.0) return@mapIndexedNotNull null

            SanitizedItem(
                originalIndex = itemIndex,
                item = item,
                resolvedColor = resolveTreemapItemColor(
                    groupColor = group.color,
                    item = item,
                    palette = style.fallbackItemColors,
                ),
            )
        }
        if (validItems.isEmpty()) return@mapIndexedNotNull null

        SanitizedGroup(
            originalIndex = groupIndex,
            group = group.copy(label = label, items = validItems.map { it.item }),
            resolvedGroupColor = resolvedGroupColor,
            totalWeight = validItems.sumOf { it.item.value }.toFloat().coerceAtLeast(0.01f),
            items = validItems,
        )
    }

    if (sanitized.isEmpty()) {
        return TreemapChartLayoutResult(
            itemLayouts = emptyList(),
            groupHeaders = emptyList(),
            isRenderable = false,
            emptyReason = "no valid groups",
        )
    }

    val rootRect = TreemapRect(
        left = config.contentPaddingPx,
        top = config.contentPaddingPx,
        right = config.widthPx - config.contentPaddingPx,
        bottom = config.heightPx - config.contentPaddingPx,
    )
    if (rootRect.width <= 0f || rootRect.height <= 0f) {
        return TreemapChartLayoutResult(
            itemLayouts = emptyList(),
            groupHeaders = emptyList(),
            isRenderable = false,
            emptyReason = "no drawable bounds",
        )
    }

    val itemLayouts = mutableListOf<TreemapItemLayout>()
    val groupHeaders = mutableListOf<TreemapGroupHeaderLayout>()
    val useHeaders = presentation.showGroupHeaders && sanitized.size > 1

    if (!useHeaders) {
        val flatItems = sanitized.flatMap { group ->
            group.items.map { item -> Triple(group.originalIndex, item.originalIndex, item) }
        }
        layoutSquarified(
            items = flatItems.map { WeightedItem(it, it.third.item.value.toFloat()) },
            bounds = rootRect,
        ).forEach { block ->
            val inset = insetRect(block.rect, config.tileGapPx)
            if (inset.width > 0f && inset.height > 0f) {
                itemLayouts.add(
                    TreemapItemLayout(
                        groupIndex = block.item.first,
                        itemIndex = block.item.second,
                        item = block.item.third.item,
                        rect = inset,
                        fillColor = block.item.third.resolvedColor,
                    ),
                )
            }
        }
        return TreemapChartLayoutResult(
            itemLayouts = itemLayouts,
            groupHeaders = groupHeaders,
            isRenderable = itemLayouts.isNotEmpty(),
            emptyReason = if (itemLayouts.isEmpty()) "no renderable items" else null,
        )
    }

    layoutSquarified(
        items = sanitized.map { WeightedItem(it, it.totalWeight) },
        bounds = rootRect,
    ).forEach { groupBlock ->
        val group = groupBlock.item
        val fullRect = insetRect(groupBlock.rect, config.groupGapPx)
        if (fullRect.width <= 0f || fullRect.height <= 0f) return@forEach

        val useHeaderForGroup = fullRect.height > config.headerHeightPx * HEADER_VISIBILITY_HEIGHT_MULTIPLIER
        val contentRect: TreemapRect
        if (useHeaderForGroup) {
            val headerRect = TreemapRect(
                left = fullRect.left,
                top = fullRect.top,
                right = fullRect.right,
                bottom = fullRect.top + config.headerHeightPx,
            )
            groupHeaders.add(
                TreemapGroupHeaderLayout(
                    groupIndex = group.originalIndex,
                    group = group.group,
                    rect = headerRect,
                    fillColor = withAlpha(group.resolvedGroupColor, style.headerFillAlpha),
                ),
            )
            contentRect = TreemapRect(
                left = fullRect.left,
                top = headerRect.bottom,
                right = fullRect.right,
                bottom = fullRect.bottom,
            )
        } else {
            contentRect = fullRect
        }

        layoutSquarified(
            items = group.items.map { item ->
                WeightedItem(
                    item = item,
                    weight = item.item.value.toFloat(),
                )
            },
            bounds = contentRect,
        ).forEach { itemBlock ->
            val inset = insetRect(itemBlock.rect, config.tileGapPx)
            if (inset.width > 0f && inset.height > 0f) {
                itemLayouts.add(
                    TreemapItemLayout(
                        groupIndex = group.originalIndex,
                        itemIndex = itemBlock.item.originalIndex,
                        item = itemBlock.item.item,
                        rect = inset,
                        fillColor = itemBlock.item.resolvedColor,
                    ),
                )
            }
        }
    }

    return TreemapChartLayoutResult(
        itemLayouts = itemLayouts,
        groupHeaders = groupHeaders,
        isRenderable = itemLayouts.isNotEmpty(),
        emptyReason = if (itemLayouts.isEmpty()) "no renderable items" else null,
    )
}

/**
 * Resolves the final fill color for an item.
 *
 * The precedence order is item color, derived group color, then deterministic fallback palette.
 */
fun resolveTreemapItemColor(
    groupColor: Int?,
    item: TreemapItem,
    palette: List<Int>,
): Int {
    item.color?.let { return it }
    groupColor?.let { return treemapDerivedItemColor(it, item.label) }
    return treemapFallbackColor(item.label, palette)
}

/**
 * Returns a deterministic fallback color for a label.
 */
fun treemapFallbackColor(key: String, palette: List<Int>): Int {
    val safePalette = if (palette.isNotEmpty()) palette else listOf(Color.parseColor("#2563EB"))
    val index = abs(key.hashCode()) % safePalette.size
    return safePalette[index]
}

/**
 * Derives an item color from its group's base color in a stable way.
 */
fun treemapDerivedItemColor(groupColor: Int, key: String): Int {
    val mixRatio = (((abs(key.hashCode()) % 4) + 1) * 0.08f).coerceIn(0.08f, 0.32f)
    return interpolateColor(groupColor, Color.WHITE, mixRatio)
}

private fun withAlpha(color: Int, alpha: Int): Int {
    return Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
}

private fun interpolateColor(from: Int, to: Int, t: Float): Int {
    val clamped = t.coerceIn(0f, 1f)
    val a = (Color.alpha(from) + (Color.alpha(to) - Color.alpha(from)) * clamped).toInt()
    val r = (Color.red(from) + (Color.red(to) - Color.red(from)) * clamped).toInt()
    val g = (Color.green(from) + (Color.green(to) - Color.green(from)) * clamped).toInt()
    val b = (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * clamped).toInt()
    return Color.argb(a, r, g, b)
}

private fun insetRect(src: TreemapRect, inset: Float): TreemapRect {
    return TreemapRect(
        left = src.left + inset,
        top = src.top + inset,
        right = src.right - inset,
        bottom = src.bottom - inset,
    )
}

private fun <T> layoutSquarified(
    items: List<WeightedItem<T>>,
    bounds: TreemapRect,
): List<LayoutBlock<T>> {
    if (items.isEmpty()) return emptyList()

    val totalWeight = items.sumOf { it.weight.toDouble() }.toFloat()
    if (totalWeight <= 0f || bounds.width <= 0f || bounds.height <= 0f) {
        return emptyList()
    }

    val area = bounds.width * bounds.height
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
        rect = bounds,
        outBlocks = result,
    )
    return result
}

private fun <T> squarify(
    remaining: MutableList<WeightedItem<T>>,
    currentRow: MutableList<WeightedItem<T>>,
    rect: TreemapRect,
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

    val width = min(rect.width, rect.height)
    val currentWorst = worstAspectRatio(currentRow, width)
    val newRow = ArrayList(currentRow)
    newRow.add(item)
    val newWorst = worstAspectRatio(newRow, width)

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

private fun <T> layoutRow(
    row: List<WeightedItem<T>>,
    rect: TreemapRect,
    outBlocks: MutableList<LayoutBlock<T>>,
): TreemapRect {
    val totalArea = row.sumOf { it.weight.toDouble() }.toFloat()
    val isHorizontal = rect.width >= rect.height

    return if (isHorizontal) {
        val rowHeight = totalArea / rect.width
        var x = rect.left
        val y = rect.top
        row.forEach { item ->
            val itemWidth = item.weight / rowHeight
            val blockRect = TreemapRect(
                left = x,
                top = y,
                right = min(x + itemWidth, rect.right),
                bottom = min(y + rowHeight, rect.bottom),
            )
            outBlocks.add(LayoutBlock(item.item, blockRect))
            x += itemWidth
        }
        TreemapRect(rect.left, y + rowHeight, rect.right, rect.bottom)
    } else {
        val columnWidth = totalArea / rect.height
        val x = rect.left
        var y = rect.top
        row.forEach { item ->
            val itemHeight = item.weight / columnWidth
            val blockRect = TreemapRect(
                left = x,
                top = y,
                right = min(x + columnWidth, rect.right),
                bottom = min(y + itemHeight, rect.bottom),
            )
            outBlocks.add(LayoutBlock(item.item, blockRect))
            y += itemHeight
        }
        TreemapRect(x + columnWidth, rect.top, rect.right, rect.bottom)
    }
}

private fun worstAspectRatio(
    row: List<WeightedItem<*>>,
    width: Float,
): Float {
    if (row.isEmpty() || width <= 0f) return Float.MAX_VALUE

    var worst = 0f
    val sumArea = row.sumOf { it.weight.toDouble() }.toFloat()
    val sideShort = min(width, sumArea / width)
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
