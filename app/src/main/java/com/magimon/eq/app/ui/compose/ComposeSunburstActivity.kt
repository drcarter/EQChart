package com.magimon.eq.app.ui.compose

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.magimon.eq.app.ui.theme.EQChartTheme
import com.magimon.eq.compose.sunburst.SunburstChart

class ComposeSunburstActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EQChartTheme {
                ComposeSunburstSampleScreen()
            }
        }
    }
}

@Composable
private fun ComposeSunburstSampleScreen() {
    val context = LocalContext.current
    val nodes = remember { ChartSampleData.sunburstNodes() }

    ComposeSamplePage(title = "Compose Sunburst") {
        SunburstChart(
            nodes = nodes,
            modifier = Modifier.fillMaxSize(),
            onNodeClick = { _, node, _ ->
                Toast.makeText(context, "${node.label}: ${node.value.toInt()}", Toast.LENGTH_SHORT).show()
            },
        )
    }
}
