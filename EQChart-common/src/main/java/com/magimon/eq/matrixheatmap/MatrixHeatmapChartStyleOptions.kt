package com.magimon.eq.matrixheatmap

import android.graphics.Color

/**
 * Visual styling for matrix heatmap charts.
 *
 * @property backgroundColor Chart background color.
 * @property axisLabelColor X/Y axis label color.
 * @property axisLineColor Outer chart frame color.
 * @property cellBorderColor Cell border color.
 * @property cellTextColor Cell text color.
 * @property emptyCellColor Fill color used for missing cells within a renderable grid.
 * @property minColor Lower-bound fill color.
 * @property neutralColor Neutral midpoint fill color.
 * @property maxColor Upper-bound fill color.
 * @property contentPaddingDp Outer chart padding.
 * @property axisLabelGapDp Gap between axis labels and the heatmap grid.
 * @property cellGapDp Gap between adjacent cells.
 * @property cellCornerRadiusDp Corner radius applied to each cell.
 */
data class MatrixHeatmapChartStyleOptions(
    val backgroundColor: Int = Color.WHITE,
    val axisLabelColor: Int = Color.parseColor("#5E6878"),
    val axisLineColor: Int = Color.parseColor("#C7D2DE"),
    val cellBorderColor: Int = Color.WHITE,
    val cellTextColor: Int = Color.parseColor("#102032"),
    val emptyCellColor: Int = Color.parseColor("#EEF3F8"),
    val minColor: Int = Color.parseColor("#D65454"),
    val neutralColor: Int = Color.parseColor("#F6F8FB"),
    val maxColor: Int = Color.parseColor("#1FA971"),
    val contentPaddingDp: Float = 14f,
    val axisLabelGapDp: Float = 8f,
    val cellGapDp: Float = 4f,
    val cellCornerRadiusDp: Float = 6f,
)
