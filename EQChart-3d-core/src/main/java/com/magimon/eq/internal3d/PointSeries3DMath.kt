package com.magimon.eq.internal3d

import android.opengl.Matrix
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Shared math helpers for point-based true 3D chart families.
 */
internal object PointSeries3DMath {

    /**
     * Closed numeric domain used while normalizing input values.
     */
    data class NumericRange(
        val min: Double,
        val max: Double,
    ) {
        val span: Double
            get() = max - min
    }

    /**
     * Internal orbit camera state reused across point-series renderers.
     */
    data class OrbitCamera(
        val yawDegrees: Float,
        val pitchDegrees: Float,
        val distance: Float,
        val minDistance: Float,
        val maxDistance: Float,
        val fovDegrees: Float,
    )

    /**
     * Screen-space coordinate used for overlay placement and picking.
     */
    data class ScreenPoint(
        val x: Float,
        val y: Float,
        val depth: Float,
    )

    /**
     * Resolves a stable numeric range from raw values and optional overrides.
     */
    fun resolveRange(
        values: List<Double>,
        overrideMin: Double?,
        overrideMax: Double?,
        paddingRatio: Double = 0.05,
    ): NumericRange {
        val baseMin = values.minOrNull() ?: 0.0
        val baseMax = values.maxOrNull() ?: 1.0

        var min = overrideMin ?: baseMin
        var max = overrideMax ?: baseMax
        if (min > max) {
            val temp = min
            min = max
            max = temp
        }

        if (min == max) {
            val pad = maxOf(1.0, abs(min) * paddingRatio)
            min -= pad
            max += pad
        }

        return NumericRange(min = min, max = max)
    }

    /**
     * Normalizes a value into `0..1` within [range].
     */
    fun normalize(value: Double, range: NumericRange): Float {
        if (range.span <= 0.0) return 0.5f
        return ((value - range.min) / range.span).toFloat().coerceIn(0f, 1f)
    }

    /**
     * Maps a data-space value into world-space `-1..1`.
     */
    fun mapToWorld(value: Double, range: NumericRange): Float {
        val t = normalize(value, range)
        return -1f + (2f * t)
    }

    /**
     * Computes the orbit camera eye position for the current 3D view.
     */
    fun computeEyePosition(camera: OrbitCamera): FloatArray {
        val yaw = Math.toRadians(camera.yawDegrees.toDouble())
        val pitch = Math.toRadians(camera.pitchDegrees.toDouble())
        val horizontalRadius = (camera.distance * cos(pitch)).toFloat()
        return floatArrayOf(
            (horizontalRadius * sin(yaw)).toFloat(),
            (camera.distance * sin(pitch)).toFloat(),
            (horizontalRadius * cos(yaw)).toFloat(),
        )
    }

    /**
     * Builds the camera view-projection matrix shared by renderers and overlays.
     */
    fun buildViewProjectionMatrix(
        camera: OrbitCamera,
        aspectRatio: Float,
    ): FloatArray {
        val eyePosition = computeEyePosition(camera)
        val viewMatrix = FloatArray(16)
        val projectionMatrix = FloatArray(16)
        val viewProjectionMatrix = FloatArray(16)

        Matrix.setLookAtM(
            viewMatrix,
            0,
            eyePosition[0],
            eyePosition[1],
            eyePosition[2],
            0f,
            0f,
            0f,
            0f,
            1f,
            0f,
        )
        Matrix.perspectiveM(
            projectionMatrix,
            0,
            camera.fovDegrees,
            aspectRatio,
            0.1f,
            20f,
        )
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        return viewProjectionMatrix
    }

    /**
     * Projects a world-space point into the current viewport.
     */
    fun projectWorldToScreen(
        x: Float,
        y: Float,
        z: Float,
        viewProjectionMatrix: FloatArray,
        viewportWidth: Int,
        viewportHeight: Int,
    ): ScreenPoint? {
        if (viewportWidth <= 0 || viewportHeight <= 0) return null

        val clip = FloatArray(4)
        Matrix.multiplyMV(
            clip,
            0,
            viewProjectionMatrix,
            0,
            floatArrayOf(x, y, z, 1f),
            0,
        )
        val w = clip[3]
        if (abs(w) < 1e-6f || w <= 0f) return null

        val xNdc = clip[0] / w
        val yNdc = clip[1] / w
        val zNdc = clip[2] / w
        if (xNdc !in -1f..1f || yNdc !in -1f..1f || zNdc !in -1f..1f) return null

        return ScreenPoint(
            x = (xNdc + 1f) * 0.5f * viewportWidth,
            y = (1f - yNdc) * 0.5f * viewportHeight,
            depth = zNdc,
        )
    }
}
