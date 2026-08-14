package com.patoolbox.core.export

import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile

/**
 * WAV の標本形式。
 *
 * 既定は16bit。32bit float は録音時のヘッドルームをそのまま残せるが、
 * ファイルが倍になるうえ、再生できないアプリや取り込めない編集ソフトがある。
 * 現場で撮った音を「そのまま人に渡せる」ことを優先して16bitを既定にした。
 */
enum class WavFormat(val code: Int, val bitsPerSample: Int) {
    /** リニアPCM 16bit。どこでも再生できる */
    PCM_16(1, 16),

    /** IEEE float 32bit。0dBFS を超えた成分も保持する */
    FLOAT_32(3, 32),
    ;

    val bytesPerSample: Int get() = bitsPerSample / 8
}

/**
 * WAV（RIFF）のヘッダ。
 *
 * 44バイト固定の最小構成。RIFF は **リトルエンディアン** なので、
 * Java の既定（ビッグエンディアン）とは逆になる。ここを間違えると
 * 「再生できるが長さがおかしい」という気づきにくい壊れ方をする。
 */
object WavHeader {

    const val SIZE = 44

    /**
     * @param dataBytes 標本データのバイト数。書き終わるまで分からないので、
     *   ストリーミング時は0で書いておいて最後に書き直す（[WavFileWriter]）
     */
    fun build(
        sampleRate: Int,
        channels: Int,
        format: WavFormat,
        dataBytes: Int,
    ): ByteArray {
        require(sampleRate > 0) { "サンプリング周波数が不正: $sampleRate" }
        require(channels > 0) { "チャンネル数が不正: $channels" }
        require(dataBytes >= 0) { "データ長が不正: $dataBytes" }

        val blockAlign = channels * format.bytesPerSample
        val byteRate = sampleRate * blockAlign
        val header = ByteArray(SIZE)
        var offset = 0

        fun ascii(text: String) {
            for (char in text) header[offset++] = char.code.toByte()
        }

        fun int32(value: Int) {
            header[offset++] = (value and 0xFF).toByte()
            header[offset++] = ((value ushr 8) and 0xFF).toByte()
            header[offset++] = ((value ushr 16) and 0xFF).toByte()
            header[offset++] = ((value ushr 24) and 0xFF).toByte()
        }

        fun int16(value: Int) {
            header[offset++] = (value and 0xFF).toByte()
            header[offset++] = ((value ushr 8) and 0xFF).toByte()
        }

        ascii("RIFF")
        // ファイル全体からこのフィールドまでの8バイトを引いた長さ
        int32(SIZE - 8 + dataBytes)
        ascii("WAVE")

        ascii("fmt ")
        int32(FMT_CHUNK_SIZE)
        int16(format.code)
        int16(channels)
        int32(sampleRate)
        int32(byteRate)
        int16(blockAlign)
        int16(format.bitsPerSample)

        ascii("data")
        int32(dataBytes)

        return header
    }

    /**
     * 先頭44バイトから内容を読む。
     *
     * このアプリが書いた最小構成の WAV だけを想定している。
     * 追加チャンクを持つ一般の WAV は読めないので null を返す
     * （読めたふりをして違う位置から再生する方が害が大きい）。
     */
    fun read(header: ByteArray): WavInfo? {
        if (header.size < SIZE) return null
        if (ascii(header, 0, 4) != "RIFF") return null
        if (ascii(header, 8, 4) != "WAVE") return null
        if (ascii(header, 12, 4) != "fmt ") return null
        if (ascii(header, 36, 4) != "data") return null

        val code = int16(header, 20)
        val bits = int16(header, 34)
        val format = WavFormat.entries.firstOrNull {
            it.code == code && it.bitsPerSample == bits
        } ?: return null

        return WavInfo(
            sampleRate = int32(header, 24),
            channels = int16(header, 22),
            format = format,
            dataBytes = int32(header, DATA_SIZE_OFFSET),
        )
    }

    private fun ascii(bytes: ByteArray, from: Int, length: Int) =
        String(bytes, from, length, Charsets.US_ASCII)

    private fun int32(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)

    private fun int16(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)

    /** RIFF サイズ（8バイト目）とデータ長（40バイト目）の位置。書き直しに使う */
    const val RIFF_SIZE_OFFSET = 4
    const val DATA_SIZE_OFFSET = 40

