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
import com.magimon.eq.bubble.BubbleAxisOptions
import com.magimon.eq.bubble.BubbleLegendItem
import com.magimon.eq.bubble.BubbleLegendMode
import com.magimon.eq.bubble.BubbleLayoutMode
import com.magimon.eq.bubble.BubblePresentationOptions
import com.magimon.eq.compose.BubbleChart

class ComposeBubbleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EQChartTheme {
                ComposeBubbleSampleScreen()
            }
        }
    }
}

@Composable
private fun ComposeBubbleSampleScreen() {
    val context = LocalContext.current
    val bubbleData = remember { ChartSampleData.bubbleData() }

    ComposeSamplePage(title = "Compose Bubble") {
        BubbleChart(
            data = bubbleData,
            layoutMode = BubbleLayoutMode.PACKED,
            axisOptions = BubbleAxisOptions(showAxes = false, showGrid = false, showTicks = false),
            presentationOptions = BubblePresentationOptions(
                title = "Which type of loan is the most popular?",
                showLegend = true,
                legendMode = BubbleLegendMode.AUTO_WITH_OVERRIDE,
            ),
            legendItems = listOf(
                BubbleLegendItem("Arts", Color.parseColor("#4A7FB1")),
                BubbleLegendItem("Goods", Color.parseColor("#FF9100")),
                BubbleLegendItem("Labor", Color.parseColor("#F84C5A")),
                BubbleLegendItem("Services", Color.parseColor("#62B8B4")),
            ),
            onBubbleClick = { datum ->
                Toast.makeText(context, "${datum.payload ?: datum.label ?: "Unknown"}", Toast.LENGTH_SHORT).show()
            },
        )
    }
}
