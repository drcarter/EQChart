package com.magimon.eq.bubble3d

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout

/**
 * Public Android View entry point for the true 3D bubble chart.
 *
 * This view hosts an internal [GLSurfaceView]-backed scene plus a lightweight
 * axis-title overlay, and exposes a chart-style API similar to the other
 * EQChart View components.
 */
class Bubble3DChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val sceneView = Bubble3DSceneView(context)
    private val axisLabelOverlayView = Bubble3DAxisLabelOverlayView(context)

    init {
        addView(
            sceneView,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        addView(
            axisLabelOverlayView,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        sceneView.onSceneOverlayStateChanged = { axisOptions, cameraOptions, presentationOptions ->
            axisLabelOverlayView.updateOverlayState(
                axisOptions = axisOptions,
                cameraOptions = cameraOptions,
                presentationOptions = presentationOptions,
            )
        }
    }

    /**
     * Replaces the current 3D bubble dataset.
     */
    fun setData(data: List<Bubble3DDatum>) {
        sceneView.setData(data)
    }

    /**
     * Maps arbitrary items into [Bubble3DDatum] and submits the result.
     */
    fun <T> setData(
        items: List<T>,
        mapper: (T) -> Bubble3DDatum,
    ) {
        setData(items.map(mapper))
    }

    /**
     * Applies axis/grid/tick configuration for the 3D scene.
     */
    fun setAxisOptions(options: Bubble3DAxisOptions) {
        sceneView.setAxisOptions(options)
    }

    /**
     * Applies scene colors, lighting, and radius mapping options.
     */
    fun setPresentationOptions(options: Bubble3DPresentationOptions) {
        sceneView.setPresentationOptions(options)
    }

    /**
     * Overrides automatic X/Y/Z/size range detection.
     */
    fun setScaleOverride(override: Bubble3DScaleOverride?) {
        sceneView.setScaleOverride(override)
    }

    /**
     * Applies the initial orbit camera configuration.
     */
    fun setCameraOptions(options: Bubble3DCameraOptions) {
        sceneView.setCameraOptions(options)
    }

    /**
     * Registers a listener invoked when a rendered bubble is tapped.
     */
    fun setOnBubbleClickListener(listener: (Bubble3DDatum) -> Unit) {
        sceneView.onBubbleTap = listener
    }

    /**
     * Forwards the host lifecycle resume event to the internal GL surface.
     */
    fun onResume() {
        sceneView.onResume()
    }

    /**
     * Forwards the host lifecycle pause event to the internal GL surface.
     */
    fun onPause() {
        sceneView.onPause()
    }
}
