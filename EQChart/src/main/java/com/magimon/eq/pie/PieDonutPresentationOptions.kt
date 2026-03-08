package com.magimon.eq.pie

/**
 * 파이/도넛 차트 표시/애니메이션 옵션.
 *
 * @property showLegend 범례 표시 여부
 * @property showLabels 조각 레이블 표시 여부
 * @property labelPosition 레이블 배치 정책(inside/outside/auto)
 * @property enableSelectionExpand 조각 선택 시 바깥으로 이동(explode) 효과 활성화 여부
 * @property selectedSliceExpandDp 선택 조각 이동 거리(dp)
 * @property selectedSliceExpandAnimMs 선택/해제 전환 애니메이션 길이(ms)
 * @property startAngleDeg 첫 번째 조각 시작 각도(도). 기본 `-90`은 12시 방향 시작
 * @property clockwise 조각 진행 방향. `true`면 시계방향
 * @property animateOnDataChange 데이터 변경 시 진입 애니메이션 자동 재생 여부
 * @property enterAnimationDurationMs 진입 애니메이션 길이(ms)
 * @property enterAnimationDelayMs 진입 애니메이션 시작 지연(ms)
 * @property legendTopMarginDp 차트와 범례 사이 상단 여백(dp)
 * @property legendBottomMarginDp 범례 하단 여백(dp)
 * @property legendLeftMarginDp 범례 좌측 여백(dp)
 * @property centerText 도넛 중앙 메인 텍스트
 * @property centerSubText 도넛 중앙 보조 텍스트
 * @property emptyText 유효 조각이 없을 때 표시할 안내 문구
 */
data class PieDonutPresentationOptions(
    val showLegend: Boolean = true,
    val showLabels: Boolean = true,
    val labelPosition: PieLabelPosition = PieLabelPosition.AUTO,
    val enableSelectionExpand: Boolean = false,
    val selectedSliceExpandDp: Float = 8f,
    val selectedSliceExpandAnimMs: Long = 140L,
    val startAngleDeg: Float = -90f,
    val clockwise: Boolean = true,
    val animateOnDataChange: Boolean = true,
    val enterAnimationDurationMs: Long = 650L,
    val enterAnimationDelayMs: Long = 0L,
    val legendTopMarginDp: Float = 8f,
    val legendBottomMarginDp: Float = 8f,
    val legendLeftMarginDp: Float = 4f,
    val centerText: String? = null,
    val centerSubText: String? = null,
    val emptyText: String? = "No data",
)
