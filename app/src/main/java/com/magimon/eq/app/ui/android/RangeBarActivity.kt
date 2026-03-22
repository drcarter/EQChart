package com.magimon.eq.app.ui.android

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.magimon.eq.app.ui.applySampleToolbar
import com.magimon.eq.app.ui.compose.ChartSampleData
import com.magimon.eq.rangebar.RangeBarChartPresentationOptions
import com.magimon.eq.rangebar.RangeBarChartStyleOptions
import com.magimon.eq.rangebar.RangeBarChartView

/**
 * Android View demo screen for [RangeBarChartView].
 */
class RangeBarActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val chart = RangeBarChartView(this).apply {
            setStyleOptions(
                RangeBarChartStyleOptions(
                    backgroundColor = Color.parseColor("#F8FAFC"),
                    gridColor = Color.parseColor("#D5DEE8"),
                    axisColor = Color.parseColor("#7A8798"),
                    axisLabelColor = Color.parseColor("#2D3A4A"),
                    barLabelTextColor = Color.parseColor("#1E293B"),
                ),
            )
            setPresentationOptions(
                RangeBarChartPresentationOptions(
                    showGrid = true,
                    showAxes = true,
                    showBarLabels = true,
                    xLabelFormatter = { tick ->
                        val week = tick.toInt().coerceAtLeast(0)
                        "W${week + 1}"
                    },
                    barLabelFormatter = { entry ->
                        "W${entry.startValue.toInt() + 1} - W${entry.endValue.toInt() + 1}"
                    },
                ),
            )
            setEntries(ChartSampleData.rangeBarEntries())
            setOnEntryClickListener { _, entry ->
                val duration = kotlin.math.abs(entry.end - entry.start).toInt()
                Toast.makeText(
                    this@RangeBarActivity,
                    "${entry.payload ?: entry.label}: ${duration}w",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }

        applySampleToolbar(
            title = "Range Bar / Timeline",
            content = chart,
        )
    }
}
