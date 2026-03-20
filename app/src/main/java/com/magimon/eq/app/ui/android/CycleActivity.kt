package com.magimon.eq.app.ui.android

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.magimon.eq.app.ui.applySampleToolbar
import com.magimon.eq.app.ui.compose.ChartSampleData
import com.magimon.eq.cycle.CycleChartPresentationOptions
import com.magimon.eq.cycle.CycleChartStyleOptions
import com.magimon.eq.cycle.CycleChartView

/**
 * Android View demo screen for [CycleChartView].
 */
class CycleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cycleChart = CycleChartView(this).apply {
            setStyleOptions(
                CycleChartStyleOptions(
                    backgroundColor = Color.parseColor("#F7FAFC"),
                    nodeStrokeColor = Color.WHITE,
                    selectedStrokeColor = Color.parseColor("#0F172A"),
                ),
            )
            setPresentationOptions(
                CycleChartPresentationOptions(
                    showNodeLabels = true,
                    showLinkLabels = true,
                    animateOnDataChange = true,
                ),
            )
            setNodes(ChartSampleData.cycleNodes())
            setLinks(ChartSampleData.cycleLinks())
            setOnNodeClickListener { _, node, _ ->
                Toast.makeText(this@CycleActivity, node.label, Toast.LENGTH_SHORT).show()
            }
            setOnLinkClickListener { _, link, _ ->
                val message = "${link.sourceId} -> ${link.targetId}: ${link.value.toInt()}"
                Toast.makeText(this@CycleActivity, message, Toast.LENGTH_SHORT).show()
            }
        }

        applySampleToolbar(
            title = "Cycle Chart",
            content = cycleChart,
        )
    }
}
