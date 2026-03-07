package com.magimon.eq.bubble

import android.graphics.Color

/**
 * 축/스케일과 분리된 표현 옵션.
 *
 * [BubbleAxisOptions]가 축/그리드에 집중하도록 유지하고,
 * 이 타입은 제목/범례 렌더링만 담당한다.
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
