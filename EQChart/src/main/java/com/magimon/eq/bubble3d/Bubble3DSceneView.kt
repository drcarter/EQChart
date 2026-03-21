package com.magimon.eq.bubble3d

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewConfiguration
import kotlin.math.abs

/**
 * Internal [GLSurfaceView] wrapper that bridges touch gestures to the renderer.
 */
internal class Bubble3DSceneView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : GLSurfaceView(context, attrs) {

    private val renderer = Bubble3DRenderer()
    private val scaleGestureDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                renderer.zoomBy(detector.scaleFactor)
                requestRender()
                return true
            }
        },
    )
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private var lastX = 0f
    private var lastY = 0f
    private var totalDx = 0f
    private var totalDy = 0f

    var onBubbleTap: ((Bubble3DDatum) -> Unit)? = null

    init {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    /**
     * Updates the bubble dataset and requests a redraw.
     */
    fun setData(data: List<Bubble3DDatum>) {
        renderer.setData(data)
        requestRender()
    }

    /**
     * Applies axis and grid settings to the renderer.
     */
    fun setAxisOptions(options: Bubble3DAxisOptions) {
        renderer.setAxisOptions(options)
        requestRender()
    }

    /**
     * Applies color, lighting, and radius options to the renderer.
     */
    fun setPresentationOptions(options: Bubble3DPresentationOptions) {
        renderer.setPresentationOptions(options)
        requestRender()
    }

    /**
     * Overrides automatic data range resolution in the renderer.
     */
    fun setScaleOverride(override: Bubble3DScaleOverride?) {
        renderer.setScaleOverride(override)
        requestRender()
    }

    /**
     * Applies a new orbit camera configuration and requests a redraw.
     */
    fun setCameraOptions(options: Bubble3DCameraOptions) {
        renderer.setCameraOptions(options)
        requestRender()
    }

    /**
     * Handles orbit rotation, pinch zoom, and tap selection gestures.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                totalDx = 0f
                totalDy = 0f
            }

            MotionEvent.ACTION_MOVE -> {
                if (!scaleGestureDetector.isInProgress) {
                    val dx = event.x - lastX
                    val dy = event.y - lastY
                    totalDx += abs(dx)
                    totalDy += abs(dy)
                    renderer.adjustCamera(
                        yawDelta = dx * 0.35f,
                        pitchDelta = -dy * 0.35f,
                    )
                    requestRender()
                    lastX = event.x
                    lastY = event.y
                }
            }

            MotionEvent.ACTION_UP -> {
                val moved = (totalDx > touchSlop) || (totalDy > touchSlop)
                if (!moved && !scaleGestureDetector.isInProgress) {
                    val xNdc = ((event.x / width.toFloat()) * 2f) - 1f
                    val yNdc = 1f - ((event.y / height.toFloat()) * 2f)
                    renderer.pickBubble(xNdc = xNdc, yNdc = yNdc)?.let { datum ->
                        onBubbleTap?.invoke(datum)
                    }
                    requestRender()
                }
            }
        }

        return true
    }
}
