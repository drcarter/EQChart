package com.magimon.eq.sankey

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Pixel-based layout inputs consumed by [SankeyChartLayoutEngine].
 *
 * Public callers typically derive this from the chart size and dp-based style/presentation
 * options before invoking [SankeyChartLayoutEngine.compute].
 */
data class SankeyChartLayoutConfig(
    val widthPx: Float,
    val heightPx: Float,
    val contentPaddingPx: Float,
    val nodeWidthPx: Float,
    val nodeMinHeightPx: Float,
    val nodeGapPx: Float,
    val columnGapPx: Float,
)

/**
 * Final node geometry produced by [SankeyChartLayoutEngine.compute].
 *
 * Coordinates are expressed in pixels relative to the chart canvas.
 */
data class SankeyNodeLayout(
    val originalIndex: Int,
    val node: SankeyNode,
    val stage: Int,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val totalValue: Double,
) {
    val centerY: Float
        get() = (top + bottom) * 0.5f
}

/**
 * Final link geometry produced by [SankeyChartLayoutEngine.compute].
 *
 * Each link already contains the allocated vertical band on both source and target nodes, so
 * renderers can draw a smooth ribbon without recomputing thickness distribution.
 */
data class SankeyLinkLayout(
    val originalIndex: Int,
    val link: SankeyLink,
    val sourceNodeOriginalIndex: Int,
    val targetNodeOriginalIndex: Int,
    val sourceLeft: Float,
    val sourceRight: Float,
    val targetLeft: Float,
    val targetRight: Float,
    val sourceTop: Float,
    val sourceBottom: Float,
    val targetTop: Float,
    val targetBottom: Float,
    val thickness: Float,
    val color: Int,
) {
    val sourceCenterY: Float
        get() = (sourceTop + sourceBottom) * 0.5f

    val targetCenterY: Float
        get() = (targetTop + targetBottom) * 0.5f
}

/**
 * Complete layout result for a Sankey chart.
 *
 * When [isRenderable] is `false`, callers should render an empty/fallback state instead of
 * attempting partial drawing.
 */
data class SankeyChartLayoutResult(
    val nodeLayouts: List<SankeyNodeLayout>,
    val linkLayouts: List<SankeyLinkLayout>,
    val stageCount: Int,
    val isRenderable: Boolean,
    val emptyReason: String? = null,
)

/**
 * Shared Sankey graph layout and hit-test engine.
 *
 * The engine validates node/link input, infers stages for nodes without explicit stages, rejects
 * cycles, computes per-stage node rectangles, allocates link thickness within each node, and
 * exposes hit-testing helpers for node/link selection.
 *
 * View and Compose implementations both depend on this engine so they stay behaviorally aligned.
 */
object SankeyChartLayoutEngine {

    private data class MutableNode(
        val originalIndex: Int,
        val node: SankeyNode,
        var stage: Int,
        val totalValue: Double,
        var top: Float = 0f,
        var bottom: Float = 0f,
        var left: Float = 0f,
        var right: Float = 0f,
    ) {
        val height: Float
            get() = bottom - top

        val centerY: Float
            get() = (top + bottom) * 0.5f
    }

    private data class MutableLink(
        val originalIndex: Int,
        val link: SankeyLink,
        val sourceIndex: Int,
        val targetIndex: Int,
        val value: Double,
        val color: Int,
        var sourceTop: Float = 0f,
        var sourceBottom: Float = 0f,
        var targetTop: Float = 0f,
        var targetBottom: Float = 0f,
    ) {
        val thickness: Float
            get() = sourceBottom - sourceTop
    }

