package com.magimon.eq.gauge

/**
 * Data model for a single gauge reading.
 *
 * A [GaugeValue] defines both the raw measurement and the numeric domain used to render it.
 * The chart clamps [value] into the `[minValue, maxValue]` range before drawing, so callers do
 * not need to normalize the input themselves.
 *
 * Rendering falls back to the empty state when:
 * - [value], [minValue], or [maxValue] is non-finite
 * - [maxValue] is less than or equal to [minValue]
 *
 * @property value Current raw value to render
 * @property minValue Lower bound of the gauge domain
 * @property maxValue Upper bound of the gauge domain. Must be greater than [minValue].
 * @property label Optional secondary label rendered below the main value text
 * @property payload Optional source object preserved for integrations or external state mapping
 */
data class GaugeValue(
    val value: Double,
    val minValue: Double = 0.0,
    val maxValue: Double = 100.0,
    val label: String? = null,
    val payload: Any? = null,
)
