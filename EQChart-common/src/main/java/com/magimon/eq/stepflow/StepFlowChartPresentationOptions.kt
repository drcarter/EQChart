package com.magimon.eq.stepflow

/**
 * Behavioral and layout options for the step flow infographic chart.
 */
data class StepFlowChartPresentationOptions(
    val animateOnDataChange: Boolean = true,
    val animationDurationMs: Long = 650L,
    val showStepDescriptions: Boolean = true,
    val showHubDescription: Boolean = true,
    val showTopTailDot: Boolean = true,
    val showBottomTailDot: Boolean = true,
    val hubRingStartAngleDeg: Float = -118f,
    val hubRingSweepDeg: Float = 252f,
    val spineBendFactor: Float = 0.18f,
    val emptyText: String? = "No data",
)
