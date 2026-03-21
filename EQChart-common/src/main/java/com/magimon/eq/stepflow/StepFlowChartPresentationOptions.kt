package com.magimon.eq.stepflow

/**
 * Behavioral and layout options for the step flow infographic chart.
 *
 * @property animateOnDataChange Whether renderers should animate after content changes.
 * @property animationDurationMs Duration of the enter animation in milliseconds.
 * @property showStepDescriptions Whether step card descriptions should be drawn.
 * @property showHubDescription Whether the hub description text should be drawn.
 * @property showTopTailDot Whether the decorative top tail dot should be rendered.
 * @property showBottomTailDot Whether the decorative bottom tail dot should be rendered.
 * @property hubRingStartAngleDeg Start angle used for hub ring segments in degrees.
 * @property hubRingSweepDeg Total sweep allocated to hub ring segments in degrees.
 * @property spineBendFactor Curvature factor applied to the vertical spine path.
 * @property emptyText Optional placeholder text shown when no renderable layout can be produced.
 * @see StepFlowStep
 * @see StepFlowHubContent
 * @see StepFlowChartLayoutEngine
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
