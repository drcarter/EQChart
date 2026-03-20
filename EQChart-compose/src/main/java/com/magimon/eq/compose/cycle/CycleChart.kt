package com.magimon.eq.compose.cycle

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
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
import com.magimon.eq.cycle.CycleChartLayoutResult
import com.magimon.eq.cycle.CycleChartPresentationOptions
import com.magimon.eq.cycle.CycleChartStyleOptions
import com.magimon.eq.cycle.CycleLink
import com.magimon.eq.cycle.CycleLinkLayout
import com.magimon.eq.cycle.CycleNode
import com.magimon.eq.cycle.CycleNodeLayout
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Compose cycle diagram chart with node/link tap selection.
 *
 * This composable shares the same layout engine and validation rules as the View implementation,
 * so both UI stacks render the same ring geometry and selection behavior.
 *
 * @param nodes Full node list arranged around the ring in input order
 * @param links Directed links rendered as curved connectors inside the ring
 * @param modifier Standard Compose modifier for layout and gestures
 * @param styleOptions Colors and dimensions for nodes, links, labels, and outlines
 * @param presentationOptions Layout spacing, label visibility, and animation behavior
 * @param onNodeClick Optional callback invoked as `(nodeIndex, node, payload)` when a node is tapped
 * @param onLinkClick Optional callback invoked as `(linkIndex, link, payload)` when a link is tapped
 */
