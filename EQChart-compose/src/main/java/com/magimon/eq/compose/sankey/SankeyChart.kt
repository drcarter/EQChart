package com.magimon.eq.compose.sankey

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.magimon.eq.compose.internal.newTextPaint
import com.magimon.eq.compose.internal.toComposeColor
import com.magimon.eq.sankey.SankeyChartLayoutResult
import com.magimon.eq.sankey.SankeyChartPresentationOptions
import com.magimon.eq.sankey.SankeyChartStyleOptions
import com.magimon.eq.sankey.SankeyLink
import com.magimon.eq.sankey.SankeyLinkLayout
import com.magimon.eq.sankey.SankeyNode
import com.magimon.eq.sankey.SankeyNodeLayout
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Compose Sankey chart with node/link tap selection.
 *
 * This composable shares the same layout engine and validation rules as [SankeyChartView], so
 * View and Compose render the same graph structure consistently.
 *
 * Interaction behavior:
 * - tapping a node highlights that node and its connected links, then calls [onNodeClick]
 * - tapping a link highlights that link and its endpoint nodes, then calls [onLinkClick]
 * - tapping empty space clears the current selection
 *
 * @param nodes Full node list used by the graph
 * @param links Full link list used by the graph
 * @param modifier Standard Compose modifier for size, layout, and gestures
 * @param styleOptions Colors and dimensions for nodes, links, labels, and outlines
 * @param presentationOptions Layout spacing, label visibility, and animation behavior
 * @param onNodeClick Optional callback invoked as `(nodeIndex, node, payload)` when a node is tapped
 * @param onLinkClick Optional callback invoked as `(linkIndex, link, payload)` when a link is tapped
 */
