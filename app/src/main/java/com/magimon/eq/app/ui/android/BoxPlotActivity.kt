package com.magimon.eq.app.ui.android

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.magimon.eq.app.ui.applySampleToolbar
import com.magimon.eq.app.ui.compose.ChartSampleData
import com.magimon.eq.boxplot.BoxPlotChartPresentationOptions
import com.magimon.eq.boxplot.BoxPlotChartStyleOptions
import com.magimon.eq.boxplot.BoxPlotChartView

/**
 * Android View demo screen for [BoxPlotChartView].
 */
class BoxPlotActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val chart = BoxPlotChartView(this).apply {
            setStyleOptions(
                BoxPlotChartStyleOptions(
                    backgroundColor = Color.parseColor("#F7FAFC"),
                    axisColor = Color.parseColor("#8D9AA8"),
                    axisLabelColor = Color.parseColor("#3B4350"),
                    defaultBoxColor = Color.parseColor("#2563EB"),
                    medianLineColor = Color.parseColor("#111827"),
                    outlierColor = Color.parseColor("#DC2626"),
                    valueLabelTextColor = Color.parseColor("#2D3A4A"),
                ),
            )
            setPresentationOptions(
                BoxPlotChartPresentationOptions(
                    showGrid = true,
                    showAxes = true,
                    showValueLabels = true,
                    yLabelFormatter = { value -> "${value.toInt()}ms" },
                ),
            )
            setEntries(ChartSampleData.boxPlotEntries())
            setOnEntryClickListener { _, entry ->
                Toast.makeText(
                    this@BoxPlotActivity,
                    "${entry.payload ?: entry.label}: median ${entry.median.toInt()}ms",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }

        applySampleToolbar(
            title = "Box Plot Chart",
            content = chart,
        )
    }
}
