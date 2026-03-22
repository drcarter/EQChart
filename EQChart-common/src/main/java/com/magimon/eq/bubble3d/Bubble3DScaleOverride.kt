package com.magimon.eq.bubble3d

/**
 * Manual override options for axis/size scales in 3D.
 *
 * If a value is `null`, that dimension uses an automatic min-max range.
 */
data class Bubble3DScaleOverride(
    /** Optional manual minimum for the X domain. */
    val xMin: Double? = null,
    /** Optional manual maximum for the X domain. */
    val xMax: Double? = null,
    /** Optional manual minimum for the Y domain. */
    val yMin: Double? = null,
    /** Optional manual maximum for the Y domain. */
    val yMax: Double? = null,
    /** Optional manual minimum for the Z domain. */
    val zMin: Double? = null,
    /** Optional manual maximum for the Z domain. */
    val zMax: Double? = null,
    /** Optional manual minimum for the size domain. */
    val sizeMin: Double? = null,
    /** Optional manual maximum for the size domain. */
    val sizeMax: Double? = null,
)
