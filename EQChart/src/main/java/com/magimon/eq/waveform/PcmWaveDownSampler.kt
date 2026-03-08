package com.magimon.eq.waveform

/**
 * 화면 픽셀 기준 min/max 다운샘플링 유틸리티.
 *
 * 짧은 피크를 잃지 않기 위해 구간 평균이 아닌 최소/최대값을 유지한다.
 */
internal object PcmWaveDownSampler {
    private const val SHORT_NORMALIZER = 1f / Short.MAX_VALUE.toFloat()

    /**
     * 입력 샘플을 `pixelWidth` 구간으로 나누어 각 구간의 min/max 진폭을 반환한다.
     *
     * 반환 배열은 길이 `pixelWidth * 2`이며, `[min, max, min, max, ...]` 구조를 가진다.
     * 값 범위는 대략 `[-1, 1]`이다.
     *
     * @param samples 입력 PCM 샘플(16-bit signed)
     * @param pixelWidth 출력하고 싶은 가로 픽셀 수
     */
    fun minMaxPerPixel(samples: ShortArray, pixelWidth: Int): FloatArray {
        if (samples.isEmpty() || pixelWidth <= 0) return FloatArray(0)

        val out = FloatArray(pixelWidth * 2)
        for (x in 0 until pixelWidth) {
            val start = (x * samples.size) / pixelWidth
            var end = ((x + 1) * samples.size) / pixelWidth
            if (end <= start) end = (start + 1).coerceAtMost(samples.size)

            var minSample = Short.MAX_VALUE.toInt()
            var maxSample = Short.MIN_VALUE.toInt()
            for (i in start until end) {
                val value = samples[i].toInt()
                if (value < minSample) minSample = value
                if (value > maxSample) maxSample = value
            }

            out[x * 2] = (minSample * SHORT_NORMALIZER).coerceIn(-1f, 1f)
            out[x * 2 + 1] = (maxSample * SHORT_NORMALIZER).coerceIn(-1f, 1f)
        }
        return out
    }
}
