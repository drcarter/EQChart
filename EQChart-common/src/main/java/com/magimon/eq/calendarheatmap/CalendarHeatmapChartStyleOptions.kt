package com.magimon.eq.calendarheatmap

import android.graphics.Color

/**
 * Visual styling for calendar heatmap charts.
 */
data class CalendarHeatmapChartStyleOptions(
    val backgroundColor: Int = Color.WHITE,
    val labelTextColor: Int = Color.parseColor("#5E6878"),
    val cellBorderColor: Int = Color.WHITE,
    val emptyCellColor: Int = Color.parseColor("#EBF1F5"),
    val level1Color: Int = Color.parseColor("#CDEDDC"),
    val level2Color: Int = Color.parseColor("#88D6A6"),
    val level3Color: Int = Color.parseColor("#35A86D"),
    val level4Color: Int = Color.parseColor("#156B42"),
    val contentPaddingDp: Float = 14f,
    val labelGapDp: Float = 8f,
    val cellGapDp: Float = 3f,
    val cellCornerRadiusDp: Float = 4f,
)
