package com.magimon.eq.compose.stepflow

import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.magimon.eq.stepflow.StepFlowChartLayoutConfig
import com.magimon.eq.stepflow.StepFlowChartLayoutEngine
import com.magimon.eq.stepflow.StepFlowChartLayoutResult
import com.magimon.eq.stepflow.StepFlowChartPresentationOptions
import com.magimon.eq.stepflow.StepFlowChartStyleOptions
import com.magimon.eq.stepflow.StepFlowHubContent
import com.magimon.eq.stepflow.StepFlowHubRingSegment
import com.magimon.eq.stepflow.StepFlowStep
import kotlin.math.max
import kotlin.math.roundToInt

internal data class StepFlowTapSelection(
    val selectedStepIndex: Int? = null,
    val hubSelected: Boolean = false,
)

internal fun computeStepFlowLayout(
    hubContent: StepFlowHubContent?,
    steps: List<StepFlowStep>,
    widthPx: Float,
    heightPx: Float,
    density: Density,
    styleOptions: StepFlowChartStyleOptions,
    presentationOptions: StepFlowChartPresentationOptions,
): StepFlowChartLayoutResult {
    val cardHeightPx = with(density) { styleOptions.cardHeightDp.dp.toPx() }
    val badgeRadiusPx = with(density) { styleOptions.badgeRadiusDp.dp.toPx() }
    val config = StepFlowChartLayoutConfig(
        widthPx = widthPx,
        heightPx = heightPx,
        contentPaddingPx = with(density) { styleOptions.contentPaddingDp.dp.toPx() },
        hubRadiusPx = with(density) { styleOptions.hubRadiusDp.dp.toPx() },
        hubRingThicknessPx = with(density) { styleOptions.hubRingThicknessDp.dp.toPx() },
        spineWidthPx = with(density) { styleOptions.spineWidthDp.dp.toPx() },
        spineDotRadiusPx = with(density) { styleOptions.spineDotRadiusDp.dp.toPx() },
        tailDotRadiusPx = with(density) { styleOptions.tailDotRadiusDp.dp.toPx() },
        badgeRadiusPx = badgeRadiusPx,
        cardWidthPx = with(density) { styleOptions.cardWidthDp.dp.toPx() },
        cardHeightPx = cardHeightPx,
        cardCornerRadiusPx = with(density) { styleOptions.cardCornerRadiusDp.dp.toPx() },
        connectorWidthPx = with(density) { styleOptions.connectorWidthDp.dp.toPx() },
        iconCircleRadiusPx = with(density) { styleOptions.iconCircleRadiusDp.dp.toPx() },
        cardGapPx = with(density) { 16.dp.toPx() },
        badgeOverlapPx = badgeRadiusPx * 0.48f,
        topBottomInsetPx = max(cardHeightPx * 0.55f, badgeRadiusPx * 1.5f),
    ).scaleToFit(steps.size)

    return StepFlowChartLayoutEngine.compute(
        hubContent = hubContent,
        steps = steps,
        config = config,
        styleOptions = styleOptions,
        presentationOptions = presentationOptions,
    )
}

internal fun resolveStepFlowTapSelection(
    layout: StepFlowChartLayoutResult,
    tap: Offset,
    stepHitTolerancePx: Float,
): StepFlowTapSelection {
    if (StepFlowChartLayoutEngine.hitTestHub(layout, tap.x, tap.y)) {
        return StepFlowTapSelection(hubSelected = true)
    }
    val stepHit = StepFlowChartLayoutEngine.hitTestStep(layout, tap.x, tap.y, stepHitTolerancePx)
    return if (stepHit != null) {
        StepFlowTapSelection(selectedStepIndex = stepHit)
    } else {
        StepFlowTapSelection()
    }
}

internal fun stepFlowStepHighlighted(
    stepOriginalIndex: Int,
    selectedStepIndex: Int?,
    hubSelected: Boolean,
): Boolean {
    return !hubSelected && selectedStepIndex == stepOriginalIndex
}

internal fun stepFlowStepAlpha(
    stepOriginalIndex: Int,
    selectedStepIndex: Int?,
    hubSelected: Boolean,
    renderProgress: Float,
): Float {
    return when {
        stepFlowStepHighlighted(stepOriginalIndex, selectedStepIndex, hubSelected) -> renderProgress
        hubSelected -> 0.42f * renderProgress
        selectedStepIndex != null -> 0.28f * renderProgress
        else -> renderProgress
    }
}

internal fun stepFlowHubAlpha(
    selectedStepIndex: Int?,
    hubSelected: Boolean,
    renderProgress: Float,
): Float {
    return when {
        hubSelected -> renderProgress
        selectedStepIndex != null -> 0.35f * renderProgress
        else -> renderProgress
    }
}

internal fun stepFlowRingSegmentAlpha(
    segment: StepFlowHubRingSegment,
    selectedStepIndex: Int?,
    hubSelected: Boolean,
    renderProgress: Float,
): Float {
    return when {
        hubSelected -> renderProgress
        selectedStepIndex == segment.originalIndex -> renderProgress
        selectedStepIndex != null -> 0.22f * renderProgress
        else -> renderProgress
    }
}

internal fun stepFlowSpineAlpha(
    selectedStepIndex: Int?,
    hubSelected: Boolean,
    renderProgress: Float,
): Float {
    return when {
        hubSelected -> 0.55f * renderProgress
        selectedStepIndex != null -> 0.3f * renderProgress
        else -> renderProgress
    }
}

internal fun stepFlowDisplayBody(
    raw: String?,
    showBody: Boolean,
): String {
    if (!showBody) return ""
    return raw?.trim().orEmpty()
}

internal fun stepFlowIconText(step: StepFlowStep): String {
    return step.iconText?.trim().orEmpty().ifBlank { "•" }
}

internal fun stepFlowApplyAlpha(
    color: Int,
    factor: Float,
): Int {
    val clamped = factor.coerceIn(0f, 1f)
    val outputAlpha = (Color.alpha(color) * clamped).roundToInt().coerceIn(0, 255)
    return Color.argb(outputAlpha, Color.red(color), Color.green(color), Color.blue(color))
}
