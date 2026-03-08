package com.magimon.eq.waveform

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * View that renders PCM (16-bit mono) data as a real-time waveform.
 *
 * - Input: [appendPcm16Mono], [setPcm16Mono]
 * - Rendering: min/max downsampling per pixel
 * - Memory: keeps only a recent N-ms window in a ring buffer
 *
 * v1 supports only mono 16-bit PCM (`ShortArray`).
 */
class PcmWaveFormView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density

    private var sampleRateHz: Int = DEFAULT_SAMPLE_RATE_HZ
    private var windowDurationMs: Int = DEFAULT_WINDOW_DURATION_MS
    private var styleOptions = PcmWaveFormStyleOptions()
    private val ringBuffer = PcmRingBuffer(computeCapacity(sampleRateHz, windowDurationMs))

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val centerLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    init {
        applyStyle()
    }

    /**
     * Sets rendering style options.
     *
     * @param options Style options such as background/stroke color, stroke width, and amplitude scale
     */
    fun setStyleOptions(options: PcmWaveFormStyleOptions) {
        styleOptions = options
        applyStyle()
        invalidate()
    }

    /**
     * Sets visible window duration in milliseconds.
     *
     * Very small values are clamped, and ring-buffer capacity is recalculated.
     *
     * @param durationMs Duration of the recent window to display (ms)
     */
    fun setWindowDurationMs(durationMs: Int) {
        val target = durationMs.coerceIn(200, 60_000)
        if (windowDurationMs == target) return

        windowDurationMs = target
        synchronized(ringBuffer) {
            ringBuffer.setCapacity(computeCapacity(sampleRateHz, windowDurationMs))
        }
        invalidate()
    }

    /**
     * Sets input PCM sample rate (Hz).
     *
     * Recalculates buffer capacity to preserve the same window duration.
     *
     * @param hz Input PCM sample rate (Hz)
     */
    fun setSampleRateHz(hz: Int) {
        val target = hz.coerceIn(8_000, 192_000)
        if (sampleRateHz == target) return

        sampleRateHz = target
        synchronized(ringBuffer) {
            ringBuffer.setCapacity(computeCapacity(sampleRateHz, windowDurationMs))
        }
        invalidate()
    }

    /**
     * Clears the current buffer.
     *
     * Useful for removing previous waveform traces when restarting a live stream.
     */
    fun clear() {
        ringBuffer.clear()
        postInvalidateOnAnimation()
    }

    /**
     * Replaces the entire PCM buffer.
     *
     * @param samples 16-bit mono PCM sample array
     */
    fun setPcm16Mono(samples: ShortArray) {
        ringBuffer.setAll(samples)
        postInvalidateOnAnimation()
    }

    /**
     * Appends a PCM sample chunk.
     *
     * @param samples 16-bit mono PCM chunk
     */
    fun appendPcm16Mono(samples: ShortArray) {
        if (samples.isEmpty()) return
        ringBuffer.append(samples)
        postInvalidateOnAnimation()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = (800f * density).toInt()
        val desiredHeight = (260f * density).toInt()
        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        val contentPaddingPx = styleOptions.contentPaddingDp * density
        val left = paddingLeft + contentPaddingPx
        val right = width - paddingRight - contentPaddingPx
        val top = paddingTop + contentPaddingPx
        val bottom = height - paddingBottom - contentPaddingPx
        if (right <= left || bottom <= top) return

        val centerY = (top + bottom) * 0.5f
        if (styleOptions.showCenterLine) {
            canvas.drawLine(left, centerY, right, centerY, centerLinePaint)
        }

        val samples = ringBuffer.snapshot()
        if (samples.isEmpty()) return

        val plotWidth = right - left
        val pixelWidth = plotWidth.toInt().coerceAtLeast(1)
        val minMax = PcmWaveDownSampler.minMaxPerPixel(samples, pixelWidth)
        if (minMax.isEmpty()) return

        val amplitudeScale = styleOptions.amplitudeScale.coerceIn(0.1f, 3f)
        val amplitude = ((bottom - top) * 0.5f) * amplitudeScale
        val stepX = if (pixelWidth > 1) plotWidth / (pixelWidth - 1) else 0f

        for (x in 0 until pixelWidth) {
            val minNorm = minMax[x * 2]
            val maxNorm = minMax[x * 2 + 1]
            val yTop = centerY - (maxNorm * amplitude)
            val yBottom = centerY - (minNorm * amplitude)
            val xPos = left + (x * stepX)
            canvas.drawLine(xPos, yTop, xPos, yBottom, wavePaint)
        }
    }

    /**
     * Applies style options to paint objects.
     */
    private fun applyStyle() {
        backgroundPaint.color = styleOptions.backgroundColor
        centerLinePaint.color = styleOptions.centerLineColor
        wavePaint.color = styleOptions.waveColor
        wavePaint.strokeWidth = styleOptions.strokeWidthDp.coerceAtLeast(0.5f) * density
    }

    /**
     * Computes required ring-buffer capacity from sample rate and window duration.
     */
    private fun computeCapacity(sampleRateHz: Int, windowDurationMs: Int): Int {
        val value = (sampleRateHz.toLong() * windowDurationMs.toLong()) / 1_000L
        return value.coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
    }

    private companion object {
        const val DEFAULT_SAMPLE_RATE_HZ = 44_100
        const val DEFAULT_WINDOW_DURATION_MS = 2_000
    }
}
