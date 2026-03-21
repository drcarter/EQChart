package com.magimon.eq.app.ui.android

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.magimon.eq.app.ui.applySampleToolbar
import com.magimon.eq.app.ui.compose.ChartSampleData
import com.magimon.eq.sunburst.SunburstChartPresentationOptions
import com.magimon.eq.sunburst.SunburstChartStyleOptions
import com.magimon.eq.sunburst.SunburstChartView

/**
 * Android View demo screen for [SunburstChartView].
 */
class SunburstActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val chartView = SunburstChartView(this).apply {
            setStyleOptions(
                SunburstChartStyleOptions(),
            )
            setPresentationOptions(
                SunburstChartPresentationOptions(),
            )
            setNodes(ChartSampleData.sunburstNodes())
            setOnNodeClickListener { _, node, _ ->
                Toast.makeText(
                    this@SunburstActivity,
                    "${node.label}: ${node.value.toInt()}",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }

        applySampleToolbar(
            title = "Sunburst Chart",
            content = chartView,
        )
    }
}
