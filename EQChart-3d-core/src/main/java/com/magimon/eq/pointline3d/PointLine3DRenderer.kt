package com.magimon.eq.pointline3d

import android.graphics.Color
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.magimon.eq.internal3d.PointSeries3DMath
import com.magimon.eq.internal3d.PointSeries3DShaderProgram
import com.magimon.eq.point3d.Point3DAxisOptions
import com.magimon.eq.point3d.Point3DCameraOptions
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * OpenGL ES renderer for the true 3D point-line chart.
 *
 * This renderer owns scene state, camera matrices, guide rendering, line/point
 * rendering, and nearest-point tap picking.
 */
internal class PointLine3DRenderer : GLSurfaceView.Renderer {

    data class Selection(
        val series: PointLine3DSeries,
        val datum: PointLine3DDatum,
    )

    private data class ScenePoint(
        val datum: PointLine3DDatum,
        val x: Float,
        val y: Float,
        val z: Float,
        val color: Int,
    )

    private data class SceneSeries(
        val series: PointLine3DSeries,
        val points: List<ScenePoint>,
        val lineVertices: FloatArray,
    )

    private data class SelectedPoint(
        val seriesIndex: Int,
        val pointIndex: Int,
    )

    private data class RenderSnapshot(
        val series: List<SceneSeries>,
        val axisOptions: Point3DAxisOptions,
        val cameraOptions: Point3DCameraOptions,
        val presentationOptions: PointLine3DPresentationOptions,
        val selectedPoint: SelectedPoint?,
        val viewportWidth: Int,
        val viewportHeight: Int,
    )

    private val lock = Any()

    private var data: List<PointLine3DSeries> = emptyList()
    private var axisOptions = Point3DAxisOptions()
    private var cameraOptions = Point3DCameraOptions()
    private var presentationOptions = PointLine3DPresentationOptions()
    private var scaleOverride: PointLine3DScaleOverride? = null
    private var sceneSeries: List<SceneSeries> = emptyList()
    private var selectedPoint: SelectedPoint? = null

    private var viewportWidth = 1
    private var viewportHeight = 1

    private var lineProgram: PointSeries3DShaderProgram.LineProgram? = null
    private var pointProgram: PointSeries3DShaderProgram.PointProgram? = null

    /**
     * Replaces the active series collection after filtering invalid numeric values.
     */
    fun setSeries(series: List<PointLine3DSeries>) {
        synchronized(lock) {
            data = series.mapNotNull { entry ->
                val filteredPoints = entry.points.filter { datum ->
                    datum.x.isFinite() &&
                        datum.y.isFinite() &&
                        datum.z.isFinite()
                }
                if (filteredPoints.isEmpty()) {
                    null
                } else {
                    entry.copy(points = filteredPoints)
                }
            }
            rebuildSceneLocked()
        }
    }

    /**
     * Applies axis and grid configuration.
     */
    fun setAxisOptions(options: Point3DAxisOptions) {
        synchronized(lock) {
            axisOptions = options
        }
    }

    /**
     * Applies scene colors, line visibility, and point marker styling.
     */
    fun setPresentationOptions(options: PointLine3DPresentationOptions) {
        synchronized(lock) {
            presentationOptions = options
        }
    }

    /**
     * Overrides automatic X/Y/Z domain resolution.
     */
    fun setScaleOverride(override: PointLine3DScaleOverride?) {
        synchronized(lock) {
            scaleOverride = override
            rebuildSceneLocked()
        }
    }

    /**
     * Applies a new orbit camera configuration.
     */
    fun setCameraOptions(options: Point3DCameraOptions) {
        synchronized(lock) {
            cameraOptions = clampCamera(options)
        }
    }

