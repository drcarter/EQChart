package com.magimon.eq.compose.funnel

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
import com.magimon.eq.compose.internal.newTextPaint
import com.magimon.eq.compose.internal.toComposeColor
import com.magimon.eq.funnel.FunnelChartPresentationOptions
import com.magimon.eq.funnel.FunnelChartStyleOptions
import com.magimon.eq.funnel.FunnelStage
import com.magimon.eq.funnel.resolveFunnelChartLayout

internal data class RenderFunnelStage(
    val topLeftX: Float,
    val topRightX: Float,
    val bottomRightX: Float,
    val bottomLeftX: Float,
    val topY: Float,
    val bottomY: Float,
    val centerX: Float,
    val centerY: Float,
    val stage: com.magimon.eq.funnel.FunnelLayoutStage,
)

internal data class ComputedFunnelChart(
    val stages: List<RenderFunnelStage>,
)

private fun dpToPx(value: Float, density: Float): Float = value * density

internal fun computeFunnelLayout(
    widthPx: Float,
    heightPx: Float,
    stages: List<FunnelStage>,
    style: FunnelChartStyleOptions,
    presentation: FunnelChartPresentationOptions,
    progress: Float,
    density: Float,
): ComputedFunnelChart {
    val resolved = resolveFunnelChartLayout(stages, style)
    if (resolved.stages.isEmpty() || widthPx <= 0f || heightPx <= 0f) {
        return ComputedFunnelChart(stages = emptyList())
    }

    val padding = dpToPx(style.contentPaddingDp, density)
    val stageGap = dpToPx(style.stageGapDp, density)
    val availableHeight = (heightPx - padding * 2f - stageGap * (resolved.stages.size - 1)).coerceAtLeast(1f)
    val stageHeight = availableHeight / resolved.stages.size.toFloat()
    val centerX = widthPx * 0.5f
    val maxWidth = (widthPx - padding * 2f).coerceAtLeast(1f)
    val animatedProgress = if (presentation.animationDirection) progress else 1f

    return ComputedFunnelChart(
        stages = resolved.stages.mapIndexed { index, stage ->
            val topY = padding + index * (stageHeight + stageGap)
            val bottomY = topY + stageHeight
            val topWidth = maxWidth * stage.topWidthRatio * animatedProgress
            val bottomWidth = maxWidth * stage.bottomWidthRatio * animatedProgress
            RenderFunnelStage(
                topLeftX = centerX - topWidth * 0.5f,
                topRightX = centerX + topWidth * 0.5f,
                bottomRightX = centerX + bottomWidth * 0.5f,
                bottomLeftX = centerX - bottomWidth * 0.5f,
                topY = topY,
                bottomY = bottomY,
                centerX = centerX,
                centerY = (topY + bottomY) * 0.5f,
                stage = stage,
            )
        },
    )
}

private fun pointInStage(x: Float, y: Float, stage: RenderFunnelStage): Boolean {
    val points = listOf(
        Offset(stage.topLeftX, stage.topY),
        Offset(stage.topRightX, stage.topY),
        Offset(stage.bottomRightX, stage.bottomY),
        Offset(stage.bottomLeftX, stage.bottomY),
    )
    var inside = false
    var j = points.lastIndex
    for (i in points.indices) {
        val yi = points[i].y
        val yj = points[j].y
        val xi = points[i].x
        val xj = points[j].x
        val intersects = ((yi > y) != (yj > y)) &&
            (x < (xj - xi) * (y - yi) / ((yj - yi).takeIf { it != 0f } ?: 1f) + xi)
        if (intersects) inside = !inside
        j = i
    }
    return inside
}

/**
 * Compose funnel chart for ordered conversion-stage style data.
 *
 * The composable consumes shared [FunnelStage] contracts and uses
 * [com.magimon.eq.funnel.resolveFunnelChartLayout] to normalize tapered stage widths before
 * drawing them on a Compose [Canvas].
 *
 * @param stages Ordered funnel stages rendered from top to bottom.
 * @param modifier Compose modifier applied to the chart container.
 * @param styleOptions Shared visual styling for borders, colors, and spacing.
 * @param presentationOptions Shared behavior for labels, values, empty state, and animation.
 * @param onStageClick Optional callback invoked when a rendered stage is tapped.
 * @see FunnelStage
 * @see FunnelChartStyleOptions
 * @see FunnelChartPresentationOptions
 */
