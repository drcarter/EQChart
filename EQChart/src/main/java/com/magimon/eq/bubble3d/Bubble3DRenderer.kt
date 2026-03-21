package com.magimon.eq.bubble3d

import android.graphics.Color
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

/**
 * OpenGL ES renderer for the true 3D bubble chart.
 *
 * Responsibilities are intentionally split so this type owns scene state,
 * camera matrices, sphere rendering, simple guide rendering, and tap picking.
 */
internal class Bubble3DRenderer : GLSurfaceView.Renderer {

    private val lock = Any()

    private var data: List<Bubble3DDatum> = emptyList()
    private var axisOptions = Bubble3DAxisOptions()
    private var cameraOptions = Bubble3DCameraOptions()
    private var presentationOptions = Bubble3DPresentationOptions()
    private var scaleOverride: Bubble3DScaleOverride? = null
    private var sceneBubbles: List<Bubble3DChartMath.SceneBubble> = emptyList()
    private var selectedDatum: Bubble3DDatum? = null

    private var viewportWidth = 1
    private var viewportHeight = 1

    private var sphereProgram: Bubble3DShaderProgram.SphereProgram? = null
    private var lineProgram: Bubble3DShaderProgram.LineProgram? = null
    private var sphereMesh: Bubble3DMeshFactory.SphereMesh? = null

    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewProjectionMatrix = FloatArray(16)
    private val inverseViewProjectionMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val lineMvpMatrix = FloatArray(16)

    /**
     * Replaces the active bubble dataset after filtering invalid numeric values.
     */
    fun setData(points: List<Bubble3DDatum>) {
        synchronized(lock) {
            data = points.filter { datum ->
                datum.x.isFinite() &&
                    datum.y.isFinite() &&
                    datum.z.isFinite() &&
                    datum.size.isFinite()
            }
            rebuildSceneLocked()
        }
    }

    /**
     * Applies axis and grid configuration.
     */
    fun setAxisOptions(options: Bubble3DAxisOptions) {
        synchronized(lock) {
            axisOptions = options
        }
    }

    /**
     * Applies scene colors, lighting, and radius bounds.
     */
    fun setPresentationOptions(options: Bubble3DPresentationOptions) {
        synchronized(lock) {
            presentationOptions = options
            rebuildSceneLocked()
        }
    }

    /**
     * Overrides automatic domain resolution for position and size values.
     */
    fun setScaleOverride(override: Bubble3DScaleOverride?) {
        synchronized(lock) {
            scaleOverride = override
            rebuildSceneLocked()
        }
    }

    /**
     * Applies a new orbit camera configuration.
     */
    fun setCameraOptions(options: Bubble3DCameraOptions) {
        synchronized(lock) {
            cameraOptions = clampCamera(options)
        }
    }

    /**
     * Rotates the orbit camera by the supplied deltas.
     */
    fun adjustCamera(
        yawDelta: Float,
        pitchDelta: Float,
    ) {
        synchronized(lock) {
            cameraOptions = clampCamera(
                cameraOptions.copy(
                    yawDegrees = cameraOptions.yawDegrees + yawDelta,
                    pitchDegrees = cameraOptions.pitchDegrees + pitchDelta,
                ),
            )
        }
    }

    /**
     * Zooms the camera by adjusting the orbit distance.
     */
    fun zoomBy(scaleFactor: Float) {
        if (scaleFactor <= 0f) return
        synchronized(lock) {
            cameraOptions = clampCamera(
                cameraOptions.copy(
                    distance = cameraOptions.distance / scaleFactor,
                ),
            )
        }
    }

    /**
     * Resolves the nearest tapped bubble using ray-sphere intersection.
     */
    fun pickBubble(
        xNdc: Float,
        yNdc: Float,
    ): Bubble3DDatum? {
        synchronized(lock) {
            if (sceneBubbles.isEmpty()) return null
            val ray = Bubble3DChartMath.rayFromNormalizedScreen(
                xNdc = xNdc,
                yNdc = yNdc,
                inverseViewProjection = inverseViewProjectionMatrix,
            ) ?: return null

            val hit = sceneBubbles
                .mapNotNull { bubble ->
                    Bubble3DChartMath.intersectRaySphere(
                        ray = ray,
                        centerX = bubble.x,
                        centerY = bubble.y,
                        centerZ = bubble.z,
                        radius = bubble.radius,
                    )?.let { distance -> bubble to distance }
                }
                .minByOrNull { (_, distance) -> distance }
                ?.first

            selectedDatum = hit?.datum
            return hit?.datum
        }
    }