@Composable
fun SankeyChart(
    nodes: List<SankeyNode>,
    links: List<SankeyLink>,
    modifier: Modifier = Modifier,
    styleOptions: SankeyChartStyleOptions = SankeyChartStyleOptions(),
    presentationOptions: SankeyChartPresentationOptions = SankeyChartPresentationOptions(),
    onNodeClick: ((nodeIndex: Int, node: SankeyNode, payload: Any?) -> Unit)? = null,
    onLinkClick: ((linkIndex: Int, link: SankeyLink, payload: Any?) -> Unit)? = null,
) {
    val density = LocalDensity.current
    var selectedNodeIndex by remember { mutableStateOf<Int?>(null) }
    var selectedLinkIndex by remember { mutableStateOf<Int?>(null) }
    val renderProgress = remember { Animatable(1f) }

    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val layout = remember(nodes, links, widthPx, heightPx, styleOptions, presentationOptions) {
            computeSankeyLayout(
                nodes = nodes,
                links = links,
                widthPx = widthPx,
                heightPx = heightPx,
                density = density,
                styleOptions = styleOptions,
                presentationOptions = presentationOptions,
            )
        }

        LaunchedEffect(layout.isRenderable, presentationOptions.animateOnDataChange, presentationOptions.animationDurationMs) {
            if (layout.isRenderable && presentationOptions.animateOnDataChange) {
                renderProgress.snapTo(0f)
                renderProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = presentationOptions.animationDurationMs.toInt().coerceAtLeast(0)),
                )
            } else {
                renderProgress.snapTo(1f)
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(layout) {
                    detectTapGestures { tap ->
                        val selection = resolveSankeyTapSelection(
                            layout = layout,
                            tap = tap,
                            linkHitTolerancePx = with(density) { 8.dp.toPx() },
                        )
                        if (selection.selectedNodeIndex != null) {
                            selectedNodeIndex = selection.selectedNodeIndex
                            selectedLinkIndex = null
                            nodes.getOrNull(selection.selectedNodeIndex)?.let {
                                onNodeClick?.invoke(selection.selectedNodeIndex, it, it.payload)
                            }
                            return@detectTapGestures
                        }

                        if (selection.selectedLinkIndex != null) {
                            selectedLinkIndex = selection.selectedLinkIndex
                            selectedNodeIndex = null
                            links.getOrNull(selection.selectedLinkIndex)?.let {
                                onLinkClick?.invoke(selection.selectedLinkIndex, it, it.payload)
                            }
                            return@detectTapGestures
                        }

                        selectedNodeIndex = null
                        selectedLinkIndex = null
                    }
                },
        ) {
            drawRect(styleOptions.backgroundColor.toComposeColor())

            if (!layout.isRenderable) {
                val text = presentationOptions.emptyText?.trim().orEmpty()
                if (text.isNotEmpty()) {
                    drawContext.canvas.nativeCanvas.drawText(
                        text,
                        size.width * 0.5f,
                        size.height * 0.5f,
                        newTextPaint(
                            color = styleOptions.nodeLabelTextColor,
                            textSizePx = with(density) { 13f.sp.toPx() },
                            align = Paint.Align.CENTER,
                        ),
                    )
                }
                return@Canvas
            }

            layout.linkLayouts.forEach { link ->
                drawSankeyLink(
                    link = link,
                    color = sankeyLinkColor(link, layout, presentationOptions, selectedNodeIndex, selectedLinkIndex, renderProgress.value),
                )
            }
            layout.nodeLayouts.forEach { node ->
                drawSankeyNode(
                    node = node,
                    layout = layout,
                    styleOptions = styleOptions,
                    densityPx = density.density,
                    renderProgress = renderProgress.value,
                    selectedNodeIndex = selectedNodeIndex,
                    selectedLinkIndex = selectedLinkIndex,
                )
            }
            if (presentationOptions.showNodeLabels) {
                val paint = newTextPaint(
                    color = styleOptions.nodeLabelTextColor,
                    textSizePx = with(density) { styleOptions.nodeLabelTextSizeSp.sp.toPx() },
                )
                layout.nodeLayouts.forEach { node ->
                    paint.textAlign = if (node.stage == max(0, layout.stageCount - 1)) Paint.Align.RIGHT else Paint.Align.LEFT
                    paint.color = withAlpha(styleOptions.nodeLabelTextColor, sankeyNodeLabelAlpha(node, layout, selectedNodeIndex, selectedLinkIndex, renderProgress.value))
                    val x = if (node.stage == max(0, layout.stageCount - 1)) node.left - with(density) { 8.dp.toPx() } else node.right + with(density) { 8.dp.toPx() }
                    val y = node.centerY - ((paint.descent() + paint.ascent()) * 0.5f)
                    drawContext.canvas.nativeCanvas.drawText(node.node.label, x, y, paint)
                }
            }
            if (presentationOptions.showLinkValues) {
                val paint = newTextPaint(
                    color = styleOptions.linkValueTextColor,
                    textSizePx = with(density) { styleOptions.linkValueTextSizeSp.sp.toPx() },
                    align = Paint.Align.CENTER,
                )
                layout.linkLayouts.forEach { link ->
                    if (link.thickness < with(density) { styleOptions.linkValueTextSizeSp.sp.toPx() }) return@forEach
                    paint.color = withAlpha(styleOptions.linkValueTextColor, sankeyLinkValueAlpha(link, selectedNodeIndex, selectedLinkIndex, layout, renderProgress.value))
                    val text = sankeyLinkLabel(link)
                    val x = (link.sourceRight + link.targetLeft) * 0.5f
                    val y = ((link.sourceCenterY + link.targetCenterY) * 0.5f) - ((paint.descent() + paint.ascent()) * 0.5f)
                    drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSankeyLink(
    link: SankeyLinkLayout,
    color: androidx.compose.ui.graphics.Color,
) {
    val startX = link.sourceRight
    val endX = link.targetLeft
    val controlOffset = (endX - startX) * 0.35f
    val path = Path().apply {
        moveTo(startX, link.sourceTop)
        cubicTo(startX + controlOffset, link.sourceTop, endX - controlOffset, link.targetTop, endX, link.targetTop)
        lineTo(endX, link.targetBottom)
        cubicTo(endX - controlOffset, link.targetBottom, startX + controlOffset, link.sourceBottom, startX, link.sourceBottom)
        close()
    }
    drawPath(path = path, color = color)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSankeyNode(
    node: SankeyNodeLayout,
    layout: SankeyChartLayoutResult,
    styleOptions: SankeyChartStyleOptions,
    densityPx: Float,
    renderProgress: Float,
    selectedNodeIndex: Int?,
    selectedLinkIndex: Int?,
) {
    val cornerRadiusPx = styleOptions.nodeCornerRadiusDp * densityPx
    drawRoundRect(
        color = withAlpha(node.node.color, sankeyNodeFillAlpha(node, layout, selectedNodeIndex, selectedLinkIndex, renderProgress)).toComposeColor(),
        topLeft = Offset(node.left, node.top),
        size = androidx.compose.ui.geometry.Size(node.right - node.left, node.bottom - node.top),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx),
    )

    val highlighted = sankeyNodeHighlighted(node.originalIndex, layout, selectedNodeIndex, selectedLinkIndex)
    if (highlighted || styleOptions.nodeStrokeWidthDp > 0f) {
        drawRoundRect(
            color = (if (highlighted) styleOptions.selectedStrokeColor else styleOptions.nodeStrokeColor).let {
                withAlpha(it, if (highlighted) renderProgress else renderProgress * 0.9f).toComposeColor()
            },
            topLeft = Offset(node.left, node.top),
            size = androidx.compose.ui.geometry.Size(node.right - node.left, node.bottom - node.top),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx),
            style = Stroke(width = (if (highlighted) styleOptions.selectedStrokeWidthDp else styleOptions.nodeStrokeWidthDp) * densityPx),
        )
    }
}
