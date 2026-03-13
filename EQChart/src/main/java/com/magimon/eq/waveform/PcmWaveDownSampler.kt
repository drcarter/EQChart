package com.magimon.eq.waveform

/**
 * Pixel-based min/max downsampling utility.
 *
 * Keeps min/max values per segment (instead of average) to preserve short peaks.
 */
internal object PcmWaveDownSampler {
    private const val SHORT_NORMALIZER = 1f / Short.MAX_VALUE.toFloat()

    /**
     * Splits input samples into `pixelWidth` segments and returns min/max amplitude per segment.
     *
     * Returned array length is `pixelWidth * 2` with shape `[min, max, min, max, ...]`.
     * Values are approximately in `[-1, 1]`.
     *
     * @param samples Input PCM samples (16-bit signed)
     * @param pixelWidth Target output width in pixels
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
