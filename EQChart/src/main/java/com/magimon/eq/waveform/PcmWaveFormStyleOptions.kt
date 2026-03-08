package com.magimon.eq.waveform

import android.graphics.Color

/**
 * Rendering style options for [PcmWaveFormView].
 *
 * @property backgroundColor Chart background color
 * @property waveColor Waveform stroke color
 * @property centerLineColor Center guideline color
 * @property strokeWidthDp Waveform stroke width (dp)
 * @property contentPaddingDp Inner padding for the waveform area (dp)
 * @property showCenterLine Whether to render the center guideline
 * @property amplitudeScale Amplitude scale factor (default 1.0, larger values increase visual height)
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
