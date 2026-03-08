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
import com.magimon.eq.compose.DonutChart
import com.magimon.eq.pie.PieDonutPresentationOptions
import com.magimon.eq.pie.PieDonutStyleOptions
import com.magimon.eq.pie.PieLabelPosition
import kotlin.math.roundToInt

class ComposeDonutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EQChartTheme {
                ComposeDonutSampleScreen()
            }
        }
    }
}

@Composable
private fun ComposeDonutSampleScreen() {
    val context = LocalContext.current
    val slices = remember { ChartSampleData.donutSlices() }

    ComposeSamplePage(title = "Compose Donut") {
        DonutChart(
            slices = slices,
            innerRadiusRatio = 0.58f,
            styleOptions = PieDonutStyleOptions(
                backgroundColor = Color.parseColor("#FFFFFF"),
                sliceStrokeColor = Color.WHITE,
                labelTextColor = Color.parseColor("#1F2A37"),
                centerTextColor = Color.parseColor("#1A2433"),
                centerSubTextColor = Color.parseColor("#6F7E8E"),
            ),
            presentationOptions = PieDonutPresentationOptions(
                showLegend = true,
                showLabels = true,
                labelPosition = PieLabelPosition.AUTO,
                enableSelectionExpand = true,
                selectedSliceExpandDp = 10f,
                centerText = "Total",
                centerSubText = slices.sumOf { it.value }.roundToInt().toString(),
                startAngleDeg = -90f,
                clockwise = true,
            ),
            onSliceClick = { _, slice, _ ->
                Toast.makeText(context, "${slice.label}: ${slice.value}", Toast.LENGTH_SHORT).show()
            },
        )
    }
}
