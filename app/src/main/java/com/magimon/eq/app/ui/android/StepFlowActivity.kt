package com.magimon.eq.app.ui.android

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.magimon.eq.app.ui.applySampleToolbar
import com.magimon.eq.app.ui.compose.ChartSampleData
import com.magimon.eq.app.ui.stepFlowSampleStyleOptions
import com.magimon.eq.stepflow.StepFlowChartPresentationOptions
import com.magimon.eq.stepflow.StepFlowChartView

/**
 * Android View demo screen for [StepFlowChartView].
 */
class StepFlowActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val chartView = StepFlowChartView(this).apply {
            setHubContent(ChartSampleData.stepFlowHubContent())
            setSteps(ChartSampleData.stepFlowSteps())
            setStyleOptions(
                stepFlowSampleStyleOptions(resources.configuration.screenWidthDp),
            )
            setPresentationOptions(
                StepFlowChartPresentationOptions(
                    animateOnDataChange = true,
                    showStepDescriptions = true,
                    showHubDescription = true,
                    showTopTailDot = true,
                    showBottomTailDot = true,
                ),
            )
            setOnStepClickListener { _, step, _ ->
                Toast.makeText(this@StepFlowActivity, step.title, Toast.LENGTH_SHORT).show()
            }
        }

        applySampleToolbar(
            title = "Step Flow Chart",
            content = chartView,
        )
    }
}
