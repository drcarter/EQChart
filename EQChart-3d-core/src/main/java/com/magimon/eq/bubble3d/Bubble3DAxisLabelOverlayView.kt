package com.magimon.eq.bubble3d

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View

/**
 * Canvas-based overlay responsible for drawing the three axis titles.
 *
 * Rendering text through a normal Android [View] keeps the OpenGL renderer
 * focused on geometry while still allowing camera-aware label placement.
 */
internal class Bubble3DAxisLabelOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val density = resources.displayMetrics.density
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 13f * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setShadowLayer(8f * density, 0f, 2f * density, Color.argb(185, 0, 0, 0))
    }
    private val horizontalPadding = 28f * density
    private val verticalPadding = 24f * density

    private var axisOptions = Bubble3DAxisOptions()
    private var cameraOptions = Bubble3DCameraOptions()
    private var presentationOptions = Bubble3DPresentationOptions()

    init {
        setWillNotDraw(false)
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    /**
     * Applies the latest overlay-relevant scene state and redraws the titles.
     */
    fun updateOverlayState(
        axisOptions: Bubble3DAxisOptions,
        cameraOptions: Bubble3DCameraOptions,
        presentationOptions: Bubble3DPresentationOptions,
    ) {
        this.axisOptions = axisOptions
        this.cameraOptions = cameraOptions
        this.presentationOptions = presentationOptions
        invalidate()
    }

    /**
     * Draws axis titles near the positive endpoints of the canonical X/Y/Z axes.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!axisOptions.showAxes || width <= 0 || height <= 0) return

        val viewProjectionMatrix = Bubble3DChartMath.buildViewProjectionMatrix(
            camera = cameraOptions,
            aspectRatio = width.toFloat() / height.toFloat(),
        )

        drawAxisTitle(
            canvas = canvas,
            title = axisOptions.xAxisTitle,
            worldX = 1.16f,
            worldY = -1.08f,
            worldZ = -1.08f,
            color = presentationOptions.xAxisColor,
            viewProjectionMatrix = viewProjectionMatrix,
        )
        drawAxisTitle(
            canvas = canvas,
            title = axisOptions.yAxisTitle,
            worldX = -1.08f,
            worldY = 1.16f,
            worldZ = -1.08f,
            color = presentationOptions.yAxisColor,
            viewProjectionMatrix = viewProjectionMatrix,
        )
        drawAxisTitle(
            canvas = canvas,
            title = axisOptions.zAxisTitle,
            worldX = -1.08f,
            worldY = -1.08f,
            worldZ = 1.16f,
            color = presentationOptions.zAxisColor,
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

        val point = Bubble3DChartMath.projectWorldToScreen(
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
