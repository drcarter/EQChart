package com.magimon.eq.gantt

import android.graphics.Color

/**
 * Visual styling for Gantt charts.
 */
data class GanttChartStyleOptions(
    val backgroundColor: Int = Color.WHITE,
    val gridColor: Int = Color.parseColor("#D7DFE8"),
    val axisColor: Int = Color.parseColor("#8F9CAB"),
    val axisLabelColor: Int = Color.parseColor("#5E6878"),
    val taskLabelTextColor: Int = Color.parseColor("#263244"),
    val defaultTaskColor: Int = Color.parseColor("#2563EB"),
    val progressBarColor: Int = Color.parseColor("#93C5FD"),
    val dependencyLineColor: Int = Color.parseColor("#64748B"),
    val milestoneStrokeColor: Int = Color.parseColor("#0F172A"),
    val todayIndicatorColor: Int = Color.parseColor("#DC2626"),
    val selectedTaskStrokeColor: Int = Color.parseColor("#0F172A"),
    val rowSpacingDp: Float = 12f,
    val taskHeightRatio: Float = 0.58f,
    val taskCornerRadiusDp: Float = 4f,
    val selectedTaskPaddingDp: Float = 1.5f,
    val contentPaddingDp: Float = 14f,
    val minTaskWidthDp: Float = 8f,
    val milestoneSizeDp: Float = 10f,
    val dependencyStrokeWidthDp: Float = 1.5f,
    val dependencyArrowSizeDp: Float = 6f,
)
