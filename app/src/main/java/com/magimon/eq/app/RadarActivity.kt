package com.magimon.eq.app

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.magimon.eq.radar.RadarAxis
import com.magimon.eq.radar.RadarChartPresentationOptions
import com.magimon.eq.radar.RadarChartStyleOptions
import com.magimon.eq.radar.RadarChartView
import com.magimon.eq.radar.RadarSeries
import kotlin.math.roundToInt

/**
 * GIF 스타일 레이더 차트 데모 화면.
 */
class RadarActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val axes = listOf(
            RadarAxis("sweet"),
            RadarAxis("price"),
            RadarAxis("color"),
            RadarAxis("fresh"),
            RadarAxis("good"),
        )

        val series = listOf(
            RadarSeries(
                name = "Apple",
                color = Color.parseColor("#B899FF"),
                values = listOf(48.0, 80.0, 84.0, 34.0, 40.0),
                payload = "Apple",
            ),
            RadarSeries(
                name = "Banana",
                color = Color.parseColor("#6F8695"),
                values = listOf(30.0, 40.0, 90.0, 82.0, 62.0),
                payload = "Banana",
            ),
        )

        val radarView = RadarChartView(this).apply {
            setStyleOptions(
                RadarChartStyleOptions(
                    backgroundColor = Color.parseColor("#FFFFFF"),
                    gridColor = Color.parseColor("#CFD9E6"),
                    axisColor = Color.parseColor("#C7D2DF"),
                    axisLabelColor = Color.parseColor("#646D7B"),
                    legendTextColor = Color.parseColor("#1E2530"),
                    fillAlpha = 82,
                ),
            )
            setPresentationOptions(
                RadarChartPresentationOptions(
                    showLegend = true,
                    showAxisLabels = true,
                    gridLevels = 5,
                    animateOnDataChange = true,
                    enterAnimationDurationMs = 760L,
                ),
            )
            setValueMax(100.0)
            setAxes(axes)
            setSeries(series)
            setOnPointClickListener { seriesIndex, axisIndex, value, _ ->
                val message = "${series[seriesIndex].name} | ${axes[axisIndex].label}: ${value.roundToInt()}"
                Toast.makeText(this@RadarActivity, message, Toast.LENGTH_SHORT).show()
            }
        }

        setContentView(radarView)
    }
}
