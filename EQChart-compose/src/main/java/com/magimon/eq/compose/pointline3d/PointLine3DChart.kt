package com.magimon.eq.compose.pointline3d

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
import com.magimon.eq.pointline3d.PointLine3DChartView
import com.magimon.eq.pointline3d.PointLine3DDatum
import com.magimon.eq.pointline3d.PointLine3DPresentationOptions
import com.magimon.eq.pointline3d.PointLine3DScaleOverride
import com.magimon.eq.pointline3d.PointLine3DSeries

/**
 * Compose wrapper for the OpenGL-based true 3D point-line chart.
 *
 * The underlying renderer is implemented by [PointLine3DChartView], so this
 * composable bridges Compose state and lifecycle into the existing Android
 * View chart surface instead of duplicating the 3D renderer.
 *
 * @param series Connected 3D line series to render
 * @param modifier Standard Compose modifier for layout and input
 * @param axisOptions Axis, grid, and tick configuration for the 3D scene
 * @param presentationOptions Scene colors plus line and point styling
 * @param cameraOptions Initial orbit camera configuration
 * @param scaleOverride Optional manual min/max overrides for x, y, and z
 * @param onPointClick Optional callback invoked with the selected series/point
 */
@Composable
fun PointLine3DChart(
    series: List<PointLine3DSeries>,
    modifier: Modifier = Modifier,
    axisOptions: Point3DAxisOptions = Point3DAxisOptions(),
    presentationOptions: PointLine3DPresentationOptions = PointLine3DPresentationOptions(),
    cameraOptions: Point3DCameraOptions = Point3DCameraOptions(),
    scaleOverride: PointLine3DScaleOverride? = null,
    onPointClick: ((PointLine3DSeries, PointLine3DDatum) -> Unit)? = null,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnPointClick by rememberUpdatedState(onPointClick)
    var chartView by remember { mutableStateOf<PointLine3DChartView?>(null) }

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
            PointLine3DChartView(context).apply {
                chartView = this
                setSeries(series)
                setAxisOptions(axisOptions)
                setPresentationOptions(presentationOptions)
                setScaleOverride(scaleOverride)
                setCameraOptions(cameraOptions)
                setOnPointClickListener { selectedSeries, selectedPoint ->
                    latestOnPointClick?.invoke(selectedSeries, selectedPoint)
                }
            }
        },
        update = { view ->
            chartView = view
            view.setSeries(series)
            view.setAxisOptions(axisOptions)
            view.setPresentationOptions(presentationOptions)
            view.setScaleOverride(scaleOverride)
            view.setCameraOptions(cameraOptions)
            view.setOnPointClickListener { selectedSeries, selectedPoint ->
                latestOnPointClick?.invoke(selectedSeries, selectedPoint)
            }
        },
    )
}
