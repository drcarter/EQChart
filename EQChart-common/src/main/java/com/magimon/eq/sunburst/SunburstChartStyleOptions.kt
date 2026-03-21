package com.magimon.eq.sunburst

import android.graphics.Color

/**
 * Styling options for Sunburst chart rendering.
 *
 * @property backgroundColor Canvas background color behind all rings.
 * @property segmentStrokeColor Default stroke color drawn around each segment.
 * @property selectedSegmentStrokeColor Stroke color used for the currently selected segment.
 * @property segmentStrokeWidthDp Stroke width around each segment in dp.
 * @property labelTextColor Text color used for segment labels.
 * @property labelTextSizeSp Text size used for segment labels in sp.
 * @property contentPaddingDp Outer padding between the chart and view bounds in dp.
 * @property centerTextColor Text color used for the primary center label.
 * @property centerTextSizeSp Text size used for the primary center label in sp.
 * @property centerSubTextColor Text color used for the secondary center label.
 * @property centerSubTextSizeSp Text size used for the secondary center label in sp.
 * @property segmentColors Palette used when a [SunburstNode] does not specify its own [SunburstNode.color].
 * @see SunburstNode
 * @see SunburstChartPresentationOptions
 * @see SunburstChartLayoutEngine
 */
data class SunburstChartStyleOptions(
    val backgroundColor: Int = Color.WHITE,
    val segmentStrokeColor: Int = Color.WHITE,
    val selectedSegmentStrokeColor: Int = Color.parseColor("#1F2937"),
    val segmentStrokeWidthDp: Float = 1.4f,
    val labelTextColor: Int = Color.parseColor("#243040"),
    val labelTextSizeSp: Float = 11.5f,
    val contentPaddingDp: Float = 12f,
    val centerTextColor: Int = Color.parseColor("#1F2A37"),
    val centerTextSizeSp: Float = 17f,
    val centerSubTextColor: Int = Color.parseColor("#6B7A8B"),
    val centerSubTextSizeSp: Float = 12f,
    val segmentColors: List<Int> = listOf(
        Color.parseColor("#2563EB"),
        Color.parseColor("#14B8A6"),
        Color.parseColor("#F97316"),
        Color.parseColor("#DC2626"),
        Color.parseColor("#7C3AED"),
        Color.parseColor("#06B6D4"),
    ),
)
