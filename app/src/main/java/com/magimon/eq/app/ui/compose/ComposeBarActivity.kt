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
import com.magimon.eq.compose.BarChart
import com.magimon.eq.bar.BarChartPresentationOptions
import com.magimon.eq.bar.BarChartStyleOptions
import com.magimon.eq.bar.BarLayoutMode
import com.magimon.eq.bar.BarOrientation

/**
 * Compose demo screen for [BarChart] using grouped vertical mode.
 */
class ComposeBarActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EQChartTheme {
                ComposeBarSampleScreen()
            }
        }
    }
}

@Composable
private fun ComposeBarSampleScreen() {
    val context = LocalContext.current
    val series = remember { ChartSampleData.barSeries() }
    val categories = remember(series) {
        series.firstOrNull()?.points.orEmpty().map { it.category }
    }

    ComposeSamplePage(title = "Compose Bar") {
        BarChart(
            series = series,
            styleOptions = BarChartStyleOptions(
                backgroundColor = Color.parseColor("#F7FAFC"),
                axisColor = Color.parseColor("#8D9AA8"),
                axisLabelColor = Color.parseColor("#3B4350"),
                legendTextColor = Color.parseColor("#273447"),
                barValueTextColor = Color.parseColor("#273447"),
            ),
            presentationOptions = BarChartPresentationOptions(
                showLegend = true,
                showGrid = true,
                showAxes = true,
                showBarLabels = true,
                layoutMode = BarLayoutMode.GROUPED,
                orientation = BarOrientation.VERTICAL,
            ),
            onBarClick = { seriesIndex, categoryIndex, value, _ ->
                val seriesLabel = series.getOrNull(seriesIndex)?.name ?: "Series $seriesIndex"
                val categoryLabel = categories.getOrNull(categoryIndex) ?: "Category $categoryIndex"
                Toast.makeText(
                    context,
                    "$seriesLabel | $categoryLabel: ${value.toInt()}",
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )
    }
}
