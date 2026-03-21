package com.magimon.eq.app.ui.android

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.magimon.eq.app.ui.applySampleToolbar
import com.magimon.eq.app.ui.compose.ChartSampleData
import com.magimon.eq.funnel.FunnelChartPresentationOptions
import com.magimon.eq.funnel.FunnelChartStyleOptions
import com.magimon.eq.funnel.FunnelChartView

/**
 * Android View demo screen for [FunnelChartView].
 */
class FunnelActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val chart = FunnelChartView(this).apply {
            setStyleOptions(
                FunnelChartStyleOptions(
                    backgroundColor = Color.parseColor("#F7FAFC"),
                    stageBorderColor = Color.WHITE,
                    selectedStageBorderColor = Color.parseColor("#111827"),
                ),
            )
            setPresentationOptions(
                FunnelChartPresentationOptions(
                    showLabels = true,
                    showValues = true,
                    valueLabelFormatter = { value -> "${value.toInt()} leads" },
                ),
            )
            setStages(ChartSampleData.funnelStages())
            setOnStageClickListener { _, stage, value ->
                Toast.makeText(
                    this@FunnelActivity,
                    "${stage.payload ?: stage.label}: ${value.toInt()}",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }

        applySampleToolbar(
            title = "Funnel Chart",
            content = chart,
        )
    }
}
