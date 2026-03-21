package com.magimon.eq.app.ui.android

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.magimon.eq.app.ui.applySampleToolbar
import com.magimon.eq.app.ui.compose.ChartSampleData
import com.magimon.eq.histogram.HistogramChartPresentationOptions
import com.magimon.eq.histogram.HistogramChartStyleOptions
import com.magimon.eq.histogram.HistogramChartView

/**
 * Android View demo screen for [HistogramChartView].
 */
class HistogramActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val chart = HistogramChartView(this).apply {
            setStyleOptions(
                HistogramChartStyleOptions(
                    backgroundColor = Color.parseColor("#F7FAFC"),
                    axisColor = Color.parseColor("#8D9AA8"),
                    axisLabelColor = Color.parseColor("#3B4350"),
                    barColor = Color.parseColor("#2B80FF"),
                    barValueTextColor = Color.parseColor("#2D3A4A"),
                ),
            )
            setPresentationOptions(
                HistogramChartPresentationOptions(
                    showGrid = true,
                    showAxes = true,
                    showBarLabels = true,
                    valueLabelFormatter = { value -> value.toInt().toString() },
                ),
            )
            setBins(ChartSampleData.histogramBins())
            setOnBinClickListener { _, bin, value ->
                Toast.makeText(
                    this@HistogramActivity,
                    "${bin.payload ?: bin.label ?: "${bin.start.toInt()}-${bin.end.toInt()}"}: ${value.toInt()}",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }

        applySampleToolbar(
            title = "Histogram Chart",
            content = chart,
        )
    }
}
