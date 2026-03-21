package com.magimon.eq.stepflow

import android.graphics.Color

/**
 * Visual styling options for the step flow infographic chart.
 *
 * These values are shared by View and Compose renderers and should remain renderer-agnostic.
 *
 * @property backgroundColor Canvas background color.
 * @property contentPaddingDp Outer padding around the chart content in dp.
 * @property hubRadiusDp Radius of the central hub in dp.
 * @property hubRingThicknessDp Thickness of the colored hub ring in dp.
 * @property hubFillColor Fill color for the central hub.
 * @property hubStrokeColor Default stroke color for the hub outline.
 * @property hubStrokeWidthDp Stroke width for the hub outline in dp.
 * @property hubEyebrowTextColor Text color for the optional hub eyebrow.
 * @property hubEyebrowTextSizeSp Text size for the optional hub eyebrow in sp.
 * @property hubTitleTextColor Text color for the hub title.
 * @property hubTitleTextSizeSp Text size for the hub title in sp.
 * @property hubBodyTextColor Text color for the hub description.
 * @property hubBodyTextSizeSp Text size for the hub description in sp.
 * @property spineColor Color of the curved connecting spine and its dots.
 * @property spineWidthDp Stroke width of the curved spine in dp.
 * @property spineDotRadiusDp Radius of each step anchor dot on the spine in dp.
 * @property tailDotRadiusDp Radius of the decorative top and bottom tail dots in dp.
 * @property badgeRadiusDp Radius of the circular step badge in dp.
 * @property badgeFillColor Fill color of the circular step badge.
 * @property badgeStrokeColor Default stroke color for the step badge.
 * @property badgeStrokeWidthDp Stroke width for the step badge in dp.
 * @property badgeTextColor Text color inside the step badge.
 * @property badgeTextSizeSp Text size inside the step badge in sp.
 * @property cardWidthDp Preferred width of each step card in dp.
 * @property cardHeightDp Preferred height of each step card in dp.
 * @property cardCornerRadiusDp Corner radius of each step card in dp.
 * @property cardTextPaddingStartDp Start padding reserved for card text content in dp.
 * @property cardTextPaddingEndDp End padding reserved for card text content in dp.
 * @property cardTitleTextColor Text color of the step card title.
 * @property cardTitleTextSizeSp Text size of the step card title in sp.
 * @property cardBodyTextColor Text color of the step card description.
 * @property cardBodyTextSizeSp Text size of the step card description in sp.
 * @property connectorWidthDp Stroke width of the connector from spine to badge in dp.
 * @property connectorColor Connector stroke color.
 * @property iconCircleRadiusDp Radius of the trailing icon circle in dp.
 * @property iconCircleFillColor Fill color of the trailing icon circle.
 * @property iconTextColor Text color rendered inside the trailing icon circle.
 * @property iconTextSizeSp Text size rendered inside the trailing icon circle in sp.
 * @property selectedStrokeColor Stroke color used for the selected hub or step.
 * @property selectedStrokeWidthDp Stroke width used for the selected hub or step in dp.
 * @see StepFlowStep
 * @see StepFlowHubContent
 * @see StepFlowChartLayoutEngine
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
