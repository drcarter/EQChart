package com.magimon.eq.gauge

import android.graphics.Color

/**
 * Visual styling options for the gauge chart.
 *
 * The gauge is composed of a background track, optional threshold ranges, a current-value arc,
 * ticks, an indicator, and several text layers. This type centralizes the colors and dimensions
 * for each of those elements so View and Compose implementations remain visually aligned.
 *
 * @property backgroundColor Chart background color
 * @property trackColor Base track color drawn behind value/range overlays
 * @property progressColor Current-value progress arc color
 * @property progressThicknessDp Thickness used for the track, threshold ranges, and progress arc
 * @property tickColor Tick mark color
 * @property tickLengthDp Tick length in dp
 * @property tickThicknessDp Tick stroke width in dp
 * @property indicatorColor Needle/indicator line color
 * @property indicatorThicknessDp Needle/indicator stroke width in dp
 * @property indicatorCenterColor Center hub fill color
 * @property indicatorCenterRadiusDp Center hub radius in dp
 * @property valueTextColor Main value text color
 * @property valueTextSizeSp Main value text size (sp)
 * @property labelTextColor Secondary center label text color
 * @property labelTextSizeSp Secondary center label text size (sp)
 * @property minMaxTextColor Min/max label text color
 * @property minMaxTextSizeSp Min/max label text size (sp)
 * @property contentPaddingDp Outer padding around the chart content
 */
data class GaugeChartStyleOptions(
    val backgroundColor: Int = Color.WHITE,
    val trackColor: Int = Color.parseColor("#E7EDF4"),
    val progressColor: Int = Color.parseColor("#2B80FF"),
    val progressThicknessDp: Float = 18f,
    val tickColor: Int = Color.parseColor("#8FA2B7"),
    val tickLengthDp: Float = 10f,
    val tickThicknessDp: Float = 2f,
    val indicatorColor: Int = Color.parseColor("#1F2A37"),
    val indicatorThicknessDp: Float = 3f,
    val indicatorCenterColor: Int = Color.parseColor("#1F2A37"),
    val indicatorCenterRadiusDp: Float = 5f,
    val valueTextColor: Int = Color.parseColor("#1F2A37"),
    val valueTextSizeSp: Float = 24f,
    val labelTextColor: Int = Color.parseColor("#6B7A8B"),
    val labelTextSizeSp: Float = 13f,
    val minMaxTextColor: Int = Color.parseColor("#6B7A8B"),
    val minMaxTextSizeSp: Float = 12f,
    val contentPaddingDp: Float = 16f,
)
