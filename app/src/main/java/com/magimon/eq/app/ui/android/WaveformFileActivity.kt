package com.magimon.eq.app.ui.android

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import com.magimon.eq.app.R
import com.magimon.eq.waveform.PcmWaveFormStyleOptions
import com.magimon.eq.waveform.PcmWaveFormView
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

/**
 * Demo activity that plays a sample audio file and visualizes its waveform.
 *
 * Playback uses [MediaPlayer], while PCM extraction uses [MediaExtractor]/[MediaCodec].
 */
class WaveformFileActivity : AppCompatActivity() {

    /**
     * Decoded PCM result model.
     *
     * @property sampleRateHz PCM sample rate (Hz)
     * @property samples Mono 16-bit PCM samples
     */
    private data class DecodedPcm(
        val sampleRateHz: Int,
        val samples: ShortArray,
    )

    private lateinit var waveformView: PcmWaveFormView
    private lateinit var playButton: Button
    private lateinit var pauseButton: Button
    private lateinit var stopButton: Button

    private val mainHandler = Handler(Looper.getMainLooper())
    private val decodeExecutor = Executors.newSingleThreadExecutor()
    private var mediaPlayer: MediaPlayer? = null
    private var decodedPcm: DecodedPcm? = null
    private var lastPushedSampleIndex = 0

