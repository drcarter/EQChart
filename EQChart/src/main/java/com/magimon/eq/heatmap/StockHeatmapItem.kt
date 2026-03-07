package com.magimon.eq.heatmap

/**
 * 히트맵에 표시되는 개별 종목 데이터.
 *
 * @param symbol 티커 심볼 (예: AAPL)
 * @param name 표시 이름
 * @param sector 섹션 그룹핑에 사용할 업종명 (예: Technology)
 * @param price 최근 거래 가격
 * @param changePct 기준 기간 대비 등락률(%) (예: -1.23 은 -1.23%)
 * @param marketCap 시가총액(모든 항목에서 동일 통화 단위 가정)
 * @param sizeRatio 블록 면적에 사용할 상대 크기 비율(옵션).
 * 값이 있고 0보다 크면 `marketCap` 대신 이 값을 면적 가중치로 사용한다.
 */
data class StockHeatmapItem(
    val symbol: String,
    val name: String,
    val sector: String,
    val price: Double,
    val changePct: Double,
    val marketCap: Double,
    val sizeRatio: Double? = null,
)
