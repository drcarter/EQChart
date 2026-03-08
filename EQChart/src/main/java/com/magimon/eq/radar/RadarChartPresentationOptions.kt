package com.magimon.eq.radar

/**
 * 레이더 차트 표시/애니메이션 옵션.
 *
 * @property showLegend 범례 표시 여부
 * @property showAxisLabels 축 라벨 표시 여부
 * @property showPoints 포인트 점 표시 여부
 * @property gridLevels 동심 다각형 레벨 수
 * @property animateOnDataChange 데이터 변경 시 등장 애니메이션 자동 재생 여부
 * @property enterAnimationDurationMs 등장 애니메이션 길이(ms)
 * @property enterAnimationDelayMs 등장 애니메이션 시작 지연(ms)
 * @property startAngleDeg 첫 번째 축 시작 각도(도). 기본 -90도는 12시 방향 시작
 * @property legendTextSizeSp 범례 텍스트 크기(sp)
 * @property axisLabelTextSizeSp 축 라벨 텍스트 크기(sp)
 * @property legendLeftMarginDp 범례 좌측 여백(dp)
 * @property legendTopMarginDp 범례 상단 여백(dp)
 */
data class RadarChartPresentationOptions(
    val showLegend: Boolean = true,
    val showAxisLabels: Boolean = true,
    val showPoints: Boolean = true,
    val gridLevels: Int = 5,
    val animateOnDataChange: Boolean = true,
    val enterAnimationDurationMs: Long = 700L,
    val enterAnimationDelayMs: Long = 40L,
    val startAngleDeg: Float = -90f,
    val legendTextSizeSp: Float = 13f,
    val axisLabelTextSizeSp: Float = 15f,
    val legendLeftMarginDp: Float = 4f,
    val legendTopMarginDp: Float = 4f,
)
