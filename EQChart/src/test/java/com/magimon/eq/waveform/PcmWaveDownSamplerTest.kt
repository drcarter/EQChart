package com.magimon.eq.waveform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PcmWaveDownSamplerTest {

    @Test
    fun minMaxPerPixel_returnsEmptyForInvalidInput() {
        assertTrue(PcmWaveDownSampler.minMaxPerPixel(shortArrayOf(), 10).isEmpty())
        assertTrue(PcmWaveDownSampler.minMaxPerPixel(shortArrayOf(1, 2), 0).isEmpty())
    }

    @Test
    fun minMaxPerPixel_preservesPeakInBucket() {
        val samples = shortArrayOf(
            0,
            0,
            Short.MAX_VALUE,
            0,
            0,
            Short.MIN_VALUE,
            0,
            0,
        )
        val out = PcmWaveDownSampler.minMaxPerPixel(samples, 2)

        assertEquals(4, out.size)
        assertTrue(out[1] > 0.99f)
        assertTrue(out[2] < -0.99f)
    }

    @Test
    fun minMaxPerPixel_outputSizeMatchesPixelWidth() {
        val samples = ShortArray(100) { it.toShort() }
        val out = PcmWaveDownSampler.minMaxPerPixel(samples, 37)

        assertEquals(74, out.size)
    }
}
