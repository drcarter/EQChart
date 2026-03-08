package com.magimon.eq.pie

import android.graphics.Color

/**
 * 파이/도넛 차트 렌더링 스타일 옵션.
 *
 * @property backgroundColor 차트 배경색
 * @property sliceStrokeColor 조각 경계선 색상
 * @property sliceStrokeWidthDp 조각 경계선 두께(dp)
 * @property labelTextColor 조각 레이블 텍스트 색상
 * @property labelTextSizeSp 조각 레이블 텍스트 크기(sp)
 * @property labelLineColor 바깥 레이블 리더라인 색상
 * @property legendTextColor 범례 텍스트 색상
 * @property legendTextSizeSp 범례 텍스트 크기(sp)
 * @property legendMarkerSizeDp 범례 마커 크기(dp)
 * @property legendItemSpacingDp 범례 항목 간 가로 간격(dp)
 * @property legendRowSpacingDp 범례 줄 간 세로 간격(dp)
 * @property legendMarkerTextGapDp 범례 마커와 텍스트 간 간격(dp)
 * @property contentPaddingDp 차트 콘텐츠 패딩(dp)
 * @property centerTextColor 도넛 중앙 메인 텍스트 색상
 * @property centerTextSizeSp 도넛 중앙 메인 텍스트 크기(sp)
 * @property centerSubTextColor 도넛 중앙 보조 텍스트 색상
 * @property centerSubTextSizeSp 도넛 중앙 보조 텍스트 크기(sp)
 */
data class PieDonutStyleOptions(
    val backgroundColor: Int = Color.WHITE,
    val sliceStrokeColor: Int = Color.WHITE,
    val sliceStrokeWidthDp: Float = 1.6f,
    val labelTextColor: Int = Color.parseColor("#243040"),
    val labelTextSizeSp: Float = 12f,
    val labelLineColor: Int = Color.parseColor("#8FA2B7"),
    val legendTextColor: Int = Color.parseColor("#243040"),
    val legendTextSizeSp: Float = 12f,
    val legendMarkerSizeDp: Float = 10f,
    val legendItemSpacingDp: Float = 8f,
    val legendRowSpacingDp: Float = 8f,
    val legendMarkerTextGapDp: Float = 6f,
    val contentPaddingDp: Float = 12f,
    val centerTextColor: Int = Color.parseColor("#1F2A37"),
    val centerTextSizeSp: Float = 18f,
    val centerSubTextColor: Int = Color.parseColor("#6B7A8B"),
    val centerSubTextSizeSp: Float = 12f,
)
