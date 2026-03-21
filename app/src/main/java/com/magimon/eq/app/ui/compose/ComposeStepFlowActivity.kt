package com.magimon.eq.app.ui.compose

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.magimon.eq.app.ui.stepFlowSampleStyleOptions
import com.magimon.eq.app.ui.theme.EQChartTheme
import com.magimon.eq.compose.stepflow.StepFlowChart
import com.magimon.eq.stepflow.StepFlowChartPresentationOptions

class ComposeStepFlowActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EQChartTheme {
                ComposeStepFlowSampleScreen()
            }
        }
    }
}

@Composable
private fun ComposeStepFlowSampleScreen() {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val hubContent = remember { ChartSampleData.stepFlowHubContent() }
    val steps = remember { ChartSampleData.stepFlowSteps() }
    val styleOptions = remember(configuration.screenWidthDp) {
        stepFlowSampleStyleOptions(configuration.screenWidthDp)
    }

    ComposeSamplePage(title = "Compose Step Flow") {
        StepFlowChart(
            hubContent = hubContent,
            steps = steps,
            styleOptions = styleOptions,
            presentationOptions = StepFlowChartPresentationOptions(
                showStepDescriptions = true,
                showHubDescription = true,
                showTopTailDot = true,
                showBottomTailDot = true,
            ),
            onStepClick = { _, step, _ ->
                Toast.makeText(context, step.title, Toast.LENGTH_SHORT).show()
            },
        )
    }
}
