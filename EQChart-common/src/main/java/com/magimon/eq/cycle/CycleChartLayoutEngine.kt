package com.magimon.eq.cycle

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Pixel-based layout inputs consumed by [CycleChartLayoutEngine].
 *
 * Public callers typically derive this from the chart size and dp-based style/presentation
 * options before invoking [CycleChartLayoutEngine.compute].
 */
data class CycleChartLayoutConfig(
    val widthPx: Float,
    val heightPx: Float,
    val contentPaddingPx: Float,
    val nodeRadiusPx: Float,
    val linkMinThicknessPx: Float,
    val linkMaxThicknessPx: Float,
    val linkInsetPx: Float,
    val startAngleDeg: Float,
    val clockwise: Boolean,
    val linkInnerRadiusFactor: Float,
)

/**
 * Final node geometry produced by [CycleChartLayoutEngine.compute].
 *
 * Coordinates are expressed in pixels relative to the chart canvas.
 */
data class CycleNodeLayout(
    val originalIndex: Int,
    val node: CycleNode,
    val centerX: Float,
    val centerY: Float,
    val radius: Float,
    val angleDeg: Float,
) {
    val left: Float
        get() = centerX - radius

    val top: Float
        get() = centerY - radius

    val right: Float
        get() = centerX + radius

    val bottom: Float
        get() = centerY + radius
}

/**
 * Final quadratic link geometry produced by [CycleChartLayoutEngine.compute].
 *
 * Renderers can draw a curved connector from `(startX, startY)` to `(endX, endY)` using
 * `(controlX, controlY)` as the quadratic control point.
 */
data class CycleLinkLayout(
    val originalIndex: Int,
    val link: CycleLink,
    val sourceNodeOriginalIndex: Int,
    val targetNodeOriginalIndex: Int,
    val startX: Float,
    val startY: Float,
    val controlX: Float,
    val controlY: Float,
    val endX: Float,
    val endY: Float,
    val midX: Float,
    val midY: Float,
    val thickness: Float,
    val color: Int,
)

/**
 * Complete layout result for a cycle diagram chart.
 *
 * When [isRenderable] is `false`, callers should render an empty/fallback state instead of
 * attempting partial drawing.
 */
data class CycleChartLayoutResult(
    val nodeLayouts: List<CycleNodeLayout>,
    val linkLayouts: List<CycleLinkLayout>,
    val centerX: Float,
    val centerY: Float,
    val orbitRadius: Float,
    val isRenderable: Boolean,
    val emptyReason: String? = null,
)

/**
 * Shared cycle diagram layout and hit-test engine.
 *
 * The engine validates node/link input, places nodes around a circular orbit in input order,
 * computes curved connector geometry for each valid link, and exposes hit-testing helpers for
 * node/link selection so View and Compose renderers stay behaviorally aligned.
 */
object CycleChartLayoutEngine {

