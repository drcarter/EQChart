package com.magimon.eq.compose.cycle

import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.magimon.eq.cycle.CycleChartLayoutConfig
import com.magimon.eq.cycle.CycleChartLayoutEngine
import com.magimon.eq.cycle.CycleChartLayoutResult
import com.magimon.eq.cycle.CycleChartPresentationOptions
import com.magimon.eq.cycle.CycleChartStyleOptions
import com.magimon.eq.cycle.CycleLinkLayout
import com.magimon.eq.cycle.CycleNode
import com.magimon.eq.cycle.CycleLink
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal data class CycleTapSelection(
    val selectedNodeIndex: Int? = null,
    val selectedLinkIndex: Int? = null,
)

internal data class CycleArrowHead(
    val tip: Offset,
    val left: Offset,
    val right: Offset,
)

internal fun computeCycleLayout(
    nodes: List<CycleNode>,
    links: List<CycleLink>,
    widthPx: Float,
    heightPx: Float,
    density: Density,
    styleOptions: CycleChartStyleOptions,
    presentationOptions: CycleChartPresentationOptions,
): CycleChartLayoutResult {
    return CycleChartLayoutEngine.compute(
        nodes = nodes,
        links = links,
        config = CycleChartLayoutConfig(
            widthPx = widthPx,
            heightPx = heightPx,
            contentPaddingPx = with(density) { styleOptions.contentPaddingDp.dp.toPx() },
            nodeRadiusPx = with(density) { styleOptions.nodeRadiusDp.dp.toPx() },
            linkMinThicknessPx = with(density) { styleOptions.linkMinThicknessDp.dp.toPx() },
            linkMaxThicknessPx = with(density) { styleOptions.linkMaxThicknessDp.dp.toPx() },
            linkInsetPx = with(density) { styleOptions.linkInsetDp.dp.toPx() },
            startAngleDeg = presentationOptions.startAngleDeg,
            clockwise = presentationOptions.clockwise,
            linkInnerRadiusFactor = presentationOptions.linkInnerRadiusFactor,
        ),
        styleOptions = styleOptions,
    )
}

internal fun resolveCycleTapSelection(
    layout: CycleChartLayoutResult,
    tap: Offset,
    linkHitTolerancePx: Float,
): CycleTapSelection {
    val nodeHit = CycleChartLayoutEngine.hitTestNode(layout, tap.x, tap.y)
    if (nodeHit != null) {
        return CycleTapSelection(selectedNodeIndex = nodeHit)
    }
    val linkHit = CycleChartLayoutEngine.hitTestLink(layout, tap.x, tap.y, linkHitTolerancePx)
    return if (linkHit != null) {
        CycleTapSelection(selectedLinkIndex = linkHit)
    } else {
        CycleTapSelection()
    }
}

internal fun cycleNodeHighlighted(
    nodeOriginalIndex: Int,
    layout: CycleChartLayoutResult,
    selectedNodeIndex: Int?,
    selectedLinkIndex: Int?,
): Boolean {
    if (selectedNodeIndex != null) return selectedNodeIndex == nodeOriginalIndex
    val linkIndex = selectedLinkIndex ?: return false
    val link = layout.linkLayouts.firstOrNull { it.originalIndex == linkIndex } ?: return false
    return link.sourceNodeOriginalIndex == nodeOriginalIndex || link.targetNodeOriginalIndex == nodeOriginalIndex
}

internal fun cycleLinkHighlighted(
    linkOriginalIndex: Int,
    layout: CycleChartLayoutResult,
    selectedNodeIndex: Int?,
    selectedLinkIndex: Int?,
): Boolean {
    if (selectedLinkIndex != null) return selectedLinkIndex == linkOriginalIndex
    val nodeIndex = selectedNodeIndex ?: return false
    val link = layout.linkLayouts.firstOrNull { it.originalIndex == linkOriginalIndex } ?: return false
    return link.sourceNodeOriginalIndex == nodeIndex || link.targetNodeOriginalIndex == nodeIndex
}

