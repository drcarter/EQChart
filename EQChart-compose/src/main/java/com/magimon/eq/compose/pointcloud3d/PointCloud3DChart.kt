package com.magimon.eq.compose.pointcloud3d

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.magimon.eq.point3d.Point3DAxisOptions
import com.magimon.eq.point3d.Point3DCameraOptions
import com.magimon.eq.pointcloud3d.PointCloud3DChartView
import com.magimon.eq.pointcloud3d.PointCloud3DDatum
import com.magimon.eq.pointcloud3d.PointCloud3DPresentationOptions
import com.magimon.eq.pointcloud3d.PointCloud3DScaleOverride

/**
 * Compose wrapper for the OpenGL-based true 3D point-cloud chart.
 *
 * The underlying renderer is implemented by [PointCloud3DChartView], so this
 * composable bridges Compose state and lifecycle into the existing Android
 * View chart surface instead of duplicating the 3D renderer.
 *
 * @param data 3D point-cloud dataset to render
 * @param modifier Standard Compose modifier for layout and input
 * @param axisOptions Shared 3D axis, grid, and tick configuration
 * @param presentationOptions Scene colors plus point-cloud styling options
 * @param cameraOptions Initial orbit camera configuration
 * @param scaleOverride Optional manual min/max overrides for x, y, z, and size
 * @param onPointClick Optional callback invoked with the selected datum
 */
@Composable
fun PointCloud3DChart(
    data: List<PointCloud3DDatum>,
    modifier: Modifier = Modifier,
    axisOptions: Point3DAxisOptions = Point3DAxisOptions(),
    presentationOptions: PointCloud3DPresentationOptions = PointCloud3DPresentationOptions(),
    cameraOptions: Point3DCameraOptions = Point3DCameraOptions(),
    scaleOverride: PointCloud3DScaleOverride? = null,
    onPointClick: ((PointCloud3DDatum) -> Unit)? = null,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnPointClick by rememberUpdatedState(onPointClick)
    var chartView by remember { mutableStateOf<PointCloud3DChartView?>(null) }

    DisposableEffect(lifecycleOwner, chartView) {
        val view = chartView
        if (view == null) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> view.onResume()
                    Lifecycle.Event.ON_PAUSE,
                    Lifecycle.Event.ON_STOP,
                    Lifecycle.Event.ON_DESTROY,
                    -> view.onPause()

                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                view.onResume()
            }
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                view.onPause()
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            PointCloud3DChartView(context).apply {
                chartView = this
                setData(data)
                setAxisOptions(axisOptions)
                setPresentationOptions(presentationOptions)
                setScaleOverride(scaleOverride)
                setCameraOptions(cameraOptions)
                setOnPointClickListener { datum ->
                    latestOnPointClick?.invoke(datum)
                }
            }
        },
        update = { view ->
            chartView = view
            view.setData(data)
            view.setAxisOptions(axisOptions)
            view.setPresentationOptions(presentationOptions)
            view.setScaleOverride(scaleOverride)
            view.setCameraOptions(cameraOptions)
            view.setOnPointClickListener { datum ->
                latestOnPointClick?.invoke(datum)
            }
        },
    )
}
