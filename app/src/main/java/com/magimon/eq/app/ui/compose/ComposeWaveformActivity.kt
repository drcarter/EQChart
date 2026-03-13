package com.magimon.eq.app.ui.compose

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RawRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.magimon.eq.app.R
import com.magimon.eq.app.ui.theme.EQChartTheme
import com.magimon.eq.compose.PcmWaveformChart
import com.magimon.eq.compose.PcmWaveformController
import com.magimon.eq.compose.rememberPcmWaveformController
import com.magimon.eq.waveform.PcmWaveFormStyleOptions
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

class ComposeWaveformActivity : ComponentActivity() {

    private data class DecodedPcm(
        val sampleRateHz: Int,
        val samples: ShortArray,
    )

    private lateinit var controller: PcmWaveformController
    private val mainHandler = Handler(Looper.getMainLooper())
    private val decodeExecutor = Executors.newSingleThreadExecutor()
    private var mediaPlayer: MediaPlayer? = null
    private var decodedPcm: DecodedPcm? = null
    private var lastPushedSampleIndex = 0

    private var playEnabled by mutableStateOf(false)
    private var pauseEnabled by mutableStateOf(false)
    private var stopEnabled by mutableStateOf(false)

    private val feedRunnable = object : Runnable {
        override fun run() {
            val player = mediaPlayer ?: return
            val pcm = decodedPcm ?: return
            if (!player.isPlaying) return

            val targetIndex = ((player.currentPosition.toLong() * pcm.sampleRateHz) / 1000L)
                .toInt()
                .coerceIn(0, pcm.samples.size)

            if (targetIndex < lastPushedSampleIndex) {
                controller.clear()
                lastPushedSampleIndex = 0
            }

            if (targetIndex > lastPushedSampleIndex) {
                val chunk = pcm.samples.copyOfRange(lastPushedSampleIndex, targetIndex)
                controller.appendPcm16Mono(chunk)
                lastPushedSampleIndex = targetIndex
            }

            if (player.isPlaying) {
                mainHandler.postDelayed(this, FEED_INTERVAL_MS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EQChartTheme {
                val rememberedController = rememberPcmWaveformController(
                    sampleRateHz = DEFAULT_SAMPLE_RATE_HZ,
                    windowDurationMs = 2_500,
                )
                controller = rememberedController

                val isPlayEnabled = playEnabled
                val isPauseEnabled = pauseEnabled
                val isStopEnabled = stopEnabled

                ComposeWaveformSampleScreen(
                    controller = controller,
                    isPlayEnabled = isPlayEnabled,
                    isPauseEnabled = isPauseEnabled,
                    isStopEnabled = isStopEnabled,
                    onPlay = { playSample() },
                    onPause = { pauseSample() },
                    onStop = { stopSample(resetWave = true) },
                )
            }
        }

        decodeSampleAudioAsync(R.raw.sample_tone)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopFeedLoop()
        mediaPlayer?.release()
        mediaPlayer = null
        decodeExecutor.shutdownNow()
    }

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
            controller.clear()
            lastPushedSampleIndex = 0
        }

        controller.setSampleRateHz(decoded.sampleRateHz)
        player.start()
        startFeedLoop()
        pauseEnabled = true
        stopEnabled = true
    }

    private fun pauseSample() {
        val player = mediaPlayer ?: return
        if (!player.isPlaying) return
        player.pause()
        stopFeedLoop()
        pauseEnabled = false
    }

    private fun stopSample(resetWave: Boolean) {
        stopFeedLoop()
        mediaPlayer?.release()
        mediaPlayer = null

        pauseEnabled = false
        stopEnabled = false
        playEnabled = decodedPcm != null

        lastPushedSampleIndex = 0
        if (resetWave) {
            controller.clear()
        }
    }

    private fun createPlayer(): MediaPlayer? {
        val player = MediaPlayer.create(this, R.raw.sample_tone) ?: return null
        player.setOnCompletionListener {
            stopSample(resetWave = true)
        }
        mediaPlayer = player
        return player
    }

    private fun decodeSampleAudioAsync(@RawRes resId: Int) {
        decodeExecutor.execute {
            val decoded = decodePcm16MonoFromRaw(resId)
            runOnUiThread {
                if (decoded == null) {
                    Toast.makeText(this, "PCM decode failed.", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }

                decodedPcm = decoded
                if (::controller.isInitialized) {
                    controller.setSampleRateHz(decoded.sampleRateHz)
                }
                playEnabled = true
                Toast.makeText(this, "Sample audio is ready.", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
                val index = base + (ch * 2)
                val sample = ((bytes[index + 1].toInt() shl 8) or (bytes[index].toInt() and 0xFF)).toShort().toInt()
                sum += sample
            }
            out[frame] = (sum / channelCount)
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return out
    }

    private fun startFeedLoop() {
        stopFeedLoop()
        mainHandler.post(feedRunnable)
    }

    private fun stopFeedLoop() {
        mainHandler.removeCallbacks(feedRunnable)
    }

    companion object {
        const val FEED_INTERVAL_MS = 33L
        const val CODEC_TIMEOUT_US = 10_000L
        const val DEFAULT_SAMPLE_RATE_HZ = 44_100
    }
}

@Composable
private fun ComposeWaveformSampleScreen(
    controller: PcmWaveformController,
    isPlayEnabled: Boolean,
    isPauseEnabled: Boolean,
    isStopEnabled: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
) {
    ComposeSamplePage(title = "Compose Waveform") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
            ) {
                PcmWaveformChart(
                    controller = controller,
                    styleOptions = PcmWaveFormStyleOptions(
                        backgroundColor = 0xFF0E1620.toInt(),
                        waveColor = 0xFF62D5FF.toInt(),
                        centerLineColor = 0xFF2D3A46.toInt(),
                        strokeWidthDp = 1.4f,
                        contentPaddingDp = 10f,
                    ),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(0.3333f),
                    onClick = onPlay,
                    enabled = isPlayEnabled,
                ) {
                    Text(text = "Play Sample File")
                }
                Button(
                    modifier = Modifier.fillMaxWidth(0.3333f),
                    onClick = onPause,
                    enabled = isPauseEnabled,
                ) {
                    Text(text = "Pause")
                }
                Button(
                    modifier = Modifier.fillMaxWidth(0.3333f),
                    onClick = onStop,
                    enabled = isStopEnabled,
                ) {
                    Text(text = "Stop")
                }
            }
        }
    }
}