@Composable
fun FunnelChart(
    stages: List<FunnelStage>,
    modifier: Modifier = Modifier,
    styleOptions: FunnelChartStyleOptions = FunnelChartStyleOptions(),
    presentationOptions: FunnelChartPresentationOptions = FunnelChartPresentationOptions(),
    onStageClick: ((index: Int, stage: FunnelStage, value: Double) -> Unit)? = null,
) {
    val density = LocalDensity.current
    val densityPx = density.density
    val scaledDensity = density.fontScale * densityPx
    val progress = remember { Animatable(1f) }
    var selectedStage by remember { mutableStateOf<RenderFunnelStage?>(null) }

    LaunchedEffect(stages, presentationOptions.animateOnDataChange, presentationOptions.enterAnimationDurationMs, presentationOptions.enterAnimationDelayMs) {
        if (presentationOptions.animateOnDataChange && stages.isNotEmpty()) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = presentationOptions.enterAnimationDurationMs.toInt().coerceAtLeast(0),
                    delayMillis = presentationOptions.enterAnimationDelayMs.toInt().coerceAtLeast(0),
                ),
            )
        } else {
            progress.snapTo(1f)
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val computed = remember(stages, styleOptions, presentationOptions, widthPx, heightPx, progress.value) {
            computeFunnelLayout(
                widthPx = widthPx,
                heightPx = heightPx,
                stages = stages,
                style = styleOptions,
                presentation = presentationOptions,
                progress = progress.value.coerceIn(0f, 1f),
                density = densityPx,
            )
        }

        val labelPaint = newTextPaint(
            color = styleOptions.labelTextColor,
            textSizePx = presentationOptions.labelTextSizeSp * scaledDensity,
            align = Paint.Align.CENTER,
        )
        val valuePaint = newTextPaint(
            color = styleOptions.valueTextColor,
            textSizePx = presentationOptions.valueTextSizeSp * scaledDensity,
            align = Paint.Align.CENTER,
        )
        val emptyTextPaint = newTextPaint(
            color = styleOptions.labelTextColor,
            textSizePx = 14f * scaledDensity,
            align = Paint.Align.CENTER,
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(computed.stages, onStageClick) {
                    detectTapGestures { tap ->
                        val hit = computed.stages.lastOrNull { stage -> pointInStage(tap.x, tap.y, stage) }
                        selectedStage = hit
                        hit?.let {
                            onStageClick?.invoke(
                                it.stage.index,
                                FunnelStage(
                                    label = it.stage.label,
                                    value = it.stage.value,
                                    color = it.stage.color,
                                    payload = it.stage.payload,
                                ),
                                it.stage.value,
                            )
                        }
                    }
                },
        ) {
            drawRect(styleOptions.backgroundColor.toComposeColor())

            if (computed.stages.isEmpty()) {
                if (presentationOptions.emptyText.isNotBlank()) {
                    drawContext.canvas.nativeCanvas.drawText(
                        presentationOptions.emptyText,
                        size.width * 0.5f,
                        size.height * 0.5f,
                        emptyTextPaint,
                    )
                }
                return@Canvas
            }

            computed.stages.forEach { stage ->
                val path = Path().apply {
                    moveTo(stage.topLeftX, stage.topY)
                    lineTo(stage.topRightX, stage.topY)
                    lineTo(stage.bottomRightX, stage.bottomY)
                    lineTo(stage.bottomLeftX, stage.bottomY)
                    close()
                }
                drawPath(color = stage.stage.color.toComposeColor(), path = path)
                drawPath(
                    color = styleOptions.stageBorderColor.toComposeColor(),
                    path = path,
                    style = Stroke(width = 2f * densityPx),
                )
                if (selectedStage == stage) {
                    drawPath(
                        color = styleOptions.selectedStageBorderColor.toComposeColor(),
                        path = path,
                        style = Stroke(width = 2.5f * densityPx),
                    )
                }
                if (presentationOptions.showLabels) {
                    val labelY = if (presentationOptions.showValues) stage.centerY - 2f * densityPx else stage.centerY + labelPaint.textSize * 0.35f
                    drawContext.canvas.nativeCanvas.drawText(
                        presentationOptions.stageLabelFormatter(
                            FunnelStage(
                                label = stage.stage.label,
                                value = stage.stage.value,
                                color = stage.stage.color,
                                payload = stage.stage.payload,
                            ),
                        ),
                        stage.centerX,
                        labelY,
                        labelPaint,
                    )
                }
                if (presentationOptions.showValues) {
                    val valueY = if (presentationOptions.showLabels) stage.centerY + valuePaint.textSize + 2f * densityPx else stage.centerY + valuePaint.textSize * 0.35f
                    drawContext.canvas.nativeCanvas.drawText(
                        presentationOptions.valueLabelFormatter(stage.stage.value),
                        stage.centerX,
                        valueY,
                        valuePaint,
                    )
                }
            }
        }
    }
}
