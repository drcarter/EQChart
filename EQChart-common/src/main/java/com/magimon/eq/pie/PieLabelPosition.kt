package com.magimon.eq.pie

/**
 * Slice label placement policy.
 */
enum class PieLabelPosition {
    /**
     * Places labels inside each slice.
     *
     * Keeps labels inside even for small slices, so text may overlap or clip.
     */
    INSIDE,

    /**
     * Places labels outside slices and connects them with leader lines.
     *
     * Improves readability for narrow slices, but uses more outer area.
     */
    OUTSIDE,

    /**
     * Automatically chooses inside/outside using slice angle and text width.
     *
     * Uses inside labels when space is sufficient; otherwise switches to outside labels.
     * Current implementation uses approximately 20+ degree slices plus text-width fit checks.
     */
    AUTO,
}
