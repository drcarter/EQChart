package com.magimon.eq.internal3d

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View

/**
 * Canvas-based overlay responsible for drawing camera-aware axis titles.
 */
internal class PointSeries3DAxisLabelOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    /**
     * Immutable scene snapshot used by the overlay.
     */
    data class State(
        val showAxes: Boolean,
        val camera: PointSeries3DMath.OrbitCamera,
        val xAxisTitle: String?,
        val yAxisTitle: String?,
        val zAxisTitle: String?,
        val xAxisColor: Int,
        val yAxisColor: Int,
        val zAxisColor: Int,
    )

    private val density = resources.displayMetrics.density
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 13f * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setShadowLayer(8f * density, 0f, 2f * density, Color.argb(185, 0, 0, 0))
    }
    private val horizontalPadding = 28f * density
    private val verticalPadding = 24f * density

    private var state = State(
        showAxes = true,
        camera = PointSeries3DMath.OrbitCamera(
            yawDegrees = -34f,
            pitchDegrees = 20f,
            distance = 4.8f,
            minDistance = 2.4f,
            maxDistance = 12f,
            fovDegrees = 45f,
        ),
        xAxisTitle = null,
        yAxisTitle = null,
        zAxisTitle = null,
        xAxisColor = Color.WHITE,
        yAxisColor = Color.WHITE,
        zAxisColor = Color.WHITE,
    )

    init {
        setWillNotDraw(false)
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    /**
     * Applies the latest overlay-relevant scene state and redraws the titles.
     */
    fun updateOverlayState(state: State) {
        this.state = state
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!state.showAxes || width <= 0 || height <= 0) return

        val viewProjectionMatrix = PointSeries3DMath.buildViewProjectionMatrix(
            camera = state.camera,
            aspectRatio = width.toFloat() / height.toFloat(),
        )

        drawAxisTitle(
            canvas = canvas,
            title = state.xAxisTitle,
            worldX = 1.16f,
            worldY = -1.08f,
            worldZ = -1.08f,
            color = state.xAxisColor,
            viewProjectionMatrix = viewProjectionMatrix,
        )
        drawAxisTitle(
            canvas = canvas,
            title = state.yAxisTitle,
            worldX = -1.08f,
            worldY = 1.16f,
            worldZ = -1.08f,
            color = state.yAxisColor,
            viewProjectionMatrix = viewProjectionMatrix,
        )
        drawAxisTitle(
            canvas = canvas,
            title = state.zAxisTitle,
            worldX = -1.08f,
            worldY = -1.08f,
            worldZ = 1.16f,
            color = state.zAxisColor,
            viewProjectionMatrix = viewProjectionMatrix,
        )
    }

    private fun drawAxisTitle(
        canvas: Canvas,
        title: String?,
        worldX: Float,
        worldY: Float,
        worldZ: Float,
        color: Int,
        viewProjectionMatrix: FloatArray,
    ) {
        if (title.isNullOrBlank()) return

        val point = PointSeries3DMath.projectWorldToScreen(
            x = worldX,
            y = worldY,
            z = worldZ,
            viewProjectionMatrix = viewProjectionMatrix,
            viewportWidth = width,
            viewportHeight = height,
        ) ?: return

        textPaint.color = color
        val textHeight = textPaint.fontMetrics.run { bottom - top }
        val clampedX = point.x.coerceIn(horizontalPadding, width - horizontalPadding)
        val clampedBaseline = point.y.coerceIn(verticalPadding + textHeight, height - verticalPadding)
        canvas.drawText(title, clampedX, clampedBaseline, textPaint)
    }
}