    /**
     * Loop that feeds PCM chunks into [waveformView] based on current playback position.
     */
    private val feedRunnable = object : Runnable {
        override fun run() {
            val player = mediaPlayer ?: return
            val pcm = decodedPcm ?: return
            if (!player.isPlaying) return

            val targetIndex = ((player.currentPosition.toLong() * pcm.sampleRateHz) / 1000L)
                .toInt()
                .coerceIn(0, pcm.samples.size)

            if (targetIndex < lastPushedSampleIndex) {
                waveformView.clear()
                lastPushedSampleIndex = 0
            }

            if (targetIndex > lastPushedSampleIndex) {
                val chunk = pcm.samples.copyOfRange(lastPushedSampleIndex, targetIndex)
                waveformView.appendPcm16Mono(chunk)
                lastPushedSampleIndex = targetIndex
            }

            if (player.isPlaying) {
                mainHandler.postDelayed(this, FEED_INTERVAL_MS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        waveformView = PcmWaveFormView(this).apply {
            setStyleOptions(
                PcmWaveFormStyleOptions(
                    backgroundColor = 0xFF0E1620.toInt(),
                    waveColor = 0xFF62D5FF.toInt(),
                    centerLineColor = 0xFF2D3A46.toInt(),
                    strokeWidthDp = 1.4f,
                    contentPaddingDp = 10f,
                ),
            )
            setWindowDurationMs(2_500)
        }

        playButton = Button(this).apply {
            text = "Play Sample File"
            isEnabled = false
            setOnClickListener { playSample() }
        }
        pauseButton = Button(this).apply {
            text = "Pause"
            isEnabled = false
            setOnClickListener { pauseSample() }
        }
        stopButton = Button(this).apply {
            text = "Stop"
            isEnabled = false
            setOnClickListener { stopSample(resetWave = true) }
        }

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(playButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(pauseButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(stopButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            addView(
                waveformView,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (280 * resources.displayMetrics.density).toInt(),
                ),
            )
            addView(
                controls,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
        }

        setContentView(root)
        decodeSampleAudioAsync(R.raw.sample_tone)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopFeedLoop()
        mediaPlayer?.release()
        mediaPlayer = null
        decodeExecutor.shutdownNow()
    }

    /**
     * Starts sample audio playback and starts the waveform feed loop.
     */
    private fun playSample() {
        val decoded = decodedPcm
        if (decoded == null) {
            Toast.makeText(this, "Preparing audio file.", Toast.LENGTH_SHORT).show()
            return
        }

        val player = mediaPlayer ?: createPlayer()
        if (player == null) {
            Toast.makeText(this, "Failed to create audio player.", Toast.LENGTH_SHORT).show()
            return
        }

        if (player.currentPosition <= 0) {
            waveformView.clear()
            lastPushedSampleIndex = 0
        }

        waveformView.setSampleRateHz(decoded.sampleRateHz)
        player.start()
        startFeedLoop()
        pauseButton.isEnabled = true
        stopButton.isEnabled = true
    }

    /**
     * Pauses playback and stops the waveform feed loop.
     */
    private fun pauseSample() {
        val player = mediaPlayer ?: return
        if (!player.isPlaying) return
        player.pause()
        stopFeedLoop()
        pauseButton.isEnabled = false
    }

    /**
     * Stops playback and releases the player.
     *
     * @param resetWave If `true`, clears the waveform buffer as well
     */
    private fun stopSample(resetWave: Boolean) {
        stopFeedLoop()
        mediaPlayer?.release()
        mediaPlayer = null
        pauseButton.isEnabled = false
        stopButton.isEnabled = false
        playButton.isEnabled = decodedPcm != null

        lastPushedSampleIndex = 0
        if (resetWave) {
            waveformView.clear()
        }
    }

    /**
     * Creates a [MediaPlayer] for the sample audio.
     */
    private fun createPlayer(): MediaPlayer? {
        val player = MediaPlayer.create(this, R.raw.sample_tone) ?: return null
        player.setOnCompletionListener {
            stopSample(resetWave = true)
        }
        mediaPlayer = player
        return player
    }

    /**
     * Decodes sample audio to PCM on a background thread.
     */
    private fun decodeSampleAudioAsync(@RawRes resId: Int) {
        decodeExecutor.execute {
            val decoded = decodePcm16MonoFromRaw(resId)
            runOnUiThread {
                if (decoded == null) {
                    Toast.makeText(this, "PCM decode failed.", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }

                decodedPcm = decoded
                waveformView.setSampleRateHz(decoded.sampleRateHz)
                playButton.isEnabled = true
                Toast.makeText(this, "Sample audio is ready.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Decodes `res/raw` audio and converts it to 16-bit mono PCM.
     *
     * @param resId `res/raw` resource ID
     * @return PCM result on success, or `null` on failure
     */
    private fun decodePcm16MonoFromRaw(@RawRes resId: Int): DecodedPcm? {
        val afd = resources.openRawResourceFd(resId) ?: return null
        val extractor = MediaExtractor()

        try {
            extractor.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        } finally {
            afd.close()
        }

        val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
            val mime = extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME) ?: return@firstOrNull false
            mime.startsWith("audio/")
        } ?: run {
            extractor.release()
            return null
        }

        extractor.selectTrack(trackIndex)
        val inputFormat = extractor.getTrackFormat(trackIndex)
        val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: run {
            extractor.release()
            return null
        }

        var sampleRateHz = if (inputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        } else {
            DEFAULT_SAMPLE_RATE_HZ
        }
        var channelCount = if (inputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        } else {
            1
        }

        val decoder = MediaCodec.createDecoderByType(mime)
        val bufferInfo = MediaCodec.BufferInfo()
        val pcmBytes = ByteArrayOutputStream()

        try {
            decoder.configure(inputFormat, null, null, 0)
            decoder.start()

            var inputDone = false
            var outputDone = false
            while (!outputDone) {
                if (!inputDone) {
                    val inputIndex = decoder.dequeueInputBuffer(CODEC_TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inputIndex) ?: continue
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                            )
                            inputDone = true
                        } else {
                            decoder.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                when (val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, CODEC_TIMEOUT_US)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outputFormat = decoder.outputFormat
                        if (outputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            sampleRateHz = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        }
                        if (outputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                            channelCount = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        }
                    }

                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit

                    else -> {
                        if (outputIndex >= 0) {
                            val outputBuffer = decoder.getOutputBuffer(outputIndex)
                            if (bufferInfo.size > 0 && outputBuffer != null) {
                                outputBuffer.position(bufferInfo.offset)
                                outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                                val chunk = ByteArray(bufferInfo.size)
                                outputBuffer.get(chunk)
                                pcmBytes.write(chunk)
                            }
                            decoder.releaseOutputBuffer(outputIndex, false)

                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                outputDone = true
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            return null
        } finally {
            try {
                decoder.stop()
            } catch (_: Exception) {
            }
            decoder.release()
            extractor.release()
        }

        val mono = convertPcm16ToMono(pcmBytes.toByteArray(), channelCount)
        return DecodedPcm(
            sampleRateHz = sampleRateHz.coerceIn(8_000, 192_000),
            samples = mono,
        )
    }

    /**
     * Converts PCM 16-bit byte array into mono samples.
     *
     * When channel count is 2 or more, downmixes by averaging per frame.
     */
    private fun convertPcm16ToMono(bytes: ByteArray, channelCount: Int): ShortArray {
        if (bytes.isEmpty()) return ShortArray(0)
        if (channelCount <= 1) {
            val sampleCount = bytes.size / 2
            return ShortArray(sampleCount) { index ->
                val base = index * 2
                ((bytes[base + 1].toInt() shl 8) or (bytes[base].toInt() and 0xFF)).toShort()
            }
        }

        val frameSize = channelCount * 2
        val frameCount = bytes.size / frameSize
        val out = ShortArray(frameCount)

        for (frame in 0 until frameCount) {
            var sum = 0
            val base = frame * frameSize
            for (ch in 0 until channelCount) {
                val idx = base + (ch * 2)
                val sample = ((bytes[idx + 1].toInt() shl 8) or (bytes[idx].toInt() and 0xFF)).toShort().toInt()
                sum += sample
            }
            out[frame] = (sum / channelCount).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    /**
     * Starts the playback-position-based waveform feed loop.
     */
    private fun startFeedLoop() {
        stopFeedLoop()
        mainHandler.post(feedRunnable)
    }

    /**
     * Stops the waveform feed loop.
     */
    private fun stopFeedLoop() {
        mainHandler.removeCallbacks(feedRunnable)
    }

    private companion object {
        const val FEED_INTERVAL_MS = 33L
        const val CODEC_TIMEOUT_US = 10_000L
        const val DEFAULT_SAMPLE_RATE_HZ = 44_100
    }
}