    /**
     * Computes a full Sankey layout from node/link input and pixel-based layout constraints.
     *
     * Invalid links are ignored, while invalid graph structures such as cycles or backward stage
     * assignments result in a non-renderable [SankeyChartLayoutResult].
     */
    fun compute(
        nodes: List<SankeyNode>,
        links: List<SankeyLink>,
        config: SankeyChartLayoutConfig,
        styleOptions: SankeyChartStyleOptions,
    ): SankeyChartLayoutResult {
        if (config.widthPx <= 0f || config.heightPx <= 0f) {
            return invalid("non-positive size")
        }
        if (config.nodeWidthPx <= 0f) {
            return invalid("node width")
        }

        val trimmedIds = nodes.map { it.id.trim() }
        if (trimmedIds.any { it.isEmpty() }) {
            return invalid("blank node id")
        }
        if (trimmedIds.distinct().size != trimmedIds.size) {
            return invalid("duplicate node id")
        }
        if (nodes.any { (it.stage ?: 0) < 0 }) {
            return invalid("negative stage")
        }

        val nodeIndexById = trimmedIds.withIndex().associate { it.value to it.index }
        val validLinksWithIndex = links.mapIndexedNotNull { index, link ->
            val sourceIndex = nodeIndexById[link.sourceId.trim()] ?: return@mapIndexedNotNull null
            val targetIndex = nodeIndexById[link.targetId.trim()] ?: return@mapIndexedNotNull null
            if (!link.value.isFinite() || link.value <= 0.0) return@mapIndexedNotNull null
            Triple(index, sourceIndex, targetIndex)
        }
        if (validLinksWithIndex.isEmpty()) {
            return invalid("no valid links")
        }

        val usedNodeIndexes = linkedSetOf<Int>().apply {
            validLinksWithIndex.forEach { (_, sourceIndex, targetIndex) ->
                add(sourceIndex)
                add(targetIndex)
            }
        }.toList()
        val remappedByOriginal = usedNodeIndexes.withIndex().associate { it.value to it.index }

        val compactNodes = usedNodeIndexes.map { originalIndex ->
            val node = nodes[originalIndex]
            MutableNode(
                originalIndex = originalIndex,
                node = node,
                stage = node.stage ?: 0,
                totalValue = 0.0,
            )
        }.toMutableList()

        val compactLinks = validLinksWithIndex.map { (originalLinkIndex, originalSourceIndex, originalTargetIndex) ->
            val sourceIndex = remappedByOriginal.getValue(originalSourceIndex)
            val targetIndex = remappedByOriginal.getValue(originalTargetIndex)
            MutableLink(
                originalIndex = originalLinkIndex,
                link = links[originalLinkIndex],
                sourceIndex = sourceIndex,
                targetIndex = targetIndex,
                value = links[originalLinkIndex].value,
                color = links[originalLinkIndex].color ?: nodes[originalSourceIndex].color,
            )
        }.toMutableList()

        val indegree = IntArray(compactNodes.size)
        val outgoing = Array(compactNodes.size) { mutableListOf<Int>() }
        val incoming = Array(compactNodes.size) { mutableListOf<Int>() }
        compactLinks.forEachIndexed { linkIndex, link ->
            if (link.sourceIndex == link.targetIndex) {
                return invalid("self cycle")
            }
            indegree[link.targetIndex] += 1
            outgoing[link.sourceIndex].add(linkIndex)
            incoming[link.targetIndex].add(linkIndex)
        }

        val topo = mutableListOf<Int>()
        val queue = ArrayDeque<Int>()
        indegree.forEachIndexed { index, value ->
            if (value == 0) queue.add(index)
        }
        while (queue.isNotEmpty()) {
            val next = queue.removeFirst()
            topo.add(next)
            outgoing[next].forEach { linkIndex ->
                val target = compactLinks[linkIndex].targetIndex
                indegree[target] -= 1
                if (indegree[target] == 0) {
                    queue.add(target)
                }
            }
        }
        if (topo.size != compactNodes.size) {
            return invalid("cycle")
        }

        topo.forEach { nodeIndex ->
            outgoing[nodeIndex].forEach { linkIndex ->
                val link = compactLinks[linkIndex]
                val sourceStage = compactNodes[nodeIndex].stage
                val target = compactNodes[link.targetIndex]
                val requiredStage = sourceStage + 1
                if (target.node.stage != null) {
                    if (target.node.stage < requiredStage) {
                        return invalid("backward explicit stage")
                    }
                    target.stage = target.node.stage
                } else if (target.stage < requiredStage) {
                    target.stage = requiredStage
                }
            }
        }

        compactLinks.forEach { link ->
            if (compactNodes[link.targetIndex].stage <= compactNodes[link.sourceIndex].stage) {
                return invalid("backward link")
            }
        }

        val incomingTotals = DoubleArray(compactNodes.size)
        val outgoingTotals = DoubleArray(compactNodes.size)
        compactLinks.forEach { link ->
            outgoingTotals[link.sourceIndex] += link.value
            incomingTotals[link.targetIndex] += link.value
        }
        compactNodes.forEachIndexed { index, node ->
            compactNodes[index] = node.copy(totalValue = max(incomingTotals[index], outgoingTotals[index]))
        }

        val stageGroups = compactNodes.indices.groupBy { compactNodes[it].stage }.toSortedMap()
        val stageCount = (stageGroups.keys.maxOrNull() ?: 0) + 1

        val availableHeight = config.heightPx - (config.contentPaddingPx * 2f)
        val availableWidth = config.widthPx - (config.contentPaddingPx * 2f)
        if (availableHeight <= 0f || availableWidth <= 0f) {
            return invalid("insufficient size")
        }

        val valueScale = stageGroups.values.map { indices ->
            val totalValue = indices.sumOf { compactNodes[it].totalValue }
            val gaps = config.nodeGapPx * max(0, indices.size - 1)
            val usable = availableHeight - gaps
            if (totalValue <= 0.0 || usable <= 0f) 0f else (usable / totalValue).toFloat()
        }.minOrNull()?.takeIf { it > 0f } ?: return invalid("invalid scale")

        val stageHeights = mutableMapOf<Int, FloatArray>()
        stageGroups.forEach { (stage, indices) ->
            stageHeights[stage] = fitHeightsToStage(
                values = indices.map { compactNodes[it].totalValue },
                valueScale = valueScale,
                minHeightPx = config.nodeMinHeightPx,
                gapPx = config.nodeGapPx,
                availableHeightPx = availableHeight,
            )
        }

        val maxColumnGap = if (stageCount <= 1) 0f else {
            ((availableWidth - (config.nodeWidthPx * stageCount)) / (stageCount - 1)).coerceAtLeast(0f)
        }
        val actualColumnGap = min(config.columnGapPx, maxColumnGap)
        val usedWidth = (config.nodeWidthPx * stageCount) + (actualColumnGap * max(0, stageCount - 1))
        val startLeft = config.contentPaddingPx + ((availableWidth - usedWidth) * 0.5f).coerceAtLeast(0f)

        stageGroups.forEach { (stage, indices) ->
            val heights = stageHeights.getValue(stage)
            val totalHeight = heights.sum() + (config.nodeGapPx * max(0, indices.size - 1))
            var cursor = config.contentPaddingPx + ((availableHeight - totalHeight) * 0.5f).coerceAtLeast(0f)
            val left = startLeft + (stage * (config.nodeWidthPx + actualColumnGap))
            indices.forEachIndexed { localIndex, nodeIndex ->
                val height = heights[localIndex]
                val node = compactNodes[nodeIndex]
                node.top = cursor
                node.bottom = cursor + height
                node.left = left
                node.right = left + config.nodeWidthPx
                cursor += height + config.nodeGapPx
            }
        }

        repeat(4) {
            for (stage in 1 until stageCount) {
                relaxStage(
                    stage = stage,
                    stageGroups = stageGroups,
                    compactNodes = compactNodes,
                    links = compactLinks,
                    nodeGapPx = config.nodeGapPx,
                    availableHeightPx = availableHeight,
                    contentPaddingPx = config.contentPaddingPx,
                    useIncoming = true,
                )
            }
            for (stage in (stageCount - 2) downTo 0) {
                relaxStage(
                    stage = stage,
                    stageGroups = stageGroups,
                    compactNodes = compactNodes,
                    links = compactLinks,
                    nodeGapPx = config.nodeGapPx,
                    availableHeightPx = availableHeight,
                    contentPaddingPx = config.contentPaddingPx,
                    useIncoming = false,
                )
            }
        }

        val outgoingOrder = Array(compactNodes.size) { mutableListOf<Int>() }
        val incomingOrder = Array(compactNodes.size) { mutableListOf<Int>() }
        compactLinks.forEachIndexed { linkIndex, link ->
            outgoingOrder[link.sourceIndex].add(linkIndex)
            incomingOrder[link.targetIndex].add(linkIndex)
        }
        outgoingOrder.indices.forEach { nodeIndex ->
            outgoingOrder[nodeIndex].sortBy { compactNodes[compactLinks[it].targetIndex].centerY }
            incomingOrder[nodeIndex].sortBy { compactNodes[compactLinks[it].sourceIndex].centerY }
        }

        val sourceOffsets = FloatArray(compactNodes.size)
        val targetOffsets = FloatArray(compactNodes.size)
        compactNodes.forEachIndexed { index, node ->
            val outgoingThickness = outgoingOrder[index].sumOf { compactLinks[it].value * valueScale.toDouble() }.toFloat()
            val incomingThickness = incomingOrder[index].sumOf { compactLinks[it].value * valueScale.toDouble() }.toFloat()
            sourceOffsets[index] = node.top + ((node.height - outgoingThickness) * 0.5f).coerceAtLeast(0f)
            targetOffsets[index] = node.top + ((node.height - incomingThickness) * 0.5f).coerceAtLeast(0f)
        }

        outgoingOrder.forEachIndexed { nodeIndex, linkIndexes ->
            linkIndexes.forEach { linkIndex ->
                val link = compactLinks[linkIndex]
                val thickness = (link.value * valueScale).toFloat()
                link.sourceTop = sourceOffsets[nodeIndex]
                link.sourceBottom = sourceOffsets[nodeIndex] + thickness
                sourceOffsets[nodeIndex] += thickness
            }
        }
        incomingOrder.forEachIndexed { nodeIndex, linkIndexes ->
            linkIndexes.forEach { linkIndex ->
                val link = compactLinks[linkIndex]
                val thickness = (link.value * valueScale).toFloat()
                link.targetTop = targetOffsets[nodeIndex]
                link.targetBottom = targetOffsets[nodeIndex] + thickness
                targetOffsets[nodeIndex] += thickness
            }
        }

        return SankeyChartLayoutResult(
            nodeLayouts = compactNodes.map {
                SankeyNodeLayout(
                    originalIndex = it.originalIndex,
                    node = it.node,
                    stage = it.stage,
                    left = it.left,
                    top = it.top,
                    right = it.right,
                    bottom = it.bottom,
                    totalValue = it.totalValue,
                )
            },
            linkLayouts = compactLinks.map { link ->
                SankeyLinkLayout(
                    originalIndex = link.originalIndex,
                    link = link.link,
                    sourceNodeOriginalIndex = compactNodes[link.sourceIndex].originalIndex,
                    targetNodeOriginalIndex = compactNodes[link.targetIndex].originalIndex,
                    sourceLeft = compactNodes[link.sourceIndex].left,
                    sourceRight = compactNodes[link.sourceIndex].right,
                    targetLeft = compactNodes[link.targetIndex].left,
                    targetRight = compactNodes[link.targetIndex].right,
                    sourceTop = link.sourceTop,
                    sourceBottom = link.sourceBottom,
                    targetTop = link.targetTop,
                    targetBottom = link.targetBottom,
                    thickness = link.thickness,
                    color = link.color.takeIf { it != 0 } ?: styleOptions.defaultLinkColor,
                )
            },
            stageCount = stageCount,
            isRenderable = true,
            emptyReason = null,
        )
    }