    /**
     * Returns a stable snapshot of the current orbit camera configuration.
     */
    fun getCameraOptionsSnapshot(): Point3DCameraOptions {
        return synchronized(lock) { cameraOptions }
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
     * Resolves the nearest tapped point using screen-space distance.
     */
    fun pickPoint(screenX: Float, screenY: Float): Selection? {
        synchronized(lock) {
            if (sceneSeries.isEmpty() || viewportWidth <= 0 || viewportHeight <= 0) return null

            val viewProjectionMatrix = PointSeries3DMath.buildViewProjectionMatrix(
                camera = cameraOptions.toOrbitCamera(),
                aspectRatio = viewportWidth.toFloat() / viewportHeight.toFloat(),
            )
            val hitRadiusPx = (presentationOptions.pointSize * presentationOptions.selectedPointScale).coerceAtLeast(16f)

            var bestSelection: Selection? = null
            var bestSelectionKey: SelectedPoint? = null
            var bestDistance = Float.MAX_VALUE
            var bestDepth = Float.MAX_VALUE

            sceneSeries.forEachIndexed { seriesIndex, sceneSeries ->
                sceneSeries.points.forEachIndexed { pointIndex, point ->
                    val projected = PointSeries3DMath.projectWorldToScreen(
                        x = point.x,
                        y = point.y,
                        z = point.z,
                        viewProjectionMatrix = viewProjectionMatrix,
                        viewportWidth = viewportWidth,
                        viewportHeight = viewportHeight,
                    ) ?: return@forEachIndexed

                    val dx = projected.x - screenX
                    val dy = projected.y - screenY
                    val distance = kotlin.math.sqrt((dx * dx) + (dy * dy))
                    if (distance <= hitRadiusPx && (distance < bestDistance || (distance == bestDistance && projected.depth < bestDepth))) {
                        bestDistance = distance
                        bestDepth = projected.depth
                        bestSelection = Selection(sceneSeries.series, point.datum)
                        bestSelectionKey = SelectedPoint(seriesIndex = seriesIndex, pointIndex = pointIndex)
                    }
                }
            }

            selectedPoint = bestSelectionKey
            return bestSelection
        }
    }

    override fun onSurfaceCreated(
        gl: GL10?,
        config: EGLConfig?,
    ) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        lineProgram = PointSeries3DShaderProgram.createLineProgram()
        pointProgram = PointSeries3DShaderProgram.createPointProgram()
    }

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

