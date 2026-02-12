package com.magimon.eq.app

import android.os.Bundle
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.magimon.eq.heatmap.StockHeatmapHelper
import com.magimon.eq.heatmap.StockHeatmapView

/**
 * Example activity demonstrating the StockHeatmapView usage
 */
class HeatmapActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create the heatmap view
        val heatmapView = StockHeatmapView(this).apply {
            // Set sample data
            setData(StockHeatmapHelper.createSampleData())

            // Set click listener
            setOnItemClickListener { item ->
                val message = "${item.symbol}: ${StockHeatmapHelper.formatChange(item.change)} " +
                        "| Market Cap: ${StockHeatmapHelper.formatMarketCap(item.marketCap)}"
                Toast.makeText(this@HeatmapActivity, message, Toast.LENGTH_SHORT).show()
            }
        }

        // Wrap in ScrollView for scrolling support
        val scrollView = ScrollView(this).apply {
            addView(heatmapView)
        }

        setContentView(scrollView)
    }
}