    /**
     * Returns the original node index under the given point, or `null` when no node is hit.
     */
    fun hitTestNode(layout: SankeyChartLayoutResult, x: Float, y: Float): Int? {
        return layout.nodeLayouts.asReversed().firstOrNull { node ->
            x in node.left..node.right && y in node.top..node.bottom
        }?.originalIndex
    }

    /**
     * Returns the original link index near the given point, or `null` when no link is hit.
     *
     * Link hit testing uses a sampled cubic-curve distance check with [tolerancePx] as the
     * minimum tap radius.
     */
    fun hitTestLink(
        layout: SankeyChartLayoutResult,
        x: Float,
        y: Float,
        tolerancePx: Float,
    ): Int? {
        return layout.linkLayouts.asReversed().firstOrNull { link ->
            val threshold = max(tolerancePx, (link.thickness * 0.5f) + 2f)
            cubicDistanceToPoint(
                x = x,
                y = y,
                link = link,
            ) <= threshold
        }?.originalIndex
    }

    private fun relaxStage(
        stage: Int,
        stageGroups: Map<Int, List<Int>>,
        compactNodes: List<MutableNode>,
        links: List<MutableLink>,
        nodeGapPx: Float,
        availableHeightPx: Float,
        contentPaddingPx: Float,
        useIncoming: Boolean,
    ) {
        val indices = stageGroups[stage] ?: return
        if (indices.size <= 1) return

        val ordered = indices.map { nodeIndex ->
            val related = links.filter {
                if (useIncoming) it.targetIndex == nodeIndex else it.sourceIndex == nodeIndex
            }
            val desiredCenter = if (related.isEmpty()) {
                compactNodes[nodeIndex].centerY
            } else {
                val weighted = related.sumOf { link ->
                    val otherCenter = if (useIncoming) {
                        compactNodes[link.sourceIndex].centerY
                    } else {
                        compactNodes[link.targetIndex].centerY
                    }
                    otherCenter.toDouble() * link.value
                }
                (weighted / related.sumOf { it.value }).toFloat()
            }
            nodeIndex to desiredCenter
        }.sortedBy { it.second }

        var cursor = contentPaddingPx
        ordered.forEach { (nodeIndex, desiredCenter) ->
            val node = compactNodes[nodeIndex]
            val desiredTop = desiredCenter - (node.height * 0.5f)
            val nextTop = max(cursor, desiredTop)
            node.top = nextTop
            node.bottom = nextTop + node.height
            cursor = node.bottom + nodeGapPx
        }

        val overflow = (ordered.lastOrNull()?.let { compactNodes[it.first].bottom } ?: contentPaddingPx) - (contentPaddingPx + availableHeightPx)
        if (overflow > 0f) {
            ordered.forEach { (nodeIndex, _) ->
                compactNodes[nodeIndex].top -= overflow
                compactNodes[nodeIndex].bottom -= overflow
            }
        }
        val minTop = ordered.minOfOrNull { compactNodes[it.first].top } ?: contentPaddingPx
        if (minTop < contentPaddingPx) {
            val delta = contentPaddingPx - minTop
            ordered.forEach { (nodeIndex, _) ->
                compactNodes[nodeIndex].top += delta
                compactNodes[nodeIndex].bottom += delta
            }
        }
    }

