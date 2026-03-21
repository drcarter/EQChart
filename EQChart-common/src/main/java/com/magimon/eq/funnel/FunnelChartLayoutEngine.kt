package com.magimon.eq.funnel

import kotlin.math.max

/**
 * Render-ready funnel stage description resolved from input stages.
 */
data class FunnelLayoutStage(
    val index: Int,
    val label: String,
    val value: Double,
    val topWidthRatio: Float,
    val bottomWidthRatio: Float,
    val color: Int,
    val payload: Any?,
)

/**
 * Shared normalized layout result for funnel charts.
 */
data class FunnelChartLayout(
    val stages: List<FunnelLayoutStage>,
    val maxValue: Double,
)

/**
 * Resolves sanitized stages, relative widths, and display colors for funnel charts.
 */
fun resolveFunnelChartLayout(
    stages: List<FunnelStage>,
    style: FunnelChartStyleOptions = FunnelChartStyleOptions(),
): FunnelChartLayout {
    val sanitized = stages.filter { stage ->
        stage.label.isNotBlank() && stage.value.isFinite() && stage.value > 0.0
    }
    if (sanitized.isEmpty()) {
        return FunnelChartLayout(
            stages = emptyList(),
            maxValue = 1.0,
        )
    }

    var maxValue = 0.0
    for (stage in sanitized) {
        if (stage.value > maxValue) maxValue = stage.value
    }
    fun widthRatio(value: Double): Float {
        val normalized = (value / maxValue).toFloat().coerceIn(0f, 1f)
        return style.minStageWidthRatio + (1f - style.minStageWidthRatio) * normalized
    }

    val layoutStages = sanitized.mapIndexed { index, stage ->
        val topWidth = widthRatio(stage.value)
        val bottomWidth = when {
            sanitized.size == 1 -> topWidth
            index < sanitized.lastIndex -> widthRatio(sanitized[index + 1].value)
            else -> max(style.tipWidthRatio, style.minStageWidthRatio * 0.7f)
        }
        FunnelLayoutStage(
            index = index,
            label = stage.label,
            value = stage.value,
            topWidthRatio = topWidth,
            bottomWidthRatio = bottomWidth.coerceIn(0.05f, 1f),
            color = stage.color ?: style.stageColors[index % style.stageColors.size],
            payload = stage.payload,
        )
    }

    return FunnelChartLayout(
        stages = layoutStages,
        maxValue = maxValue,
    )
}
