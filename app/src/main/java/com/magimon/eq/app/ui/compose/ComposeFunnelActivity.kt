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
import com.magimon.eq.compose.funnel.FunnelChart
import com.magimon.eq.funnel.FunnelChartPresentationOptions
import com.magimon.eq.funnel.FunnelChartStyleOptions

/**
 * Compose demo screen for [FunnelChart].
 */
class ComposeFunnelActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EQChartTheme {
                ComposeFunnelSampleScreen()
            }
        }
    }
}

@Composable
private fun ComposeFunnelSampleScreen() {
    val context = LocalContext.current
    val stages = remember { ChartSampleData.funnelStages() }

    ComposeSamplePage(title = "Compose Funnel") {
        FunnelChart(
            stages = stages,
            styleOptions = FunnelChartStyleOptions(
                backgroundColor = Color.parseColor("#F7FAFC"),
                stageBorderColor = Color.WHITE,
                selectedStageBorderColor = Color.parseColor("#111827"),
            ),
            presentationOptions = FunnelChartPresentationOptions(
                showLabels = true,
                showValues = true,
                valueLabelFormatter = { value -> "${value.toInt()} leads" },
            ),
            onStageClick = { _, stage, value ->
                Toast.makeText(
                    context,
                    "${stage.payload ?: stage.label}: ${value.toInt()}",
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )
    }
}
