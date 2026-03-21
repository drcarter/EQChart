package com.magimon.eq.stepflow

import android.graphics.Color

/**
 * Visual styling options for the step flow infographic chart.
 *
 * These values are shared by View and Compose renderers and should remain renderer-agnostic.
 */
data class StepFlowChartStyleOptions(
    val backgroundColor: Int = Color.parseColor("#2B2B2B"),
    val contentPaddingDp: Float = 18f,
    val hubRadiusDp: Float = 92f,
    val hubRingThicknessDp: Float = 10f,
    val hubFillColor: Int = Color.WHITE,
    val hubStrokeColor: Int = Color.parseColor("#E5E7EB"),
    val hubStrokeWidthDp: Float = 1.5f,
    val hubEyebrowTextColor: Int = Color.parseColor("#374151"),
    val hubEyebrowTextSizeSp: Float = 11f,
    val hubTitleTextColor: Int = Color.parseColor("#111827"),
    val hubTitleTextSizeSp: Float = 24f,
    val hubBodyTextColor: Int = Color.parseColor("#6B7280"),
    val hubBodyTextSizeSp: Float = 10f,
    val spineColor: Int = Color.WHITE,
    val spineWidthDp: Float = 3f,
    val spineDotRadiusDp: Float = 8f,
    val tailDotRadiusDp: Float = 6f,
    val badgeRadiusDp: Float = 26f,
    val badgeFillColor: Int = Color.WHITE,
    val badgeStrokeColor: Int = Color.parseColor("#E5E7EB"),
    val badgeStrokeWidthDp: Float = 1.5f,
    val badgeTextColor: Int = Color.parseColor("#1F2937"),
    val badgeTextSizeSp: Float = 12f,
    val cardWidthDp: Float = 170f,
    val cardHeightDp: Float = 56f,
    val cardCornerRadiusDp: Float = 28f,
    val cardTextPaddingStartDp: Float = 28f,
    val cardTextPaddingEndDp: Float = 44f,
    val cardTitleTextColor: Int = Color.WHITE,
    val cardTitleTextSizeSp: Float = 14f,
    val cardBodyTextColor: Int = Color.parseColor("#F3F4F6"),
    val cardBodyTextSizeSp: Float = 9f,
    val connectorWidthDp: Float = 2f,
    val connectorColor: Int = Color.parseColor("#F8FAFC"),
    val iconCircleRadiusDp: Float = 14f,
    val iconCircleFillColor: Int = Color.WHITE,
    val iconTextColor: Int = Color.parseColor("#374151"),
    val iconTextSizeSp: Float = 13f,
    val selectedStrokeColor: Int = Color.parseColor("#FFFFFF"),
    val selectedStrokeWidthDp: Float = 2.5f,
)
