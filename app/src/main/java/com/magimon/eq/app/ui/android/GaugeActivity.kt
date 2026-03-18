package com.magimon.eq.app.ui.android

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.magimon.eq.app.ui.applySampleToolbar
import com.magimon.eq.gauge.GaugeChartPresentationOptions
import com.magimon.eq.gauge.GaugeChartStyleOptions
import com.magimon.eq.gauge.GaugeChartView
import com.magimon.eq.app.ui.compose.ChartSampleData

class GaugeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val chartView = GaugeChartView(this).apply {
            setStyleOptions(
                GaugeChartStyleOptions(
                    backgroundColor = Color.parseColor("#F7FAFC"),
                    progressColor = Color.parseColor("#2B80FF"),
                    indicatorColor = Color.parseColor("#1F2A37"),
                    indicatorCenterColor = Color.parseColor("#1F2A37"),
                ),
            )
            setPresentationOptions(
                GaugeChartPresentationOptions(
                    showTicks = true,
                    tickCount = 5,
                    showMinMaxLabels = true,
                    showValueText = true,
                    showCenterLabel = true,
                    animateOnValueChange = true,
                ),
            )
            setRanges(ChartSampleData.gaugeRanges())
            setValue(ChartSampleData.gaugeValue())
        }

        applySampleToolbar(
            title = "Gauge Chart",
            content = chartView,
        )
    }
}
