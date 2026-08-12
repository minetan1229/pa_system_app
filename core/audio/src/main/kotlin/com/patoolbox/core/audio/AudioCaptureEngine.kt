package com.patoolbox.core.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.os.Process
import androidx.core.content.ContextCompat
import com.patoolbox.core.model.AudioInputType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * マイク入力。専用スレッドでブロッキング読み出しを回す。
 *
 * 低レイテンシのコールバック方式（AAudio/Oboe）を使っていないのは、計測では
 * 「切れずに全サンプルが取れること」が重要で、数ミリ秒の遅延は問題にならないため。
 * その代わりに読み出しループでは一切割り当てをせず、GC を踏まないようにしている。
 *
 * DSP は [BlockListener] の中（＝オーディオスレッド）で回し、UI へは
 * 計算結果だけを渡すこと。ブロックの配列は次の読み出しで上書きされる。
 */
@Singleton
class AudioCaptureEngine @Inject constructor(
    // Hilt が読むのはコンストラクタ引数の注釈なので、適用先を明示しておく
    @param:ApplicationContext private val context: Context,
) {
    data class Config(
        val sampleRate: Int = DEFAULT_SAMPLE_RATE,
        val blockSize: Int = DEFAULT_BLOCK_SIZE,
        /** [AudioDeviceCatalog] で選んだ入力デバイス。null なら端末の既定 */
        val preferredDeviceId: Int? = null,
    )

    /** 実際に開けた入力の情報。UI の「校正状態」表示に使う。 */
    data class Session(
        val sampleRate: Int,
        val blockSize: Int,
        val inputSource: MicInputSource,
        val device: AudioInputDevice?,
    ) {
        val inputType: AudioInputType get() = device?.type ?: AudioInputType.BUILTIN_MIC
        val calibrationKey: String
            get() = device?.calibrationKey ?: AudioInputDevice.BUILTIN_KEY
    }

    fun interface BlockListener {
        /**
         * オーディオスレッドから呼ばれる。ここでブロックしたり割り当てたりしないこと。
         * @param buffer 次の読み出しで上書きされる共有バッファ
         */
        fun onBlock(buffer: FloatArray, length: Int)
    }

    @Volatile
    private var running = false
    private var thread: Thread? = null
    private var record: AudioRecord? = null

    val isRunning: Boolean get() = running

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    /** 端末が申告しているネイティブのサンプリング周波数。診断表示用。 */
    fun nativeSampleRate(): Int {
        val audioManager = context.getSystemService(AudioManager::class.java)
        return audioManager?.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
            ?.toIntOrNull()
            ?: DEFAULT_SAMPLE_RATE
    }

    @SuppressLint("MissingPermission") // hasPermission() で確認してから開く
    fun start(config: Config = Config(), listener: BlockListener): Session {
        check(!running) { "すでに録音中" }
        check(hasPermission()) { "RECORD_AUDIO 権限がない状態で start() が呼ばれた" }

        val source = MicInputSource.resolve(context)
        val minBufferBytes = AudioRecord.getMinBufferSize(
            config.sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_FLOAT,
        )
        check(minBufferBytes > 0) {
            "この端末では ${config.sampleRate}Hz の Float モノラル入力を開けない"
        }
        val bufferBytes = maxOf(
            minBufferBytes,
            config.blockSize * BYTES_PER_FLOAT * BUFFER_BLOCK_COUNT,
        )

        val newRecord = AudioRecord.Builder()
            .setAudioSource(source.androidSource)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(config.sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(bufferBytes)
            .build()

        if (newRecord.state != AudioRecord.STATE_INITIALIZED) {
            newRecord.release()
            error("AudioRecord の初期化に失敗した")
        }

        config.preferredDeviceId?.let { id ->
            val audioManager = context.getSystemService(AudioManager::class.java)
            audioManager?.getDevices(AudioManager.GET_DEVICES_INPUTS)
                ?.firstOrNull { it.id == id }
                ?.let { newRecord.preferredDevice = it }
        }

        record = newRecord
        running = true
        newRecord.startRecording()

        val session = Session(
            sampleRate = config.sampleRate,
            blockSize = config.blockSize,
            inputSource = source,
            device = newRecord.routedDevice?.toInputDevice(),
        )

        thread = Thread {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            val buffer = FloatArray(config.blockSize)
            while (running) {
                val read = newRecord.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                if (read > 0) {
                    listener.onBlock(buffer, read)
                } else if (read < 0) {
                    // ERROR_INVALID_OPERATION / ERROR_DEAD_OBJECT。復帰は見込めないので抜ける
                    break
                }
            }
        }.apply {
            name = "pa-audio-capture"
            start()
        }

        return session
    }

    fun stop() {
        if (!running) return
        running = false

        thread?.join(THREAD_JOIN_TIMEOUT_MS)
        thread = null

        record?.let { rec ->
            runCatching { rec.stop() }
            rec.release()
        }
        record = null
    }

    companion object {
        /**
         * 48kHz 固定。端末のネイティブが 44.1kHz でも AudioRecord が変換するが、
         * オクターブバンドの境界や重み付けフィルタの係数が毎回変わらない方が
         * 測定値の再現性を確認しやすい。
         */
        const val DEFAULT_SAMPLE_RATE = 48000

        /**
         * 1ブロック = 約 21ms（48kHz）。
         * RTA の FFT（8192点）は 4ブロック分を溜めてから解析する。
         */
        const val DEFAULT_BLOCK_SIZE = 1024

        private const val BYTES_PER_FLOAT = 4
        private const val BUFFER_BLOCK_COUNT = 8
        private const val THREAD_JOIN_TIMEOUT_MS = 500L
    }
}