    private const val FMT_CHUNK_SIZE = 16
}

data class WavInfo(
    val sampleRate: Int,
    val channels: Int,
    val format: WavFormat,
    val dataBytes: Int,
) {
    val sampleCount: Int get() = dataBytes / (format.bytesPerSample * channels.coerceAtLeast(1))
    val durationSeconds: Double
        get() = if (sampleRate > 0) sampleCount.toDouble() / sampleRate else 0.0
}

/**
 * WAV をストリーミングで書く。
 *
 * ヘッダには全体長が入るが、録音中はそれが分からない。0を書いておき、
 * [close] で先頭に戻って書き直す。そのため追記のみのストリームではなく
 * シーク可能なファイルを直接扱う。
 *
 * **[close] を呼ばないとヘッダの長さが0のままになり、再生できないファイルが残る。**
 * 録音の停止経路（利用者の操作・画面破棄・エラー）すべてで必ず通すこと。
 */
class WavFileWriter(
    file: File,
    private val sampleRate: Int,
    private val format: WavFormat = WavFormat.PCM_16,
    private val channels: Int = 1,
) : Closeable {

    private val output = RandomAccessFile(file, "rw")
    private var dataBytes = 0

    /** 書き込んだ標本数。表示する録音時間の根拠 */
    var sampleCount: Int = 0
        private set

    /** 全体を通した絶対値の最大。過大入力の判定に使う */
    var peakAmplitude: Float = 0f
        private set

    /** 変換用のバッファ。毎ブロック確保しない */
    private var scratch = ByteArray(0)

    init {
        output.setLength(0)
        output.write(WavHeader.build(sampleRate, channels, format, dataBytes = 0))
    }

    val durationSeconds: Double get() = sampleCount.toDouble() / sampleRate

    fun write(buffer: FloatArray, length: Int = buffer.size) {
        if (length <= 0) return
        val needed = length * format.bytesPerSample
        if (scratch.size < needed) scratch = ByteArray(needed)

        var offset = 0
        for (i in 0 until length) {
            val sample = buffer[i]
            val magnitude = if (sample < 0f) -sample else sample
            if (magnitude > peakAmplitude) peakAmplitude = magnitude

            when (format) {
                WavFormat.PCM_16 -> {
                    // ±1.0 を超える入力はここで潰れる。潰さずに巻き返すと
                    // 大音量が「小さい音」に化けて、聞くまで気づけない
                    val clamped = sample.coerceIn(-1f, 1f)
                    val value = (clamped * PCM_16_SCALE).toInt().coerceIn(-32768, 32767)
                    scratch[offset++] = (value and 0xFF).toByte()
                    scratch[offset++] = ((value shr 8) and 0xFF).toByte()
                }

                WavFormat.FLOAT_32 -> {
                    val bits = sample.toRawBits()
                    scratch[offset++] = (bits and 0xFF).toByte()
                    scratch[offset++] = ((bits ushr 8) and 0xFF).toByte()
                    scratch[offset++] = ((bits ushr 16) and 0xFF).toByte()
                    scratch[offset++] = ((bits ushr 24) and 0xFF).toByte()
                }
            }
        }

        output.write(scratch, 0, needed)
        dataBytes += needed
        sampleCount += length
    }

    /** ヘッダの長さを書き直して閉じる。 */
    override fun close() {
        try {
            output.seek(WavHeader.RIFF_SIZE_OFFSET.toLong())
            writeLittleEndianInt(WavHeader.SIZE - 8 + dataBytes)
            output.seek(WavHeader.DATA_SIZE_OFFSET.toLong())
            writeLittleEndianInt(dataBytes)
        } finally {
            output.close()
        }
    }

    private fun writeLittleEndianInt(value: Int) {
        output.write(
            byteArrayOf(
                (value and 0xFF).toByte(),
                ((value ushr 8) and 0xFF).toByte(),
                ((value ushr 16) and 0xFF).toByte(),
                ((value ushr 24) and 0xFF).toByte(),
            ),
        )
    }

    private companion object {
        /**
         * 32767 ではなく 32768 を掛ける。-1.0 が -32768 に正確に対応し、
         * +1.0 側は coerceIn で 32767 に収まる。32767 を掛けると
         * 振幅が 1/32768 だけ小さくなる（実害は無いが、往復で値が合わなくなる）
         */
        const val PCM_16_SCALE = 32768f
    }
}
