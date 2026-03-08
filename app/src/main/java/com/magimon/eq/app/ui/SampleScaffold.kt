package com.magimon.eq.app.ui

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat

/**
 * Wraps a sample screen content with a top toolbar.
 */
private const val TOOLBAR_ELEVATION_DP = 2f
const val SAMPLE_CHROME_COLOR_HEX = "#0F172A"

fun AppCompatActivity.attachSampleToolbar(
    title: String,
    content: View,
    showBack: Boolean = true,
): View {
    val density = resources.displayMetrics.density
    val toolbarHeight = (56 * density).toInt()

    val toolbar = Toolbar(this).apply {
        setTitle(title)
        setTitleTextColor(Color.WHITE)
        setBackgroundColor(Color.parseColor(SAMPLE_CHROME_COLOR_HEX))
        elevation = TOOLBAR_ELEVATION_DP * density

        if (showBack) {
            setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
        }
        setPadding(16, 0, 16, 0)
    }

    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
    }

    ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
        val statusBarsInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
        view.setPadding(
            view.paddingLeft,
            statusBarsInsets.top,
            view.paddingRight,
            view.paddingBottom,
        )
        insets
    }
    root.addView(
        toolbar,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            toolbarHeight,
        ),
    )

    root.addView(
        content,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f,
        ),
    )

    return root
}

fun AppCompatActivity.applySampleToolbar(
    title: String,
    content: View,
    showBack: Boolean = true,
) {
    val chromeColor = Color.parseColor(SAMPLE_CHROME_COLOR_HEX)
    window.statusBarColor = chromeColor
    WindowCompat.getInsetsController(window, window.decorView)?.isAppearanceLightStatusBars = false

    val root = attachSampleToolbar(title, content, showBack)
    setContentView(root)
    ViewCompat.requestApplyInsets(root)
}
