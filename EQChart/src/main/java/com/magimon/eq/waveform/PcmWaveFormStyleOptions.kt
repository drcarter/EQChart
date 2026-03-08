package com.magimon.eq.waveform

import android.graphics.Color

/**
 * [PcmWaveFormView]의 렌더링 스타일 옵션.
 *
 * @property backgroundColor 차트 배경색
 * @property waveColor 파형 선 색상
 * @property centerLineColor 중앙 기준선 색상
 * @property strokeWidthDp 파형 선 두께(dp)
 * @property contentPaddingDp 파형 영역 안쪽 여백(dp)
 * @property showCenterLine 중앙 기준선 표시 여부
 * @property amplitudeScale 진폭 배율(1.0 기본, 클수록 파형 높이 증가)
 */
data class PcmWaveFormStyleOptions(
    val backgroundColor: Int = Color.parseColor("#101418"),
    val waveColor: Int = Color.parseColor("#6BD2FF"),
    val centerLineColor: Int = Color.parseColor("#2E3A46"),
    val strokeWidthDp: Float = 1.5f,
    val contentPaddingDp: Float = 8f,
    val showCenterLine: Boolean = true,
    val amplitudeScale: Float = 1f,
)
