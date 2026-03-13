package com.magimon.eq.app.ui.compose

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.magimon.eq.app.ui.applySampleToolbar

class ComposeSamplesMenuActivity : AppCompatActivity() {

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

        container.addView(menuButton("Heatmap", buttonParams) { ComposeHeatmapActivity::class.java })
        container.addView(menuButton("Bubble", buttonParams) { ComposeBubbleActivity::class.java })
        container.addView(menuButton("Line", buttonParams) { ComposeLineActivity::class.java })
        container.addView(menuButton("Area", buttonParams) { ComposeAreaActivity::class.java })
        container.addView(menuButton("Bar", buttonParams) { ComposeBarActivity::class.java })
        container.addView(menuButton("PCM Waveform", buttonParams) { ComposeWaveformActivity::class.java })
        container.addView(menuButton("Radar", buttonParams) { ComposeRadarActivity::class.java })
        container.addView(menuButton("Pie", buttonParams) { ComposePieActivity::class.java })
        container.addView(menuButton("Donut", buttonParams) { ComposeDonutActivity::class.java })

        applySampleToolbar(
            title = "Compose Samples",
            content = container,
        )
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
                startActivity(Intent(this@ComposeSamplesMenuActivity, destination()))
            }
        }
    }
}
