package com.patoolbox.core.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTimestamp
import android.media.AudioTrack
import android.os.Process
import androidx.core.content.ContextCompat
import com.patoolbox.core.dsp.LogSweepSource
import com.patoolbox.core.dsp.RoomAnalysis
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * スイープを鳴らしながら同時に録る、一発測定の実行部。
 *
 * 常用の [AudioCaptureEngine] / [AudioPlaybackEngine] と分けているのは要件が違うため。
 * こちらは「出した信号が何だったかを1サンプルの狂いもなく知っている」ことが必須で、
 * そのために出力バッファは自前で作って参照信号と同一物にしてある。
 * 再生側のフェード処理に依存すると、参照と実際に出た音がずれる。
 */
@Singleton
class SweepMeasurementEngine @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    data class Config(
        val sampleRate: Int = AudioCaptureEngine.DEFAULT_SAMPLE_RATE,
        /** スイープの長さ。長いほど雑音に強くなるが、その間 現場に音を出し続ける */
        val sweepSeconds: Double = 2.0,
        /** スイープの後に録り続ける時間。残響を取り込むための余白 */
        val tailSeconds: Double = 3.0,
        val levelDbFs: Double = -12.0,
        val startHz: Double = 50.0,
        val endHz: Double = 16000.0,
        val preferredDeviceId: Int? = null,
    ) {
        val sweepSamples: Int get() = (sweepSeconds * sampleRate).toInt()
        val totalSamples: Int get() = ((sweepSeconds + tailSeconds) * sampleRate).toInt()
    }

    data class Capture(
        /** 実際に出力した信号そのもの */
        val reference: DoubleArray,
        val recorded: DoubleArray,
        val sampleRate: Int,
        val peakAmplitude: Double,
        /**
         * 出力の先頭サンプルが録音のどの位置にあたるか。
         * 端末のタイムスタンプが取れなかった場合は null（＝絶対遅延は出せない）。
         */
        val alignmentOffsetSamples: Double?,
        val inputSource: MicInputSource,
    ) {
        val clipped: Boolean get() = peakAmplitude >= RoomAnalysis.CLIP_THRESHOLD

        /** 何も返ってきていない（スピーカーが鳴っていない／マイクが繋がっていない）。 */
        val silent: Boolean get() = peakAmplitude < SILENT_THRESHOLD
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * 1回測る。処理は重い（数百万サンプルの FFT を伴う解析が後段にある）ので
     * 必ず IO ディスパッチャで回す。
     */
    @SuppressLint("MissingPermission") // hasPermission() で確認してから開く
    suspend fun measure(config: Config = Config()): Capture = withContext(Dispatchers.IO) {
        check(hasPermission()) { "RECORD_AUDIO 権限がない状態で measure() が呼ばれた" }

        val sweep = renderSweep(config)
        val record = openRecord(config)
        val track = openTrack(config)

        val recorded = FloatArray(config.totalSamples)
        var captured = 0
        val outputTimestamp = AudioTimestamp()
        val inputTimestamp = AudioTimestamp()
        var timestampsValid = false

        try {
            record.startRecording()

            // 録音を専用スレッドで回し、その間に出力を書く。
            // 逆にする（出力を別スレッド）と、書き込みのブロッキングで
            // 録音の取りこぼしが起きたときに気づけない
            val reader = Thread {
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
                while (captured < recorded.size) {
                    val read = record.read(
                        recorded,
                        captured,
                        recorded.size - captured,
                        AudioRecord.READ_BLOCKING,
                    )
                    if (read <= 0) break
                    captured += read
                }
            }.apply {
                name = "pa-sweep-capture"
                start()
            }

            track.play()
            var written = 0
            while (written < sweep.size) {
                val wrote = track.write(
                    sweep,
                    written,
                    sweep.size - written,
                    AudioTrack.WRITE_BLOCKING,
                )
                if (wrote <= 0) break
                written += wrote
            }

            // 出力と入力を同じ時計に乗せる。書き終えた直後が一番確実に取れる
            timestampsValid = track.getTimestamp(outputTimestamp) &&
                record.getTimestamp(inputTimestamp, AudioTimestamp.TIMEBASE_MONOTONIC) ==
                AudioRecord.SUCCESS

            reader.join(READER_JOIN_TIMEOUT_MS)
        } finally {
            runCatching { track.stop() }
            track.release()
            runCatching { record.stop() }
            record.release()
        }

        val length = captured.coerceAtLeast(1)
        var peak = 0.0
        for (i in 0 until length) {
            val magnitude = abs(recorded[i].toDouble())
            if (magnitude > peak) peak = magnitude
        }

        Capture(
            reference = DoubleArray(sweep.size) { sweep[it].toDouble() },
            recorded = DoubleArray(length) { recorded[it].toDouble() },
            sampleRate = config.sampleRate,
            peakAmplitude = peak,
            alignmentOffsetSamples = if (timestampsValid) {
                alignmentOffset(outputTimestamp, inputTimestamp, config.sampleRate)
            } else {
                null
            },
            inputSource = MicInputSource.resolve(context),
        )
    }

    /**
     * 出力の先頭サンプルが鳴った瞬間に、録音側が何サンプル目を取っていたか。
     *
     * これが分かると「録音の何サンプル目に返ってきたか」から出力からの経過時間が引ける。
     * ただしここで消えるのはソフトウェア側のバッファ遅延までで、
     * アナログ段やスピーカー・マイクの処理遅延は残る。その残りは
     * ループバック校正で測って引くしかない（端末ごとにほぼ一定）。
     */
    private fun alignmentOffset(
        output: AudioTimestamp,
        input: AudioTimestamp,
        sampleRate: Int,
    ): Double {
        val nanosPerSample = NANOS_PER_SECOND / sampleRate
        val outputStartNanos = output.nanoTime - output.framePosition * nanosPerSample
        return input.framePosition +
            (outputStartNanos - input.nanoTime) / nanosPerSample
    }

    /**
     * 出力する信号を作る。両端にフェードを掛けるのは、PA に繋いだ状態で
     * 信号を切ったときの「ボツッ」を出さないため。参照信号もこの配列そのものを使うので、
     * フェードによる波形の違いが測定誤差にならない。
     */
    private fun renderSweep(config: Config): FloatArray {
        val source = LogSweepSource(
            sampleRate = config.sampleRate,
            startHz = config.startHz,
            endHz = config.endHz,
            durationSeconds = config.sweepSeconds,
            levelDbFs = config.levelDbFs,
        )
        val buffer = FloatArray(config.sweepSamples)
        source.fill(buffer, buffer.size)

        val fade = (FADE_SECONDS * config.sampleRate).toInt().coerceAtLeast(1)
        for (i in 0 until minOf(fade, buffer.size)) {
            val gain = i.toFloat() / fade
            buffer[i] *= gain
            buffer[buffer.size - 1 - i] *= gain
        }
        return buffer
    }

    private fun openRecord(config: Config): AudioRecord {
        val minBufferBytes = AudioRecord.getMinBufferSize(
            config.sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_FLOAT,
        )
        check(minBufferBytes > 0) {
            "この端末では ${config.sampleRate}Hz の Float モノラル入力を開けない"
        }

        @SuppressLint("MissingPermission")
        val record = AudioRecord.Builder()
            .setAudioSource(MicInputSource.resolve(context).androidSource)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(config.sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(maxOf(minBufferBytes, config.sampleRate * BYTES_PER_FLOAT))
            .build()

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            error("AudioRecord の初期化に失敗した")
        }

        config.preferredDeviceId?.let { id ->
            context.getSystemService(AudioManager::class.java)
                ?.getDevices(AudioManager.GET_DEVICES_INPUTS)
                ?.firstOrNull { it.id == id }
                ?.let { record.preferredDevice = it }
        }
        return record
    }

    private fun openTrack(config: Config): AudioTrack {
        val minBufferBytes = AudioTrack.getMinBufferSize(
            config.sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT,
        )
        check(minBufferBytes > 0) { "この端末では ${config.sampleRate}Hz の Float 出力を開けない" }

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(config.sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(maxOf(minBufferBytes, config.sampleRate * BYTES_PER_FLOAT / 2))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        if (track.state != AudioTrack.STATE_INITIALIZED) {
            track.release()
            error("AudioTrack の初期化に失敗した")
        }
        return track
    }

    companion object {
        private const val FADE_SECONDS = 0.02
        private const val BYTES_PER_FLOAT = 4
        private const val READER_JOIN_TIMEOUT_MS = 10_000L
        private const val NANOS_PER_SECOND = 1_000_000_000.0
        private const val SILENT_THRESHOLD = 0.001
    }
}
