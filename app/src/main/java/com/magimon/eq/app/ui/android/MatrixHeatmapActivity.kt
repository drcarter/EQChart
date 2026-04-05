package com.magimon.eq.app.ui.android

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.magimon.eq.app.ui.applySampleToolbar
import com.magimon.eq.app.ui.compose.ChartSampleData
import com.magimon.eq.matrixheatmap.MatrixHeatmapChartView
import com.magimon.eq.matrixheatmap.formatMatrixHeatmapValue

/**
 * Example activity demonstrating the MatrixHeatmapChartView usage.
 */
class MatrixHeatmapActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val matrixHeatmapView = MatrixHeatmapChartView(this).apply {
            setData(ChartSampleData.matrixHeatmapData())
            setOnCellClickListener { cell ->
                Toast.makeText(
                    this@MatrixHeatmapActivity,
                    "${cell.xKey} / ${cell.yKey}: ${formatMatrixHeatmapValue(cell.value)}",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }

        applySampleToolbar(
            title = "Matrix Heatmap",
            content = matrixHeatmapView,
        )
    }
}
