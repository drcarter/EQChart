package com.magimon.eq.app.ui.android

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.magimon.eq.pie.PieChartView
import com.magimon.eq.pie.PieDonutPresentationOptions
import com.magimon.eq.pie.PieDonutStyleOptions
import com.magimon.eq.pie.PieLabelPosition
import com.magimon.eq.pie.PieSlice
import com.magimon.eq.app.ui.applySampleToolbar

/**
 * Pie chart demo screen.
 */
class PieActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val slices = listOf(
            PieSlice("Direct", 43.0, Color.parseColor("#2B80FF"), "Direct"),
            PieSlice("Social", 18.0, Color.parseColor("#13C3A3"), "Social"),
            PieSlice("Search", 26.0, Color.parseColor("#FF9F1C"), "Search"),
            PieSlice("Referral", 13.0, Color.parseColor("#EF476F"), "Referral"),
        )

        val chartView = PieChartView(this).apply {
            setStyleOptions(
                PieDonutStyleOptions(
                    backgroundColor = Color.parseColor("#F7FAFC"),
                    sliceStrokeColor = Color.WHITE,
                    labelTextColor = Color.parseColor("#1F2A37"),
                    legendTextColor = Color.parseColor("#1F2A37"),
                ),
            )
            setPresentationOptions(
                PieDonutPresentationOptions(
                    showLegend = true,
                    showLabels = true,
                    labelPosition = PieLabelPosition.AUTO,
                    enableSelectionExpand = true,
                    selectedSliceExpandDp = 10f,
                    startAngleDeg = -90f,
                    clockwise = true,
                ),
            )
            setData(slices)
            setOnSliceClickListener { _, slice, _ ->
                Toast.makeText(this@PieActivity, "${slice.label}: ${slice.value}", Toast.LENGTH_SHORT).show()
            }
        }

        applySampleToolbar(
            title = "Pie Chart",
            content = chartView,
        )
    }
}
