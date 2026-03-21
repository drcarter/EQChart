package com.magimon.eq.histogram

import android.graphics.Color

/**
 * Visual styling for histogram charts.
 *
 * @property backgroundColor Chart background color.
 * @property gridColor Grid line color.
 * @property axisColor Axis line color.
 * @property axisLabelColor Axis and bin label color.
 * @property barValueTextColor Value label color.
 * @property barColor Default fill color for histogram bars.
 * @property categorySpacingDp Gap between adjacent bins.
 * @property barCornerRadiusDp Corner radius for bar rectangles.
 * @property selectedBarPaddingDp Extra inset for selected outlines.
 * @property contentPaddingDp Outer chart padding.
 */
data class HistogramChartStyleOptions(
    val backgroundColor: Int = Color.WHITE,
    val gridColor: Int = Color.parseColor("#D7DFE8"),
    val axisColor: Int = Color.parseColor("#8F9CAB"),
    val axisLabelColor: Int = Color.parseColor("#5E6878"),
    val barValueTextColor: Int = Color.parseColor("#263244"),
    val barColor: Int = Color.parseColor("#2B80FF"),
    val categorySpacingDp: Float = 6f,
    val barCornerRadiusDp: Float = 3f,
    val selectedBarPaddingDp: Float = 1.5f,
    val contentPaddingDp: Float = 14f,
)
