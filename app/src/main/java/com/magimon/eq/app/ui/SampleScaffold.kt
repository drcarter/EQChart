package com.magimon.eq.app.ui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.magimon.eq.app.R
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Wraps a sample screen content with a top toolbar.
 */
private const val TOOLBAR_ELEVATION_DP = 2f
private val Context.sampleChromeColor: Int
    get() = ContextCompat.getColor(this, R.color.eqchart_chrome)

fun AppCompatActivity.attachSampleToolbar(
    title: String,
    content: View,
    showBack: Boolean = true,
): View {
    val density = resources.displayMetrics.density
    val toolbarHeight = (56 * density).toInt()
    val horizontalPadding = (16 * density).toInt()

    val toolbar = Toolbar(this).apply {
        setTitle(title)
        setTitleTextColor(android.graphics.Color.WHITE)
        setBackgroundColor(sampleChromeColor)
        elevation = TOOLBAR_ELEVATION_DP * density

        if (showBack) {
            setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
        }
        setPadding(horizontalPadding, 0, horizontalPadding, 0)
    }

    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
    }

    root.addView(
        toolbar,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            toolbarHeight,
        ),
    )

    ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
        val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
        view.setPadding(horizontalPadding, statusBarTop, horizontalPadding, 0)

        val layoutParams = view.layoutParams as LinearLayout.LayoutParams
        val targetHeight = toolbarHeight + statusBarTop
        if (layoutParams.height != targetHeight) {
            layoutParams.height = targetHeight
            view.layoutParams = layoutParams
        }
        insets
    }

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
    val chromeColor = sampleChromeColor
    window.statusBarColor = chromeColor
    WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars =
        ColorUtils.calculateLuminance(chromeColor) > 0.5

    val root = attachSampleToolbar(title, content, showBack)
    setContentView(root)
    ViewCompat.requestApplyInsets(root)
}
