package com.magimon.eq.app.ui.compose

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.magimon.eq.app.ui.theme.EQChartTheme
import com.magimon.eq.compose.treemap.TreemapChart

class ComposeTreemapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EQChartTheme {
                ComposeTreemapSampleScreen()
            }
        }
    }
}

@Composable
private fun ComposeTreemapSampleScreen() {
    val context = LocalContext.current
    val groups = remember { ChartSampleData.treemapGroups() }

    ComposeSamplePage(title = "Compose Treemap") {
        TreemapChart(
            groups = groups,
            onItemClick = { item ->
                val supporting = item.supportingText?.let { " | $it" }.orEmpty()
                Toast.makeText(
                    context,
                    "${item.label}: ${item.value.toInt()}$supporting",
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )
    }
}
