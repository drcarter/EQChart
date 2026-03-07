package com.magimon.eq.bubble

/**
 * [BubbleChartView]에서 사용하는 버블 데이터 모델.
 *
 * @property x [BubbleLayoutMode.SCATTER] 모드에서 사용하는 X 값
 * @property y [BubbleLayoutMode.SCATTER] 모드에서 사용하는 Y 값
 * @property size 버블 반지름 매핑에 사용하는 값
 * @property color 버블 채우기 색상
 * @property label 버블 내부 라벨(옵션). `\n`을 사용하면 여러 줄로 표시 가능
 * @property legendGroup 자동 범례 생성 시 사용할 그룹 라벨(옵션)
 * @property payload 클릭 콜백으로 전달할 임의 객체(옵션)
 */
data class BubbleDatum(
    val x: Double,
    val y: Double,
    val size: Double,
    val color: Int,
    val label: String? = null,
    val legendGroup: String? = null,
    val payload: Any? = null,
)
