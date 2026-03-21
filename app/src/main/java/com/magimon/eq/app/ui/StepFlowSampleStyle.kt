package com.magimon.eq.app.ui

import android.graphics.Color
import com.magimon.eq.stepflow.StepFlowChartStyleOptions

fun stepFlowSampleStyleOptions(screenWidthDp: Int): StepFlowChartStyleOptions {
    val compact = screenWidthDp in 1..400
    val hubRadius = if (compact) 70f else 92f
    val badgeRadius = if (compact) 22f else 26f
    val cardWidth = if (compact) 136f else 170f
    val cardHeight = if (compact) 52f else 56f

    return StepFlowChartStyleOptions(
        backgroundColor = Color.parseColor("#2B2B2B"),
        contentPaddingDp = if (compact) 14f else 18f,
        hubRadiusDp = hubRadius,
        hubRingThicknessDp = if (compact) 8f else 10f,
        hubFillColor = Color.parseColor("#F8FAFC"),
        hubStrokeColor = Color.parseColor("#D1D5DB"),
        hubEyebrowTextSizeSp = if (compact) 9f else 11f,
        hubTitleTextSizeSp = if (compact) 18f else 24f,
        hubBodyTextSizeSp = if (compact) 8.5f else 10f,
        connectorColor = Color.parseColor("#F8FAFC"),
        badgeRadiusDp = badgeRadius,
        badgeTextSizeSp = if (compact) 10f else 12f,
        cardWidthDp = cardWidth,
        cardHeightDp = cardHeight,
        cardCornerRadiusDp = if (compact) 24f else 28f,
        cardTextPaddingStartDp = if (compact) 22f else 28f,
        cardTextPaddingEndDp = if (compact) 38f else 44f,
        cardTitleTextSizeSp = if (compact) 12f else 14f,
        cardBodyTextSizeSp = if (compact) 8f else 9f,
        iconCircleRadiusDp = if (compact) 12f else 14f,
        iconTextSizeSp = if (compact) 11f else 13f,
        selectedStrokeColor = Color.WHITE,
    )
}
