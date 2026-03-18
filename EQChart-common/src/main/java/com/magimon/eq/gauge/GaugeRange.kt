package com.magimon.eq.gauge

/**
 * Colored reference band shown behind the current gauge value.
 *
 * A range is mapped onto the same numeric domain as [GaugeValue]. At render time the chart clamps
 * the band into the active gauge domain and silently ignores invalid ranges such as:
 * - non-finite bounds
 * - `endValue <= startValue`
 *
 * Ranges are typically used for threshold visualization such as normal/warning/critical zones.
 *
 * @property startValue Inclusive start of the reference band
 * @property endValue Inclusive end of the reference band
 * @property color Arc color used for the band
 * @property label Optional semantic label for documentation or external UI
 */
data class GaugeRange(
    val startValue: Double,
    val endValue: Double,
    val color: Int,
    val label: String? = null,
)
