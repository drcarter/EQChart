package com.magimon.eq.stepflow

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Pixel-based layout configuration consumed by [StepFlowChartLayoutEngine].
 *
 * @property widthPx Available renderer width in pixels.
 * @property heightPx Available renderer height in pixels.
 * @property contentPaddingPx Outer content padding in pixels.
 * @property hubRadiusPx Radius of the central hub in pixels.
 * @property hubRingThicknessPx Thickness of the colored hub ring in pixels.
 * @property spineWidthPx Stroke width of the curved spine in pixels.
 * @property spineDotRadiusPx Radius of each step anchor dot in pixels.
 * @property tailDotRadiusPx Radius of the decorative top and bottom tail dots in pixels.
 * @property badgeRadiusPx Radius of the circular step badge in pixels.
 * @property cardWidthPx Preferred width of each step card in pixels.
 * @property cardHeightPx Preferred height of each step card in pixels.
 * @property cardCornerRadiusPx Corner radius of each step card in pixels.
 * @property connectorWidthPx Stroke width of the connector between spine and badge in pixels.
 * @property iconCircleRadiusPx Radius of the trailing icon circle in pixels.
 * @property cardGapPx Gap between the spine/badge cluster and the step card in pixels.
 * @property badgeOverlapPx Overlap amount between the badge and card in pixels.
 * @property topBottomInsetPx Reserved inset above the first step and below the last step in pixels.
 */
data class StepFlowChartLayoutConfig(
    val widthPx: Float,
    val heightPx: Float,
    val contentPaddingPx: Float,
    val hubRadiusPx: Float,
    val hubRingThicknessPx: Float,
    val spineWidthPx: Float,
    val spineDotRadiusPx: Float,
    val tailDotRadiusPx: Float,
    val badgeRadiusPx: Float,
    val cardWidthPx: Float,
    val cardHeightPx: Float,
    val cardCornerRadiusPx: Float,
    val connectorWidthPx: Float,
    val iconCircleRadiusPx: Float,
    val cardGapPx: Float,
    val badgeOverlapPx: Float,
    val topBottomInsetPx: Float,
) {
    fun scaleToFit(stepCount: Int): StepFlowChartLayoutConfig {
        if (stepCount <= 0) return this

        val requiredWidth = preferredRequiredWidthPx()
        val requiredHeight = preferredRequiredHeightPx(stepCount)
        val widthScale = if (requiredWidth > 0f) widthPx / requiredWidth else 1f
        val heightScale = if (requiredHeight > 0f) heightPx / requiredHeight else 1f
        val scale = min(1f, min(widthScale, heightScale))

        if (scale >= 1f) return this

        return copy(
            contentPaddingPx = contentPaddingPx * scale,
            hubRadiusPx = hubRadiusPx * scale,
            hubRingThicknessPx = hubRingThicknessPx * scale,
            spineWidthPx = spineWidthPx * scale,
            spineDotRadiusPx = spineDotRadiusPx * scale,
            tailDotRadiusPx = tailDotRadiusPx * scale,
            badgeRadiusPx = badgeRadiusPx * scale,
            cardWidthPx = cardWidthPx * scale,
            cardHeightPx = cardHeightPx * scale,
            cardCornerRadiusPx = cardCornerRadiusPx * scale,
            connectorWidthPx = connectorWidthPx * scale,
            iconCircleRadiusPx = iconCircleRadiusPx * scale,
            cardGapPx = cardGapPx * scale,
            badgeOverlapPx = badgeOverlapPx * scale,
            topBottomInsetPx = topBottomInsetPx * scale,
        )
    }

    private fun preferredRequiredWidthPx(): Float {
        val spineLead = max(cardGapPx * 0.45f, spineDotRadiusPx * 1.8f)
        return (contentPaddingPx * 2f) +
            (hubRadiusPx * 2f) +
            (hubRingThicknessPx * 2f) +
            spineLead +
            (badgeRadiusPx * 1.65f) +
            cardWidthPx
    }

    private fun preferredRequiredHeightPx(stepCount: Int): Float {
        val stepGap = if (stepCount > 1) (badgeRadiusPx * 2.1f) * (stepCount - 1) else 0f
        return (contentPaddingPx * 2f) + (topBottomInsetPx * 2f) + stepGap
    }
}

