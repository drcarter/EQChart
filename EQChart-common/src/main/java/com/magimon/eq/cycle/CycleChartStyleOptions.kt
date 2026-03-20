package com.magimon.eq.cycle

import android.graphics.Color

/**
 * Visual styling options for the cycle diagram chart.
 *
 * These values affect node size, connector thickness range, label appearance, and selection
 * affordances shared by View and Compose renderers.
 *
 * @property backgroundColor Chart background color
 * @property nodeRadiusDp Node radius used for circular node rendering
 * @property nodeStrokeColor Default node outline color
 * @property nodeStrokeWidthDp Default node outline width
 * @property defaultLinkColor Fallback link color used when a link and source node do not resolve one
 * @property linkMinThicknessDp Minimum rendered link thickness
 * @property linkMaxThicknessDp Maximum rendered link thickness
 * @property linkArrowSizeDp Arrow head size used by renderers
 * @property linkInsetDp Extra inset applied so links start slightly inside the node perimeter
 * @property nodeLabelTextColor Text color used for node labels
 * @property nodeLabelTextSizeSp Text size used for node labels
 * @property linkLabelTextColor Text color used for link labels
 * @property linkLabelTextSizeSp Text size used for link labels
 * @property selectedStrokeColor Outline color used for highlighted nodes or links
 * @property selectedStrokeWidthDp Outline width used for highlighted nodes or links
 * @property contentPaddingDp Outer padding around the rendered chart
 */
data class CycleChartStyleOptions(
    val backgroundColor: Int = Color.WHITE,
    val nodeRadiusDp: Float = 24f,
    val nodeStrokeColor: Int = Color.WHITE,
    val nodeStrokeWidthDp: Float = 2f,
    val defaultLinkColor: Int = Color.parseColor("#64748B"),
    val linkMinThicknessDp: Float = 3f,
    val linkMaxThicknessDp: Float = 10f,
    val linkArrowSizeDp: Float = 8f,
    val linkInsetDp: Float = 3f,
    val nodeLabelTextColor: Int = Color.parseColor("#1F2937"),
    val nodeLabelTextSizeSp: Float = 12f,
    val linkLabelTextColor: Int = Color.parseColor("#334155"),
    val linkLabelTextSizeSp: Float = 11f,
    val selectedStrokeColor: Int = Color.parseColor("#0F172A"),
    val selectedStrokeWidthDp: Float = 2.5f,
    val contentPaddingDp: Float = 18f,
)
