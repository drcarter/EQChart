package com.magimon.eq.app

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

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

        val heatmapButton = Button(this).apply {
            text = "Open Heatmap Chart"
            layoutParams = buttonParams
            setOnClickListener {
                startActivity(Intent(this@MainActivity, HeatmapActivity::class.java))
            }
        }

        val bubbleButton = Button(this).apply {
            text = "Open Bubble Chart"
            layoutParams = buttonParams
            setOnClickListener {
                startActivity(Intent(this@MainActivity, BubbleActivity::class.java))
            }
        }

        val waveformButton = Button(this).apply {
            text = "Open PCM Waveform"
            layoutParams = buttonParams
            setOnClickListener {
                startActivity(Intent(this@MainActivity, WaveformFileActivity::class.java))
            }
        }

        container.addView(heatmapButton)
        container.addView(bubbleButton)
        container.addView(waveformButton)

        setContentView(container)
    }
}
