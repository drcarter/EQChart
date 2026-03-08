package com.magimon.eq.app

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.magimon.eq.app.ui.android.ViewSamplesMenuActivity
import com.magimon.eq.app.ui.compose.ComposeSamplesMenuActivity

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

        val viewMenuButton = Button(this).apply {
            text = "Android View Samples"
            layoutParams = buttonParams
            setOnClickListener {
                startActivity(Intent(this@MainActivity, ViewSamplesMenuActivity::class.java))
            }
        }

        val composeMenuButton = Button(this).apply {
            text = "Compose Samples"
            layoutParams = buttonParams
            setOnClickListener {
                startActivity(Intent(this@MainActivity, ComposeSamplesMenuActivity::class.java))
            }
        }

        container.addView(viewMenuButton)
        container.addView(composeMenuButton)

        setContentView(container)
    }
}
