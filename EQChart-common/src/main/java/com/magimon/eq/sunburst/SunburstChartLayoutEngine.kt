package com.magimon.eq.sunburst

import android.graphics.Color
import kotlin.math.max

/**
 * Normalized render segment for a Sunburst chart.
 *
 * Radius ratios are expressed in the `0..1` domain relative to the chart's outer radius.
 *
 * @property label Segment label copied from the source [SunburstNode].
 * @property value Resolved numeric value represented by the segment.
 * @property depth Zero-based depth, where `0` is the innermost visible ring.
 * @property startAngleDeg Segment start angle in degrees.
 * @property sweepAngleDeg Segment angular sweep in degrees.
 * @property innerRadiusRatio Inner radius normalized against the chart's outer radius.
 * @property outerRadiusRatio Outer radius normalized against the chart's outer radius.
 * @property color Resolved fill color used by renderers.
 * @property payload Optional payload propagated from the source node.
 * @property path Zero-based child index path from the root list to this segment.
 * @property hasChildren Whether the source node produced one or more child segments.
 * @see SunburstNode
 */
data class SunburstSegmentLayout(
    val label: String,
    val value: Double,
    val depth: Int,
    val startAngleDeg: Float,
    val sweepAngleDeg: Float,
    val innerRadiusRatio: Float,
    val outerRadiusRatio: Float,
    val color: Int,
    val payload: Any?,
    val path: List<Int>,
    val hasChildren: Boolean,
)

/**
 * Shared normalized layout result for Sunburst renderers.
 *
 * @property segments Flattened render order of all resolved segments.
 * @property depthCount Number of visible depth levels in the layout.
 * @property totalValue Total resolved value across all root nodes.
 * @property isRenderable Whether the input produced a valid layout.
 * @property emptyReason Optional failure reason when [isRenderable] is `false`.
 * @see SunburstSegmentLayout
 * @see SunburstNode
 */
data class SunburstChartLayoutResult(
    val segments: List<SunburstSegmentLayout>,
    val depthCount: Int,
    val totalValue: Double,
    val isRenderable: Boolean,
    val emptyReason: String? = null,
)

/**
 * Shared layout and hit-test engine for Sunburst charts.
 *
 * The engine validates [SunburstNode] trees, resolves inherited colors, normalizes segment
 * geometry, and exposes hit-testing helpers for renderer implementations.
 *
 * @see SunburstNode
 * @see SunburstSegmentLayout
 * @see SunburstChartLayoutResult
 */
object SunburstChartLayoutEngine {

    private data class ResolvedNode(
        val label: String,
        val value: Double,
        val depth: Int,
        val color: Int,
        val payload: Any?,
        val path: List<Int>,
        val children: List<ResolvedNode>,
    ) {
        val depthCount: Int
            get() {
                var childDepth = 0
                children.forEach { child ->
                    if (child.depthCount > childDepth) {
                        childDepth = child.depthCount
                    }
                }
                return 1 + childDepth
            }
    }

    /**
     * Builds a renderer-agnostic normalized layout for a Sunburst chart.
     *
     * Invalid nodes are filtered out. Rendering fails when no valid nodes remain or when the
     * supplied style and presentation options produce impossible geometry.
     *
     * @param nodes Root nodes that define the chart hierarchy.
     * @param styleOptions Visual options that affect colors and stroke defaults.
     * @param presentationOptions Presentation options that affect ring geometry and animation defaults.
     * @return A normalized [SunburstChartLayoutResult] suitable for View and Compose renderers.
     * @see SunburstNode
     * @see SunburstChartStyleOptions
     * @see SunburstChartPresentationOptions
     */
    fun compute(
        nodes: List<SunburstNode>,
        styleOptions: SunburstChartStyleOptions = SunburstChartStyleOptions(),
        presentationOptions: SunburstChartPresentationOptions = SunburstChartPresentationOptions(),
    ): SunburstChartLayoutResult {
        if (presentationOptions.innerHoleRatio < 0f || presentationOptions.innerHoleRatio >= 1f) {
            return invalid("inner hole ratio")
        }
        if (presentationOptions.ringGapRatio < 0f || presentationOptions.ringGapRatio >= 1f) {
            return invalid("ring gap ratio")
        }
        if (styleOptions.segmentColors.isEmpty()) {
            return invalid("segment colors")
        }

        val resolvedRoots = nodes.mapIndexedNotNull { index, node ->
            resolveNode(
                node = node,
                depth = 0,
                siblingIndex = index,
                parentColor = null,
                path = listOf(index),
                palette = styleOptions.segmentColors,
            )
        }
        if (resolvedRoots.isEmpty()) {
            return invalid("no valid nodes")
        }

        var depthCount = 0
        resolvedRoots.forEach { root ->
            if (root.depthCount > depthCount) {
                depthCount = root.depthCount
            }
        }
        val totalGap = presentationOptions.ringGapRatio * (depthCount - 1)
        val ringWidthRatio = (1f - presentationOptions.innerHoleRatio - totalGap) / depthCount
        if (ringWidthRatio <= 0f) {
            return invalid("insufficient ring width")
        }

        val totalValue = resolvedRoots.sumOf { it.value }

        val fullSweep = if (presentationOptions.clockwise) 360f else -360f
        val segments = mutableListOf<SunburstSegmentLayout>()
        var currentStart = presentationOptions.startAngleDeg

        resolvedRoots.forEach { root ->
            val sweep = fullSweep * (root.value / totalValue).toFloat()
            appendSegments(
                node = root,
                startAngleDeg = currentStart,
                sweepAngleDeg = sweep,
                innerHoleRatio = presentationOptions.innerHoleRatio,
                ringGapRatio = presentationOptions.ringGapRatio,
                ringWidthRatio = ringWidthRatio,
                into = segments,
            )
            currentStart += sweep
        }

        return SunburstChartLayoutResult(
            segments = segments,
            depthCount = depthCount,
            totalValue = totalValue,
            isRenderable = true,
        )
    }

