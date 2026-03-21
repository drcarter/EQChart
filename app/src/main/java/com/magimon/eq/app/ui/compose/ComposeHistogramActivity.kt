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
import com.magimon.eq.compose.histogram.HistogramChart
import com.magimon.eq.histogram.HistogramChartPresentationOptions
import com.magimon.eq.histogram.HistogramChartStyleOptions

/**
 * Compose demo screen for [HistogramChart].
 */
class ComposeHistogramActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EQChartTheme {
                ComposeHistogramSampleScreen()
            }
        }
    }
}

@Composable
private fun ComposeHistogramSampleScreen() {
    val context = LocalContext.current
    val bins = remember { ChartSampleData.histogramBins() }

    ComposeSamplePage(title = "Compose Histogram") {
        HistogramChart(
            bins = bins,
            styleOptions = HistogramChartStyleOptions(
                backgroundColor = Color.parseColor("#F7FAFC"),
                axisColor = Color.parseColor("#8D9AA8"),
                axisLabelColor = Color.parseColor("#3B4350"),
                barColor = Color.parseColor("#2B80FF"),
                barValueTextColor = Color.parseColor("#273447"),
            ),
            presentationOptions = HistogramChartPresentationOptions(
                showGrid = true,
                showAxes = true,
                showBarLabels = true,
            ),
            onBinClick = { _, bin, value ->
                Toast.makeText(
                    context,
                    "${bin.payload ?: bin.label ?: "${bin.start.toInt()}-${bin.end.toInt()}"}: ${value.toInt()}",
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )
    }
}
