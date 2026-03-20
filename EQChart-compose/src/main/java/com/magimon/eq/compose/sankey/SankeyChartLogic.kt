package com.magimon.eq.compose.sankey

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.magimon.eq.compose.internal.toComposeColor
import com.magimon.eq.sankey.SankeyChartLayoutConfig
import com.magimon.eq.sankey.SankeyChartLayoutEngine
import com.magimon.eq.sankey.SankeyChartLayoutResult
import com.magimon.eq.sankey.SankeyChartPresentationOptions
import com.magimon.eq.sankey.SankeyChartStyleOptions
import com.magimon.eq.sankey.SankeyLink
import com.magimon.eq.sankey.SankeyLinkLayout
import com.magimon.eq.sankey.SankeyNode
import com.magimon.eq.sankey.SankeyNodeLayout
import kotlin.math.roundToInt

internal data class SankeyTapSelection(
    val selectedNodeIndex: Int? = null,
    val selectedLinkIndex: Int? = null,
)

internal fun computeSankeyLayout(
    nodes: List<SankeyNode>,
    links: List<SankeyLink>,
    widthPx: Float,
    heightPx: Float,
    density: Density,
    styleOptions: SankeyChartStyleOptions,
    presentationOptions: SankeyChartPresentationOptions,
): SankeyChartLayoutResult {
    return SankeyChartLayoutEngine.compute(
        nodes = nodes,
        links = links,
        config = SankeyChartLayoutConfig(
            widthPx = widthPx,
            heightPx = heightPx,
            contentPaddingPx = with(density) { styleOptions.contentPaddingDp.dp.toPx() },
            nodeWidthPx = with(density) { styleOptions.nodeWidthDp.dp.toPx() },
            nodeMinHeightPx = with(density) { styleOptions.nodeMinHeightDp.dp.toPx() },
            nodeGapPx = with(density) { presentationOptions.nodeGapDp.dp.toPx() },
            columnGapPx = with(density) { presentationOptions.columnGapDp.dp.toPx() },
        ),
        styleOptions = styleOptions,
    )
}

internal fun resolveSankeyTapSelection(
    layout: SankeyChartLayoutResult,
    tap: Offset,
    linkHitTolerancePx: Float,
): SankeyTapSelection {
    val nodeHit = SankeyChartLayoutEngine.hitTestNode(layout, tap.x, tap.y)
    if (nodeHit != null) {
        return SankeyTapSelection(selectedNodeIndex = nodeHit)
    }

    val linkHit = SankeyChartLayoutEngine.hitTestLink(layout, tap.x, tap.y, linkHitTolerancePx)
    if (linkHit != null) {
        return SankeyTapSelection(selectedLinkIndex = linkHit)
    }

    return SankeyTapSelection()
}

internal fun sankeyLinkCenterPoint(
    link: SankeyLinkLayout,
    t: Float = 0.5f,
): Offset {
    val progress = t.coerceIn(0f, 1f)
    val startX = link.sourceRight
    val endX = link.targetLeft
    val controlOffset = (endX - startX) * 0.35f
    val x = cubicInterpolate(
        p0 = startX,
        p1 = startX + controlOffset,
        p2 = endX - controlOffset,
        p3 = endX,
        t = progress,
    )
    val y = cubicInterpolate(
        p0 = link.sourceCenterY,
        p1 = link.sourceCenterY,
        p2 = link.targetCenterY,
        p3 = link.targetCenterY,
        t = progress,
    )
    return Offset(x, y)
}

internal fun sankeyLinkLabel(link: SankeyLinkLayout): String {
    return link.link.label?.takeIf { it.isNotBlank() } ?: sankeyFormatValue(link.link.value)
}

internal fun sankeyLinkColor(
    link: SankeyLinkLayout,
    layout: SankeyChartLayoutResult,
    presentationOptions: SankeyChartPresentationOptions,
    selectedNodeIndex: Int?,
    selectedLinkIndex: Int?,
    renderProgress: Float,
): Color {
    val hasSelection = selectedNodeIndex != null || selectedLinkIndex != null
    val alpha = when {
        sankeyLinkHighlighted(link.originalIndex, layout, selectedNodeIndex, selectedLinkIndex) -> 0.92f * renderProgress
        hasSelection -> 0.16f * renderProgress
        else -> presentationOptions.linkAlpha.coerceIn(0f, 1f) * renderProgress
    }
    return withAlpha(link.color, alpha).toComposeColor()
}

