package com.magimon.eq.app.ui.android

import android.os.Bundle
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.magimon.eq.heatmap.StockHeatmapHelper
import com.magimon.eq.heatmap.StockHeatmapView
import com.magimon.eq.app.ui.applySampleToolbar

/**
 * Example activity demonstrating the StockHeatmapView usage
 */
class HeatmapActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val heatmapView = StockHeatmapView(this).apply {
            setSections(StockHeatmapHelper.createSampleSections())

            setOnItemClickListener { item ->
                val ratioPart = item.sizeRatio?.let { " | Ratio: ${String.format("%.1f", it)}" } ?: ""
                val message = "${item.symbol}: ${StockHeatmapHelper.formatChange(item.changePct)}" +
                    " | Market Cap: ${StockHeatmapHelper.formatMarketCap(item.marketCap)}" +
                    ratioPart
                Toast.makeText(this@HeatmapActivity, message, Toast.LENGTH_SHORT).show()
            }
        }

        val scrollView = ScrollView(this).apply {
            addView(heatmapView)
        }

        applySampleToolbar(
            title = "Stock Heatmap",
            content = scrollView,
        )
    }
}