    /**
     * Resolves the top-most segment that contains the supplied polar coordinate.
     *
     * @param layout Previously computed layout to inspect.
     * @param normalizedRadiusRatio Touch radius normalized against the chart's outer radius.
     * @param angleDeg Touch angle in degrees, where `0` points to the right and positive values
     * rotate clockwise in Android screen coordinates.
     * @return The matching [SunburstSegmentLayout], or `null` when the point is outside all segments.
     * @see compute
     * @see SunburstSegmentLayout
     */
    fun hitTestSegment(
        layout: SunburstChartLayoutResult,
        normalizedRadiusRatio: Float,
        angleDeg: Float,
    ): SunburstSegmentLayout? {
        if (!layout.isRenderable || normalizedRadiusRatio < 0f) return null

        val normalizedAngle = normalizeAngle(angleDeg)
        return layout.segments.asReversed().firstOrNull { segment ->
            normalizedRadiusRatio >= segment.innerRadiusRatio &&
                normalizedRadiusRatio <= segment.outerRadiusRatio &&
                isAngleInSweep(
                    angleDeg = normalizedAngle,
                    startDeg = segment.startAngleDeg,
                    sweepDeg = segment.sweepAngleDeg,
                )
        }
    }

    private fun appendSegments(
        node: ResolvedNode,
        startAngleDeg: Float,
        sweepAngleDeg: Float,
        innerHoleRatio: Float,
        ringGapRatio: Float,
        ringWidthRatio: Float,
        into: MutableList<SunburstSegmentLayout>,
    ) {
        val innerRatio = innerHoleRatio + node.depth * (ringWidthRatio + ringGapRatio)
        val outerRatio = innerRatio + ringWidthRatio
        into += SunburstSegmentLayout(
            label = node.label,
            value = node.value,
            depth = node.depth,
            startAngleDeg = startAngleDeg,
            sweepAngleDeg = sweepAngleDeg,
            innerRadiusRatio = innerRatio,
            outerRadiusRatio = outerRatio,
            color = node.color,
            payload = node.payload,
            path = node.path,
            hasChildren = node.children.isNotEmpty(),
        )

        if (node.children.isEmpty()) return

        var childStart = startAngleDeg
        node.children.forEach { child ->
            val childSweep = sweepAngleDeg * (child.value / node.value).toFloat()
            appendSegments(
                node = child,
                startAngleDeg = childStart,
                sweepAngleDeg = childSweep,
                innerHoleRatio = innerHoleRatio,
                ringGapRatio = ringGapRatio,
                ringWidthRatio = ringWidthRatio,
                into = into,
            )
            childStart += childSweep
        }
    }

    private fun resolveNode(
        node: SunburstNode,
        depth: Int,
        siblingIndex: Int,
        parentColor: Int?,
        path: List<Int>,
        palette: List<Int>,
    ): ResolvedNode? {
        val label = node.label.trim()
        if (label.isEmpty()) return null

        val resolvedColor = when {
            node.color != null -> node.color
            parentColor != null -> lightenColor(parentColor, 0.12f + ((siblingIndex % 3) * 0.08f))
            else -> palette[siblingIndex % palette.size]
        }

        val resolvedChildren = node.children.mapIndexedNotNull { index, child ->
            resolveNode(
                node = child,
                depth = depth + 1,
                siblingIndex = index,
                parentColor = resolvedColor,
                path = path + index,
                palette = palette,
            )
        }

        val resolvedValue = if (resolvedChildren.isNotEmpty()) {
            resolvedChildren.sumOf { it.value }
        } else {
            node.value.takeIf { it.isFinite() && it > 0.0 } ?: return null
        }

        return ResolvedNode(
            label = label,
            value = resolvedValue,
            depth = depth,
            color = resolvedColor,
            payload = node.payload,
            path = path,
            children = resolvedChildren,
        )
    }

    private fun lightenColor(color: Int, fraction: Float): Int {
        val amount = fraction.coerceIn(0f, 1f)
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb(
            Color.alpha(color),
            red + ((255 - red) * amount).toInt(),
            green + ((255 - green) * amount).toInt(),
            blue + ((255 - blue) * amount).toInt(),
        )
    }

    private fun invalid(reason: String): SunburstChartLayoutResult {
        return SunburstChartLayoutResult(
            segments = emptyList(),
            depthCount = 0,
            totalValue = 0.0,
            isRenderable = false,
            emptyReason = reason,
        )
    }

    private fun normalizeAngle(angle: Float): Float {
        var normalized = angle % 360f
        if (normalized < 0f) normalized += 360f
        return normalized
    }

    private fun isAngleInSweep(angleDeg: Float, startDeg: Float, sweepDeg: Float): Boolean {
        val angle = normalizeAngle(angleDeg)
        val start = normalizeAngle(startDeg)
        return if (sweepDeg > 0f) {
            normalizeAngle(angle - start) <= sweepDeg + 0.0001f
        } else {
            normalizeAngle(start - angle) <= -sweepDeg + 0.0001f
        }
    }
}
