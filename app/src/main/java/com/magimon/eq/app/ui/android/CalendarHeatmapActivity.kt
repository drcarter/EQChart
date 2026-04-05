package com.magimon.eq.app.ui.android

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.magimon.eq.app.ui.applySampleToolbar
import com.magimon.eq.app.ui.compose.CalendarHeatmapSampleData
import com.magimon.eq.calendarheatmap.CalendarHeatmapChartView
import com.magimon.eq.calendarheatmap.formatCalendarHeatmapValue

class CalendarHeatmapActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val chart = CalendarHeatmapChartView(this).apply {
            setData(CalendarHeatmapSampleData.yearActivityData())
            setOnDayClickListener { day ->
                Toast.makeText(
                    this@CalendarHeatmapActivity,
                    "${day.month}/${day.dayOfMonth}: ${formatCalendarHeatmapValue(day.value)}",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }

        applySampleToolbar(
            title = "Calendar Heatmap",
            content = chart,
        )
    }
}
