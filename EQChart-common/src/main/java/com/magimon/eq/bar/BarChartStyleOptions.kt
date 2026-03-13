package com.magimon.eq.bar

import android.graphics.Color

/**
 * Visual styling for bar charts.
 *
 * @property backgroundColor Chart background color.
 * @property gridColor Grid line color.
 * @property axisColor X/Y axis color.
 * @property axisLabelColor Axis label color.
 * @property legendTextColor Legend text color.
 * @property barValueTextColor Optional bar value label color.
 * @property barSpacingDp Gap between adjacent bars inside one category.
 * @property categorySpacingDp Gap between category groups.
 * @property barCornerRadiusDp Corner radius for bar rectangles.
 * @property selectedBarPaddingDp Additional inset used when rendering selected bar outline.
 * @property contentPaddingDp Outer padding around chart content.
 * @property legendMarkerSizeDp Legend marker size.
 * @property legendItemSpacingDp Horizontal legend item spacing.
 * @property legendRowSpacingDp Vertical legend row spacing.
 * @property legendMarkerTextGapDp Gap between legend marker and text.
 */
data class BarChartStyleOptions(
    val backgroundColor: Int = Color.WHITE,
    val gridColor: Int = Color.parseColor("#D7DFE8"),
    val axisColor: Int = Color.parseColor("#8F9CAB"),
    val axisLabelColor: Int = Color.parseColor("#5E6878"),
    val legendTextColor: Int = Color.parseColor("#273447"),
    val barValueTextColor: Int = Color.parseColor("#263244"),
    val barSpacingDp: Float = 4f,
    val categorySpacingDp: Float = 12f,
    val barCornerRadiusDp: Float = 3f,
    val selectedBarPaddingDp: Float = 1.5f,
    val contentPaddingDp: Float = 14f,
    val legendMarkerSizeDp: Float = 9f,
    val legendItemSpacingDp: Float = 8f,
    val legendRowSpacingDp: Float = 6f,
    val legendMarkerTextGapDp: Float = 6f,
)

