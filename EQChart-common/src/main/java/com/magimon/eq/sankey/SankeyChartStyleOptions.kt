package com.magimon.eq.sankey

import android.graphics.Color

/**
 * Visual styling options for the Sankey chart.
 *
 * These values affect node geometry, link defaults, text appearance, and selection outlines.
 *
 * @property backgroundColor Chart background color
 * @property nodeWidthDp Fixed node width in dp
 * @property nodeMinHeightDp Minimum node height used when a very small flow would otherwise disappear
 * @property nodeCornerRadiusDp Rounded corner radius for node rectangles
 * @property nodeStrokeColor Default node outline color
 * @property nodeStrokeWidthDp Default node outline width
 * @property defaultLinkColor Fallback color for links without a resolved source color
 * @property nodeLabelTextColor Text color used for node labels
 * @property nodeLabelTextSizeSp Text size used for node labels
 * @property linkValueTextColor Text color used for inline link labels/values
 * @property linkValueTextSizeSp Text size used for inline link labels/values
 * @property selectedStrokeColor Outline color used for highlighted nodes
 * @property selectedStrokeWidthDp Outline width used for highlighted nodes
 * @property contentPaddingDp Outer padding around the rendered chart
 */
data class SankeyChartStyleOptions(
    val backgroundColor: Int = Color.WHITE,
    val nodeWidthDp: Float = 18f,
    val nodeMinHeightDp: Float = 12f,
    val nodeCornerRadiusDp: Float = 6f,
    val nodeStrokeColor: Int = Color.WHITE,
    val nodeStrokeWidthDp: Float = 1f,
    val defaultLinkColor: Int = Color.parseColor("#94A3B8"),
    val nodeLabelTextColor: Int = Color.parseColor("#1F2A37"),
    val nodeLabelTextSizeSp: Float = 12f,
    val linkValueTextColor: Int = Color.parseColor("#334155"),
    val linkValueTextSizeSp: Float = 11f,
    val selectedStrokeColor: Int = Color.parseColor("#0F172A"),
    val selectedStrokeWidthDp: Float = 2f,
    val contentPaddingDp: Float = 16f,
)
