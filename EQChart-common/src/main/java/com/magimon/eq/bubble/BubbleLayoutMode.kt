package com.magimon.eq.bubble

/**
 * Bubble coordinate/layout strategy.
 *
 * Switch with [BubbleChartView.setLayoutMode]. Each mode changes how data is interpreted
 * and how axes/grid are used.
 */
enum class BubbleLayoutMode {
    /**
     * Scatter mode that linearly maps `x/y` values into the plot area.
     *
     * - Uses [BubbleDatum.x] and [BubbleDatum.y] directly.
     * - Axis/grid/tick settings from [BubbleAxisOptions] are applied.
     * - Bubbles can overlap when multiple points share nearby coordinates.
     */
    SCATTER,

    /**
     * Packed mode that clusters bubbles near the center while resolving overlap.
     *
     * - Ignores `x/y` and derives radius from `size` only.
     * - Starts from a spiral seed and iteratively resolves collisions.
     * - Axes/ticks are typically disabled because they have no semantic meaning in this mode.
     */
    PACKED,
}
