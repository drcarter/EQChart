package com.magimon.eq.compose.heatmap

import android.graphics.Color
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.magimon.eq.compose.treemap.TreemapChart
import com.magimon.eq.compose.treemap.computeTreemapLayout
import com.magimon.eq.heatmap.StockHeatmapItem
import com.magimon.eq.heatmap.StockHeatmapSection
import com.magimon.eq.treemap.TreemapChartPresentationOptions
import com.magimon.eq.treemap.TreemapChartStyleOptions
import com.magimon.eq.treemap.TreemapGroup
import com.magimon.eq.treemap.TreemapItem
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal data class HeatmapBlockRect(
    val item: StockHeatmapItem,
    val rect: RectF,
)

internal data class HeatmapSectionGroup(
    val name: String,
    val color: Int,
    val totalWeight: Float,
    val items: List<StockHeatmapItem>,
)

internal data class HeatmapSectionLayout(
    val group: HeatmapSectionGroup,
    val headerRect: RectF,
)

internal data class HeatmapWeightedItem<T>(
    val item: T,
    val weight: Float,
)

internal data class HeatmapLayoutBlock<T>(
    val item: T,
    val rect: RectF,
)

internal data class HeatmapComputed(
    val blocks: List<HeatmapBlockRect>,
    val sectionHeaders: List<HeatmapSectionLayout>,
)

/**
 * Stock heatmap composable that renders explicit section groups.
 *
 * Sections with blank names or without positive-weight items are ignored before layout.
 *
 * @param sections Grouped heatmap data to render
 * @param modifier Standard Compose modifier for layout and gestures
 * @param onItemClick Optional callback invoked with the tapped stock item
 */
@Composable
fun StockHeatmapChart(
    sections: List<StockHeatmapSection>,
    modifier: Modifier = Modifier,
    onItemClick: ((StockHeatmapItem) -> Unit)? = null,
) {
    val groups = remember(sections) { heatmapSectionsToTreemapGroups(sections) }
    TreemapChart(
        groups = groups,
        modifier = modifier,
        onItemClick = { item ->
            val original = item.payload as? StockHeatmapItem
            if (original != null) {
                onItemClick?.invoke(original)
            }
        },
    )
}

/**
 * Convenience heatmap composable that groups a flat item list by `sector`.
 *
 * Sector colors are resolved through the same helper mapping used by the View-based heatmap.
 *
 * @param items Flat stock item list to group and render
 * @param modifier Standard Compose modifier for layout and gestures
 * @param onItemClick Optional callback invoked with the tapped stock item
 */
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

    StockHeatmapChart(sections = sections, modifier = modifier, onItemClick = onItemClick)
}

/**
 * Resolves all section/header/item rectangles for the heatmap from grouped stock data.
 */
internal fun computeHeatmapLayout(
    sections: List<StockHeatmapSection>,
    width: Float,
    height: Float,
    density: Float,
): HeatmapComputed {
    val treemapGroups = heatmapSectionsToTreemapGroups(sections)
    val result = computeTreemapLayout(
        groups = treemapGroups,
        widthPx = width,
        heightPx = height,
        density = density,
        styleOptions = TreemapChartStyleOptions(),
        presentationOptions = TreemapChartPresentationOptions(),
    )
    return HeatmapComputed(
        blocks = result.itemLayouts.mapNotNull { layout ->
            val item = layout.item.payload as? StockHeatmapItem ?: return@mapNotNull null
            HeatmapBlockRect(
                item = item,
                rect = RectF(layout.rect.left, layout.rect.top, layout.rect.right, layout.rect.bottom),
            )
        },
        sectionHeaders = result.groupHeaders.map { header ->
            val stocks = sections.getOrNull(header.groupIndex)?.stocks.orEmpty()
            HeatmapSectionLayout(
                group = HeatmapSectionGroup(
                    name = header.group.label,
                    color = header.fillColor,
                    totalWeight = stocks.sumOf { heatmapItemWeight(it).toDouble() }.toFloat(),
                    items = stocks,
                ),
                headerRect = RectF(header.rect.left, header.rect.top, header.rect.right, header.rect.bottom),
            )
        },
    )
}

internal fun heatmapItemWeight(item: StockHeatmapItem): Float {
    val ratio = item.sizeRatio ?: 0.0
    return if (ratio > 0.0) ratio.toFloat() else item.marketCap.toFloat().coerceAtLeast(0f)
}

internal fun heatmapMapSectorToColor(sector: String): Int {
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

internal fun heatmapMapChangeToColor(changePct: Double): Int {
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

internal fun heatmapFormatChange(changePct: Double): String = String.format("%+.2f%%", changePct)

private fun heatmapInterpolateColor(from: Int, to: Int, t: Float): Int {
    val clamped = t.coerceIn(0f, 1f)
    val a = (Color.alpha(from) + (Color.alpha(to) - Color.alpha(from)) * clamped).toInt()
    val r = (Color.red(from) + (Color.red(to) - Color.red(from)) * clamped).toInt()
    val g = (Color.green(from) + (Color.green(to) - Color.green(from)) * clamped).toInt()
    val b = (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * clamped).toInt()
    return Color.argb(a, r, g, b)
}

private fun heatmapSectionsToTreemapGroups(sections: List<StockHeatmapSection>): List<TreemapGroup> {
    return sections.map { section ->
        TreemapGroup(
            label = section.name,
            color = section.color,
            items = section.stocks.map { item ->
                TreemapItem(
                    label = item.symbol,
                    value = heatmapItemWeight(item).toDouble(),
                    color = heatmapMapChangeToColor(item.changePct),
                    supportingText = heatmapFormatChange(item.changePct),
                    payload = item,
                )
            },
        )
    }
}