    override fun onDrawFrame(gl: GL10?) {
        val snapshot = synchronized(lock) {
            RenderSnapshot(
                series = sceneSeries,
                axisOptions = axisOptions,
                cameraOptions = cameraOptions,
                presentationOptions = presentationOptions,
                selectedPoint = selectedPoint,
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

        val viewProjectionMatrix = PointSeries3DMath.buildViewProjectionMatrix(
            camera = snapshot.cameraOptions.toOrbitCamera(),
            aspectRatio = snapshot.viewportWidth.toFloat() / snapshot.viewportHeight.toFloat(),
        )

        drawGuides(snapshot, viewProjectionMatrix)
        drawSeriesLines(snapshot, viewProjectionMatrix)
        drawPointMarkers(snapshot, viewProjectionMatrix)
    }

    private fun drawGuides(
        snapshot: RenderSnapshot,
        viewProjectionMatrix: FloatArray,
    ) {
        val program = lineProgram ?: return
        if (!snapshot.axisOptions.showAxes && !snapshot.axisOptions.showGridPlanes && !snapshot.axisOptions.showTicks) return

        GLES20.glUseProgram(program.programId)
        GLES20.glUniformMatrix4fv(program.mvpMatrixHandle, 1, false, viewProjectionMatrix, 0)

        if (snapshot.axisOptions.showGridPlanes) {
            drawLines(
                program = program,
                vertices = buildGridVertices(snapshot.axisOptions.gridDivisions),
                color = snapshot.presentationOptions.gridColor,
                lineWidth = 1.25f,
                alpha = 0.78f,
                mode = GLES20.GL_LINES,
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
                lineWidth = 2f,
                alpha = 1f,
                mode = GLES20.GL_LINES,
                perSegmentColors = intArrayOf(
                    snapshot.presentationOptions.xAxisColor,
                    snapshot.presentationOptions.yAxisColor,
                    snapshot.presentationOptions.zAxisColor,
                ),
            )
        }

        if (snapshot.axisOptions.showTicks) {
            drawTickMarks(program, snapshot, viewProjectionMatrix)
        }
    }

    private fun drawTickMarks(
        program: PointSeries3DShaderProgram.LineProgram,
        snapshot: RenderSnapshot,
        viewProjectionMatrix: FloatArray,
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

        GLES20.glUniformMatrix4fv(program.mvpMatrixHandle, 1, false, viewProjectionMatrix, 0)
        drawLines(
            program = program,
            vertices = vertices.toFloatArray(),
            color = snapshot.presentationOptions.gridColor,
            lineWidth = 1.2f,
            alpha = 0.88f,
            mode = GLES20.GL_LINES,
        )
    }

    private fun drawSeriesLines(
        snapshot: RenderSnapshot,
        viewProjectionMatrix: FloatArray,
    ) {
        val program = lineProgram ?: return
        if (!snapshot.presentationOptions.showLines) return

        GLES20.glUseProgram(program.programId)
        GLES20.glUniformMatrix4fv(program.mvpMatrixHandle, 1, false, viewProjectionMatrix, 0)
        snapshot.series.forEach { series ->
            if (series.lineVertices.size < 6) return@forEach
            val lineWidth = (snapshot.presentationOptions.lineWidth * (series.series.lineWidthPx / 3f)).coerceAtLeast(1f)
            drawLines(
                program = program,
                vertices = series.lineVertices,
                color = series.series.lineColor,
                lineWidth = lineWidth,
                alpha = snapshot.presentationOptions.lineAlpha.coerceIn(0f, 1f),
                mode = GLES20.GL_LINE_STRIP,
            )
        }
    }

    private fun drawPointMarkers(
        snapshot: RenderSnapshot,
        viewProjectionMatrix: FloatArray,
    ) {
        val program = pointProgram ?: return
        if (!snapshot.presentationOptions.showPointMarkers) return

        GLES20.glUseProgram(program.programId)
        GLES20.glUniformMatrix4fv(program.mvpMatrixHandle, 1, false, viewProjectionMatrix, 0)

        snapshot.series.forEachIndexed { seriesIndex, sceneSeries ->
            sceneSeries.points.forEachIndexed { pointIndex, point ->
                val selected = snapshot.selectedPoint?.seriesIndex == seriesIndex &&
                    snapshot.selectedPoint.pointIndex == pointIndex
                val pointSize = snapshot.presentationOptions.pointSize *
                    sceneSeries.series.pointRadiusScale *
                    if (selected) snapshot.presentationOptions.selectedPointScale else 1f
                drawPoints(
                    program = program,
                    vertices = floatArrayOf(point.x, point.y, point.z),
                    color = if (selected) brightenColor(point.color) else point.color,
                    pointSize = pointSize,
                    alpha = snapshot.presentationOptions.pointAlpha.coerceIn(0f, 1f),
                )
            }
        }
    }

    private fun drawLines(
        program: PointSeries3DShaderProgram.LineProgram,
        vertices: FloatArray,
        color: Int,
        lineWidth: Float,
        alpha: Float,
        mode: Int,
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
        GLES20.glLineWidth(lineWidth)

        if (perSegmentColors == null || perSegmentColors.isEmpty()) {
            GLES20.glUniform4f(
                program.colorHandle,
                Color.red(color) / 255f,
                Color.green(color) / 255f,
                Color.blue(color) / 255f,
                alpha,
            )
            GLES20.glDrawArrays(mode, 0, vertices.size / 3)
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
                    alpha,
                )
                GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2)
                vertexOffset += 2
            }
        }

