package com.magimon.eq.app.ui.android

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.magimon.eq.app.ui.applySampleToolbar
import com.magimon.eq.app.ui.compose.ChartSampleData
import com.magimon.eq.line.LineChartPresentationOptions
import com.magimon.eq.line.LineChartStyleOptions
import com.magimon.eq.line.LineChartView
import com.magimon.eq.line.LineSeries

/**
 * Android View demo screen for [LineChartView] with marker interaction.
 */
class LineActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val lineView = LineChartView(this).apply {
            setStyleOptions(
                LineChartStyleOptions(
                    backgroundColor = Color.parseColor("#F7FAFC"),
                    axisColor = Color.parseColor("#8D9AA8"),
                    axisLabelColor = Color.parseColor("#3B4350"),
                    legendTextColor = Color.parseColor("#273447"),
                    pointLabelTextSizeSp = 11f,
                    legendTextSizeSp = 12f,
                ),
            )
            setPresentationOptions(
                LineChartPresentationOptions(
                    showLegend = true,
                    showGrid = true,
                    showAxes = true,
                    showPoints = true,
                    showAreaFill = false,
                ),
            )
            setSeries(
                ChartSampleData.lineSeries().mapIndexed { index, series ->
                    if (index == 0) {
                        series.copy(payload = "Traffic")
                    } else {
                        series.copy(payload = "Conversion")
                    }
                },
            )
            setOnPointClickListener { _, _, point, payload ->
                val label = payload as? String ?: "Point"
                Toast.makeText(
                    this@LineActivity,
                    "$label: x=${point.x.toInt()}, y=${point.y.toInt()}",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }

        applySampleToolbar(
            title = "Line Chart",
            content = lineView,
        )
    }
}
