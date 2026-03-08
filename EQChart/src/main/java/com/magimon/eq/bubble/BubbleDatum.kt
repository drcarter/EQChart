package com.magimon.eq.bubble

/**
 * Data model for one bubble used by [BubbleChartView].
 *
 * @property x X value used in [BubbleLayoutMode.SCATTER]
 * @property y Y value used in [BubbleLayoutMode.SCATTER]
 * @property size Value used to map bubble radius
 * @property color Bubble fill color
 * @property label Optional label rendered inside the bubble. Use `\n` for multiline text.
 * @property legendGroup Optional group label used for auto legend generation
 * @property payload Optional custom object delivered in click callbacks
 */
data class BubbleDatum(
    val x: Double,
    val y: Double,
    val size: Double,
    val color: Int,
    val label: String? = null,
    val legendGroup: String? = null,
    val payload: Any? = null,
)
