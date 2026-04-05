package com.magimon.eq.app.ui.compose

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.magimon.eq.app.ui.theme.EQChartTheme
import com.magimon.eq.compose.matrixheatmap.MatrixHeatmapChart
import com.magimon.eq.matrixheatmap.formatMatrixHeatmapValue

class ComposeMatrixHeatmapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EQChartTheme {
                ComposeMatrixHeatmapSampleScreen()
            }
        }
    }
}

@Composable
private fun ComposeMatrixHeatmapSampleScreen() {
    val context = LocalContext.current
    val data = remember { ChartSampleData.matrixHeatmapData() }

    ComposeSamplePage(title = "Compose Matrix Heatmap") {
        MatrixHeatmapChart(
            data = data,
            onCellClick = { cell ->
                Toast.makeText(
                    context,
                    "${cell.xKey} / ${cell.yKey}: ${formatMatrixHeatmapValue(cell.value)}",
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )
    }
}
