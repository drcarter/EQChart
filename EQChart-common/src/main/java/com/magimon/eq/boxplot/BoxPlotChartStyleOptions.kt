package com.magimon.eq.boxplot

import android.graphics.Color

/**
 * Visual styling for box plot charts.
 */
data class BoxPlotChartStyleOptions(
    val backgroundColor: Int = Color.WHITE,
    val gridColor: Int = Color.parseColor("#D7DFE8"),
    val axisColor: Int = Color.parseColor("#8F9CAB"),
    val axisLabelColor: Int = Color.parseColor("#5E6878"),
    val valueLabelTextColor: Int = Color.parseColor("#263244"),
    val defaultBoxColor: Int = Color.parseColor("#2B80FF"),
    val medianLineColor: Int = Color.parseColor("#0F172A"),
    val outlierColor: Int = Color.parseColor("#EF4444"),
    val selectedOutlineColor: Int = Color.parseColor("#0F172A"),
    val categorySpacingDp: Float = 10f,
    val boxWidthRatio: Float = 0.52f,
    val boxCornerRadiusDp: Float = 3f,
    val whiskerStrokeWidthDp: Float = 1.8f,
    val outlierRadiusDp: Float = 3f,
    val selectedPaddingDp: Float = 1.5f,
    val contentPaddingDp: Float = 14f,
)
