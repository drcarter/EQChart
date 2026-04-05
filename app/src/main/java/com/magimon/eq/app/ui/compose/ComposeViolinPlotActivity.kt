package com.magimon.eq.app.ui.compose

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.magimon.eq.app.ui.theme.EQChartTheme
import com.magimon.eq.compose.violin.ViolinPlotChart
import com.magimon.eq.violin.ViolinPlotChartPresentationOptions
import com.magimon.eq.violin.ViolinPlotChartStyleOptions

/**
 * Compose demo screen for [ViolinPlotChart].
 */
class ComposeViolinPlotActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EQChartTheme {
                ComposeViolinPlotSampleScreen()
            }
        }
    }
}

@Composable
private fun ComposeViolinPlotSampleScreen() {
    val context = LocalContext.current
    val series = remember { ViolinPlotSampleData.series() }

    ComposeSamplePage(title = "Compose Violin Plot") {
        ViolinPlotChart(
            series = series,
            styleOptions = ViolinPlotChartStyleOptions(
                backgroundColor = Color.parseColor("#F7FAFC"),
                axisColor = Color.parseColor("#8D9AA8"),
                axisLabelColor = Color.parseColor("#3B4350"),
                quartileBandColor = Color.parseColor("#BFD3FF"),
                valueLabelTextColor = Color.parseColor("#273447"),
            ),
            presentationOptions = ViolinPlotChartPresentationOptions(
                showGrid = true,
                showAxes = true,
                showValueLabels = true,
                yLabelFormatter = { value -> "${value.toInt()}ms" },
            ),
            onSeriesClick = { _, clickedSeries ->
                Toast.makeText(
                    context,
                    "${clickedSeries.payload ?: clickedSeries.label}: ${clickedSeries.samples.size} samples",
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )
    }
}
