package com.magimon.eq.compose

import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Converts an Android ARGB color int into a Compose [Color].
 */
internal fun Int.toComposeColor(): Color = Color(this)

/**
 * Resolves a Cartesian offset on a circle from degrees and radius.
 */
internal fun degreeToOffset(angleDeg: Float, radius: Float): Offset {
    val rad = angleDeg * (PI.toFloat() / 180f)
    return Offset(cos(rad) * radius, sin(rad) * radius)
}

/**
 * Normalizes an angle into the `0..360` domain.
 */
internal fun normalizeAngle(angle: Float): Float {
    var value = angle % 360f
    if (value < 0f) value += 360f
    return value
}

/**
 * Returns whether [angleDeg] falls inside the directed sweep described by [startDeg] and [sweepDeg].
 */
internal fun isAngleInSweep(angleDeg: Float, startDeg: Float, sweepDeg: Float): Boolean {
    if (sweepDeg == 0f) return false
    val angle = normalizeAngle(angleDeg)
    val start = normalizeAngle(startDeg)
    return if (sweepDeg > 0f) {
        val delta = normalizeAngle(angle - start)
        delta <= sweepDeg + 0.0001f
    } else {
        val delta = normalizeAngle(start - angle)
        delta <= -sweepDeg + 0.0001f
    }
}

/**
 * Creates a configured anti-aliased [Paint] instance for Compose canvas text rendering.
 */
internal fun newTextPaint(
    color: Int,
    textSizePx: Float,
    align: Paint.Align = Paint.Align.LEFT,
    bold: Boolean = false,
): Paint {
    return Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        this.textSize = textSizePx
        this.textAlign = align
        isFakeBoldText = bold
    }
}
