package com.magimon.eq.app.ui.android

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.magimon.eq.app.ui.applySampleToolbar
import com.magimon.eq.app.ui.compose.ChartSampleData
import com.magimon.eq.waterfall.WaterfallChartPresentationOptions
import com.magimon.eq.waterfall.WaterfallChartStyleOptions
import com.magimon.eq.waterfall.WaterfallChartView

/**
 * Android View demo screen for [WaterfallChartView].
 */
class WaterfallActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val chart = WaterfallChartView(this).apply {
            setStyleOptions(
                WaterfallChartStyleOptions(
                    backgroundColor = Color.parseColor("#F7FAFC"),
                    axisColor = Color.parseColor("#8D9AA8"),
                    axisLabelColor = Color.parseColor("#3B4350"),
                    connectorColor = Color.parseColor("#A1AEBE"),
                    positiveBarColor = Color.parseColor("#13C3A3"),
                    negativeBarColor = Color.parseColor("#EF476F"),
                    subtotalBarColor = Color.parseColor("#FF9F1C"),
                    totalBarColor = Color.parseColor("#2B80FF"),
                    barValueTextColor = Color.parseColor("#2D3A4A"),
                ),
            )
            setPresentationOptions(
                WaterfallChartPresentationOptions(
                    showGrid = true,
                    showAxes = true,
                    showBarLabels = true,
                    showConnectorLines = true,
                    valueLabelFormatter = { value ->
                        if (value > 0.0) "+${value.toInt()}" else value.toInt().toString()
                    },
                ),
            )
            setEntries(ChartSampleData.waterfallEntries())
            setOnEntryClickListener { _, entry, cumulativeTotal ->
                Toast.makeText(
                    this@WaterfallActivity,
                    "${entry.payload ?: entry.label}: ${cumulativeTotal.toInt()}",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }

        applySampleToolbar(
            title = "Waterfall Chart",
            content = chart,
        )
    }
}
