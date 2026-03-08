package com.magimon.eq.pie

/**
 * 파이/도넛 차트의 단일 조각 데이터.
 *
 * @property label 조각 레이블(범례/레이블 표시용)
 * @property value 조각 비율 계산 값(`>0` 이고 유한한 값만 렌더링)
 * @property color 조각 채우기 색상
 * @property payload 클릭 콜백으로 전달할 원본 데이터(옵션)
 */
data class PieSlice(
    val label: String,
    val value: Double,
    val color: Int,
    val payload: Any? = null,
)
