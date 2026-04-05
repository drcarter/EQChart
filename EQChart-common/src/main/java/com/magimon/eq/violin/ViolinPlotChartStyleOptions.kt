package com.magimon.eq.violin

import android.graphics.Color

/**
 * Visual styling for violin plot charts.
 */
data class ViolinPlotChartStyleOptions(
    val backgroundColor: Int = Color.WHITE,
    val gridColor: Int = Color.parseColor("#D7DFE8"),
    val axisColor: Int = Color.parseColor("#8F9CAB"),
    val axisLabelColor: Int = Color.parseColor("#5E6878"),
    val valueLabelTextColor: Int = Color.parseColor("#263244"),
    val defaultViolinColor: Int = Color.parseColor("#2B80FF"),
    val quartileBandColor: Int = Color.parseColor("#A5B4FC"),
    val medianLineColor: Int = Color.parseColor("#0F172A"),
    val selectedOutlineColor: Int = Color.parseColor("#0F172A"),
    val categorySpacingDp: Float = 10f,
    val violinWidthRatio: Float = 0.72f,
    val violinStrokeWidthDp: Float = 1.2f,
    val selectedPaddingDp: Float = 1.5f,
    val contentPaddingDp: Float = 14f,
)
