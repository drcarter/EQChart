package com.magimon.eq.app.ui.android

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class ViewSamplesMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }

        val buttonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin = (8 * resources.displayMetrics.density).toInt()
            bottomMargin = (8 * resources.displayMetrics.density).toInt()
        }

        container.addView(
            menuButton("Heatmap", buttonParams) { HeatmapActivity::class.java },
        )
        container.addView(
            menuButton("Bubble", buttonParams) { BubbleActivity::class.java },
        )
        container.addView(
            menuButton("PCM Waveform", buttonParams) { WaveformFileActivity::class.java },
        )
        container.addView(
            menuButton("Radar", buttonParams) { RadarActivity::class.java },
        )
        container.addView(
            menuButton("Pie", buttonParams) { PieActivity::class.java },
        )
        container.addView(
            menuButton("Donut", buttonParams) { DonutActivity::class.java },
        )

        setContentView(container)
    }

    private fun menuButton(
        title: String,
        params: LinearLayout.LayoutParams,
        destination: () -> Class<*>,
    ): Button {
        return Button(this).apply {
            text = title
            layoutParams = params
            setOnClickListener {
                startActivity(Intent(this@ViewSamplesMenuActivity, destination()))
            }
        }
    }
}
