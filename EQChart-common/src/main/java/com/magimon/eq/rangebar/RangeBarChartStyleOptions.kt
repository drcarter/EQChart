package com.magimon.eq.rangebar

import android.graphics.Color

/**
 * Visual styling for range bar and timeline charts.
 *
 * @property backgroundColor Chart background color.
 * @property gridColor Vertical grid line color.
 * @property axisColor Axis line color.
 * @property axisLabelColor Axis and row label color.
 * @property barLabelTextColor Text color for interval labels.
 * @property defaultBarColor Default bar fill color when an entry does not override its color.
 * @property selectedBarStrokeColor Stroke color used for the selected interval outline.
 * @property rowSpacingDp Gap between rows.
 * @property barHeightRatio Fraction of each row used by the rendered bar.
 * @property barCornerRadiusDp Corner radius for interval bars.
 * @property selectedBarPaddingDp Extra outline inset around the selected interval.
 * @property contentPaddingDp Outer padding around the chart.
 * @property minBarWidthDp Minimum rendered width for collapsed intervals.
 */
data class RangeBarChartStyleOptions(
    val backgroundColor: Int = Color.WHITE,
    val gridColor: Int = Color.parseColor("#D7DFE8"),
    val axisColor: Int = Color.parseColor("#8F9CAB"),
    val axisLabelColor: Int = Color.parseColor("#5E6878"),
    val barLabelTextColor: Int = Color.parseColor("#263244"),
    val defaultBarColor: Int = Color.parseColor("#2563EB"),
    val selectedBarStrokeColor: Int = Color.parseColor("#0F172A"),
    val rowSpacingDp: Float = 12f,
    val barHeightRatio: Float = 0.58f,
    val barCornerRadiusDp: Float = 4f,
    val selectedBarPaddingDp: Float = 1.5f,
    val contentPaddingDp: Float = 14f,
    val minBarWidthDp: Float = 6f,
)
