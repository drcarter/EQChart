package com.magimon.eq.app.ui.android

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.magimon.eq.app.ui.applySampleToolbar
import com.magimon.eq.app.ui.compose.ChartSampleData
import com.magimon.eq.bar.BarChartPresentationOptions
import com.magimon.eq.bar.BarChartStyleOptions
import com.magimon.eq.bar.BarChartView
import com.magimon.eq.bar.BarLayoutMode
import com.magimon.eq.bar.BarOrientation

/**
 * Android View demo screen for [BarChartView] supporting grouped/stacked modes.
 */
class BarActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val barChart = BarChartView(this).apply {
            setStyleOptions(
                BarChartStyleOptions(
                    backgroundColor = Color.parseColor("#F7FAFC"),
                    axisColor = Color.parseColor("#8D9AA8"),
                    axisLabelColor = Color.parseColor("#3B4350"),
                    legendTextColor = Color.parseColor("#273447"),
                    barValueTextColor = Color.parseColor("#2D3A4A"),
                ),
            )
            setPresentationOptions(
                BarChartPresentationOptions(
                    showLegend = true,
                    showGrid = true,
                    showAxes = true,
                    showBarLabels = true,
                    layoutMode = BarLayoutMode.GROUPED,
                    orientation = BarOrientation.VERTICAL,
                ),
            )
            setSeries(ChartSampleData.barSeries())
            setOnBarClickListener { _, categoryIndex, value, payload ->
                Toast.makeText(
                    this@BarActivity,
                    "${payload?.toString() ?: "Category $categoryIndex"}: ${value.toInt()}",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }

        applySampleToolbar(
            title = "Bar Chart",
            content = barChart,
        )
    }
}

