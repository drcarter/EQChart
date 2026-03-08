package com.magimon.eq.pie

import android.graphics.Color

/**
 * Pie/donut rendering style options.
 *
 * @property backgroundColor Chart background color
 * @property sliceStrokeColor Slice border color
 * @property sliceStrokeWidthDp Slice border thickness (dp)
 * @property labelTextColor Slice label text color
 * @property labelTextSizeSp Slice label text size (sp)
 * @property labelLineColor Leader-line color for outside labels
 * @property legendTextColor Legend text color
 * @property legendTextSizeSp Legend text size (sp)
 * @property legendMarkerSizeDp Legend marker size (dp)
 * @property legendItemSpacingDp Horizontal spacing between legend items (dp)
 * @property legendRowSpacingDp Vertical spacing between legend rows (dp)
 * @property legendMarkerTextGapDp Gap between legend marker and text (dp)
 * @property contentPaddingDp Content padding around chart area (dp)
 * @property centerTextColor Donut center primary text color
 * @property centerTextSizeSp Donut center primary text size (sp)
 * @property centerSubTextColor Donut center secondary text color
 * @property centerSubTextSizeSp Donut center secondary text size (sp)
 */
data class PieDonutStyleOptions(
    val backgroundColor: Int = Color.WHITE,
    val sliceStrokeColor: Int = Color.WHITE,
    val sliceStrokeWidthDp: Float = 1.6f,
    val labelTextColor: Int = Color.parseColor("#243040"),
    val labelTextSizeSp: Float = 12f,
    val labelLineColor: Int = Color.parseColor("#8FA2B7"),
    val legendTextColor: Int = Color.parseColor("#243040"),
    val legendTextSizeSp: Float = 12f,
    val legendMarkerSizeDp: Float = 10f,
    val legendItemSpacingDp: Float = 8f,
    val legendRowSpacingDp: Float = 8f,
    val legendMarkerTextGapDp: Float = 6f,
    val contentPaddingDp: Float = 12f,
    val centerTextColor: Int = Color.parseColor("#1F2A37"),
    val centerTextSizeSp: Float = 18f,
    val centerSubTextColor: Int = Color.parseColor("#6B7A8B"),
    val centerSubTextSizeSp: Float = 12f,
)
