package com.magimon.eq.bubble

/**
 * Defines how [BubbleChartView] builds legend items.
 *
 * Chooses the legend data-source priority. Visual rendering still depends on `showLegend=true`.
 */
enum class BubbleLegendMode {
    /**
     * Auto-generate legend items from chart data.
     *
     * - Uses [BubbleDatum.legendGroup] and color to build items.
     * - Ignores explicit items from `setLegendItems`.
     * - Legend updates automatically when data changes.
     */
    AUTO,

    /**
     * Explicit-items-only mode.
     *
     * - Renders only items provided by `setLegendItems`.
     * - Can show groups that are not present in the data.
     * - If explicit items are empty, the legend may be empty as well.
     */
    EXPLICIT,

    /**
     * Explicit-first mode with auto-generation fallback.
     *
     * - Behaves like [EXPLICIT] when at least one explicit item exists.
     * - Falls back to [AUTO] only when explicit items are empty.
     * - Useful when mixing fixed legend templates with dynamic datasets.
     */
    AUTO_WITH_OVERRIDE,
}
