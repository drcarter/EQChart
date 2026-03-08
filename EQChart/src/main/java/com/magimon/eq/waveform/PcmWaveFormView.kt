package com.magimon.eq.waveform

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * PCM(16-bit mono) 데이터를 실시간 파형으로 렌더링하는 View.
 *
 * - 입력: [appendPcm16Mono], [setPcm16Mono]
 * - 렌더링: 픽셀당 min/max 다운샘플링
 * - 메모리: 최근 Nms 윈도우만 링버퍼로 유지
 *
 * v1 기준으로 모노 16-bit PCM(`ShortArray`)만 지원한다.
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
     * 렌더링 스타일을 설정한다.
     *
     * @param options 배경/선색/선두께/진폭 스케일 등 스타일 옵션
     */
    fun setStyleOptions(options: PcmWaveFormStyleOptions) {
        styleOptions = options
        applyStyle()
        invalidate()
    }

    /**
     * 표시 윈도우 길이(ms)를 설정한다.
     *
     * 너무 작은 값은 보정되며, 변경 시 내부 링버퍼 용량도 함께 재계산된다.
     *
     * @param durationMs 표시할 최근 구간 길이(ms)
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
     * 입력 PCM의 샘플레이트(Hz)를 설정한다.
     *
     * 샘플레이트가 바뀌면 동일한 윈도우 길이를 유지하도록 버퍼 용량을 재계산한다.
     *
     * @param hz 입력 PCM 샘플레이트(Hz)
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
     * 현재 버퍼를 비운다.
     *
     * 실시간 스트림 재시작 시 이전 파형 흔적을 제거할 때 사용한다.
     */
    fun clear() {
        ringBuffer.clear()
        postInvalidateOnAnimation()
    }

    /**
     * 전체 PCM 버퍼를 교체한다.
     *
     * @param samples 16-bit mono PCM 샘플 배열
     */
    fun setPcm16Mono(samples: ShortArray) {
        ringBuffer.setAll(samples)
        postInvalidateOnAnimation()
    }

    /**
     * PCM 샘플 청크를 뒤에 추가한다.
     *
     * @param samples 16-bit mono PCM 청크
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
     * 스타일 옵션을 Paint 객체에 반영한다.
     */
    private fun applyStyle() {
        backgroundPaint.color = styleOptions.backgroundColor
        centerLinePaint.color = styleOptions.centerLineColor
        wavePaint.color = styleOptions.waveColor
        wavePaint.strokeWidth = styleOptions.strokeWidthDp.coerceAtLeast(0.5f) * density
    }

    /**
     * 현재 샘플레이트/윈도우 길이 조합에 필요한 링버퍼 용량을 계산한다.
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
