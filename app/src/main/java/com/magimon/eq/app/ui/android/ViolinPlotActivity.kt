package com.magimon.eq.app.ui.android

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.magimon.eq.app.ui.applySampleToolbar
import com.magimon.eq.app.ui.compose.ViolinPlotSampleData
import com.magimon.eq.violin.ViolinPlotChartPresentationOptions
import com.magimon.eq.violin.ViolinPlotChartStyleOptions
import com.magimon.eq.violin.ViolinPlotChartView

/**
 * Android View demo screen for [ViolinPlotChartView].
 */
class ViolinPlotActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val chart = ViolinPlotChartView(this).apply {
            setStyleOptions(
                ViolinPlotChartStyleOptions(
                    backgroundColor = Color.parseColor("#F7FAFC"),
                    axisColor = Color.parseColor("#8D9AA8"),
                    axisLabelColor = Color.parseColor("#3B4350"),
                    quartileBandColor = Color.parseColor("#BFD3FF"),
                    valueLabelTextColor = Color.parseColor("#2D3A4A"),
                ),
            )
            setPresentationOptions(
                ViolinPlotChartPresentationOptions(
                    showGrid = true,
                    showAxes = true,
                    showValueLabels = true,
                    yLabelFormatter = { value -> "${value.toInt()}ms" },
                ),
            )
            setSeries(ViolinPlotSampleData.series())
            setOnSeriesClickListener { _, series ->
                Toast.makeText(
                    this@ViolinPlotActivity,
                    "${series.payload ?: series.label}: ${series.samples.size} samples",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }

        applySampleToolbar(
            title = "Violin Plot Chart",
            content = chart,
        )
    }
}
