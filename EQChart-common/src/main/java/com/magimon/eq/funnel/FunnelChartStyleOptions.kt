package com.magimon.eq.funnel

import android.graphics.Color

/**
 * Visual styling for funnel charts.
 *
 * @property backgroundColor Chart background color.
 * @property stageBorderColor Stage outline color.
 * @property selectedStageBorderColor Selected stage outline color.
 * @property labelTextColor Label text color.
 * @property valueTextColor Value text color.
 * @property stageColors Default stage colors used when a stage does not override its own color.
 * @property contentPaddingDp Outer padding around the chart.
 * @property stageGapDp Gap between stages.
 * @property minStageWidthRatio Minimum width ratio for the smallest stage.
 * @property tipWidthRatio Bottom width ratio for the last stage.
 */
data class FunnelChartStyleOptions(
    val backgroundColor: Int = Color.WHITE,
    val stageBorderColor: Int = Color.parseColor("#FFFFFF"),
    val selectedStageBorderColor: Int = Color.parseColor("#1F2937"),
    val labelTextColor: Int = Color.WHITE,
    val valueTextColor: Int = Color.parseColor("#E5E7EB"),
    val stageColors: List<Int> = listOf(
        Color.parseColor("#2563EB"),
        Color.parseColor("#0EA5E9"),
        Color.parseColor("#14B8A6"),
        Color.parseColor("#F59E0B"),
        Color.parseColor("#EF4444"),
    ),
    val contentPaddingDp: Float = 16f,
    val stageGapDp: Float = 6f,
    val minStageWidthRatio: Float = 0.28f,
    val tipWidthRatio: Float = 0.16f,
)
