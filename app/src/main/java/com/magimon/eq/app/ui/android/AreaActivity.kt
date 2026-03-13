package com.magimon.eq.app.ui.android

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.magimon.eq.app.ui.applySampleToolbar
import com.magimon.eq.app.ui.compose.ChartSampleData
import com.magimon.eq.line.AreaChartView
import com.magimon.eq.line.LineChartPresentationOptions
import com.magimon.eq.line.LineChartStyleOptions

/**
 * Android View demo screen for [AreaChartView] with filled series.
 */
class AreaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val areaSeries = ChartSampleData.areaSeries()

        val areaChart = AreaChartView(this).apply {
            setStyleOptions(
                LineChartStyleOptions(
                    backgroundColor = Color.parseColor("#F7FAFC"),
                    axisColor = Color.parseColor("#8D9AA8"),
                    axisLabelColor = Color.parseColor("#3B4350"),
                    legendTextColor = Color.parseColor("#273447"),
                    areaFillAlpha = 100,
                ),
            )
            setPresentationOptions(
                LineChartPresentationOptions(
                    showLegend = true,
                    showGrid = true,
                    showAxes = true,
                    showPoints = true,
                    showAreaFill = true,
                ),
            )
            setSeries(areaSeries)
            setOnPointClickListener { _, _, point, _ ->
                Toast.makeText(
                    this@AreaActivity,
                    "x=${point.x.toInt()}, y=${point.y.toInt()}",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }

        applySampleToolbar(
            title = "Area Chart",
            content = areaChart,
        )
    }
}