    /**
     * Initializes OpenGL state and compiles shared shader/mesh resources.
     */
    override fun onSurfaceCreated(
        gl: GL10?,
        config: EGLConfig?,
    ) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)

        sphereProgram = Bubble3DShaderProgram.createSphereProgram()
        lineProgram = Bubble3DShaderProgram.createLineProgram()
        sphereMesh = Bubble3DMeshFactory.createSphereMesh()
    }

    /**
     * Updates the viewport dimensions after surface size changes.
     */
    override fun onSurfaceChanged(
        gl: GL10?,
        width: Int,
        height: Int,
    ) {
        GLES20.glViewport(0, 0, width, height)
        synchronized(lock) {
            viewportWidth = width.coerceAtLeast(1)
            viewportHeight = height.coerceAtLeast(1)
        }
    }

    /**
     * Draws one frame of the current 3D bubble scene.
     */
    override fun onDrawFrame(gl: GL10?) {
        val snapshot = synchronized(lock) {
            val bubbles = sceneBubbles
            val axes = axisOptions
            val camera = cameraOptions
            val presentation = presentationOptions
            val selected = selectedDatum
            RenderSnapshot(
                bubbles = bubbles,
                axisOptions = axes,
                cameraOptions = camera,
                presentationOptions = presentation,
                selectedDatum = selected,
                viewportWidth = viewportWidth,
                viewportHeight = viewportHeight,
            )
        }

        val clearColor = snapshot.presentationOptions.backgroundColor
        GLES20.glClearColor(
            Color.red(clearColor) / 255f,
            Color.green(clearColor) / 255f,
            Color.blue(clearColor) / 255f,
            1f,
        )
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val aspectRatio = snapshot.viewportWidth.toFloat() / snapshot.viewportHeight.toFloat()
        val eyePosition = computeEyePosition(snapshot.cameraOptions)
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
            snapshot.cameraOptions.fovDegrees,
            aspectRatio,
            0.1f,
            20f,
        )
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        Matrix.invertM(inverseViewProjectionMatrix, 0, viewProjectionMatrix, 0)

        drawGuides(snapshot)
        drawBubbles(snapshot, eyePosition)
    }

    private fun drawGuides(snapshot: RenderSnapshot) {
        val program = lineProgram ?: return

        if (!snapshot.axisOptions.showAxes && !snapshot.axisOptions.showGridPlanes && !snapshot.axisOptions.showTicks) {
            return
        }

        GLES20.glUseProgram(program.programId)
        GLES20.glUniformMatrix4fv(program.mvpMatrixHandle, 1, false, viewProjectionMatrix, 0)

        if (snapshot.axisOptions.showGridPlanes) {
            drawLines(
                program = program,
                vertices = buildGridVertices(snapshot.axisOptions.gridDivisions),
                color = snapshot.presentationOptions.gridColor,
            )
        }

        if (snapshot.axisOptions.showAxes) {
            drawLines(
                program = program,
                vertices = floatArrayOf(
                    -1f, -1f, -1f, 1f, -1f, -1f,
                    -1f, -1f, -1f, -1f, 1f, -1f,
                    -1f, -1f, -1f, -1f, -1f, 1f,
                ),
                color = snapshot.presentationOptions.xAxisColor,
                perSegmentColors = intArrayOf(
                    snapshot.presentationOptions.xAxisColor,
                    snapshot.presentationOptions.yAxisColor,
                    snapshot.presentationOptions.zAxisColor,
                ),
            )
        }

        if (snapshot.axisOptions.showTicks) {
            drawTickMarks(program, snapshot)
        }
    }

    private fun drawTickMarks(
        program: Bubble3DShaderProgram.LineProgram,
        snapshot: RenderSnapshot,
    ) {
        val tickSize = 0.04f
        val divisions = snapshot.axisOptions.gridDivisions.coerceAtLeast(2)
        val vertices = ArrayList<Float>(divisions * 18)
        for (index in 0..divisions) {
            val t = -1f + (2f * index / divisions.toFloat())
            vertices += t
            vertices += -1f
            vertices += -1f
            vertices += t
            vertices += -1f - tickSize
            vertices += -1f

            vertices += -1f
            vertices += t
            vertices += -1f
            vertices += -1f - tickSize
            vertices += t
            vertices += -1f

            vertices += -1f
            vertices += -1f
            vertices += t
            vertices += -1f
            vertices += -1f - tickSize
            vertices += t
        }

        drawLines(
            program = program,
            vertices = vertices.toFloatArray(),
            color = snapshot.presentationOptions.gridColor,
        )
    }

    private fun drawBubbles(
        snapshot: RenderSnapshot,
        eyePosition: FloatArray,
    ) {
        val program = sphereProgram ?: return
        val mesh = sphereMesh ?: return

        GLES20.glUseProgram(program.programId)
        GLES20.glUniform3f(
            program.lightDirectionHandle,
            -0.45f,
            0.8f,
            0.35f,
        )
        GLES20.glUniform1f(
            program.ambientHandle,
            snapshot.presentationOptions.ambientLight.coerceIn(0.05f, 0.95f),
        )

        val ordered = snapshot.bubbles.sortedByDescending { bubble ->
            val dx = eyePosition[0] - bubble.x
            val dy = eyePosition[1] - bubble.y
            val dz = eyePosition[2] - bubble.z
            (dx * dx) + (dy * dy) + (dz * dz)
        }

        mesh.vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(
            program.positionHandle,
            3,
            GLES20.GL_FLOAT,
            false,
            0,
            mesh.vertexBuffer,
        )
        GLES20.glEnableVertexAttribArray(program.positionHandle)

        mesh.normalBuffer.position(0)
        GLES20.glVertexAttribPointer(
            program.normalHandle,
            3,
            GLES20.GL_FLOAT,
            false,
            0,
            mesh.normalBuffer,
        )
        GLES20.glEnableVertexAttribArray(program.normalHandle)

        ordered.forEach { bubble ->
            val color = if (snapshot.selectedDatum == bubble.datum) {
                brightenColor(bubble.datum.color)
            } else {
                bubble.datum.color
            }
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, bubble.x, bubble.y, bubble.z)
            val scale = if (snapshot.selectedDatum == bubble.datum) bubble.radius * 1.06f else bubble.radius
            Matrix.scaleM(modelMatrix, 0, scale, scale, scale)
            Matrix.multiplyMM(mvpMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0)

            GLES20.glUniformMatrix4fv(program.modelMatrixHandle, 1, false, modelMatrix, 0)
            GLES20.glUniformMatrix4fv(program.mvpMatrixHandle, 1, false, mvpMatrix, 0)
            GLES20.glUniform4f(
                program.colorHandle,
                Color.red(color) / 255f,
                Color.green(color) / 255f,
                Color.blue(color) / 255f,
                1f,
            )

            mesh.indexBuffer.position(0)
            GLES20.glDrawElements(
                GLES20.GL_TRIANGLES,
                mesh.indexCount,
                GLES20.GL_UNSIGNED_SHORT,
                mesh.indexBuffer,
            )
        }

        GLES20.glDisableVertexAttribArray(program.positionHandle)
        GLES20.glDisableVertexAttribArray(program.normalHandle)
    }

    private fun drawLines(
        program: Bubble3DShaderProgram.LineProgram,
        vertices: FloatArray,
        color: Int,
        perSegmentColors: IntArray? = null,
    ) {
        if (vertices.isEmpty()) return

        val buffer = vertices.toFloatBuffer()
        GLES20.glVertexAttribPointer(
            program.positionHandle,
            3,
            GLES20.GL_FLOAT,
            false,
            0,
            buffer,
        )
        GLES20.glEnableVertexAttribArray(program.positionHandle)
        GLES20.glLineWidth(1.5f)

        if (perSegmentColors == null || perSegmentColors.isEmpty()) {
            GLES20.glUniform4f(
                program.colorHandle,
                Color.red(color) / 255f,
                Color.green(color) / 255f,
                Color.blue(color) / 255f,
                1f,
            )
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, vertices.size / 3)
        } else {
            var vertexOffset = 0
            for (segmentColor in perSegmentColors) {
                buffer.position(vertexOffset * 3)
                GLES20.glVertexAttribPointer(
                    program.positionHandle,
                    3,
                    GLES20.GL_FLOAT,
                    false,
                    0,
                    buffer,
                )
                GLES20.glUniform4f(
                    program.colorHandle,
                    Color.red(segmentColor) / 255f,
                    Color.green(segmentColor) / 255f,
                    Color.blue(segmentColor) / 255f,
                    1f,
                )
                GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2)
                vertexOffset += 2
            }
        }

        GLES20.glDisableVertexAttribArray(program.positionHandle)
    }

    private fun buildGridVertices(divisions: Int): FloatArray {
        val safeDivisions = divisions.coerceIn(2, 10)
        val vertices = ArrayList<Float>((safeDivisions + 1) * 24)
        for (index in 0..safeDivisions) {
            val t = -1f + (2f * index / safeDivisions.toFloat())

            vertices += -1f
            vertices += -1f
            vertices += t
            vertices += 1f
            vertices += -1f
            vertices += t

            vertices += t
            vertices += -1f
            vertices += -1f
            vertices += t
            vertices += -1f
            vertices += 1f

            vertices += -1f
            vertices += t
            vertices += -1f
            vertices += 1f
            vertices += t
            vertices += -1f

            vertices += t
            vertices += -1f
            vertices += -1f
            vertices += t
            vertices += 1f
            vertices += -1f
        }
        return vertices.toFloatArray()
    }

    private fun computeEyePosition(camera: Bubble3DCameraOptions): FloatArray {
        val yaw = Math.toRadians(camera.yawDegrees.toDouble())
        val pitch = Math.toRadians(camera.pitchDegrees.toDouble())
        val radius = camera.distance

        val horizontalRadius = (radius * cos(pitch)).toFloat()
        val x = (horizontalRadius * sin(yaw)).toFloat()
        val y = (radius * sin(pitch)).toFloat()
        val z = (horizontalRadius * cos(yaw)).toFloat()
        return floatArrayOf(x, y, z)
    }

    private fun clampCamera(camera: Bubble3DCameraOptions): Bubble3DCameraOptions {
        return camera.copy(
            pitchDegrees = camera.pitchDegrees.coerceIn(-80f, 80f),
            distance = camera.distance.coerceIn(camera.minDistance, camera.maxDistance),
        )
    }

    private fun rebuildSceneLocked() {
        if (data.isEmpty()) {
            sceneBubbles = emptyList()
            selectedDatum = null
            return
        }

        val override = scaleOverride
        val xRange = Bubble3DChartMath.resolveRange(data.map { it.x }, override?.xMin, override?.xMax)
        val yRange = Bubble3DChartMath.resolveRange(data.map { it.y }, override?.yMin, override?.yMax)
        val zRange = Bubble3DChartMath.resolveRange(data.map { it.z }, override?.zMin, override?.zMax)
        val sizeRange = Bubble3DChartMath.resolveRange(data.map { it.size }, override?.sizeMin, override?.sizeMax)

        sceneBubbles = data.map { datum ->
            val radius = Bubble3DChartMath.mapRadius(
                sizeValue = datum.size,
                sizeRange = sizeRange,
                minRadius = presentationOptions.minBubbleRadius,
                maxRadius = presentationOptions.maxBubbleRadius,
            )
            Bubble3DChartMath.SceneBubble(
                datum = datum,
                x = Bubble3DChartMath.clampWorldCoordinate(Bubble3DChartMath.mapToWorld(datum.x, xRange), radius),
                y = Bubble3DChartMath.clampWorldCoordinate(Bubble3DChartMath.mapToWorld(datum.y, yRange), radius),
                z = Bubble3DChartMath.clampWorldCoordinate(Bubble3DChartMath.mapToWorld(datum.z, zRange), radius),
                radius = radius,
            )
        }

        if (selectedDatum != null && sceneBubbles.none { it.datum == selectedDatum }) {
            selectedDatum = null
        }
    }

    private fun brightenColor(color: Int): Int {
        return Color.argb(
            Color.alpha(color),
            ((Color.red(color) * 0.78f) + (255f * 0.22f)).toInt().coerceIn(0, 255),
            ((Color.green(color) * 0.78f) + (255f * 0.22f)).toInt().coerceIn(0, 255),
            ((Color.blue(color) * 0.78f) + (255f * 0.22f)).toInt().coerceIn(0, 255),
        )
    }

    private fun FloatArray.toFloatBuffer(): FloatBuffer {
        return ByteBuffer
            .allocateDirect(size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(this@toFloatBuffer)
                position(0)
            }
    }

    private data class RenderSnapshot(
        val bubbles: List<Bubble3DChartMath.SceneBubble>,
        val axisOptions: Bubble3DAxisOptions,
        val cameraOptions: Bubble3DCameraOptions,
        val presentationOptions: Bubble3DPresentationOptions,
        val selectedDatum: Bubble3DDatum?,
        val viewportWidth: Int,
        val viewportHeight: Int,
    )
}
