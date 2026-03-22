package com.magimon.eq.compose.stepflow

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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.magimon.eq.compose.internal.newTextPaint
import com.magimon.eq.compose.internal.toComposeColor
import com.magimon.eq.stepflow.StepFlowChartPresentationOptions
import com.magimon.eq.stepflow.StepFlowChartStyleOptions
import com.magimon.eq.stepflow.StepFlowHubContent
import com.magimon.eq.stepflow.StepFlowSpineSegment
import com.magimon.eq.stepflow.StepFlowStep
import com.magimon.eq.stepflow.StepFlowStepLayout

/**
 * Compose step flow infographic chart with an optional hub and ordered steps.
 *
 * The composable consumes shared [StepFlowHubContent] and [StepFlowStep] contracts, then uses the
 * common layout engine through Compose-specific helpers to render the hub, spine, badges, and
 * cards.
 *
 * @param hubContent Optional central hub content.
 * @param steps Ordered steps rendered along the curved spine.
 * @param modifier Compose modifier applied to the chart container.
 * @param styleOptions Shared visual styling for hub, spine, badges, cards, and selection.
 * @param presentationOptions Shared behavior controlling descriptions, curvature, and animation.
 * @param onStepClick Optional callback invoked when a rendered step is tapped.
 * @param onHubClick Optional callback invoked when the central hub is tapped.
 * @see StepFlowHubContent
 * @see StepFlowStep
 * @see StepFlowChartStyleOptions
 * @see StepFlowChartPresentationOptions
 */
