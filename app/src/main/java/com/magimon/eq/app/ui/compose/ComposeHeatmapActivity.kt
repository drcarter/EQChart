package com.magimon.eq.app.ui.compose

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.magimon.eq.app.ui.theme.EQChartTheme
import com.magimon.eq.compose.StockHeatmapChart
import com.magimon.eq.heatmap.StockHeatmapHelper

class ComposeHeatmapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EQChartTheme {
                ComposeHeatmapSampleScreen()
            }
        }
    }
}

@Composable
private fun ComposeHeatmapSampleScreen() {
    val context = LocalContext.current
    val sections = remember { ChartSampleData.heatmapSections() }

    ComposeSamplePage(title = "Compose Heatmap") {
        StockHeatmapChart(
            sections = sections,
            onItemClick = { item ->
                val ratioPart = item.sizeRatio?.let { " | Ratio: ${String.format("%.1f", it)}" } ?: ""
                val message =
                    "${item.symbol}: ${StockHeatmapHelper.formatChange(item.changePct)} | " +
                    "Market Cap: ${StockHeatmapHelper.formatMarketCap(item.marketCap)}$ratioPart"
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            },
        )
    }
}
