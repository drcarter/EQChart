package com.magimon.eq.app.ui.android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.magimon.eq.app.ui.applySampleToolbar
import com.magimon.eq.app.ui.compose.ChartSampleData
import com.magimon.eq.sankey.SankeyChartPresentationOptions
import com.magimon.eq.sankey.SankeyChartStyleOptions
import com.magimon.eq.sankey.SankeyChartView

class SankeyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val chartView = SankeyChartView(this).apply {
            setStyleOptions(
                SankeyChartStyleOptions(
                    backgroundColor = android.graphics.Color.parseColor("#F7FAFC"),
                ),
            )
            setPresentationOptions(
                SankeyChartPresentationOptions(
                    showNodeLabels = true,
                    showLinkValues = true,
                    animateOnDataChange = true,
                ),
            )
            setNodes(ChartSampleData.sankeyNodes())
            setLinks(ChartSampleData.sankeyLinks())
        }

        applySampleToolbar(
            title = "Sankey Chart",
            content = chartView,
        )
    }
}
