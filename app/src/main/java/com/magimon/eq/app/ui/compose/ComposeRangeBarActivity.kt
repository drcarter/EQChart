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
import com.magimon.eq.compose.rangebar.RangeBarChart
import com.magimon.eq.rangebar.RangeBarChartPresentationOptions
import com.magimon.eq.rangebar.RangeBarChartStyleOptions

/**
 * Compose demo screen for [RangeBarChart].
 */
class ComposeRangeBarActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EQChartTheme {
                ComposeRangeBarSampleScreen()
            }
        }
    }
}

@Composable
private fun ComposeRangeBarSampleScreen() {
    val context = LocalContext.current
    val entries = remember { ChartSampleData.rangeBarEntries() }

    ComposeSamplePage(title = "Compose Range Bar Timeline") {
        RangeBarChart(
            entries = entries,
            styleOptions = RangeBarChartStyleOptions(
                backgroundColor = Color.parseColor("#F8FAFC"),
                gridColor = Color.parseColor("#D5DEE8"),
                axisColor = Color.parseColor("#7A8798"),
                axisLabelColor = Color.parseColor("#2D3A4A"),
                barLabelTextColor = Color.parseColor("#1E293B"),
            ),
            presentationOptions = RangeBarChartPresentationOptions(
                showGrid = true,
                showAxes = true,
                showBarLabels = true,
                xLabelFormatter = { tick ->
                    val week = tick.toInt().coerceAtLeast(0)
                    "W${week + 1}"
                },
                barLabelFormatter = { entry ->
                    "W${entry.startValue.toInt() + 1} - W${entry.endValue.toInt() + 1}"
                },
            ),
            onEntryClick = { _, entry, duration ->
                Toast.makeText(
                    context,
                    "${entry.payload ?: entry.label}: ${duration.toInt()}w",
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )
    }
}