@Composable
fun CycleChart(
    nodes: List<CycleNode>,
    links: List<CycleLink>,
    modifier: Modifier = Modifier,
    styleOptions: CycleChartStyleOptions = CycleChartStyleOptions(),
    presentationOptions: CycleChartPresentationOptions = CycleChartPresentationOptions(),
    onNodeClick: ((nodeIndex: Int, node: CycleNode, payload: Any?) -> Unit)? = null,
    onLinkClick: ((linkIndex: Int, link: CycleLink, payload: Any?) -> Unit)? = null,
) {
    val density = LocalDensity.current
    var selectedNodeIndex by remember { mutableStateOf<Int?>(null) }
    var selectedLinkIndex by remember { mutableStateOf<Int?>(null) }
    val renderProgress = remember { Animatable(1f) }

    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val layout = remember(nodes, links, widthPx, heightPx, styleOptions, presentationOptions) {
            computeCycleLayout(
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
                .pointerInput(layout, styleOptions.linkMaxThicknessDp) {
                    detectTapGestures { tap ->
                        val selection = resolveCycleTapSelection(
                            layout = layout,
                            tap = tap,
                            linkHitTolerancePx = max(10.dp.toPx(), (styleOptions.linkMaxThicknessDp.dp.toPx() * 0.5f)),
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
                            selectedNodeIndex = null
                            selectedLinkIndex = selection.selectedLinkIndex
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
                            textSizePx = 13f.sp.toPx(),
                            align = Paint.Align.CENTER,
                        ),
                    )
                }
                return@Canvas
            }

            layout.linkLayouts.forEach { link ->
                drawCycleLink(
                    link = link,
                    layout = layout,
                    styleOptions = styleOptions,
                    presentationOptions = presentationOptions,
                    renderProgress = renderProgress.value,
                    selectedNodeIndex = selectedNodeIndex,
                    selectedLinkIndex = selectedLinkIndex,
                )
            }
            layout.nodeLayouts.forEach { node ->
                drawCycleNode(
                    node = node,
                    layout = layout,
                    styleOptions = styleOptions,
                    renderProgress = renderProgress.value,
                    selectedNodeIndex = selectedNodeIndex,
                    selectedLinkIndex = selectedLinkIndex,
                )
            }

            if (presentationOptions.showNodeLabels) {
                val paint = newTextPaint(
                    color = styleOptions.nodeLabelTextColor,
                    textSizePx = styleOptions.nodeLabelTextSizeSp.sp.toPx(),
                )
                layout.nodeLayouts.forEach { node ->
                    val directionX = node.centerX - layout.centerX
                    val directionY = node.centerY - layout.centerY
                    val length = sqrt((directionX * directionX) + (directionY * directionY)).coerceAtLeast(1f)
                    val unitX = directionX / length
                    val unitY = directionY / length
                    val offset = node.radius + 14.dp.toPx()
                    val x = node.centerX + (unitX * offset)
                    val y = node.centerY + (unitY * offset) - ((paint.descent() + paint.ascent()) * 0.5f)
                    paint.textAlign = when {
                        abs(unitX) < 0.18f -> Paint.Align.CENTER
                        unitX > 0f -> Paint.Align.LEFT
                        else -> Paint.Align.RIGHT
                    }
                    paint.color = cycleApplyAlpha(
                        styleOptions.nodeLabelTextColor,
                        cycleNodeLabelAlpha(node.originalIndex, layout, selectedNodeIndex, selectedLinkIndex),
                    )
                    drawContext.canvas.nativeCanvas.drawText(node.node.label, x, y, paint)
                }
            }

            if (presentationOptions.showLinkLabels) {
                val paint = newTextPaint(
                    color = styleOptions.linkLabelTextColor,
                    textSizePx = styleOptions.linkLabelTextSizeSp.sp.toPx(),
                    align = Paint.Align.CENTER,
                )
                layout.linkLayouts.forEach { link ->
                    paint.color = cycleApplyAlpha(
                        styleOptions.linkLabelTextColor,
                        cycleLinkLabelAlpha(link.originalIndex, layout, selectedNodeIndex, selectedLinkIndex),
                    )
                    val baselineY = link.midY - ((paint.descent() + paint.ascent()) * 0.5f)
                    drawContext.canvas.nativeCanvas.drawText(cycleLinkLabel(link), link.midX, baselineY, paint)
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCycleLink(
    link: CycleLinkLayout,
    layout: CycleChartLayoutResult,
    styleOptions: CycleChartStyleOptions,
    presentationOptions: CycleChartPresentationOptions,
    renderProgress: Float,
    selectedNodeIndex: Int?,
    selectedLinkIndex: Int?,
) {
    val highlighted = cycleLinkHighlighted(link.originalIndex, layout, selectedNodeIndex, selectedLinkIndex)
    val strokeWidth = if (highlighted) {
        max(link.thickness, styleOptions.selectedStrokeWidthDp.dp.toPx())
    } else {
        link.thickness
    }
    val alpha = cycleLinkStrokeAlpha(
        linkOriginalIndex = link.originalIndex,
        layout = layout,
        presentationOptions = presentationOptions,
        selectedNodeIndex = selectedNodeIndex,
        selectedLinkIndex = selectedLinkIndex,
        renderProgress = renderProgress,
    )
    val path = Path().apply {
        moveTo(link.startX, link.startY)
        quadraticTo(link.controlX, link.controlY, link.endX, link.endY)
    }
    drawPath(
        path = path,
        color = cycleApplyAlpha(link.color, alpha).toComposeColor(),
        style = Stroke(width = strokeWidth),
    )

    cycleArrowHead(link, styleOptions.linkArrowSizeDp.dp.toPx())?.let { arrow ->
        val arrowPath = Path().apply {
            moveTo(arrow.tip.x, arrow.tip.y)
            lineTo(arrow.left.x, arrow.left.y)
            lineTo(arrow.right.x, arrow.right.y)
            close()
        }
        val arrowColor = if (highlighted) styleOptions.selectedStrokeColor else link.color
        drawPath(
            path = arrowPath,
            color = cycleApplyAlpha(arrowColor, alpha).toComposeColor(),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCycleNode(
    node: CycleNodeLayout,
    layout: CycleChartLayoutResult,
    styleOptions: CycleChartStyleOptions,
    renderProgress: Float,
    selectedNodeIndex: Int?,
    selectedLinkIndex: Int?,
) {
    val highlighted = cycleNodeHighlighted(node.originalIndex, layout, selectedNodeIndex, selectedLinkIndex)
    drawCircle(
        color = cycleApplyAlpha(
            node.node.color,
            cycleNodeFillAlpha(node.originalIndex, layout, selectedNodeIndex, selectedLinkIndex, renderProgress),
        ).toComposeColor(),
        radius = node.radius,
        center = Offset(node.centerX, node.centerY),
    )

    drawCircle(
        color = (if (highlighted) styleOptions.selectedStrokeColor else styleOptions.nodeStrokeColor).toComposeColor(),
        radius = node.radius,
        center = Offset(node.centerX, node.centerY),
        style = Stroke(
            width = if (highlighted) {
                styleOptions.selectedStrokeWidthDp.dp.toPx()
            } else {
                styleOptions.nodeStrokeWidthDp.dp.toPx()
            },
        ),
    )
}