@Composable
fun StepFlowChart(
    hubContent: StepFlowHubContent?,
    steps: List<StepFlowStep>,
    modifier: Modifier = Modifier,
    styleOptions: StepFlowChartStyleOptions = StepFlowChartStyleOptions(),
    presentationOptions: StepFlowChartPresentationOptions = StepFlowChartPresentationOptions(),
    onStepClick: ((stepIndex: Int, step: StepFlowStep, payload: Any?) -> Unit)? = null,
    onHubClick: ((content: StepFlowHubContent, payload: Any?) -> Unit)? = null,
) {
    val density = LocalDensity.current
    var selectedStepIndex by remember { mutableStateOf<Int?>(null) }
    var hubSelected by remember { mutableStateOf(false) }
    val renderProgress = remember { Animatable(1f) }

    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val layout = remember(hubContent, steps, widthPx, heightPx, styleOptions, presentationOptions) {
            computeStepFlowLayout(
                hubContent = hubContent,
                steps = steps,
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
                        val selection = resolveStepFlowTapSelection(
                            layout = layout,
                            tap = tap,
                            stepHitTolerancePx = 8.dp.toPx(),
                        )
                        when {
                            selection.hubSelected && hubContent != null -> {
                                hubSelected = true
                                selectedStepIndex = null
                                onHubClick?.invoke(hubContent, hubContent.payload)
                            }
                            selection.selectedStepIndex != null -> {
                                hubSelected = false
                                selectedStepIndex = selection.selectedStepIndex
                                steps.getOrNull(selection.selectedStepIndex)?.let {
                                    onStepClick?.invoke(selection.selectedStepIndex, it, it.payload)
                                }
                            }
                            else -> {
                                hubSelected = false
                                selectedStepIndex = null
                            }
                        }
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
                            color = styleOptions.cardBodyTextColor,
                            textSizePx = 13f.sp.toPx(),
                            align = Paint.Align.CENTER,
                        ),
                    )
                }
                return@Canvas
            }

            layout.spineSegments.forEach { segment ->
                drawSpineSegment(
                    segment = segment,
                    color = stepFlowApplyAlpha(
                        styleOptions.spineColor,
                        stepFlowSpineAlpha(selectedStepIndex, hubSelected, renderProgress.value),
                    ),
                    strokeWidth = with(density) { styleOptions.spineWidthDp.dp.toPx() },
                )
            }
            layout.tailDots.forEach { tail ->
                drawCircle(
                    color = stepFlowApplyAlpha(
                        styleOptions.spineColor,
                        stepFlowSpineAlpha(selectedStepIndex, hubSelected, renderProgress.value),
                    ).toComposeColor(),
                    radius = tail.radius,
                    center = Offset(tail.centerX, tail.centerY),
                )
            }

            layout.hubLayout?.let { hub ->
                layout.hubRingSegments.forEach { segment ->
                    drawArc(
                        color = stepFlowApplyAlpha(
                            segment.color,
                            stepFlowRingSegmentAlpha(segment, selectedStepIndex, hubSelected, renderProgress.value),
                        ).toComposeColor(),
                        startAngle = segment.startAngleDeg,
                        sweepAngle = segment.sweepAngleDeg,
                        useCenter = false,
                        topLeft = Offset(hub.left - hub.ringThickness, hub.top - hub.ringThickness),
                        size = Size(
                            (hub.radius + hub.ringThickness) * 2f,
                            (hub.radius + hub.ringThickness) * 2f,
                        ),
                        style = Stroke(width = hub.ringThickness, cap = StrokeCap.Round),
                    )
                }
                drawCircle(
                    color = stepFlowApplyAlpha(
                        styleOptions.hubFillColor,
                        stepFlowHubAlpha(selectedStepIndex, hubSelected, renderProgress.value),
                    ).toComposeColor(),
                    radius = hub.radius,
                    center = Offset(hub.centerX, hub.centerY),
                )
                drawCircle(
                    color = stepFlowApplyAlpha(
                        if (hubSelected) styleOptions.selectedStrokeColor else styleOptions.hubStrokeColor,
                        stepFlowHubAlpha(selectedStepIndex, hubSelected, renderProgress.value),
                    ).toComposeColor(),
                    radius = hub.radius,
                    center = Offset(hub.centerX, hub.centerY),
                    style = Stroke(
                        width = with(density) {
                            (if (hubSelected) styleOptions.selectedStrokeWidthDp else styleOptions.hubStrokeWidthDp).dp.toPx()
                        },
                    ),
                )

                val eyebrowText = hub.content.eyebrow?.trim().orEmpty()
                if (eyebrowText.isNotEmpty()) {
                    drawContext.canvas.nativeCanvas.drawText(
                        eyebrowText,
                        hub.eyebrowCenterX,
                        hub.eyebrowBaselineY,
                        newTextPaint(
                            color = stepFlowApplyAlpha(
                                styleOptions.hubEyebrowTextColor,
                                stepFlowHubAlpha(selectedStepIndex, hubSelected, renderProgress.value),
                            ),
                            textSizePx = styleOptions.hubEyebrowTextSizeSp.sp.toPx(),
                            align = Paint.Align.CENTER,
                        ),
                    )
                }
                drawContext.canvas.nativeCanvas.drawText(
                    hub.content.title,
                    hub.titleCenterX,
                    hub.titleBaselineY,
                    newTextPaint(
                        color = stepFlowApplyAlpha(
                            styleOptions.hubTitleTextColor,
                            stepFlowHubAlpha(selectedStepIndex, hubSelected, renderProgress.value),
                        ),
                        textSizePx = styleOptions.hubTitleTextSizeSp.sp.toPx(),
                        align = Paint.Align.CENTER,
                        bold = true,
                    ),
                )
                val hubBody = stepFlowDisplayBody(hub.content.description, presentationOptions.showHubDescription)
                if (hubBody.isNotEmpty()) {
                    drawContext.canvas.nativeCanvas.drawText(
                        hubBody,
                        hub.descriptionCenterX,
                        hub.descriptionBaselineY,
                        newTextPaint(
                            color = stepFlowApplyAlpha(
                                styleOptions.hubBodyTextColor,
                                stepFlowHubAlpha(selectedStepIndex, hubSelected, renderProgress.value),
                            ),
                            textSizePx = styleOptions.hubBodyTextSizeSp.sp.toPx(),
                            align = Paint.Align.CENTER,
                        ),
                    )
                }
            }

            layout.stepLayouts.forEach { stepLayout ->
                drawStepFlowStep(
                    stepLayout = stepLayout,
                    styleOptions = styleOptions,
                    presentationOptions = presentationOptions,
                    renderProgress = renderProgress.value,
                    selectedStepIndex = selectedStepIndex,
                    hubSelected = hubSelected,
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSpineSegment(
    segment: StepFlowSpineSegment,
    color: Int,
    strokeWidth: Float,
) {
    val path = Path().apply {
        moveTo(segment.startX, segment.startY)
        quadraticTo(segment.controlX, segment.controlY, segment.endX, segment.endY)
    }
    drawPath(
        path = path,
        color = color.toComposeColor(),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStepFlowStep(
    stepLayout: StepFlowStepLayout,
    styleOptions: StepFlowChartStyleOptions,
    presentationOptions: StepFlowChartPresentationOptions,
    renderProgress: Float,
    selectedStepIndex: Int?,
    hubSelected: Boolean,
) {
    val alpha = stepFlowStepAlpha(stepLayout.originalIndex, selectedStepIndex, hubSelected, renderProgress)
    val highlighted = stepFlowStepHighlighted(stepLayout.originalIndex, selectedStepIndex, hubSelected)
    val strokeColor = if (highlighted) styleOptions.selectedStrokeColor else styleOptions.badgeStrokeColor
    val strokeWidth = if (highlighted) styleOptions.selectedStrokeWidthDp.dp.toPx() else styleOptions.badgeStrokeWidthDp.dp.toPx()

    drawLine(
        color = stepFlowApplyAlpha(styleOptions.connectorColor, alpha).toComposeColor(),
        start = Offset(stepLayout.connectorStartX, stepLayout.connectorStartY),
        end = Offset(stepLayout.connectorEndX, stepLayout.connectorEndY),
        strokeWidth = styleOptions.connectorWidthDp.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawCircle(
        color = stepFlowApplyAlpha(stepLayout.step.accentColor, alpha).toComposeColor(),
        radius = stepLayout.spineDotRadius,
        center = Offset(stepLayout.spineDotCenterX, stepLayout.spineDotCenterY),
    )
    drawRoundRect(
        color = stepFlowApplyAlpha(stepLayout.step.accentColor, alpha).toComposeColor(),
        topLeft = Offset(stepLayout.cardLeft, stepLayout.cardTop),
        size = Size(stepLayout.width, stepLayout.height),
        cornerRadius = CornerRadius(stepLayout.cardCornerRadius, stepLayout.cardCornerRadius),
        style = Fill,
    )
    drawRoundRect(
        color = stepFlowApplyAlpha(strokeColor, alpha).toComposeColor(),
        topLeft = Offset(stepLayout.cardLeft, stepLayout.cardTop),
        size = Size(stepLayout.width, stepLayout.height),
        cornerRadius = CornerRadius(stepLayout.cardCornerRadius, stepLayout.cardCornerRadius),
        style = Stroke(width = if (highlighted) strokeWidth else 0f),
    )
    drawCircle(
        color = stepFlowApplyAlpha(styleOptions.badgeFillColor, alpha).toComposeColor(),
        radius = stepLayout.badgeRadius,
        center = Offset(stepLayout.badgeCenterX, stepLayout.badgeCenterY),
    )
    drawCircle(
        color = stepFlowApplyAlpha(strokeColor, alpha).toComposeColor(),
        radius = stepLayout.badgeRadius,
        center = Offset(stepLayout.badgeCenterX, stepLayout.badgeCenterY),
        style = Stroke(width = strokeWidth),
    )
    drawCircle(
        color = stepFlowApplyAlpha(styleOptions.iconCircleFillColor, alpha).toComposeColor(),
        radius = stepLayout.iconRadius,
        center = Offset(stepLayout.iconCenterX, stepLayout.iconCenterY),
    )

    val badgePaint = newTextPaint(
        color = stepFlowApplyAlpha(styleOptions.badgeTextColor, alpha),
        textSizePx = styleOptions.badgeTextSizeSp.sp.toPx(),
        align = Paint.Align.CENTER,
        bold = true,
    )
    val badgeBaseline = stepLayout.badgeCenterY - ((badgePaint.descent() + badgePaint.ascent()) * 0.5f)
    drawContext.canvas.nativeCanvas.drawText(
        stepLayout.step.badgeLabel,
        stepLayout.badgeCenterX,
        badgeBaseline,
        badgePaint,
    )

    val titlePaint = newTextPaint(
        color = stepFlowApplyAlpha(styleOptions.cardTitleTextColor, alpha),
        textSizePx = styleOptions.cardTitleTextSizeSp.sp.toPx(),
        bold = true,
    )
    drawContext.canvas.nativeCanvas.drawText(
        stepLayout.step.title,
        stepLayout.titleX,
        stepLayout.titleBaselineY,
        titlePaint,
    )

    val bodyText = stepFlowDisplayBody(stepLayout.step.description, presentationOptions.showStepDescriptions)
    if (bodyText.isNotEmpty()) {
        drawContext.canvas.nativeCanvas.drawText(
            bodyText,
            stepLayout.bodyX,
            stepLayout.bodyBaselineY,
            newTextPaint(
                color = stepFlowApplyAlpha(styleOptions.cardBodyTextColor, alpha),
                textSizePx = styleOptions.cardBodyTextSizeSp.sp.toPx(),
            ),
        )
    }

    val iconPaint = newTextPaint(
        color = stepFlowApplyAlpha(stepLayout.step.accentColor, alpha),
        textSizePx = styleOptions.iconTextSizeSp.sp.toPx(),
        align = Paint.Align.CENTER,
        bold = true,
    )
    val iconBaseline = stepLayout.iconCenterY - ((iconPaint.descent() + iconPaint.ascent()) * 0.5f)
    drawContext.canvas.nativeCanvas.drawText(
        stepFlowIconText(stepLayout.step),
        stepLayout.iconCenterX,
        iconBaseline,
        iconPaint,
    )
}
