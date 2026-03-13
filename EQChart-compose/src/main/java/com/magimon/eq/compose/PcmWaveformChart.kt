package com.magimon.eq.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.magimon.eq.waveform.PcmWaveFormStyleOptions

class PcmWaveformController internal constructor(
    sampleRateHz: Int,
    windowDurationMs: Int,
) {
    private var sampleRateHzState by mutableIntStateOf(sampleRateHz.coerceIn(8_000, 192_000))
    private var windowDurationMsState by mutableIntStateOf(windowDurationMs.coerceIn(200, 60_000))
    private val ringBuffer = ComposePcmRingBuffer(computeCapacity(sampleRateHzState, windowDurationMsState))

    internal var version by mutableLongStateOf(0L)
        private set

    fun setWindowDurationMs(durationMs: Int) {
        val target = durationMs.coerceIn(200, 60_000)
        if (target == windowDurationMsState) return

        windowDurationMsState = target
        synchronized(ringBuffer) {
            ringBuffer.setCapacity(computeCapacity(sampleRateHzState, windowDurationMsState))
        }
        version += 1
    }

    fun setSampleRateHz(hz: Int) {
        val target = hz.coerceIn(8_000, 192_000)
        if (target == sampleRateHzState) return

        sampleRateHzState = target
        synchronized(ringBuffer) {
            ringBuffer.setCapacity(computeCapacity(sampleRateHzState, windowDurationMsState))
        }
        version += 1
    }

    fun clear() {
        ringBuffer.clear()
        version += 1
    }

    fun setPcm16Mono(samples: ShortArray) {
        ringBuffer.setAll(samples)
        version += 1
    }

    fun appendPcm16Mono(samples: ShortArray) {
        if (samples.isEmpty()) return
        ringBuffer.append(samples)
        version += 1
    }

    internal fun snapshot(): ShortArray = ringBuffer.snapshot()

    private fun computeCapacity(sampleRateHz: Int, windowDurationMs: Int): Int {
        val value = (sampleRateHz.toLong() * windowDurationMs.toLong()) / 1_000L
        return value.coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
    }
}

@Composable
fun rememberPcmWaveformController(
    sampleRateHz: Int = 44_100,
    windowDurationMs: Int = 2_000,
): PcmWaveformController {
    val controller = remember {
        PcmWaveformController(
            sampleRateHz = sampleRateHz,
            windowDurationMs = windowDurationMs,
        )
    }

    LaunchedEffect(sampleRateHz) {
        controller.setSampleRateHz(sampleRateHz)
    }
    LaunchedEffect(windowDurationMs) {
        controller.setWindowDurationMs(windowDurationMs)
    }

    return controller
}

@Composable
fun PcmWaveformChart(
    controller: PcmWaveformController,
    modifier: Modifier = Modifier,
    styleOptions: PcmWaveFormStyleOptions = PcmWaveFormStyleOptions(),
) {
    val density = LocalDensity.current
    val version = controller.version
    val samples = remember(version) { controller.snapshot() }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(styleOptions.backgroundColor.toComposeColor())

        val contentPaddingPx = with(density) { styleOptions.contentPaddingDp.dp.toPx() }
        val left = contentPaddingPx
        val right = size.width - contentPaddingPx
        val top = contentPaddingPx
        val bottom = size.height - contentPaddingPx
        if (right <= left || bottom <= top) return@Canvas

        val centerY = (top + bottom) * 0.5f
        if (styleOptions.showCenterLine) {
            drawLine(
                color = styleOptions.centerLineColor.toComposeColor(),
                start = Offset(left, centerY),
                end = Offset(right, centerY),
                strokeWidth = with(density) { 1.dp.toPx() },
            )
        }

        if (samples.isEmpty()) return@Canvas

        val plotWidth = right - left
        val pixelWidth = plotWidth.toInt().coerceAtLeast(1)
        val minMax = minMaxPerPixel(samples, pixelWidth)
        if (minMax.isEmpty()) return@Canvas

        val amplitudeScale = styleOptions.amplitudeScale.coerceIn(0.1f, 3f)
        val amplitude = ((bottom - top) * 0.5f) * amplitudeScale
        val stepX = if (pixelWidth > 1) plotWidth / (pixelWidth - 1) else 0f

        for (x in 0 until pixelWidth) {
            val minNorm = minMax[x * 2]
            val maxNorm = minMax[x * 2 + 1]
            val yTop = centerY - (maxNorm * amplitude)
            val yBottom = centerY - (minNorm * amplitude)
            val xPos = left + (x * stepX)
            drawLine(
                color = styleOptions.waveColor.toComposeColor(),
                start = Offset(xPos, yTop),
                end = Offset(xPos, yBottom),
                strokeWidth = with(density) { styleOptions.strokeWidthDp.dp.toPx() }.coerceAtLeast(1f),
                cap = StrokeCap.Round,
            )
        }
    }
}

private class ComposePcmRingBuffer(capacity: Int) {
    private var data = ShortArray(capacity.coerceAtLeast(1))
    private var writeIndex = 0
    private var size = 0

    @Synchronized
    fun clear() {
        writeIndex = 0
        size = 0
    }

    @Synchronized
    fun setCapacity(newCapacity: Int) {
        val target = newCapacity.coerceAtLeast(1)
        if (target == data.size) return

        val snapshot = snapshotUnsafe()
        data = ShortArray(target)
        writeIndex = 0
        size = 0

        val from = (snapshot.size - target).coerceAtLeast(0)
        append(snapshot, from, snapshot.size)
    }

    @Synchronized
    fun setAll(samples: ShortArray) {
        clear()
        val from = (samples.size - data.size).coerceAtLeast(0)
        append(samples, from, samples.size)
    }

    @Synchronized
    fun append(samples: ShortArray, fromIndex: Int = 0, toIndex: Int = samples.size) {
        val start = fromIndex.coerceIn(0, samples.size)
        val end = toIndex.coerceIn(start, samples.size)
        if (start >= end) return

        for (index in start until end) {
            data[writeIndex] = samples[index]
            writeIndex = (writeIndex + 1) % data.size
            if (size < data.size) size++
        }
    }

    @Synchronized
    fun snapshot(): ShortArray = snapshotUnsafe()

    private fun snapshotUnsafe(): ShortArray {
        if (size == 0) return ShortArray(0)

        val out = ShortArray(size)
        val start = (writeIndex - size + data.size) % data.size
        for (index in 0 until size) {
            out[index] = data[(start + index) % data.size]
        }
        return out
    }
}

private fun minMaxPerPixel(samples: ShortArray, pixelWidth: Int): FloatArray {
    if (samples.isEmpty() || pixelWidth <= 0) return FloatArray(0)

    val normalizer = 1f / Short.MAX_VALUE.toFloat()
    val out = FloatArray(pixelWidth * 2)

    for (x in 0 until pixelWidth) {
        val start = (x * samples.size) / pixelWidth
        var end = ((x + 1) * samples.size) / pixelWidth
        if (end <= start) end = (start + 1).coerceAtMost(samples.size)

        var minSample = Short.MAX_VALUE.toInt()
        var maxSample = Short.MIN_VALUE.toInt()

        for (index in start until end) {
            val value = samples[index].toInt()
            if (value < minSample) minSample = value
            if (value > maxSample) maxSample = value
        }

        out[x * 2] = (minSample * normalizer).coerceIn(-1f, 1f)
        out[x * 2 + 1] = (maxSample * normalizer).coerceIn(-1f, 1f)
    }

    return out
}
