package com.magimon.eq.bubble

import android.graphics.Color

/**
 * Presentation options separated from axis/scale behavior.
 *
 * [BubbleAxisOptions] stays focused on axes/grid, while this type controls
 * title and legend rendering.
 */
data class BubblePresentationOptions(
    val title: String? = null,
    val showLegend: Boolean = false,
    val legendMode: BubbleLegendMode = BubbleLegendMode.AUTO,
    val titleColor: Int = Color.parseColor("#2A2A2A"),
    val titleTextSizeSp: Float = 20f,
    val titleBottomSpacingDp: Float = 8f,
    val legendTextColor: Int = Color.parseColor("#2A2A2A"),
    val legendTextSizeSp: Float = 12f,
    val legendMarkerSizeDp: Float = 10f,
    val legendItemSpacingDp: Float = 6f,
    val legendSectionTopPaddingDp: Float = 8f,
    val legendBottomMarginDp: Float = 8f,
    val legendLeftMarginDp: Float = 8f,
    val legendMarkerTextGapDp: Float = 6f,
)
