package com.patoolbox.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import com.patoolbox.core.dsp.SignalSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * テスト信号の出力。
 *
 * 開始と停止に必ずフェードを掛ける。PA に繋いだ状態で信号を切ると
 * 「ボツッ」という過渡音が大音量で出て、最悪ドライバを痛める。
 * 現場で使う道具として、これは省略できない。
 */
@Singleton
class AudioPlaybackEngine @Inject constructor() {

    @Volatile
    private var running = false

    @Volatile
    private var fadingOut = false

    private var thread: Thread? = null
    private var track: AudioTrack? = null

    val isPlaying: Boolean get() = running

    fun start(source: SignalSource, blockSize: Int = DEFAULT_BLOCK_SIZE) {
        check(!running) { "すでに再生中" }

        val sampleRate = source.sampleRate
        val minBufferBytes = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT,
        )
        check(minBufferBytes > 0) { "この端末では ${sampleRate}Hz の Float 出力を開けない" }

        val newTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(maxOf(minBufferBytes, blockSize * BYTES_PER_FLOAT * 4))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        if (newTrack.state != AudioTrack.STATE_INITIALIZED) {
            newTrack.release()
            error("AudioTrack の初期化に失敗した")
        }

        track = newTrack
        running = true
        fadingOut = false
        newTrack.play()

        thread = Thread {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            val buffer = FloatArray(blockSize)
            val fadeSamples = (sampleRate * FADE_SECONDS).toInt().coerceAtLeast(1)
            val step = 1.0f / fadeSamples
            var gain = 0f

            while (running) {
                source.fill(buffer, buffer.size)

                for (i in buffer.indices) {
                    gain = if (fadingOut) {
                        (gain - step).coerceAtLeast(0f)
                    } else {
                        (gain + step).coerceAtMost(1f)
                    }
                    buffer[i] *= gain
                }

                newTrack.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)

                if (fadingOut && gain <= 0f) break
            }

            runCatching { newTrack.stop() }
            newTrack.release()
            running = false
        }.apply {
            name = "pa-audio-playback"
            start()
        }
    }

    /** フェードアウトしてから止める。 */
    fun stop() {
        if (!running) return
        fadingOut = true

        thread?.join(THREAD_JOIN_TIMEOUT_MS)
        thread = null

        // AudioTrack の解放は必ず再生スレッド側で行う（二重解放を避ける）。
        // join がタイムアウトした場合も、running=false でループを抜けて解放される。
        running = false
        track = null
    }

    companion object {
        const val DEFAULT_BLOCK_SIZE = 1024

        /** 立ち上がり・立ち下がりの時間。クリック音を出さない最小限 */
        private const val FADE_SECONDS = 0.02

        private const val BYTES_PER_FLOAT = 4
        private const val THREAD_JOIN_TIMEOUT_MS = 500L
    }
}
