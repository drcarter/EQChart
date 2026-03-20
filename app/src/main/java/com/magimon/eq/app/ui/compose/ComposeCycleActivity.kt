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
import com.magimon.eq.compose.cycle.CycleChart
import com.magimon.eq.cycle.CycleChartPresentationOptions
import com.magimon.eq.cycle.CycleChartStyleOptions

class ComposeCycleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EQChartTheme {
                ComposeCycleSampleScreen()
            }
        }
    }
}

@Composable
private fun ComposeCycleSampleScreen() {
    val context = LocalContext.current
    val nodes = remember { ChartSampleData.cycleNodes() }
    val links = remember { ChartSampleData.cycleLinks() }

    ComposeSamplePage(title = "Compose Cycle") {
        CycleChart(
            nodes = nodes,
            links = links,
            styleOptions = CycleChartStyleOptions(
                backgroundColor = Color.parseColor("#F7FAFC"),
                nodeStrokeColor = Color.WHITE,
                selectedStrokeColor = Color.parseColor("#0F172A"),
            ),
            presentationOptions = CycleChartPresentationOptions(
                showNodeLabels = true,
                showLinkLabels = true,
            ),
            onNodeClick = { _, node, _ ->
                Toast.makeText(context, node.label, Toast.LENGTH_SHORT).show()
            },
            onLinkClick = { _, link, _ ->
                val message = "${link.sourceId} -> ${link.targetId}: ${link.value.toInt()}"
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            },
        )
    }
}