internal fun cycleNodeFillAlpha(
    nodeOriginalIndex: Int,
    layout: CycleChartLayoutResult,
    selectedNodeIndex: Int?,
    selectedLinkIndex: Int?,
    renderProgress: Float,
): Float {
    return when {
        cycleNodeHighlighted(nodeOriginalIndex, layout, selectedNodeIndex, selectedLinkIndex) -> renderProgress
        cycleHasSelection(selectedNodeIndex, selectedLinkIndex) -> 0.34f
        else -> renderProgress
    }
}

internal fun cycleNodeLabelAlpha(
    nodeOriginalIndex: Int,
    layout: CycleChartLayoutResult,
    selectedNodeIndex: Int?,
    selectedLinkIndex: Int?,
): Float {
    return when {
        cycleNodeHighlighted(nodeOriginalIndex, layout, selectedNodeIndex, selectedLinkIndex) -> 1f
        cycleHasSelection(selectedNodeIndex, selectedLinkIndex) -> 0.45f
        else -> 1f
    }
}

internal fun cycleLinkStrokeAlpha(
    linkOriginalIndex: Int,
    layout: CycleChartLayoutResult,
    presentationOptions: CycleChartPresentationOptions,
    selectedNodeIndex: Int?,
    selectedLinkIndex: Int?,
    renderProgress: Float,
): Float {
    return when {
        cycleLinkHighlighted(linkOriginalIndex, layout, selectedNodeIndex, selectedLinkIndex) -> renderProgress
        cycleHasSelection(selectedNodeIndex, selectedLinkIndex) -> 0.18f * renderProgress
        else -> presentationOptions.linkAlpha.coerceIn(0f, 1f) * renderProgress
    }
}

internal fun cycleLinkLabelAlpha(
    linkOriginalIndex: Int,
    layout: CycleChartLayoutResult,
    selectedNodeIndex: Int?,
    selectedLinkIndex: Int?,
): Float {
    return when {
        cycleLinkHighlighted(linkOriginalIndex, layout, selectedNodeIndex, selectedLinkIndex) -> 1f
        cycleHasSelection(selectedNodeIndex, selectedLinkIndex) -> 0.32f
        else -> 1f
    }
}

internal fun cycleLinkLabel(link: CycleLinkLayout): String {
    return link.link.label?.trim().orEmpty().ifBlank { cycleFormatValue(link.link.value) }
}

internal fun cycleQuadraticPoint(
    link: CycleLinkLayout,
    t: Float,
): Offset {
    val clamped = t.coerceIn(0f, 1f)
    val inv = 1f - clamped
    return Offset(
        x = (inv * inv * link.startX) + (2f * inv * clamped * link.controlX) + (clamped * clamped * link.endX),
        y = (inv * inv * link.startY) + (2f * inv * clamped * link.controlY) + (clamped * clamped * link.endY),
    )
}

internal fun cycleArrowHead(
    link: CycleLinkLayout,
    arrowSizePx: Float,
): CycleArrowHead? {
    if (arrowSizePx <= 0f) return null
    val tip = Offset(link.endX, link.endY)
    val tangent = tip - Offset(link.controlX, link.controlY)
    val length = sqrt((tangent.x * tangent.x) + (tangent.y * tangent.y))
    if (length <= 1e-4f) return null

    val dir = Offset(tangent.x / length, tangent.y / length)
    val perp = Offset(-dir.y, dir.x)
    val back = tip - (dir * arrowSizePx)
    val wing = arrowSizePx * 0.55f
    return CycleArrowHead(
        tip = tip,
        left = back + (perp * wing),
        right = back - (perp * wing),
    )
}

internal fun cycleApplyAlpha(
    color: Int,
    factor: Float,
): Int {
    val clamped = factor.coerceIn(0f, 1f)
    val baseAlpha = Color.alpha(color) / 255f
    val outputAlpha = (baseAlpha * clamped * 255f).roundToInt().coerceIn(0, 255)
    return Color.argb(outputAlpha, Color.red(color), Color.green(color), Color.blue(color))
}

private fun cycleHasSelection(
    selectedNodeIndex: Int?,
    selectedLinkIndex: Int?,
): Boolean {
    return selectedNodeIndex != null || selectedLinkIndex != null
}

private fun cycleFormatValue(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.roundToInt().toString()
    } else {
        String.format("%.1f", value)
    }
}
