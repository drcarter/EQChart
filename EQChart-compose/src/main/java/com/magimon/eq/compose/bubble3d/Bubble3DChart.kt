package com.magimon.eq.compose.bubble3d

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
import com.magimon.eq.bubble3d.Bubble3DAxisOptions
import com.magimon.eq.bubble3d.Bubble3DCameraOptions
import com.magimon.eq.bubble3d.Bubble3DChartView
import com.magimon.eq.bubble3d.Bubble3DDatum
import com.magimon.eq.bubble3d.Bubble3DPresentationOptions
import com.magimon.eq.bubble3d.Bubble3DScaleOverride

/**
 * Compose wrapper for the OpenGL-based true 3D bubble chart.
 *
 * The underlying renderer is currently implemented by [Bubble3DChartView], so
 * this composable bridges Compose state and lifecycle into the existing Android
 * View chart surface instead of duplicating the 3D renderer.
 *
 * @param data Bubble dataset to render in 3D space
 * @param modifier Standard Compose modifier for layout and input
 * @param axisOptions Axis, grid, and tick configuration for the 3D scene
 * @param presentationOptions Scene colors, lighting, and radius mapping options
 * @param cameraOptions Initial orbit camera configuration
 * @param scaleOverride Optional manual min/max overrides for x, y, z, and size
 * @param onBubbleClick Optional callback invoked with the selected datum
 */
@Composable
fun Bubble3DChart(
    data: List<Bubble3DDatum>,
    modifier: Modifier = Modifier,
    axisOptions: Bubble3DAxisOptions = Bubble3DAxisOptions(),
    presentationOptions: Bubble3DPresentationOptions = Bubble3DPresentationOptions(),
    cameraOptions: Bubble3DCameraOptions = Bubble3DCameraOptions(),
    scaleOverride: Bubble3DScaleOverride? = null,
    onBubbleClick: ((Bubble3DDatum) -> Unit)? = null,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnBubbleClick by rememberUpdatedState(onBubbleClick)
    var chartView by remember { mutableStateOf<Bubble3DChartView?>(null) }

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
            Bubble3DChartView(context).apply {
                chartView = this
                setData(data)
                setAxisOptions(axisOptions)
                setPresentationOptions(presentationOptions)
                setScaleOverride(scaleOverride)
                setCameraOptions(cameraOptions)
                setOnBubbleClickListener { datum ->
                    latestOnBubbleClick?.invoke(datum)
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
            view.setOnBubbleClickListener { datum ->
                latestOnBubbleClick?.invoke(datum)
            }
        },
    )
}