internal fun sankeyNodeFillAlpha(
    node: SankeyNodeLayout,
    layout: SankeyChartLayoutResult,
    selectedNodeIndex: Int?,
    selectedLinkIndex: Int?,
    renderProgress: Float,
): Float {
    val hasSelection = selectedNodeIndex != null || selectedLinkIndex != null
    return when {
        sankeyNodeHighlighted(node.originalIndex, layout, selectedNodeIndex, selectedLinkIndex) -> renderProgress
        hasSelection -> 0.36f * renderProgress
        else -> renderProgress
    }
}

internal fun sankeyNodeLabelAlpha(
    node: SankeyNodeLayout,
    layout: SankeyChartLayoutResult,
    selectedNodeIndex: Int?,
    selectedLinkIndex: Int?,
    renderProgress: Float,
): Float {
    val hasSelection = selectedNodeIndex != null || selectedLinkIndex != null
    return when {
        sankeyNodeHighlighted(node.originalIndex, layout, selectedNodeIndex, selectedLinkIndex) -> renderProgress
        hasSelection -> 0.4f * renderProgress
        else -> renderProgress
    }
}

internal fun sankeyLinkValueAlpha(
    link: SankeyLinkLayout,
    selectedNodeIndex: Int?,
    selectedLinkIndex: Int?,
    layout: SankeyChartLayoutResult,
    renderProgress: Float,
): Float {
    val hasSelection = selectedNodeIndex != null || selectedLinkIndex != null
    return when {
        sankeyLinkHighlighted(link.originalIndex, layout, selectedNodeIndex, selectedLinkIndex) -> renderProgress
        hasSelection -> 0.24f * renderProgress
        else -> 0.72f * renderProgress
    }
}

internal fun sankeyNodeHighlighted(
    nodeOriginalIndex: Int,
    layout: SankeyChartLayoutResult,
    selectedNodeIndex: Int?,
    selectedLinkIndex: Int?,
): Boolean {
    val node = selectedNodeIndex
    if (node != null) return nodeOriginalIndex == node

    val linkIndex = selectedLinkIndex ?: return false
    val link = layout.linkLayouts.firstOrNull { it.originalIndex == linkIndex } ?: return false
    return link.sourceNodeOriginalIndex == nodeOriginalIndex || link.targetNodeOriginalIndex == nodeOriginalIndex
}

internal fun sankeyLinkHighlighted(
    linkOriginalIndex: Int,
    layout: SankeyChartLayoutResult,
    selectedNodeIndex: Int?,
    selectedLinkIndex: Int?,
): Boolean {
    val node = selectedNodeIndex
    if (node != null) {
        return layout.linkLayouts.any {
            it.originalIndex == linkOriginalIndex &&
                (it.sourceNodeOriginalIndex == node || it.targetNodeOriginalIndex == node)
        }
    }
    return selectedLinkIndex == linkOriginalIndex
}

internal fun withAlpha(color: Int, alphaFraction: Float): Int {
    val alpha = (android.graphics.Color.alpha(color) * alphaFraction.coerceIn(0f, 1f)).roundToInt().coerceIn(0, 255)
    return android.graphics.Color.argb(alpha, android.graphics.Color.red(color), android.graphics.Color.green(color), android.graphics.Color.blue(color))
}

internal fun sankeyFormatValue(value: Double): String {
    return if (value % 1.0 == 0.0) value.roundToInt().toString() else String.format("%.1f", value)
}

private fun cubicInterpolate(
    p0: Float,
    p1: Float,
    p2: Float,
    p3: Float,
    t: Float,
): Float {
    val oneMinusT = 1f - t
    return (oneMinusT * oneMinusT * oneMinusT * p0) +
        (3f * oneMinusT * oneMinusT * t * p1) +
        (3f * oneMinusT * t * t * p2) +
        (t * t * t * p3)
}