        GLES20.glDisableVertexAttribArray(program.positionHandle)
    }

    private fun drawPoints(
        program: PointSeries3DShaderProgram.PointProgram,
        vertices: FloatArray,
        color: Int,
        pointSize: Float,
        alpha: Float,
    ) {
        val positionBuffer = vertices.toFloatBuffer()
        val sizeBuffer = floatArrayOf(pointSize.coerceAtLeast(1f)).toFloatBuffer()
        val colorBuffer = floatArrayOf(
            Color.red(color) / 255f,
            Color.green(color) / 255f,
            Color.blue(color) / 255f,
            alpha,
        ).toFloatBuffer()
        GLES20.glVertexAttribPointer(
            program.positionHandle,
            3,
            GLES20.GL_FLOAT,
            false,
            0,
            positionBuffer,
        )
        GLES20.glEnableVertexAttribArray(program.positionHandle)
        GLES20.glVertexAttribPointer(
            program.pointSizeHandle,
            1,
            GLES20.GL_FLOAT,
            false,
            0,
            sizeBuffer,
        )
        GLES20.glEnableVertexAttribArray(program.pointSizeHandle)
        GLES20.glVertexAttribPointer(
            program.colorHandle,
            4,
            GLES20.GL_FLOAT,
            false,
            0,
            colorBuffer,
        )
        GLES20.glEnableVertexAttribArray(program.colorHandle)
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, vertices.size / 3)
        GLES20.glDisableVertexAttribArray(program.colorHandle)
        GLES20.glDisableVertexAttribArray(program.pointSizeHandle)
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

    private fun clampCamera(camera: Point3DCameraOptions): Point3DCameraOptions {
        val minDistance = camera.minDistance.coerceAtLeast(0.6f)
        val maxDistance = camera.maxDistance.coerceAtLeast(minDistance)
        return camera.copy(
            pitchDegrees = camera.pitchDegrees.coerceIn(-80f, 80f),
            minDistance = minDistance,
            maxDistance = maxDistance,
            distance = camera.distance.coerceIn(minDistance, maxDistance),
        )
    }

    private fun rebuildSceneLocked() {
        val flattened = data.flatMap { it.points }
        if (flattened.isEmpty()) {
            sceneSeries = emptyList()
            selectedPoint = null
            return
        }

        val override = scaleOverride
        val xRange = PointSeries3DMath.resolveRange(flattened.map { it.x }, override?.xMin, override?.xMax)
        val yRange = PointSeries3DMath.resolveRange(flattened.map { it.y }, override?.yMin, override?.yMax)
        val zRange = PointSeries3DMath.resolveRange(flattened.map { it.z }, override?.zMin, override?.zMax)

        sceneSeries = data.map { series ->
            val points = series.points.map { datum ->
                ScenePoint(
                    datum = datum,
                    x = PointSeries3DMath.mapToWorld(datum.x, xRange),
                    y = PointSeries3DMath.mapToWorld(datum.y, yRange),
                    z = PointSeries3DMath.mapToWorld(datum.z, zRange),
                    color = datum.color ?: series.pointColor,
                )
            }
            SceneSeries(
                series = series,
                points = points,
                lineVertices = points.flatMap { listOf(it.x, it.y, it.z) }.toFloatArray(),
            )
        }

        val currentSelection = selectedPoint
        if (currentSelection != null) {
            val series = sceneSeries.getOrNull(currentSelection.seriesIndex)
            if (series == null || currentSelection.pointIndex !in series.points.indices) {
                selectedPoint = null
            }
        }
    }

    private fun brightenColor(color: Int): Int {
        return Color.argb(
            Color.alpha(color),
            ((Color.red(color) * 0.76f) + (255f * 0.24f)).toInt().coerceIn(0, 255),
            ((Color.green(color) * 0.76f) + (255f * 0.24f)).toInt().coerceIn(0, 255),
            ((Color.blue(color) * 0.76f) + (255f * 0.24f)).toInt().coerceIn(0, 255),
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

    private fun Point3DCameraOptions.toOrbitCamera(): PointSeries3DMath.OrbitCamera {
        return PointSeries3DMath.OrbitCamera(
            yawDegrees = yawDegrees,
            pitchDegrees = pitchDegrees,
            distance = distance,
            minDistance = minDistance,
            maxDistance = maxDistance,
            fovDegrees = fovDegrees,
        )
    }
}
