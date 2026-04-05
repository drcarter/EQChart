package com.magimon.eq.app.ui.compose

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.magimon.eq.app.ui.theme.EQChartTheme
import com.magimon.eq.calendarheatmap.formatCalendarHeatmapValue
import com.magimon.eq.compose.calendarheatmap.CalendarHeatmapChart

class ComposeCalendarHeatmapActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EQChartTheme {
                ComposeCalendarHeatmapSampleScreen()
            }
        }
    }
}

@Composable
private fun ComposeCalendarHeatmapSampleScreen() {
    val context = LocalContext.current
    val data = remember { CalendarHeatmapSampleData.yearActivityData() }

    ComposeSamplePage(title = "Compose Calendar Heatmap") {
        CalendarHeatmapChart(
            data = data,
            onDayClick = { day ->
                Toast.makeText(
                    context,
                    "${day.month}/${day.dayOfMonth}: ${formatCalendarHeatmapValue(day.value)}",
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )
    }
}
