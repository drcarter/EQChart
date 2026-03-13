package com.magimon.eq.pie

import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.cos

/**
 * Pie/donut coordinate and angle math helpers.
 */
internal object PieDonutChartMath {

    data class Vec2(
        val x: Float,
        val y: Float,
    )

    data class SliceSegment(
        val index: Int,
        val slice: PieSlice,
        val ratio: Float,
        val startAngleDeg: Float,
        val sweepAngleDeg: Float,
        val midAngleDeg: Float,
    )

    data class HitResult(
        val index: Int,
        val distancePx: Float,
    )

    fun validSlices(slices: List<PieSlice>): List<PieSlice> {
        return slices.filter { it.value.isFinite() && it.value > 0.0 }
    }

    fun buildSegments(
        slices: List<PieSlice>,
        startAngleDeg: Float,
        clockwise: Boolean,
    ): List<SliceSegment> {
        val valid = validSlices(slices)
        if (valid.isEmpty()) return emptyList()

        val total = valid.sumOf { it.value }
        if (!total.isFinite() || total <= 0.0) return emptyList()

        val sign = if (clockwise) 1f else -1f
        val segments = ArrayList<SliceSegment>(valid.size)
        var cursor = startAngleDeg
        var consumedAbs = 0f

        valid.forEachIndexed { index, slice ->
            val ratio = (slice.value / total).toFloat().coerceAtLeast(0f)
            val isLast = index == valid.lastIndex
            val sweepAbs = if (isLast) {
                (360f - consumedAbs).coerceAtLeast(0f)
            } else {
                (ratio * 360f).coerceAtLeast(0f)
            }
            consumedAbs += sweepAbs
            val sweep = sweepAbs * sign
            val mid = cursor + (sweep * 0.5f)
            segments += SliceSegment(
                index = index,
                slice = slice,
                ratio = ratio,
                startAngleDeg = cursor,
                sweepAngleDeg = sweep,
                midAngleDeg = mid,
            )
            cursor += sweep
        }
        return segments
    }

    fun pointOnCircle(centerX: Float, centerY: Float, radius: Float, angleDeg: Float): Vec2 {
        val rad = Math.toRadians(angleDeg.toDouble())
        return Vec2(
            x = centerX + (cos(rad) * radius).toFloat(),
            y = centerY + (sin(rad) * radius).toFloat(),
        )
    }

    fun hitTest(
        touchX: Float,
        touchY: Float,
        centerX: Float,
        centerY: Float,
        innerRadius: Float,
        outerRadius: Float,
        segments: List<SliceSegment>,
    ): HitResult? {
        if (segments.isEmpty() || outerRadius <= 0f) return null

        val dx = touchX - centerX
        val dy = touchY - centerY
        val distance = hypot(dx, dy)
        if (distance < innerRadius || distance > outerRadius) return null

        val angle = normalizeAngle(Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat())
        val segment = segments.firstOrNull { containsAngle(angle, it.startAngleDeg, it.sweepAngleDeg) } ?: return null
        return HitResult(index = segment.index, distancePx = distance)
    }

    internal fun containsAngle(angleDeg: Float, startDeg: Float, sweepDeg: Float): Boolean {
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

    fun normalizeAngle(angleDeg: Float): Float {
        var angle = angleDeg % 360f
        if (angle < 0f) angle += 360f
        return angle
    }
}
