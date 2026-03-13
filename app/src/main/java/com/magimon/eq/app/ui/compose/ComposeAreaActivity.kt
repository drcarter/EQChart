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
import com.magimon.eq.compose.AreaChart
import com.magimon.eq.line.LineChartPresentationOptions
import com.magimon.eq.line.LineChartStyleOptions

/**
 * Compose demo screen for [AreaChart].
 */
class ComposeAreaActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EQChartTheme {
                ComposeAreaSampleScreen()
            }
        }
    }
}

@Composable
private fun ComposeAreaSampleScreen() {
    val context = LocalContext.current
    val series = remember { ChartSampleData.areaSeries() }

    ComposeSamplePage(title = "Compose Area") {
        AreaChart(
            series = series,
            styleOptions = LineChartStyleOptions(
                backgroundColor = Color.parseColor("#F7FAFC"),
                axisColor = Color.parseColor("#8D9AA8"),
                axisLabelColor = Color.parseColor("#3B4350"),
                legendTextColor = Color.parseColor("#273447"),
            ),
            presentationOptions = LineChartPresentationOptions(
                showLegend = true,
                showGrid = true,
                showAxes = true,
                showPoints = true,
                showAreaFill = true,
            ),
            onPointClick = { _, _, point, _ ->
                val label = point.payload as? String ?: "X=${point.x.toInt()}"
                Toast.makeText(
                    context,
                    "$label: ${point.y.toInt()}",
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )
    }
}