    /**
     * Computes a full cycle diagram layout from node/link input and pixel-based layout
     * constraints.
     *
     * Invalid links are ignored. The chart becomes non-renderable when there are not enough valid
     * nodes or links left to draw a meaningful diagram.
     */
    fun compute(
        nodes: List<CycleNode>,
        links: List<CycleLink>,
        config: CycleChartLayoutConfig,
        styleOptions: CycleChartStyleOptions,
    ): CycleChartLayoutResult {
        if (config.widthPx <= 0f || config.heightPx <= 0f) {
            return invalid("non-positive size")
        }
        if (config.nodeRadiusPx <= 0f) {
            return invalid("node radius")
        }
        if (config.linkMinThicknessPx <= 0f) {
            return invalid("link min thickness")
        }
        if (config.linkMaxThicknessPx <= 0f) {
            return invalid("link max thickness")
        }
        if (config.linkMaxThicknessPx < config.linkMinThicknessPx) {
            return invalid("link thickness range")
        }
        if (config.contentPaddingPx < 0f) {
            return invalid("content padding")
        }
        if (nodes.size < 2) {
            return invalid("not enough nodes")
        }

        val trimmedIds = nodes.map { it.id.trim() }
        if (trimmedIds.any { it.isEmpty() }) {
            return invalid("blank node id")
        }
        if (trimmedIds.distinct().size != trimmedIds.size) {
            return invalid("duplicate node id")
        }

        val centerX = config.widthPx * 0.5f
        val centerY = config.heightPx * 0.5f
        val orbitRadius = (min(config.widthPx, config.heightPx) * 0.5f) -
            config.contentPaddingPx -
            config.nodeRadiusPx -
            config.linkMaxThicknessPx
        if (orbitRadius <= 0f) {
            return invalid("insufficient size")
        }

        val nodeIndexById = trimmedIds.withIndex().associate { it.value to it.index }
        val nodeLayouts = nodes.mapIndexed { index, node ->
            val angleDeg = computeAngleDeg(index, nodes.size, config.startAngleDeg, config.clockwise)
            val angleRad = Math.toRadians(angleDeg.toDouble())
            CycleNodeLayout(
                originalIndex = index,
                node = node,
                centerX = centerX + (cos(angleRad) * orbitRadius).toFloat(),
                centerY = centerY + (sin(angleRad) * orbitRadius).toFloat(),
                radius = config.nodeRadiusPx,
                angleDeg = angleDeg,
            )
        }

        val validLinks = links.mapIndexedNotNull { index, link ->
            val sourceIndex = nodeIndexById[link.sourceId.trim()] ?: return@mapIndexedNotNull null
            val targetIndex = nodeIndexById[link.targetId.trim()] ?: return@mapIndexedNotNull null
            if (!link.value.isFinite() || link.value <= 0.0) return@mapIndexedNotNull null
            if (sourceIndex == targetIndex) return@mapIndexedNotNull null
            Triple(index, sourceIndex, targetIndex)
        }
        if (validLinks.isEmpty()) {
            return invalid("no valid links")
        }

        var maxValue = links[validLinks.first().first].value
        for (index in 1 until validLinks.size) {
            val candidate = links[validLinks[index].first].value
            if (candidate > maxValue) {
                maxValue = candidate
            }
        }
        val linkLayouts = validLinks.map { (originalIndex, sourceIndex, targetIndex) ->
            val source = nodeLayouts[sourceIndex]
            val target = nodeLayouts[targetIndex]
            val sourceDirX = normalizeComponent(source.centerX - centerX, source.centerY - centerY).first
            val sourceDirY = normalizeComponent(source.centerX - centerX, source.centerY - centerY).second
            val targetDirX = normalizeComponent(target.centerX - centerX, target.centerY - centerY).first
            val targetDirY = normalizeComponent(target.centerX - centerX, target.centerY - centerY).second

            val (controlDirX, controlDirY) = resolveControlDirection(
                sourceDirX = sourceDirX,
                sourceDirY = sourceDirY,
                targetDirX = targetDirX,
                targetDirY = targetDirY,
            )
            val controlRadius = orbitRadius * config.linkInnerRadiusFactor.coerceIn(0f, 1f)
            val controlX = centerX + (controlDirX * controlRadius)
            val controlY = centerY + (controlDirY * controlRadius)

            val (startDirX, startDirY) = normalizeOrFallback(
                dx = controlX - source.centerX,
                dy = controlY - source.centerY,
                fallbackX = -sourceDirX,
                fallbackY = -sourceDirY,
            )
            val (endDirX, endDirY) = normalizeOrFallback(
                dx = controlX - target.centerX,
                dy = controlY - target.centerY,
                fallbackX = -targetDirX,
                fallbackY = -targetDirY,
            )
            val startDistance = source.radius + config.linkInsetPx.coerceAtLeast(0f)
            val endDistance = target.radius + config.linkInsetPx.coerceAtLeast(0f)
            val startX = source.centerX + (startDirX * startDistance)
            val startY = source.centerY + (startDirY * startDistance)
            val endX = target.centerX + (endDirX * endDistance)
            val endY = target.centerY + (endDirY * endDistance)
            val (midX, midY) = quadraticPoint(
                startX = startX,
                startY = startY,
                controlX = controlX,
                controlY = controlY,
                endX = endX,
                endY = endY,
                t = 0.5f,
            )
            val ratio = (links[originalIndex].value / maxValue).toFloat().coerceIn(0f, 1f)
            val thickness = config.linkMinThicknessPx +
                ((config.linkMaxThicknessPx - config.linkMinThicknessPx) * ratio)
            val color = links[originalIndex].color
                ?: source.node.color.takeUnless { it == 0 }
                ?: styleOptions.defaultLinkColor

            CycleLinkLayout(
                originalIndex = originalIndex,
                link = links[originalIndex],
                sourceNodeOriginalIndex = source.originalIndex,
                targetNodeOriginalIndex = target.originalIndex,
                startX = startX,
                startY = startY,
                controlX = controlX,
                controlY = controlY,
                endX = endX,
                endY = endY,
                midX = midX,
                midY = midY,
                thickness = thickness,
                color = color,
            )
        }

        return CycleChartLayoutResult(
            nodeLayouts = nodeLayouts,
            linkLayouts = linkLayouts,
            centerX = centerX,
            centerY = centerY,
            orbitRadius = orbitRadius,
            isRenderable = true,
        )
    }

    /**
     * Resolves the tapped node index when the touch point falls within a node circle.
     */
    fun hitTestNode(
        layout: CycleChartLayoutResult,
        x: Float,
        y: Float,
    ): Int? {
        return layout.nodeLayouts.firstOrNull { node ->
            val dx = x - node.centerX
            val dy = y - node.centerY
            ((dx * dx) + (dy * dy)) <= (node.radius * node.radius)
        }?.originalIndex
    }

