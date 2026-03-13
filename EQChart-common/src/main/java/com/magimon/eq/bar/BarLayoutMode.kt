package com.magimon.eq.bar

/**
 * Defines how bars in a category are arranged.
 *
 * @property GROUPED places each series' bar side-by-side.
 * @property STACKED accumulates bar heights in each category.
 */
enum class BarLayoutMode {
    GROUPED,
    STACKED,
}

