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
import com.magimon.eq.compose.waterfall.WaterfallChart
import com.magimon.eq.waterfall.WaterfallChartPresentationOptions
import com.magimon.eq.waterfall.WaterfallChartStyleOptions

/**
 * Compose demo screen for [WaterfallChart].
 */
class ComposeWaterfallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EQChartTheme {
                ComposeWaterfallSampleScreen()
            }
        }
    }
}

@Composable
private fun ComposeWaterfallSampleScreen() {
    val context = LocalContext.current
    val entries = remember { ChartSampleData.waterfallEntries() }

    ComposeSamplePage(title = "Compose Waterfall") {
        WaterfallChart(
            entries = entries,
            styleOptions = WaterfallChartStyleOptions(
                backgroundColor = Color.parseColor("#F7FAFC"),
                axisColor = Color.parseColor("#8D9AA8"),
                axisLabelColor = Color.parseColor("#3B4350"),
                connectorColor = Color.parseColor("#A1AEBE"),
                positiveBarColor = Color.parseColor("#13C3A3"),
                negativeBarColor = Color.parseColor("#EF476F"),
                subtotalBarColor = Color.parseColor("#FF9F1C"),
                totalBarColor = Color.parseColor("#2B80FF"),
                barValueTextColor = Color.parseColor("#273447"),
            ),
            presentationOptions = WaterfallChartPresentationOptions(
                showGrid = true,
                showAxes = true,
                showBarLabels = true,
                showConnectorLines = true,
                valueLabelFormatter = { value ->
                    if (value > 0.0) "+${value.toInt()}" else value.toInt().toString()
                },
            ),
            onEntryClick = { _, entry, cumulativeTotal ->
                Toast.makeText(
                    context,
                    "${entry.payload ?: entry.label}: ${cumulativeTotal.toInt()}",
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )
    }
}
