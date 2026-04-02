package com.magimon.eq.app.ui.android

import android.os.Bundle
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.magimon.eq.app.ui.applySampleToolbar
import com.magimon.eq.app.ui.compose.ChartSampleData
import com.magimon.eq.treemap.TreemapChartView

/**
 * Example activity demonstrating the TreemapChartView usage.
 */
class TreemapActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val treemapView = TreemapChartView(this).apply {
            setGroups(ChartSampleData.treemapGroups())
            setOnItemClickListener { item ->
                val supporting = item.supportingText?.let { " | $it" }.orEmpty()
                Toast.makeText(
                    this@TreemapActivity,
                    "${item.label}: ${item.value.toInt()}$supporting",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }

        applySampleToolbar(
            title = "Treemap",
            content = ScrollView(this).apply { addView(treemapView) },
        )
    }
}
