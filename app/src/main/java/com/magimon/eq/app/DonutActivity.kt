package com.magimon.eq.app

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.magimon.eq.pie.DonutChartView
import com.magimon.eq.pie.PieDonutPresentationOptions
import com.magimon.eq.pie.PieDonutStyleOptions
import com.magimon.eq.pie.PieLabelPosition
import com.magimon.eq.pie.PieSlice
import kotlin.math.roundToInt

/**
 * Donut chart demo screen.
 */
class DonutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val slices = listOf(
            PieSlice("Engineering", 38.0, Color.parseColor("#2A9D8F"), "Engineering"),
            PieSlice("Marketing", 22.0, Color.parseColor("#3A86FF"), "Marketing"),
            PieSlice("Sales", 27.0, Color.parseColor("#FFBE0B"), "Sales"),
            PieSlice("Ops", 13.0, Color.parseColor("#FB5607"), "Ops"),
        )

        val total = slices.sumOf { it.value }.roundToInt()

        val chartView = DonutChartView(this).apply {
            setDonutInnerRadiusRatio(0.58f)
            setStyleOptions(
                PieDonutStyleOptions(
                    backgroundColor = Color.parseColor("#FFFFFF"),
                    sliceStrokeColor = Color.WHITE,
                    labelTextColor = Color.parseColor("#1F2A37"),
                    centerTextColor = Color.parseColor("#1A2433"),
                    centerSubTextColor = Color.parseColor("#6F7E8E"),
                ),
            )
            setPresentationOptions(
                PieDonutPresentationOptions(
                    showLegend = true,
                    showLabels = true,
                    labelPosition = PieLabelPosition.AUTO,
                    enableSelectionExpand = true,
                    selectedSliceExpandDp = 10f,
                    centerText = "Total",
                    centerSubText = "$total",
                    startAngleDeg = -90f,
                    clockwise = true,
                ),
            )
            setData(slices)
            setOnSliceClickListener { _, slice, _ ->
                Toast.makeText(this@DonutActivity, "${slice.label}: ${slice.value}", Toast.LENGTH_SHORT).show()
            }
        }

        setContentView(chartView)
    }
}
