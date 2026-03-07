package com.magimon.eq.heatmap

/**
 * 히트맵 섹션(그룹) 모델.
 *
 * @param name 섹션 헤더에 표시할 이름
 * @param color 섹션 헤더 배경색(ARGB)
 * @param stocks 해당 섹션에 포함될 종목 목록
 */
data class StockHeatmapSection(
    val name: String,
    val color: Int,
    val stocks: List<StockHeatmapItem>,
)
