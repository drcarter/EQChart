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
import com.magimon.eq.compose.PieChart
import com.magimon.eq.pie.PieDonutPresentationOptions
import com.magimon.eq.pie.PieDonutStyleOptions
import com.magimon.eq.pie.PieLabelPosition

class ComposePieActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EQChartTheme {
                ComposePieSampleScreen()
            }
        }
    }
}

@Composable
private fun ComposePieSampleScreen() {
    val context = LocalContext.current
    val slices = remember { ChartSampleData.pieSlices() }

    ComposeSamplePage(title = "Compose Pie") {
        PieChart(
            slices = slices,
            styleOptions = PieDonutStyleOptions(
                backgroundColor = Color.parseColor("#F7FAFC"),
                sliceStrokeColor = Color.WHITE,
                labelTextColor = Color.parseColor("#1F2A37"),
                legendTextColor = Color.parseColor("#1F2A37"),
            ),
            presentationOptions = PieDonutPresentationOptions(
                showLegend = true,
                showLabels = true,
                labelPosition = PieLabelPosition.AUTO,
                enableSelectionExpand = true,
                selectedSliceExpandDp = 10f,
            ),
            onSliceClick = { _, slice, _ ->
                Toast.makeText(context, "${slice.label}: ${slice.value}", Toast.LENGTH_SHORT).show()
            },
        )
    }
}
