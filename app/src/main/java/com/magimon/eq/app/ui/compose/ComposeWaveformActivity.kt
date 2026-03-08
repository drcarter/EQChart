package com.magimon.eq.app.ui.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.magimon.eq.app.ui.theme.EQChartTheme
import com.magimon.eq.compose.PcmWaveformChart
import com.magimon.eq.compose.rememberPcmWaveformController
import com.magimon.eq.waveform.PcmWaveFormStyleOptions

class ComposeWaveformActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EQChartTheme {
                ComposeWaveformSampleScreen()
            }
        }
    }
}

@Composable
private fun ComposeWaveformSampleScreen() {
    val controller = rememberPcmWaveformController(
        sampleRateHz = 44_100,
        windowDurationMs = 2_500,
    )

    LaunchedEffect(Unit) {
        controller.setPcm16Mono(
            ChartSampleData.generateSineWaveSamples(
                sampleRateHz = 44_100,
                durationMs = 2_500,
                frequencyHz = 220.0,
            ),
        )
    }

    ComposeSamplePage(title = "Compose Waveform") {
        PcmWaveformChart(
            controller = controller,
            styleOptions = PcmWaveFormStyleOptions(
                backgroundColor = 0xFF0E1620.toInt(),
                waveColor = 0xFF62D5FF.toInt(),
                centerLineColor = 0xFF2D3A46.toInt(),
                strokeWidthDp = 1.4f,
                contentPaddingDp = 10f,
            ),
        )
    }
}
