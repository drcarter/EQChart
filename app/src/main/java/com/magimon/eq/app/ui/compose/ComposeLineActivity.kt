package com.magimon.eq.app.ui.compose

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.magimon.eq.app.ui.theme.EQChartTheme
import com.magimon.eq.compose.LineChart
import com.magimon.eq.line.LineChartPresentationOptions
import com.magimon.eq.line.LineChartStyleOptions

/**
 * Compose demo screen for [LineChart] with click-to-toast interaction.
 */
class ComposeLineActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EQChartTheme {
                ComposeLineSampleScreen()
            }
        }
    }
}

@Composable
private fun ComposeLineSampleScreen() {
    val context = LocalContext.current
    val series = remember { ChartSampleData.lineSeries() }

    ComposeSamplePage(title = "Compose Line") {
        LineChart(
            series = series,
            styleOptions = LineChartStyleOptions(
                backgroundColor = android.graphics.Color.parseColor("#F7FAFC"),
                axisColor = android.graphics.Color.parseColor("#8D9AA8"),
                axisLabelColor = android.graphics.Color.parseColor("#3B4350"),
                legendTextColor = android.graphics.Color.parseColor("#273447"),
            ),
            presentationOptions = LineChartPresentationOptions(
                showLegend = true,
                showGrid = true,
                showAxes = true,
                showPoints = true,
                showAreaFill = false,
            ),
            onPointClick = { _, _, point, _ ->
                Toast.makeText(
                    context,
                    "x=${point.x.toInt()}, y=${point.y.toInt()}",
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )
    }
}
