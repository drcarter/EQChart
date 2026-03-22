package com.magimon.eq.pointline3d

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewConfiguration
import com.magimon.eq.point3d.Point3DAxisOptions
import com.magimon.eq.point3d.Point3DCameraOptions
import kotlin.math.abs

/**
 * Internal [GLSurfaceView] wrapper that bridges touch gestures to the renderer.
 */
internal class PointLine3DSceneView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : GLSurfaceView(context, attrs) {

    private val renderer = PointLine3DRenderer()
    private val scaleGestureDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                renderer.zoomBy(detector.scaleFactor)
                cameraOptions = renderer.getCameraOptionsSnapshot()
                notifyOverlayStateChanged()
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
    private var axisOptions = Point3DAxisOptions()
    private var cameraOptions = Point3DCameraOptions()
    private var presentationOptions = PointLine3DPresentationOptions()

    var onPointTap: ((PointLine3DSeries, PointLine3DDatum) -> Unit)? = null
    var onSceneOverlayStateChanged: ((Point3DAxisOptions, Point3DCameraOptions, PointLine3DPresentationOptions) -> Unit)? =
        null

    init {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    /**
     * Updates the series data and requests a redraw.
     */
    fun setSeries(series: List<PointLine3DSeries>) {
        renderer.setSeries(series)
        requestRender()
    }

    /**
     * Applies axis and grid settings to the renderer.
     */
    fun setAxisOptions(options: Point3DAxisOptions) {
        axisOptions = options
        renderer.setAxisOptions(options)
        notifyOverlayStateChanged()
        requestRender()
    }

    /**
     * Applies color and point-marker styling to the renderer.
     */
    fun setPresentationOptions(options: PointLine3DPresentationOptions) {
        presentationOptions = options
        renderer.setPresentationOptions(options)
        notifyOverlayStateChanged()
        requestRender()
    }

    /**
     * Overrides automatic data range resolution in the renderer.
     */
    fun setScaleOverride(override: PointLine3DScaleOverride?) {
        renderer.setScaleOverride(override)
        requestRender()
    }

    /**
     * Applies a new orbit camera configuration and requests a redraw.
     */
    fun setCameraOptions(options: Point3DCameraOptions) {
        renderer.setCameraOptions(options)
        cameraOptions = renderer.getCameraOptionsSnapshot()
        notifyOverlayStateChanged()
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
                    cameraOptions = renderer.getCameraOptionsSnapshot()
                    notifyOverlayStateChanged()
                    requestRender()
                    lastX = event.x
                    lastY = event.y
                }
            }

            MotionEvent.ACTION_UP -> {
                val moved = (totalDx > touchSlop) || (totalDy > touchSlop)
                if (!moved && !scaleGestureDetector.isInProgress) {
                    renderer.pickPoint(event.x, event.y)?.let { selection ->
                        onPointTap?.invoke(selection.series, selection.datum)
                    }
                    requestRender()
                }
            }
        }

        return true
    }

    private fun notifyOverlayStateChanged() {
        onSceneOverlayStateChanged?.invoke(axisOptions, cameraOptions, presentationOptions)
    }
}
