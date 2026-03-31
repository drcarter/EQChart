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
import com.magimon.eq.boxplot.BoxPlotChartPresentationOptions
import com.magimon.eq.boxplot.BoxPlotChartStyleOptions
import com.magimon.eq.compose.boxplot.BoxPlotChart

/**
 * Compose demo screen for [BoxPlotChart].
 */
class ComposeBoxPlotActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EQChartTheme {
                ComposeBoxPlotSampleScreen()
            }
        }
    }
}

@Composable
private fun ComposeBoxPlotSampleScreen() {
    val context = LocalContext.current
    val entries = remember { ChartSampleData.boxPlotEntries() }

    ComposeSamplePage(title = "Compose Box Plot") {
        BoxPlotChart(
            entries = entries,
            styleOptions = BoxPlotChartStyleOptions(
                backgroundColor = Color.parseColor("#F7FAFC"),
                axisColor = Color.parseColor("#8D9AA8"),
                axisLabelColor = Color.parseColor("#3B4350"),
                defaultBoxColor = Color.parseColor("#2563EB"),
                medianLineColor = Color.parseColor("#111827"),
                outlierColor = Color.parseColor("#DC2626"),
                valueLabelTextColor = Color.parseColor("#273447"),
            ),
            presentationOptions = BoxPlotChartPresentationOptions(
                showGrid = true,
                showAxes = true,
                showValueLabels = true,
                yLabelFormatter = { value -> "${value.toInt()}ms" },
            ),
            onEntryClick = { _, entry ->
                Toast.makeText(
                    context,
                    "${entry.payload ?: entry.label}: median ${entry.median.toInt()}ms",
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )
    }
}
