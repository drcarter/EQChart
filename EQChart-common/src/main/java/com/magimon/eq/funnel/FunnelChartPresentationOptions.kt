package com.magimon.eq.funnel

/**
 * Behavioral options for funnel charts.
 *
 * @property showLabels Whether to draw stage labels.
 * @property showValues Whether to draw stage values.
 * @property animateOnDataChange Whether to animate on data changes.
 * @property enterAnimationDurationMs Duration for enter animation.
 * @property enterAnimationDelayMs Delay before enter animation starts.
 * @property animationDirection If true, funnel widths animate outward from center.
 * @property emptyText Message shown when there is no renderable data.
 * @property labelTextSizeSp Text size for stage labels.
 * @property valueTextSizeSp Text size for stage values.
 * @property stageLabelFormatter Converts a [FunnelStage] into display text.
 * @property valueLabelFormatter Converts numeric stage values into display text.
 */
data class FunnelChartPresentationOptions(
    val showLabels: Boolean = true,
    val showValues: Boolean = true,
    val animateOnDataChange: Boolean = true,
    val enterAnimationDurationMs: Long = 680L,
    val enterAnimationDelayMs: Long = 30L,
    val animationDirection: Boolean = true,
    val emptyText: String = "No data",
    val labelTextSizeSp: Float = 12f,
    val valueTextSizeSp: Float = 11.5f,
    val stageLabelFormatter: (FunnelStage) -> String = { it.label },
    val valueLabelFormatter: (Double) -> String = { it.toInt().toString() },
)
