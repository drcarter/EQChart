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
import com.magimon.eq.compose.RadarChart
import com.magimon.eq.radar.RadarChartPresentationOptions
import com.magimon.eq.radar.RadarChartStyleOptions
import kotlin.math.roundToInt

class ComposeRadarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EQChartTheme {
                ComposeRadarSampleScreen()
            }
        }
    }
}

@Composable
private fun ComposeRadarSampleScreen() {
    val context = LocalContext.current
    val axes = remember { ChartSampleData.radarAxes() }
    val series = remember { ChartSampleData.radarSeries() }

    ComposeSamplePage(title = "Compose Radar") {
        RadarChart(
            axes = axes,
            series = series,
            valueMax = 100.0,
            styleOptions = RadarChartStyleOptions(
                backgroundColor = Color.parseColor("#FFFFFF"),
                gridColor = Color.parseColor("#CFD9E6"),
                axisColor = Color.parseColor("#C7D2DF"),
                axisLabelColor = Color.parseColor("#646D7B"),
                legendTextColor = Color.parseColor("#1E2530"),
                fillAlpha = 82,
            ),
            presentationOptions = RadarChartPresentationOptions(
                showLegend = true,
                showAxisLabels = true,
                gridLevels = 5,
            ),
            onPointClick = { seriesIndex, axisIndex, value, _ ->
                val message = "${series[seriesIndex].name} | ${axes[axisIndex].label}: ${value.roundToInt()}"
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            },
        )
    }
}
