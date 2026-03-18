package com.magimon.eq.gauge

/**
 * Behavioral and layout toggles for the gauge chart.
 *
 * These options control how the same [GaugeValue] is presented rather than how it is colored.
 * The defaults target a semi-circular dashboard gauge, but callers can rotate or widen the arc
 * by changing [startAngleDeg] and [sweepAngleDeg].
 *
 * @property startAngleDeg Arc start angle in degrees. `180` starts at 9 o'clock.
 * @property sweepAngleDeg Total sweep of the gauge arc in degrees
 * @property showTicks Whether evenly spaced tick marks are rendered around the track
 * @property tickCount Number of tick intervals between min/max. Values less than `1` are clamped internally.
 * @property showMinMaxLabels Whether the domain boundary labels are rendered at both ends of the arc
 * @property showValueText Whether the current clamped value is rendered in the center
 * @property showCenterLabel Whether [GaugeValue.label] is rendered under the main value text
 * @property animateOnValueChange Whether value changes animate the progress/indicator state
 * @property animationDurationMs Duration used when [animateOnValueChange] is enabled
 * @property emptyText Message shown when the gauge input is invalid or not renderable
 */
data class GaugeChartPresentationOptions(
    val startAngleDeg: Float = 180f,
    val sweepAngleDeg: Float = 180f,
    val showTicks: Boolean = true,
    val tickCount: Int = 5,
    val showMinMaxLabels: Boolean = true,
    val showValueText: Boolean = true,
    val showCenterLabel: Boolean = true,
    val animateOnValueChange: Boolean = true,
    val animationDurationMs: Long = 650L,
    val emptyText: String? = "No data",
)