    /**
     * Resolves the tapped link index by sampling its quadratic path with the supplied tolerance.
     */
    fun hitTestLink(
        layout: CycleChartLayoutResult,
        x: Float,
        y: Float,
        tolerancePx: Float,
    ): Int? {
        if (tolerancePx <= 0f) return null

        return layout.linkLayouts.minByOrNull { link ->
            distanceToQuadratic(
                x = x,
                y = y,
                link = link,
            )
        }?.takeIf { link ->
            distanceToQuadratic(x = x, y = y, link = link) <= (tolerancePx + (link.thickness * 0.5f))
        }?.originalIndex
    }

    private fun invalid(reason: String): CycleChartLayoutResult {
        return CycleChartLayoutResult(
            nodeLayouts = emptyList(),
            linkLayouts = emptyList(),
            centerX = 0f,
            centerY = 0f,
            orbitRadius = 0f,
            isRenderable = false,
            emptyReason = reason,
        )
    }

    private fun computeAngleDeg(
        index: Int,
        count: Int,
        startAngleDeg: Float,
        clockwise: Boolean,
    ): Float {
        val step = (360f / count.toFloat()) * if (clockwise) 1f else -1f
        return startAngleDeg + (index * step)
    }

    private fun resolveControlDirection(
        sourceDirX: Float,
        sourceDirY: Float,
        targetDirX: Float,
        targetDirY: Float,
    ): Pair<Float, Float> {
        val sumX = sourceDirX + targetDirX
        val sumY = sourceDirY + targetDirY
        val normalized = normalizeComponent(sumX, sumY)
        if (normalized.first != 0f || normalized.second != 0f) {
            return normalized
        }

        val perpX = -(targetDirY - sourceDirY)
        val perpY = targetDirX - sourceDirX
        val perpendicular = normalizeComponent(perpX, perpY)
        if (perpendicular.first != 0f || perpendicular.second != 0f) {
            return perpendicular
        }
        return 0f to -1f
    }

    private fun normalizeOrFallback(
        dx: Float,
        dy: Float,
        fallbackX: Float,
        fallbackY: Float,
    ): Pair<Float, Float> {
        val normalized = normalizeComponent(dx, dy)
        if (normalized.first != 0f || normalized.second != 0f) {
            return normalized
        }
        return normalizeComponent(fallbackX, fallbackY)
    }

    private fun normalizeComponent(
        x: Float,
        y: Float,
    ): Pair<Float, Float> {
        val length = hypot(x, y)
        if (length <= 1e-4f) return 0f to 0f
        return (x / length) to (y / length)
    }

    private fun quadraticPoint(
        startX: Float,
        startY: Float,
        controlX: Float,
        controlY: Float,
        endX: Float,
        endY: Float,
        t: Float,
    ): Pair<Float, Float> {
        val oneMinusT = 1f - t
        val x = (oneMinusT * oneMinusT * startX) + (2f * oneMinusT * t * controlX) + (t * t * endX)
        val y = (oneMinusT * oneMinusT * startY) + (2f * oneMinusT * t * controlY) + (t * t * endY)
        return x to y
    }

    private fun distanceToQuadratic(
        x: Float,
        y: Float,
        link: CycleLinkLayout,
    ): Float {
        var best = Float.MAX_VALUE
        var previous = quadraticPoint(
            startX = link.startX,
            startY = link.startY,
            controlX = link.controlX,
            controlY = link.controlY,
            endX = link.endX,
            endY = link.endY,
            t = 0f,
        )
        val steps = 24
        for (index in 1..steps) {
            val current = quadraticPoint(
                startX = link.startX,
                startY = link.startY,
                controlX = link.controlX,
                controlY = link.controlY,
                endX = link.endX,
                endY = link.endY,
                t = index / steps.toFloat(),
            )
            best = min(
                best,
                distanceToSegment(
                    px = x,
                    py = y,
                    ax = previous.first,
                    ay = previous.second,
                    bx = current.first,
                    by = current.second,
                ),
            )
            previous = current
        }
        return best
    }

    private fun distanceToSegment(
        px: Float,
        py: Float,
        ax: Float,
        ay: Float,
        bx: Float,
        by: Float,
    ): Float {
        val abx = bx - ax
        val aby = by - ay
        val lengthSquared = (abx * abx) + (aby * aby)
        if (lengthSquared <= 1e-4f) {
            return hypot(px - ax, py - ay)
        }
        val t = (((px - ax) * abx) + ((py - ay) * aby)) / lengthSquared
        val clamped = t.coerceIn(0f, 1f)
        val closestX = ax + (abx * clamped)
        val closestY = ay + (aby * clamped)
        return hypot(px - closestX, py - closestY)
    }
}