    private fun fitHeightsToStage(
        values: List<Double>,
        valueScale: Float,
        minHeightPx: Float,
        gapPx: Float,
        availableHeightPx: Float,
    ): FloatArray {
        if (values.isEmpty()) return floatArrayOf()
        val heights = FloatArray(values.size) { index ->
            max((values[index] * valueScale).toFloat(), minHeightPx)
        }
        var totalHeight = heights.sum() + (gapPx * max(0, heights.size - 1))
        if (totalHeight <= availableHeightPx) return heights

        val usable = (availableHeightPx - (gapPx * max(0, heights.size - 1))).coerceAtLeast(heights.size.toFloat())
        val scale = usable / heights.sum().coerceAtLeast(1f)
        heights.indices.forEach { index ->
            heights[index] = max(2f, heights[index] * scale)
        }
        totalHeight = heights.sum() + (gapPx * max(0, heights.size - 1))
        if (totalHeight <= availableHeightPx) return heights

        val extra = (totalHeight - availableHeightPx) / max(1, heights.size)
        heights.indices.forEach { index ->
            heights[index] = max(2f, heights[index] - extra)
        }
        return heights
    }

    private fun cubicDistanceToPoint(
        x: Float,
        y: Float,
        link: SankeyLinkLayout,
    ): Float {
        val startX = link.sourceRight
        val startY = link.sourceCenterY
        val endX = link.targetLeft
        val endY = link.targetCenterY
        val dx = endX - startX
        val c1x = startX + (dx * 0.35f)
        val c2x = endX - (dx * 0.35f)

        var minDistance = Float.MAX_VALUE
        var previousX = startX
        var previousY = startY
        for (step in 1..24) {
            val t = step / 24f
            val oneMinusT = 1f - t
            val sampleX =
                (oneMinusT * oneMinusT * oneMinusT * startX) +
                    (3f * oneMinusT * oneMinusT * t * c1x) +
                    (3f * oneMinusT * t * t * c2x) +
                    (t * t * t * endX)
            val sampleY =
                (oneMinusT * oneMinusT * oneMinusT * startY) +
                    (3f * oneMinusT * oneMinusT * t * startY) +
                    (3f * oneMinusT * t * t * endY) +
                    (t * t * t * endY)
            minDistance = min(minDistance, pointToSegmentDistance(x, y, previousX, previousY, sampleX, sampleY))
            previousX = sampleX
            previousY = sampleY
        }
        return minDistance
    }

    private fun pointToSegmentDistance(
        px: Float,
        py: Float,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
    ): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        if (dx == 0f && dy == 0f) {
            return sqrt(((px - x1) * (px - x1)) + ((py - y1) * (py - y1)))
        }
        val t = (((px - x1) * dx) + ((py - y1) * dy)) / ((dx * dx) + (dy * dy))
        val clamped = t.coerceIn(0f, 1f)
        val projX = x1 + (dx * clamped)
        val projY = y1 + (dy * clamped)
        return sqrt(((px - projX) * (px - projX)) + ((py - projY) * (py - projY)))
    }

    private fun invalid(reason: String): SankeyChartLayoutResult {
        return SankeyChartLayoutResult(
            nodeLayouts = emptyList(),
            linkLayouts = emptyList(),
            stageCount = 0,
            isRenderable = false,
            emptyReason = reason,
        )
    }
}
