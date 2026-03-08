package com.magimon.eq.bubble

/**
 * Manual override options for axis/size scales.
 *
 * If a value is `null`, that axis/size dimension uses an automatic min-max range.
 */
data class BubbleScaleOverride(
    val xMin: Double? = null,
    val xMax: Double? = null,
    val yMin: Double? = null,
    val yMax: Double? = null,
    val sizeMin: Double? = null,
    val sizeMax: Double? = null,
)
