package com.magimon.eq.app.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.magimon.eq.app.R

data class ChartMenuEntry(
    val title: String,
    @param:DrawableRes
    @field:DrawableRes
    val iconRes: Int,
    val accentColor: Int,
    val destination: Class<*>,
)

fun AppCompatActivity.createChartMenuGrid(entries: List<ChartMenuEntry>): View {
    val density = resources.displayMetrics.density
    val outerPadding = (16f * density).toInt()
    val gridGap = (10f * density).toInt()
    val columns = 4
    val innerWidth = resources.displayMetrics.widthPixels - (outerPadding * 2)
    val availableWidth = innerWidth - (gridGap * (columns - 1))
    val cardWidth = availableWidth / columns
    val cardMinHeight = (112f * density).toInt()
    val iconSize = (26f * density).toInt()
    val iconContainerSize = (44f * density).toInt()
    val cardPadding = (10f * density).toInt()
    val titleTopMargin = (8f * density).toInt()
    val borderWidth = (1.5f * density).toInt().coerceAtLeast(1)
    val iconBorderWidth = (1f * density).toInt().coerceAtLeast(1)

    val grid = GridLayout(this).apply {
        columnCount = columns
        useDefaultMargins = false
        alignmentMode = GridLayout.ALIGN_BOUNDS
        setPadding(outerPadding, outerPadding, outerPadding, outerPadding)
    }

    entries.forEachIndexed { index, entry ->
        val isLastColumn = ((index + 1) % columns) == 0
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            minimumHeight = cardMinHeight
            layoutParams = GridLayout.LayoutParams().apply {
                width = cardWidth
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                setMargins(0, 0, if (isLastColumn) 0 else gridGap, gridGap)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 22f * density
                setColor(Color.parseColor("#F8FAFC"))
                setStroke(borderWidth, mix(entry.accentColor, Color.WHITE, 0.34f))
            }
            isClickable = true
            isFocusable = true
            foreground = ContextCompat.getDrawable(context, android.R.drawable.list_selector_background)
            setPadding(cardPadding, cardPadding, cardPadding, cardPadding)
            setOnClickListener {
                startActivity(Intent(this@createChartMenuGrid, entry.destination))
            }
        }

        val iconContainer = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(mix(entry.accentColor, Color.WHITE, 0.82f))
                setStroke(iconBorderWidth, mix(entry.accentColor, Color.WHITE, 0.25f))
            }
        }
        val iconParams = LinearLayout.LayoutParams(iconContainerSize, iconContainerSize)
        card.addView(iconContainer, iconParams)

        val iconView = ImageView(this).apply {
            setImageResource(entry.iconRes)
            imageTintList = ColorStateList.valueOf(entry.accentColor)
            contentDescription = entry.title
        }
        iconContainer.addView(
            iconView,
            LinearLayout.LayoutParams(iconSize, iconSize),
        )

        val titleView = TextView(this).apply {
            text = entry.title
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#111827"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
            typeface = Typeface.create(typeface, Typeface.BOLD)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            includeFontPadding = false
        }
        card.addView(
            titleView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = titleTopMargin
            },
        )

        grid.addView(card)
    }

    return ScrollView(this).apply {
        isFillViewport = true
        overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        addView(
            grid,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
    }
}

private fun mix(
    from: Int,
    to: Int,
    ratio: Float,
): Int {
    val clamped = ratio.coerceIn(0f, 1f)
    val inverse = 1f - clamped
    val red = ((Color.red(from) * inverse) + (Color.red(to) * clamped)).toInt()
    val green = ((Color.green(from) * inverse) + (Color.green(to) * clamped)).toInt()
    val blue = ((Color.blue(from) * inverse) + (Color.blue(to) * clamped)).toInt()
    return Color.rgb(red, green, blue)
}
