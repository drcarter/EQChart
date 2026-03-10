package com.magimon.eq.waveform

import android.graphics.Color
import com.magimon.eq.testutil.layoutAndDraw
import com.magimon.eq.testutil.readPrivate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PcmWaveFormViewTest {

    @Test
    fun waveformView_appliesStyleAndRendersBufferedSamples() {
        val context = RuntimeEnvironment.getApplication()
        val view = PcmWaveFormView(context)

        view.setStyleOptions(
            PcmWaveFormStyleOptions(
                backgroundColor = Color.BLACK,
                waveColor = Color.GREEN,
                centerLineColor = Color.RED,
                showCenterLine = true,
                amplitudeScale = 1.5f,
            ),
        )
        view.setWindowDurationMs(100)
        view.setWindowDurationMs(200)
        view.setSampleRateHz(500_000)
        view.setSampleRateHz(192_000)
        view.setPcm16Mono(shortArrayOf(-32767, 0, 32767, -12000, 12000))
        view.appendPcm16Mono(shortArrayOf(-16000, 16000, 0))

        layoutAndDraw(view, width = 480, height = 220)

        val ringBuffer = view.readPrivate<PcmRingBuffer>("ringBuffer")
        val snapshot = ringBuffer.snapshot()

        assertTrue(snapshot.isNotEmpty())
    }

    @Test
    fun waveformView_clearRemovesSamples() {
        val context = RuntimeEnvironment.getApplication()
        val view = PcmWaveFormView(context)

        view.setStyleOptions(PcmWaveFormStyleOptions(showCenterLine = false, amplitudeScale = 0.2f))
        view.setPcm16Mono(shortArrayOf(10, 20, 30))
        view.clear()

        layoutAndDraw(view, width = 320, height = 180)

        val ringBuffer = view.readPrivate<PcmRingBuffer>("ringBuffer")
        val size = ringBuffer.size()

        assertEquals(0, size)
    }
}
