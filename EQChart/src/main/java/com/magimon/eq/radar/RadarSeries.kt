package com.magimon.eq.radar

/**
 * 레이더 차트 시리즈 데이터.
 *
 * @property name 범례와 상호작용 이벤트에 표시할 시리즈 이름
 * @property color 폴리곤/포인트 렌더링 색상
 * @property values 축 순서와 동일한 값 목록. 개수는 [RadarAxis] 개수와 같아야 한다.
 * @property payload 클릭 콜백으로 전달할 원본 데이터
 */
data class RadarSeries(
    val name: String,
    val color: Int,
    val values: List<Double>,
    val payload: Any? = null,
)
