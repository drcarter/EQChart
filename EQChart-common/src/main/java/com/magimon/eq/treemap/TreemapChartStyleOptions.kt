package com.magimon.eq.treemap

import android.graphics.Color

/**
 * Visual styling for Treemap charts.
 *
 * @property backgroundColor Chart background color.
 * @property tileBorderColor Tile outline color.
 * @property headerTextColor Group header text color.
 * @property itemLabelTextColor Primary tile label color.
 * @property itemSupportingTextColor Secondary tile label color.
 * @property fallbackItemColors Palette used when neither item nor group defines a color.
 * @property contentPaddingDp Outer chart padding.
 * @property groupGapDp Gap between group regions.
 * @property tileGapDp Gap between item tiles.
 * @property headerHeightDp Group header height.
 * @property headerFillAlpha Alpha applied to the resolved group color when drawing headers.
 */
data class TreemapChartStyleOptions(
    val backgroundColor: Int = Color.parseColor("#111316"),
    val tileBorderColor: Int = 0x55000000,
    val headerTextColor: Int = 0xFFE9EEF5.toInt(),
    val itemLabelTextColor: Int = Color.WHITE,
    val itemSupportingTextColor: Int = Color.WHITE,
    val fallbackItemColors: List<Int> = listOf(
        Color.parseColor("#2563EB"),
        Color.parseColor("#14B8A6"),
        Color.parseColor("#F59E0B"),
        Color.parseColor("#8B5CF6"),
        Color.parseColor("#EF4444"),
        Color.parseColor("#0EA5E9"),
        Color.parseColor("#84CC16"),
        Color.parseColor("#FB7185"),
    ),
    val contentPaddingDp: Float = 6f,
    val groupGapDp: Float = 3f,
    val tileGapDp: Float = 2f,
    val headerHeightDp: Float = 20f,
    val headerFillAlpha: Int = 170,
)
