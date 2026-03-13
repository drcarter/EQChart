package com.magimon.eq.line

import android.graphics.Color

/**
 * Rendering style options for line and area charts.
 *
 * @property backgroundColor Chart background color.
 * @property gridColor Grid line color.
 * @property axisColor X/Y axis line color.
 * @property axisLabelColor Axis label text color.
 * @property legendTextColor Legend label text color.
 * @property lineStrokeWidthDp Line stroke width in dp.
 * @property pointRadiusDp Point marker radius in dp.
 * @property selectedPointRadiusDp Marker radius when selected, in dp.
 * @property selectedStrokeWidthDp Highlight stroke width for selected line segment, in dp.
 * @property areaFillAlpha Alpha applied when area fill color is derived from series color.
 * @property pointLabelTextSizeSp Point value label text size in sp.
 * @property legendTextSizeSp Legend text size in sp.
 * @property legendMarkerSizeDp Legend marker width/height in dp.
 * @property legendItemSpacingDp Horizontal gap between legend items in dp.
 * @property legendRowSpacingDp Vertical gap between legend rows in dp.
 * @property legendMarkerTextGapDp Gap between marker and text in dp.
 * @property contentPaddingDp Outer padding around drawable content in dp.
 * @property axisLabelOffsetDp Offset from axis end to axis labels in dp.
 * @property touchHitRadiusDp Touch target radius for point hit-test in dp.
 */
data class LineChartStyleOptions(
    val backgroundColor: Int = Color.WHITE,
    val gridColor: Int = Color.parseColor("#D7DFE8"),
    val axisColor: Int = Color.parseColor("#8F9CAB"),
    val axisLabelColor: Int = Color.parseColor("#5E6878"),
    val legendTextColor: Int = Color.parseColor("#273447"),
    val lineStrokeWidthDp: Float = 2.2f,
    val pointRadiusDp: Float = 3.2f,
    val selectedPointRadiusDp: Float = 5.5f,
    val selectedStrokeWidthDp: Float = 2.3f,
    val areaFillAlpha: Int = 78,
    val pointLabelTextSizeSp: Float = 11f,
    val legendTextSizeSp: Float = 12f,
    val legendMarkerSizeDp: Float = 9f,
    val legendItemSpacingDp: Float = 8f,
    val legendRowSpacingDp: Float = 6f,
    val legendMarkerTextGapDp: Float = 6f,
    val contentPaddingDp: Float = 14f,
    val axisLabelOffsetDp: Float = 15f,
    val touchHitRadiusDp: Float = 15f,
)

