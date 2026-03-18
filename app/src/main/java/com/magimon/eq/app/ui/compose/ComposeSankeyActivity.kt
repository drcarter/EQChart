package com.magimon.eq.app.ui.compose

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.magimon.eq.app.ui.theme.EQChartTheme
import com.magimon.eq.compose.SankeyChart
import com.magimon.eq.sankey.SankeyChartPresentationOptions
import com.magimon.eq.sankey.SankeyChartStyleOptions

class ComposeSankeyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EQChartTheme {
                ComposeSankeySampleScreen()
            }
        }
    }
}

@Composable
private fun ComposeSankeySampleScreen() {
    val nodes = remember { ChartSampleData.sankeyNodes() }
    val links = remember { ChartSampleData.sankeyLinks() }

    ComposeSamplePage(title = "Compose Sankey") {
        SankeyChart(
            nodes = nodes,
            links = links,
            modifier = Modifier.fillMaxSize(),
            styleOptions = SankeyChartStyleOptions(
                backgroundColor = Color.parseColor("#F7FAFC"),
            ),
            presentationOptions = SankeyChartPresentationOptions(
                showNodeLabels = true,
                showLinkValues = true,
            ),
        )
    }
}
