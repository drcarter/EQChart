package com.magimon.eq.app.ui.compose

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.magimon.eq.app.ui.theme.EQChartTheme
import com.magimon.eq.compose.gantt.GanttChart
import com.magimon.eq.gantt.GanttChartPresentationOptions
import com.magimon.eq.gantt.GanttChartStyleOptions

/**
 * Compose demo screen for [GanttChart].
 */
class ComposeGanttActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EQChartTheme {
                ComposeGanttSampleScreen()
            }
        }
    }
}

@Composable
private fun ComposeGanttSampleScreen() {
    val context = LocalContext.current
    val tasks = remember { ChartSampleData.ganttTasks() }
    val dependencies = remember { ChartSampleData.ganttDependencies() }

    ComposeSamplePage(title = "Compose Gantt Chart") {
        GanttChart(
            tasks = tasks,
            dependencies = dependencies,
            styleOptions = GanttChartStyleOptions(
                backgroundColor = Color.parseColor("#F8FAFC"),
                gridColor = Color.parseColor("#D5DEE8"),
                axisColor = Color.parseColor("#7A8798"),
                axisLabelColor = Color.parseColor("#2D3A4A"),
                taskLabelTextColor = Color.parseColor("#1E293B"),
                progressBarColor = Color.parseColor("#BFDBFE"),
                dependencyLineColor = Color.parseColor("#94A3B8"),
                todayIndicatorColor = Color.parseColor("#DC2626"),
            ),
            presentationOptions = GanttChartPresentationOptions(
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
                    if (task.isMilestone) task.title ?: task.label else "${(task.progress * 100).toInt()}%"
                },
            ),
            onTaskClick = { _, task ->
                Toast.makeText(
                    context,
                    task.title ?: task.label,
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )
    }
}
