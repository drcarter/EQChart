package com.magimon.eq.waveform

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class PcmRingBufferTest {

    @Test
    fun append_preservesOrderWithinCapacity() {
        val buffer = PcmRingBuffer(5)
        buffer.append(shortArrayOf(1, 2, 3))

        assertArrayEquals(shortArrayOf(1, 2, 3), buffer.snapshot())
        assertEquals(3, buffer.size())
    }

    @Test
    fun append_overflowKeepsNewestSamples() {
        val buffer = PcmRingBuffer(4)
        buffer.append(shortArrayOf(1, 2, 3, 4, 5, 6))

        assertArrayEquals(shortArrayOf(3, 4, 5, 6), buffer.snapshot())
    }

    @Test
    fun setCapacity_keepsMostRecentSamples() {
        val buffer = PcmRingBuffer(6)
        buffer.append(shortArrayOf(10, 11, 12, 13, 14, 15))
        buffer.setCapacity(3)

        assertArrayEquals(shortArrayOf(13, 14, 15), buffer.snapshot())
        assertEquals(3, buffer.capacity())
    }

    @Test
    fun setAll_truncatesToTailWhenInputIsTooLarge() {
        val buffer = PcmRingBuffer(3)
        buffer.setAll(shortArrayOf(1, 2, 3, 4, 5))

        assertArrayEquals(shortArrayOf(3, 4, 5), buffer.snapshot())
    }
}
