package com.magimon.eq.compose

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PcmWaveformControllerTest {

    @Test
    fun appendEmptySamples_doesNotChangeVersion() {
        val controller = PcmWaveformController(
            sampleRateHz = 44_100,
            windowDurationMs = 2_000,
        )
        val versionBefore = controller.version

        controller.appendPcm16Mono(shortArrayOf())

        assertEquals(versionBefore, controller.version)
    }

    @Test
    fun clear_andMutations_incrementVersion() {
        val controller = PcmWaveformController(
            sampleRateHz = 44_100,
            windowDurationMs = 2_000,
        )

        controller.setPcm16Mono(shortArrayOf(1, 2, 3))
        val afterSetAll = controller.version
        controller.appendPcm16Mono(shortArrayOf(4, 5))
        val afterAppend = controller.version
        controller.clear()

        assertEquals(1L, afterSetAll)
        assertEquals(2L, afterAppend)
        assertEquals(3L, controller.version)
        assertTrue(controller.snapshot().isEmpty())
    }

    @Test
    fun setPcm16Mono_truncatesToCurrentCapacityTail() {
        val controller = PcmWaveformController(
            sampleRateHz = 8_000,
            windowDurationMs = 200,
        )
        val source = ShortArray(2_000) { it.toShort() }

        controller.setPcm16Mono(source)

        assertEquals(1_600, controller.snapshot().size)
        assertEquals(400, controller.snapshot().first().toInt())
        assertEquals(1_999, controller.snapshot().last().toInt())
    }

    @Test
    fun setWindowDurationAndSampleRate_recalculateCapacityWithClamping() {
        val controller = PcmWaveformController(
            sampleRateHz = 100,
            windowDurationMs = 10,
        )

        controller.setPcm16Mono(ShortArray(2_000) { it.toShort() })
        assertEquals(1_600, controller.snapshot().size)

        controller.setWindowDurationMs(400)
        controller.setPcm16Mono(ShortArray(4_000) { it.toShort() })
        assertEquals(3_200, controller.snapshot().size)

        controller.setSampleRateHz(16_000)
        controller.setPcm16Mono(ShortArray(7_000) { it.toShort() })
        assertEquals(6_400, controller.snapshot().size)
    }

    @Test
    fun appendPcm16Mono_preservesRecentOrderAcrossMultipleWrites() {
        val controller = PcmWaveformController(
            sampleRateHz = 8_000,
            windowDurationMs = 200,
        )

        controller.appendPcm16Mono(shortArrayOf(1, 2, 3))
        controller.appendPcm16Mono(shortArrayOf(4, 5))

        assertArrayEquals(shortArrayOf(1, 2, 3, 4, 5), controller.snapshot())
    }
}
