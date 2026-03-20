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
import com.magimon.eq.compose.gauge.GaugeChart
import com.magimon.eq.gauge.GaugeChartPresentationOptions
import com.magimon.eq.gauge.GaugeChartStyleOptions

class ComposeGaugeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EQChartTheme {
                ComposeGaugeSampleScreen()
            }
        }
    }
}

@Composable
private fun ComposeGaugeSampleScreen() {
    val value = remember { ChartSampleData.gaugeValue() }
    val ranges = remember { ChartSampleData.gaugeRanges() }

    ComposeSamplePage(title = "Compose Gauge") {
        GaugeChart(
            value = value,
            ranges = ranges,
            modifier = Modifier.fillMaxSize(),
            styleOptions = GaugeChartStyleOptions(
                backgroundColor = Color.parseColor("#F7FAFC"),
                progressColor = Color.parseColor("#2B80FF"),
                indicatorColor = Color.parseColor("#1F2A37"),
                indicatorCenterColor = Color.parseColor("#1F2A37"),
            ),
            presentationOptions = GaugeChartPresentationOptions(
                showTicks = true,
                tickCount = 5,
                showMinMaxLabels = true,
                showValueText = true,
                showCenterLabel = true,
            ),
        )
    }
}
