package com.magimon.eq.stepflow

/**
 * Ordered step item rendered by the step flow infographic chart.
 *
 * The chart renders steps in input order from top to bottom along a curved spine.
 *
 * @property id Stable step identifier
 * @property badgeLabel Short label rendered inside the circular step badge, for example `STEP 01`
 * @property title Primary card title shown inside the pill card
 * @property description Optional secondary body copy shown below [title]
 * @property accentColor Main accent color used for the badge, card fill, and hub ring segment
 * @property iconText Optional short icon-like text rendered inside the trailing icon slot
 * @property payload Optional source payload delivered to click callbacks
 */
data class StepFlowStep(
    val id: String,
    val badgeLabel: String,
    val title: String,
    val description: String? = null,
    val accentColor: Int,
    val iconText: String? = null,
    val payload: Any? = null,
)
