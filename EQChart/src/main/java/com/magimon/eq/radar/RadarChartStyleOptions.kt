package com.magimon.eq.radar

import android.graphics.Color

/**
 * 레이더 차트 렌더링 스타일 옵션.
 *
 * @property backgroundColor 차트 배경색
 * @property gridColor 동심 다각형 그리드 선 색상
 * @property axisColor 중심-축 라인 색상
 * @property axisLabelColor 축 라벨 텍스트 색상
 * @property legendTextColor 범례 텍스트 색상
 * @property polygonStrokeWidthDp 시리즈 외곽선 두께(dp)
 * @property gridStrokeWidthDp 그리드 선 두께(dp)
 * @property axisStrokeWidthDp 축 선 두께(dp)
 * @property axisDashLengthDp 축 점선의 dash 길이(dp)
 * @property axisDashGapDp 축 점선의 gap 길이(dp)
 * @property fillAlpha 시리즈 면 채우기 alpha(0..255)
 * @property pointRadiusDp 포인트 원 반지름(dp)
 * @property pointCoreRadiusDp 포인트 내부 코어 반지름(dp)
 * @property pointGlowRadiusDp 포인트 glow 반지름(dp)
 * @property pointGlowAlpha 포인트 glow alpha(0..255)
 * @property selectedPointRadiusDp 선택 포인트 하이라이트 반지름(dp)
 * @property selectedStrokeWidthDp 선택 포인트 하이라이트 선 두께(dp)
 * @property contentPaddingDp 차트 영역 외곽 패딩(dp)
 * @property axisLabelOffsetDp 축 끝점에서 라벨까지 거리(dp)
 * @property legendMarkerSizeDp 범례 색상 마커 크기(dp)
 * @property legendItemGapDp 범례 항목 간 가로 간격(dp)
 * @property legendRowGapDp 범례 줄 간 세로 간격(dp)
 * @property legendBottomGapDp 범례와 차트 사이 하단 간격(dp)
 * @property touchHitRadiusDp 포인트 클릭 판정 반경(dp)
 */
data class RadarChartStyleOptions(
    val backgroundColor: Int = Color.WHITE,
    val gridColor: Int = Color.parseColor("#D7DFE8"),
    val axisColor: Int = Color.parseColor("#C8D1DC"),
    val axisLabelColor: Int = Color.parseColor("#5D6675"),
    val legendTextColor: Int = Color.parseColor("#1F2430"),
    val polygonStrokeWidthDp: Float = 1.8f,
    val gridStrokeWidthDp: Float = 1f,
    val axisStrokeWidthDp: Float = 1f,
    val axisDashLengthDp: Float = 4f,
    val axisDashGapDp: Float = 4f,
    val fillAlpha: Int = 78,
    val pointRadiusDp: Float = 4.6f,
    val pointCoreRadiusDp: Float = 2f,
    val pointGlowRadiusDp: Float = 12f,
    val pointGlowAlpha: Int = 72,
    val selectedPointRadiusDp: Float = 7.5f,
    val selectedStrokeWidthDp: Float = 2.4f,
    val contentPaddingDp: Float = 12f,
    val axisLabelOffsetDp: Float = 18f,
    val legendMarkerSizeDp: Float = 10f,
    val legendItemGapDp: Float = 10f,
    val legendRowGapDp: Float = 8f,
    val legendBottomGapDp: Float = 10f,
    val touchHitRadiusDp: Float = 18f,
)
