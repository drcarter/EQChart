package com.magimon.eq.pointcloud3d

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import com.magimon.eq.internal3d.PointSeries3DAxisLabelOverlayView
import com.magimon.eq.internal3d.PointSeries3DMath
import com.magimon.eq.point3d.Point3DAxisOptions
import com.magimon.eq.point3d.Point3DCameraOptions

/**
 * Public Android View entry point for the true 3D point cloud chart.
 */
class PointCloud3DChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val sceneView = PointCloud3DSceneView(context)
    private val axisLabelOverlayView = PointSeries3DAxisLabelOverlayView(context)

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
                PointSeries3DAxisLabelOverlayView.State(
                    showAxes = axisOptions.showAxes,
                    camera = PointSeries3DMath.OrbitCamera(
                        yawDegrees = cameraOptions.yawDegrees,
                        pitchDegrees = cameraOptions.pitchDegrees,
                        distance = cameraOptions.distance,
                        minDistance = cameraOptions.minDistance,
                        maxDistance = cameraOptions.maxDistance,
                        fovDegrees = cameraOptions.fovDegrees,
                    ),
                    xAxisTitle = axisOptions.xAxisTitle,
                    yAxisTitle = axisOptions.yAxisTitle,
                    zAxisTitle = axisOptions.zAxisTitle,
                    xAxisColor = presentationOptions.xAxisColor,
                    yAxisColor = presentationOptions.yAxisColor,
                    zAxisColor = presentationOptions.zAxisColor,
                ),
            )
        }
    }

    /**
     * Replaces the current point cloud dataset.
     */
    fun setData(data: List<PointCloud3DDatum>) {
        sceneView.setData(data)
    }

    /**
     * Maps arbitrary items into [PointCloud3DDatum] and submits the result.
     */
    fun <T> setData(
        items: List<T>,
        mapper: (T) -> PointCloud3DDatum,
    ) {
        setData(items.map(mapper))
    }

    /**
     * Applies axis/grid/tick configuration for the 3D scene.
     */
    fun setAxisOptions(options: Point3DAxisOptions) {
        sceneView.setAxisOptions(options)
    }

    /**
     * Applies scene colors and point-marker styling options.
     */
    fun setPresentationOptions(options: PointCloud3DPresentationOptions) {
        sceneView.setPresentationOptions(options)
    }

    /**
     * Overrides automatic X/Y/Z/size range detection.
     */
    fun setScaleOverride(override: PointCloud3DScaleOverride?) {
        sceneView.setScaleOverride(override)
    }

    /**
     * Applies the initial orbit camera configuration.
     */
    fun setCameraOptions(options: Point3DCameraOptions) {
        sceneView.setCameraOptions(options)
    }

    /**
     * Registers a listener invoked when a rendered cloud point is tapped.
     */
    fun setOnPointClickListener(listener: (PointCloud3DDatum) -> Unit) {
        sceneView.onPointTap = listener
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