/**
 * Shared hub geometry produced by [StepFlowChartLayoutEngine.compute].
 *
 * @property content Source hub content copied into the layout.
 * @property centerX Hub center x-coordinate in pixels.
 * @property centerY Hub center y-coordinate in pixels.
 * @property radius Hub radius in pixels.
 * @property ringThickness Hub ring thickness in pixels.
 * @property eyebrowCenterX Horizontal anchor for the eyebrow text.
 * @property eyebrowBaselineY Baseline position for the eyebrow text.
 * @property titleCenterX Horizontal anchor for the title text.
 * @property titleBaselineY Baseline position for the title text.
 * @property descriptionCenterX Horizontal anchor for the description text.
 * @property descriptionBaselineY Baseline position for the description text.
 * @see StepFlowHubContent
 */
data class StepFlowHubLayout(
    val content: StepFlowHubContent,
    val centerX: Float,
    val centerY: Float,
    val radius: Float,
    val ringThickness: Float,
    val eyebrowCenterX: Float,
    val eyebrowBaselineY: Float,
    val titleCenterX: Float,
    val titleBaselineY: Float,
    val descriptionCenterX: Float,
    val descriptionBaselineY: Float,
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
 * Colored ring segment drawn around the hub.
 *
 * @property originalIndex Original zero-based step index represented by the segment.
 * @property color Segment color resolved from [StepFlowStep.accentColor].
 * @property startAngleDeg Start angle of the segment in degrees.
 * @property sweepAngleDeg Sweep angle of the segment in degrees.
 */
data class StepFlowHubRingSegment(
    val originalIndex: Int,
    val color: Int,
    val startAngleDeg: Float,
    val sweepAngleDeg: Float,
)

/**
 * Quadratic spine segment used to reconstruct the curved step spine.
 *
 * @property startX Segment start x-coordinate.
 * @property startY Segment start y-coordinate.
 * @property controlX Quadratic control point x-coordinate.
 * @property controlY Quadratic control point y-coordinate.
 * @property endX Segment end x-coordinate.
 * @property endY Segment end y-coordinate.
 */
data class StepFlowSpineSegment(
    val startX: Float,
    val startY: Float,
    val controlX: Float,
    val controlY: Float,
    val endX: Float,
    val endY: Float,
)

/**
 * Decorative tail dot rendered above or below the step spine.
 *
 * @property isTop Whether the dot belongs to the top edge instead of the bottom edge.
 * @property centerX Dot center x-coordinate.
 * @property centerY Dot center y-coordinate.
 * @property radius Dot radius in pixels.
 */
data class StepFlowTailDotLayout(
    val isTop: Boolean,
    val centerX: Float,
    val centerY: Float,
    val radius: Float,
)

/**
 * Full step geometry produced by [StepFlowChartLayoutEngine.compute].
 *
 * @property originalIndex Original zero-based step index.
 * @property step Source step copied into the layout.
 * @property spineDotCenterX Spine anchor dot center x-coordinate.
 * @property spineDotCenterY Spine anchor dot center y-coordinate.
 * @property spineDotRadius Radius of the spine anchor dot in pixels.
 * @property badgeCenterX Step badge center x-coordinate.
 * @property badgeCenterY Step badge center y-coordinate.
 * @property badgeRadius Step badge radius in pixels.
 * @property cardLeft Left edge of the step card in pixels.
 * @property cardTop Top edge of the step card in pixels.
 * @property cardRight Right edge of the step card in pixels.
 * @property cardBottom Bottom edge of the step card in pixels.
 * @property cardCornerRadius Rounded corner radius of the step card in pixels.
 * @property connectorStartX Connector start x-coordinate.
 * @property connectorStartY Connector start y-coordinate.
 * @property connectorEndX Connector end x-coordinate.
 * @property connectorEndY Connector end y-coordinate.
 * @property iconCenterX Trailing icon circle center x-coordinate.
 * @property iconCenterY Trailing icon circle center y-coordinate.
 * @property iconRadius Trailing icon circle radius in pixels.
 * @property titleX Left anchor used for the step title text.
 * @property titleBaselineY Baseline used for the step title text.
 * @property bodyX Left anchor used for the step description text.
 * @property bodyBaselineY Baseline used for the step description text.
 * @property textRight Right edge available to text content before the icon slot.
 * @see StepFlowStep
 */
data class StepFlowStepLayout(
    val originalIndex: Int,
    val step: StepFlowStep,
    val spineDotCenterX: Float,
    val spineDotCenterY: Float,
    val spineDotRadius: Float,
    val badgeCenterX: Float,
    val badgeCenterY: Float,
    val badgeRadius: Float,
    val cardLeft: Float,
    val cardTop: Float,
    val cardRight: Float,
    val cardBottom: Float,
    val cardCornerRadius: Float,
    val connectorStartX: Float,
    val connectorStartY: Float,
    val connectorEndX: Float,
    val connectorEndY: Float,
    val iconCenterX: Float,
    val iconCenterY: Float,
    val iconRadius: Float,
    val titleX: Float,
    val titleBaselineY: Float,
    val bodyX: Float,
    val bodyBaselineY: Float,
    val textRight: Float,
) {
    val width: Float
        get() = cardRight - cardLeft

    val height: Float
        get() = cardBottom - cardTop
}

/**
 * Complete shared layout result for the step flow infographic chart.
 *
 * @property hubLayout Optional hub geometry.
 * @property hubRingSegments Colored segments drawn around the hub.
 * @property spineSegments Curved spine segments connecting the step anchors.
 * @property tailDots Decorative tail dots rendered at the ends of the spine.
 * @property stepLayouts Full per-step geometry in draw order.
 * @property isRenderable Whether the layout is valid and can be rendered.
 * @property emptyReason Optional failure reason when [isRenderable] is `false`.
 * @see StepFlowHubLayout
 * @see StepFlowStepLayout
 */
data class StepFlowChartLayoutResult(
    val hubLayout: StepFlowHubLayout?,
    val hubRingSegments: List<StepFlowHubRingSegment>,
    val spineSegments: List<StepFlowSpineSegment>,
    val tailDots: List<StepFlowTailDotLayout>,
    val stepLayouts: List<StepFlowStepLayout>,
    val isRenderable: Boolean,
    val emptyReason: String? = null,
)

/**
 * Shared layout and hit-test engine for the step flow infographic chart.
 *
 * @see StepFlowStep
 * @see StepFlowHubContent
 * @see StepFlowChartLayoutResult
 */
object StepFlowChartLayoutEngine {

    /**
     * Builds a renderer-agnostic pixel layout for the step flow infographic chart.
     *
     * Validation fails when the supplied dimensions are non-positive, when required geometry would
     * overflow the available bounds, or when the step list contains invalid identifiers or text.
     *
     * @param hubContent Optional content for the central hub.
     * @param steps Ordered steps rendered along the curved spine.
     * @param config Pixel layout configuration for the current renderer bounds.
     * @param styleOptions Shared style values consumed by the renderers.
     * @param presentationOptions Shared behavioral options that affect geometry generation.
     * @return A normalized [StepFlowChartLayoutResult] for View and Compose renderers.
     * @see StepFlowChartLayoutConfig
     * @see StepFlowStep
     * @see StepFlowHubContent
     */
    fun compute(
        hubContent: StepFlowHubContent?,
        steps: List<StepFlowStep>,
        config: StepFlowChartLayoutConfig,
        styleOptions: StepFlowChartStyleOptions,
        presentationOptions: StepFlowChartPresentationOptions,
    ): StepFlowChartLayoutResult {
        if (config.widthPx <= 0f || config.heightPx <= 0f) return invalid("non-positive size")
        if (config.contentPaddingPx < 0f) return invalid("content padding")
        if (config.hubRadiusPx <= 0f) return invalid("hub radius")
        if (config.hubRingThicknessPx < 0f) return invalid("hub ring thickness")
        if (config.spineWidthPx <= 0f) return invalid("spine width")
        if (config.spineDotRadiusPx <= 0f) return invalid("spine dot radius")
        if (config.tailDotRadiusPx < 0f) return invalid("tail dot radius")
        if (config.badgeRadiusPx <= 0f) return invalid("badge radius")
        if (config.cardWidthPx <= 0f || config.cardHeightPx <= 0f) return invalid("card size")
        if (config.cardCornerRadiusPx < 0f) return invalid("card corner radius")
        if (config.connectorWidthPx <= 0f) return invalid("connector width")
        if (config.iconCircleRadiusPx < 0f) return invalid("icon radius")
        if (config.cardGapPx < 0f) return invalid("card gap")
        if (config.badgeOverlapPx < 0f) return invalid("badge overlap")
        if (config.topBottomInsetPx < 0f) return invalid("top bottom inset")
        if (steps.isEmpty()) return invalid("no steps")

        val trimmedIds = steps.map { it.id.trim() }
        if (trimmedIds.any { it.isEmpty() }) return invalid("blank step id")
        if (trimmedIds.distinct().size != trimmedIds.size) return invalid("duplicate step id")
        if (steps.any { it.badgeLabel.trim().isEmpty() }) return invalid("blank badge label")
        if (steps.any { it.title.trim().isEmpty() }) return invalid("blank step title")

        val maxHubRight = config.contentPaddingPx +
            (config.hubRadiusPx * 2f) +
            (config.hubRingThicknessPx * 2f)
        val hubCenterX = config.contentPaddingPx + config.hubRadiusPx + config.hubRingThicknessPx
        val hubCenterY = config.heightPx * 0.5f
        val spineBaseX = maxHubRight + max(config.cardGapPx * 0.45f, config.spineDotRadiusPx * 1.8f)
        val availableCardWidth = config.widthPx - config.contentPaddingPx - spineBaseX - (config.badgeRadiusPx * 1.65f)
        if (availableCardWidth <= 48f) return invalid("insufficient width")
        val cardWidth = min(config.cardWidthPx, availableCardWidth)
        val cardLeft = spineBaseX + (config.badgeRadiusPx * 1.25f)
        val cardRight = cardLeft + cardWidth

        val verticalStart = config.contentPaddingPx + config.topBottomInsetPx
        val verticalEnd = config.heightPx - config.contentPaddingPx - config.topBottomInsetPx
        if (verticalEnd <= verticalStart) return invalid("insufficient height")
        if (steps.size > 1) {
            val minimumGap = config.badgeRadiusPx * 2.1f
            val availableGap = (verticalEnd - verticalStart) / (steps.size - 1).toFloat()
            if (availableGap < minimumGap) return invalid("insufficient vertical spacing")
        }

        val hubLayout = hubContent?.let {
            StepFlowHubLayout(
                content = it,
                centerX = hubCenterX,
                centerY = hubCenterY,
                radius = config.hubRadiusPx,
                ringThickness = config.hubRingThicknessPx,
                eyebrowCenterX = hubCenterX,
                eyebrowBaselineY = hubCenterY - (config.hubRadiusPx * 0.28f),
                titleCenterX = hubCenterX,
                titleBaselineY = hubCenterY + (config.hubRadiusPx * 0.04f),
                descriptionCenterX = hubCenterX,
                descriptionBaselineY = hubCenterY + (config.hubRadiusPx * 0.34f),
            )
        }

        val ringSegments = buildRingSegments(
            steps = steps,
            startAngleDeg = presentationOptions.hubRingStartAngleDeg,
            sweepDeg = presentationOptions.hubRingSweepDeg,
        )

        val bendPx = ((config.widthPx - spineBaseX - config.contentPaddingPx) *
            presentationOptions.spineBendFactor.coerceIn(0f, 0.5f)).coerceAtLeast(0f)
        val topTail = if (presentationOptions.showTopTailDot) {
            StepFlowTailDotLayout(
                isTop = true,
                centerX = spineBaseX - (bendPx * 0.7f),
                centerY = verticalStart,
                radius = config.tailDotRadiusPx,
            )
        } else {
            null
        }
        val bottomTail = if (presentationOptions.showBottomTailDot) {
            StepFlowTailDotLayout(
                isTop = false,
                centerX = spineBaseX - (bendPx * 0.7f),
                centerY = verticalEnd,
                radius = config.tailDotRadiusPx,
            )
        } else {
            null
        }

        val stepLayouts = steps.mapIndexed { index, step ->
            val progress = if (steps.size == 1) 0.5f else index / (steps.lastIndex.toFloat())
            val dotCenterY = lerp(verticalStart, verticalEnd, progress)
            val dotCenterX = spineBaseX + (knotOffset(progress) * bendPx)
            val badgeCenterX = dotCenterX + config.badgeRadiusPx + (config.cardGapPx * 0.28f)
            val badgeCenterY = dotCenterY
            val currentCardLeft = max(cardLeft, badgeCenterX + config.badgeRadiusPx - config.badgeOverlapPx)
            val currentCardTop = dotCenterY - (config.cardHeightPx * 0.5f)
            val currentCardRight = currentCardLeft + cardWidth
            val currentCardBottom = currentCardTop + config.cardHeightPx
            val iconCenterX = currentCardRight - config.iconCircleRadiusPx - (config.cardHeightPx * 0.18f)
            val iconCenterY = dotCenterY
            val titleX = currentCardLeft + (config.cardHeightPx * 0.32f)
            val bodyX = titleX
            val textRight = iconCenterX - config.iconCircleRadiusPx - (config.cardHeightPx * 0.22f)

            StepFlowStepLayout(
                originalIndex = index,
                step = step,
                spineDotCenterX = dotCenterX,
                spineDotCenterY = dotCenterY,
                spineDotRadius = config.spineDotRadiusPx,
                badgeCenterX = badgeCenterX,
                badgeCenterY = badgeCenterY,
                badgeRadius = config.badgeRadiusPx,
                cardLeft = currentCardLeft,
                cardTop = currentCardTop,
                cardRight = currentCardRight,
                cardBottom = currentCardBottom,
                cardCornerRadius = min(config.cardCornerRadiusPx, config.cardHeightPx * 0.5f),
                connectorStartX = dotCenterX,
                connectorStartY = dotCenterY,
                connectorEndX = badgeCenterX - config.badgeRadiusPx,
                connectorEndY = badgeCenterY,
                iconCenterX = iconCenterX,
                iconCenterY = iconCenterY,
                iconRadius = config.iconCircleRadiusPx,
                titleX = titleX,
                titleBaselineY = currentCardTop + (config.cardHeightPx * 0.34f),
                bodyX = bodyX,
                bodyBaselineY = currentCardTop + (config.cardHeightPx * 0.62f),
                textRight = textRight,
            )
        }

        val spinePoints = buildList {
            topTail?.let { add(it.centerX to it.centerY) }
            stepLayouts.forEach { add(it.spineDotCenterX to it.spineDotCenterY) }
            bottomTail?.let { add(it.centerX to it.centerY) }
        }
        val spineSegments = buildSpineSegments(spinePoints)

        if (config.topBottomInsetPx < (config.cardHeightPx * 0.5f)) {
            return invalid("card vertical overflow")
        }

        return StepFlowChartLayoutResult(
            hubLayout = hubLayout,
            hubRingSegments = ringSegments,
            spineSegments = spineSegments,
            tailDots = listOfNotNull(topTail, bottomTail),
            stepLayouts = stepLayouts,
            isRenderable = true,
            emptyReason = null,
        )
    }

    fun hitTestStep(layout: StepFlowChartLayoutResult, x: Float, y: Float, tolerancePx: Float = 0f): Int? {
        if (!layout.isRenderable) return null
        val extra = tolerancePx.coerceAtLeast(0f)
        for (stepLayout in layout.stepLayouts.asReversed()) {
            if (pointInCircle(x, y, stepLayout.spineDotCenterX, stepLayout.spineDotCenterY, stepLayout.spineDotRadius + extra)) {
                return stepLayout.originalIndex
            }
            if (pointInCircle(x, y, stepLayout.badgeCenterX, stepLayout.badgeCenterY, stepLayout.badgeRadius + extra)) {
                return stepLayout.originalIndex
            }
            if (pointInRect(x, y, stepLayout.cardLeft - extra, stepLayout.cardTop - extra, stepLayout.cardRight + extra, stepLayout.cardBottom + extra)) {
                return stepLayout.originalIndex
            }
            val connectorTolerance = max(extra, stepLayout.badgeRadius * 0.18f)
            if (distanceToSegment(
                    x = x,
                    y = y,
                    startX = stepLayout.connectorStartX,
                    startY = stepLayout.connectorStartY,
                    endX = stepLayout.connectorEndX,
                    endY = stepLayout.connectorEndY,
                ) <= connectorTolerance
            ) {
                return stepLayout.originalIndex
            }
        }
        return null
    }

    fun hitTestHub(layout: StepFlowChartLayoutResult, x: Float, y: Float): Boolean {
        if (!layout.isRenderable) return false
        val hub = layout.hubLayout ?: return false
        return pointInCircle(x, y, hub.centerX, hub.centerY, hub.radius)
    }

    private fun buildRingSegments(
        steps: List<StepFlowStep>,
        startAngleDeg: Float,
        sweepDeg: Float,
    ): List<StepFlowHubRingSegment> {
        if (steps.isEmpty() || sweepDeg == 0f) return emptyList()
        val perStepSweep = sweepDeg / steps.size.toFloat()
        return steps.mapIndexed { index, step ->
            StepFlowHubRingSegment(
                originalIndex = index,
                color = step.accentColor,
                startAngleDeg = startAngleDeg + (perStepSweep * index),
                sweepAngleDeg = perStepSweep,
            )
        }
    }

    private fun buildSpineSegments(points: List<Pair<Float, Float>>): List<StepFlowSpineSegment> {
        if (points.size < 2) return emptyList()
        return buildList {
            for (index in 0 until points.lastIndex) {
                val (startX, startY) = points[index]
                val (endX, endY) = points[index + 1]
                val midX = (startX + endX) * 0.5f
                val midY = (startY + endY) * 0.5f
                val curveLift = abs(endX - startX) * 0.6f
                add(
                    StepFlowSpineSegment(
                        startX = startX,
                        startY = startY,
                        controlX = midX + curveLift,
                        controlY = midY,
                        endX = endX,
                        endY = endY,
                    ),
                )
            }
        }
    }

    private fun pointInCircle(x: Float, y: Float, centerX: Float, centerY: Float, radius: Float): Boolean {
        val dx = x - centerX
        val dy = y - centerY
        return ((dx * dx) + (dy * dy)) <= (radius * radius)
    }

    private fun pointInRect(x: Float, y: Float, left: Float, top: Float, right: Float, bottom: Float): Boolean {
        return x in left..right && y in top..bottom
    }

    private fun distanceToSegment(
        x: Float,
        y: Float,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
    ): Float {
        val segmentDx = endX - startX
        val segmentDy = endY - startY
        val segmentLengthSquared = (segmentDx * segmentDx) + (segmentDy * segmentDy)
        if (segmentLengthSquared <= 0f) return hypot(x - startX, y - startY)
        val projection = (((x - startX) * segmentDx) + ((y - startY) * segmentDy)) / segmentLengthSquared
        val clamped = projection.coerceIn(0f, 1f)
        val nearestX = startX + (segmentDx * clamped)
        val nearestY = startY + (segmentDy * clamped)
        return hypot(x - nearestX, y - nearestY)
    }

    private fun knotOffset(progress: Float): Float {
        val t = progress.coerceIn(0f, 1f)
        return sin01(t * PI.toFloat()).coerceAtLeast(0f).pow(0.85f)
    }

    private fun sin01(angleRad: Float): Float {
        return kotlin.math.sin(angleRad)
    }

    private fun lerp(start: Float, end: Float, fraction: Float): Float {
        return start + ((end - start) * fraction)
    }

    private fun invalid(reason: String): StepFlowChartLayoutResult {
        return StepFlowChartLayoutResult(
            hubLayout = null,
            hubRingSegments = emptyList(),
            spineSegments = emptyList(),
            tailDots = emptyList(),
            stepLayouts = emptyList(),
            isRenderable = false,
            emptyReason = reason,
        )
    }
}
