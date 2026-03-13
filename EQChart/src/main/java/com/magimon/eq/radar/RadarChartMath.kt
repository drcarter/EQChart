package com.magimon.eq.radar

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Radar chart coordinate and hit-test math utilities.
 *
 * Internal helper that isolates math from rendering code for better testability.
 */
internal object RadarChartMath {
    /**
     * 2D coordinate.
     */
    data class Vec2(
        val x: Float,
        val y: Float,
    )

    /**
     * Touch hit-test result.
     *
     * @property seriesIndex Selected series index
     * @property axisIndex Selected axis index
     * @property distance Distance between touch coordinate and point (px)
     */
    data class HitResult(
        val seriesIndex: Int,
        val axisIndex: Int,
        val distance: Float,
    )

    /**
     * Normalizes a raw value to the `0..1` range.
     *
     * Invalid inputs (`NaN`, `Infinity`, `max<=0`) are treated as 0.
     */
    fun normalize(value: Double, maxValue: Double): Float {
        if (!value.isFinite() || !maxValue.isFinite() || maxValue <= 0.0) return 0f
        val ratio = value / maxValue
        return ratio.coerceIn(0.0, 1.0).toFloat()
    }

    /**
     * Computes the polar-coordinate vertex for a given axis index.
     *
     * @param centerX Chart center X
     * @param centerY Chart center Y
     * @param radius Radius from center (px)
     * @param axisIndex Target axis index
     * @param axisCount Total number of axes
     * @param startAngleDeg Start angle in degrees
     */
    fun vertex(
        centerX: Float,
        centerY: Float,
        radius: Float,
        axisIndex: Int,
        axisCount: Int,
        startAngleDeg: Float = -90f,
    ): Vec2 {
        if (axisCount <= 0) return Vec2(centerX, centerY)
        val angleDeg = startAngleDeg + (360f * axisIndex.toFloat() / axisCount.toFloat())
        val angleRad = angleDeg * (PI / 180.0)
        return Vec2(
            x = centerX + (cos(angleRad) * radius).toFloat(),
            y = centerY + (sin(angleRad) * radius).toFloat(),
        )
    }

    /**
     * Converts series values into polygon vertices.
     *
     * @param values Axis-ordered value list
     * @param maxValue Normalization max value
     * @param centerX Chart center X
     * @param centerY Chart center Y
     * @param radius Maximum chart radius (px)
     * @param axisCount Total number of axes
     * @param progress Enter-animation progress (0..1)
     * @param startAngleDeg Start angle in degrees
     */
    fun polygonPoints(
        values: List<Double>,
        maxValue: Double,
        centerX: Float,
        centerY: Float,
        radius: Float,
        axisCount: Int,
        progress: Float,
        startAngleDeg: Float = -90f,
    ): List<Vec2> {
        if (axisCount <= 0 || values.isEmpty()) return emptyList()
        val count = min(axisCount, values.size)
        val renderProgress = progress.coerceIn(0f, 1f)
        val points = ArrayList<Vec2>(count)
        for (axisIndex in 0 until count) {
            val norm = normalize(values[axisIndex], maxValue)
            val localRadius = radius * norm * renderProgress
            points += vertex(
                centerX = centerX,
                centerY = centerY,
                radius = localRadius,
                axisIndex = axisIndex,
                axisCount = axisCount,
                startAngleDeg = startAngleDeg,
            )
        }
        return points
    }

    /**
     * Returns the nearest point to a touch coordinate.
     *
     * @param touchX Touch X
     * @param touchY Touch Y
     * @param pointsBySeries Point list grouped by series
     * @param hitRadiusPx Allowed hit radius (px)
     * @return Nearest point within the radius, or `null` if none
     */
    fun nearestPoint(
        touchX: Float,
        touchY: Float,
        pointsBySeries: List<List<Vec2>>,
        hitRadiusPx: Float,
    ): HitResult? {
        if (pointsBySeries.isEmpty()) return null
        val hitRadius = max(1f, hitRadiusPx)
        var best: HitResult? = null
        var bestDistance = Float.MAX_VALUE

        pointsBySeries.forEachIndexed { seriesIndex, points ->
            points.forEachIndexed { axisIndex, point ->
                val distance = hypot(touchX - point.x, touchY - point.y)
                if (distance <= hitRadius && distance < bestDistance) {
                    bestDistance = distance
                    best = HitResult(seriesIndex = seriesIndex, axisIndex = axisIndex, distance = distance)
                }
            }
        }
        return best
    }
}
