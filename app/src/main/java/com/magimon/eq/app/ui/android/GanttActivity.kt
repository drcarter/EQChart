package com.magimon.eq.app.ui.android

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.magimon.eq.app.ui.applySampleToolbar
import com.magimon.eq.app.ui.compose.ChartSampleData
import com.magimon.eq.gantt.GanttChartPresentationOptions
import com.magimon.eq.gantt.GanttChartStyleOptions
import com.magimon.eq.gantt.GanttChartView

/**
 * Android View demo screen for [GanttChartView].
 */
class GanttActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val chart = GanttChartView(this).apply {
            setStyleOptions(
                GanttChartStyleOptions(
                    backgroundColor = Color.parseColor("#F8FAFC"),
                    gridColor = Color.parseColor("#D5DEE8"),
                    axisColor = Color.parseColor("#7A8798"),
                    axisLabelColor = Color.parseColor("#2D3A4A"),
                    taskLabelTextColor = Color.parseColor("#1E293B"),
                    progressBarColor = Color.parseColor("#BFDBFE"),
                    dependencyLineColor = Color.parseColor("#94A3B8"),
                    todayIndicatorColor = Color.parseColor("#DC2626"),
                ),
            )
            setPresentationOptions(
                GanttChartPresentationOptions(
                    showGrid = true,
                    showAxes = true,
                    showTaskLabels = true,
                    showProgress = true,
                    showDependencies = true,
                    showTodayIndicator = true,
                    todayValue = 5.5,
                    xLabelFormatter = { tick ->
                        val week = tick.toInt().coerceAtLeast(0)
                        "W${week + 1}"
                    },
                    taskLabelFormatter = { task ->
                        if (task.isMilestone) {
                            task.title ?: task.label
                        } else {
                            "${(task.progress * 100).toInt()}%"
                        }
                    },
                ),
            )
            setTasks(ChartSampleData.ganttTasks())
            setDependencies(ChartSampleData.ganttDependencies())
            setOnTaskClickListener { _, task ->
                Toast.makeText(
                    this@GanttActivity,
                    task.title ?: task.label,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }

        applySampleToolbar(
            title = "Gantt Chart",
            content = chart,
        )
    }
}
