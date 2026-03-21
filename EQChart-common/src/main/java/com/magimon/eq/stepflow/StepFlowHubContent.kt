package com.magimon.eq.stepflow

/**
 * Central hub content rendered inside the large circle of the step flow infographic chart.
 *
 * @property eyebrow Optional small heading shown above the hub title
 * @property title Primary hub title
 * @property description Optional body copy rendered below the title
 * @property payload Optional source payload delivered to click callbacks
 */
data class StepFlowHubContent(
    val eyebrow: String? = null,
    val title: String,
    val description: String? = null,
    val payload: Any? = null,
)
