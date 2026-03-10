package com.magimon.eq.common

import com.magimon.eq.waveform.PcmWaveFormStyleOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WaveformContractsTest {

    @Test
    fun pcmWaveFormStyleOptions_defaultsAndCustomValues_areAccessible() {
        val defaults = PcmWaveFormStyleOptions()
        val custom = PcmWaveFormStyleOptions(
            backgroundColor = 0xFF0E1620.toInt(),
            waveColor = 0xFF62D5FF.toInt(),
            centerLineColor = 0xFF2D3A46.toInt(),
            strokeWidthDp = 2f,
            contentPaddingDp = 10f,
            showCenterLine = false,
            amplitudeScale = 1.5f,
        )
        val defaultBackgroundColor = defaults.backgroundColor
        val defaultWaveColor = defaults.waveColor
        val defaultCenterLineColor = defaults.centerLineColor

        assertEquals(defaultBackgroundColor, defaults.backgroundColor)
        assertEquals(defaultWaveColor, defaults.waveColor)
        assertEquals(defaultCenterLineColor, defaults.centerLineColor)
        assertEquals(1.5f, defaults.strokeWidthDp, 0.0f)
        assertEquals(8f, defaults.contentPaddingDp, 0.0f)
        assertTrue(defaults.showCenterLine)
        assertEquals(1f, defaults.amplitudeScale, 0.0f)

        assertEquals(0xFF0E1620.toInt(), custom.backgroundColor)
        assertEquals(0xFF62D5FF.toInt(), custom.waveColor)
        assertEquals(0xFF2D3A46.toInt(), custom.centerLineColor)
        assertEquals(2f, custom.strokeWidthDp, 0.0f)
        assertEquals(10f, custom.contentPaddingDp, 0.0f)
        assertFalse(custom.showCenterLine)
        assertEquals(1.5f, custom.amplitudeScale, 0.0f)
    }
}
