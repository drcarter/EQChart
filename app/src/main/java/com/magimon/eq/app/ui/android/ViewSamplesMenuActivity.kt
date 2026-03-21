package com.magimon.eq.app.ui.android

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.magimon.eq.app.R
import com.magimon.eq.app.ui.ChartMenuEntry
import com.magimon.eq.app.ui.applySampleToolbar
import com.magimon.eq.app.ui.createChartMenuGrid

class ViewSamplesMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = createChartMenuGrid(
            entries = listOf(
                ChartMenuEntry("Heatmap", R.drawable.ic_chart_heatmap, Color.parseColor("#0EA5E9"), HeatmapActivity::class.java),
                ChartMenuEntry("Bubble", R.drawable.ic_chart_bubble, Color.parseColor("#EC4899"), BubbleActivity::class.java),
                ChartMenuEntry("Line", R.drawable.ic_chart_line, Color.parseColor("#2563EB"), LineActivity::class.java),
                ChartMenuEntry("Area", R.drawable.ic_chart_area, Color.parseColor("#14B8A6"), AreaActivity::class.java),
                ChartMenuEntry("Bar", R.drawable.ic_chart_bar, Color.parseColor("#F97316"), BarActivity::class.java),
                ChartMenuEntry("Histogram", R.drawable.ic_chart_histogram, Color.parseColor("#2563EB"), HistogramActivity::class.java),
                ChartMenuEntry("Waterfall", R.drawable.ic_chart_waterfall, Color.parseColor("#0F766E"), WaterfallActivity::class.java),
                ChartMenuEntry("PCM Waveform", R.drawable.ic_chart_waveform, Color.parseColor("#8B5CF6"), WaveformFileActivity::class.java),
                ChartMenuEntry("Radar", R.drawable.ic_chart_radar, Color.parseColor("#7C3AED"), RadarActivity::class.java),
                ChartMenuEntry("Pie", R.drawable.ic_chart_pie, Color.parseColor("#EF4444"), PieActivity::class.java),
                ChartMenuEntry("Donut", R.drawable.ic_chart_donut, Color.parseColor("#F59E0B"), DonutActivity::class.java),
                ChartMenuEntry("Gauge", R.drawable.ic_chart_gauge, Color.parseColor("#10B981"), GaugeActivity::class.java),
                ChartMenuEntry("Sankey", R.drawable.ic_chart_sankey, Color.parseColor("#06B6D4"), SankeyActivity::class.java),
                ChartMenuEntry("Cycle", R.drawable.ic_chart_cycle, Color.parseColor("#6366F1"), CycleActivity::class.java),
                ChartMenuEntry("Step Flow", R.drawable.ic_chart_step_flow, Color.parseColor("#84CC16"), StepFlowActivity::class.java),
            ),
        )

        applySampleToolbar(
            title = "Android View Samples",
            content = container,
        )
    }
}
