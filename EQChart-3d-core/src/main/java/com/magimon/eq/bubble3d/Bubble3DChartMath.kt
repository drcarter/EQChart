package com.magimon.eq.bubble3d

import android.opengl.Matrix
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Math helpers used by the 3D bubble renderer.
 */
internal object Bubble3DChartMath {

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
     * Bubble data prepared for world-space rendering.
     */
    data class SceneBubble(
        val datum: Bubble3DDatum,
        val x: Float,
        val y: Float,
        val z: Float,
        val radius: Float,
    )

    /**
     * 3D ray used for screen-to-scene picking.
     */
    data class Ray(
        val origin: FloatArray,
        val direction: FloatArray,
    )

    /**
     * Screen-space coordinate used for overlay label placement.
     */
    data class ScreenPoint(
        val x: Float,
        val y: Float,
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
     * Maps a normalized data value into world-space `-1..1`.
     */
    fun mapToWorld(value: Double, range: NumericRange): Float {
        val t = normalize(value, range)
        return -1f + (2f * t)
    }

    /**
     * Maps a data-space size value into a world-space bubble radius.
     */
    fun mapRadius(
        sizeValue: Double,
        sizeRange: NumericRange,
        minRadius: Float,
        maxRadius: Float,
    ): Float {
        val t = normalize(sizeValue, sizeRange)
        val eased = sqrt(t.toDouble()).toFloat()
        return minRadius + ((maxRadius - minRadius) * eased)
    }

    /**
     * Keeps a bubble fully inside the canonical `-1..1` scene cube.
     */
    fun clampWorldCoordinate(value: Float, radius: Float): Float {
        val min = -1f + radius
        val max = 1f - radius
        return value.coerceIn(min, max)
    }

    /**
     * Converts a normalized device coordinate back into world space.
     */
    fun unproject(
        xNdc: Float,
        yNdc: Float,
        zNdc: Float,
        inverseViewProjection: FloatArray,
    ): FloatArray? {
        val clip = floatArrayOf(xNdc, yNdc, zNdc, 1f)
        val world = FloatArray(4)
        Matrix.multiplyMV(world, 0, inverseViewProjection, 0, clip, 0)
        val w = world[3]
        if (kotlin.math.abs(w) < 1e-6f) return null
        return floatArrayOf(world[0] / w, world[1] / w, world[2] / w)
    }

    /**
     * Builds a picking ray from normalized screen coordinates.
     */
    fun rayFromNormalizedScreen(
        xNdc: Float,
        yNdc: Float,
        inverseViewProjection: FloatArray,
    ): Ray? {
        val near = unproject(xNdc, yNdc, -1f, inverseViewProjection) ?: return null
        val far = unproject(xNdc, yNdc, 1f, inverseViewProjection) ?: return null
        val direction = normalizeVector(
            floatArrayOf(
                far[0] - near[0],
                far[1] - near[1],
                far[2] - near[2],
            ),
        )
        return Ray(origin = near, direction = direction)
    }

    /**
     * Returns the nearest positive intersection distance between [ray] and a sphere.
     */
    fun intersectRaySphere(
        ray: Ray,
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        radius: Float,
    ): Float? {
        val ox = ray.origin[0] - centerX
        val oy = ray.origin[1] - centerY
        val oz = ray.origin[2] - centerZ
        val dx = ray.direction[0]
        val dy = ray.direction[1]
        val dz = ray.direction[2]

        val a = (dx * dx) + (dy * dy) + (dz * dz)
        val b = 2f * ((ox * dx) + (oy * dy) + (oz * dz))
        val c = (ox * ox) + (oy * oy) + (oz * oz) - (radius * radius)
        val discriminant = (b * b) - (4f * a * c)
        if (discriminant < 0f) return null

        val sqrtDiscriminant = sqrt(discriminant.toDouble()).toFloat()
        val near = (-b - sqrtDiscriminant) / (2f * a)
        val far = (-b + sqrtDiscriminant) / (2f * a)
        return when {
            near > 0f -> near
            far > 0f -> far
            else -> null
        }
    }

    /**
     * Normalizes a 3D direction vector.
     */
    private fun normalizeVector(vector: FloatArray): FloatArray {
        val magnitude = sqrt(
            ((vector[0] * vector[0]) + (vector[1] * vector[1]) + (vector[2] * vector[2])).toDouble(),
        ).toFloat().coerceAtLeast(1e-6f)
        return floatArrayOf(
            vector[0] / magnitude,
            vector[1] / magnitude,
            vector[2] / magnitude,
        )
    }

    /**
     * Computes the orbit camera eye position for the current 3D view.
     */
    fun computeEyePosition(camera: Bubble3DCameraOptions): FloatArray {
        val yaw = Math.toRadians(camera.yawDegrees.toDouble())
        val pitch = Math.toRadians(camera.pitchDegrees.toDouble())
        val radius = camera.distance

        val horizontalRadius = (radius * kotlin.math.cos(pitch)).toFloat()
        val x = (horizontalRadius * kotlin.math.sin(yaw)).toFloat()
        val y = (radius * kotlin.math.sin(pitch)).toFloat()
        val z = (horizontalRadius * kotlin.math.cos(yaw)).toFloat()
        return floatArrayOf(x, y, z)
    }

    /**
     * Builds the camera view-projection matrix shared by the renderer and overlays.
     */
    fun buildViewProjectionMatrix(
        camera: Bubble3DCameraOptions,
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
        return ScreenPoint(
            x = ((xNdc + 1f) * 0.5f) * viewportWidth,
            y = ((1f - yNdc) * 0.5f) * viewportHeight,
        )
    }
}
