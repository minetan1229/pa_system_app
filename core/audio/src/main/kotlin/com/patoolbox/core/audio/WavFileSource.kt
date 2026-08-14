package com.patoolbox.core.audio

import com.patoolbox.core.dsp.SignalSource
import com.patoolbox.core.export.WavFormat
import com.patoolbox.core.export.WavHeader
import java.io.File
import java.io.RandomAccessFile

/**
 * 録音した WAV を [SignalSource] として読み出す。
 *
 * これにより既存の [AudioPlaybackEngine] をそのまま再生に使える。
 * 再生専用の経路を別に作らずに済み、フェードによるクリック音対策も共有される。
 *
 * 終端に達したら無音を返す。再生を止めるのは呼び出し側の責任で、
 * [isFinished] を見て止める。ここで勝手に止めると、
 * フェードアウトが掛からずブツッと鳴る。
 */
class WavFileSource(
    file: File,
    /** 読み出しに使うブロック長。再生側と揃える必要はない */
    bufferSamples: Int = DEFAULT_BUFFER_SAMPLES,
) : SignalSource, AutoCloseable {

    private val input = RandomAccessFile(file, "r")
    private val info = run {
        val header = ByteArray(WavHeader.SIZE)
        input.readFully(header)
        WavHeader.read(header)
    } ?: error("読めない WAV です: ${file.name}")

    override val sampleRate: Int = info.sampleRate

    val durationSeconds: Double = info.durationSeconds

    private val bytesPerSample = info.format.bytesPerSample
    private var bytes = ByteArray(bufferSamples * bytesPerSample)
    private var consumed = 0

    val isFinished: Boolean get() = consumed >= info.dataBytes

    override fun fill(buffer: FloatArray, length: Int) {
        val needed = length * bytesPerSample
        if (bytes.size < needed) bytes = ByteArray(needed)

        val remaining = info.dataBytes - consumed
        val toRead = minOf(needed, remaining.coerceAtLeast(0))
        val read = if (toRead > 0) {
            input.read(bytes, 0, toRead).coerceAtLeast(0)
        } else {
            0
        }
        consumed += read

        val decoded = read / bytesPerSample
        for (i in 0 until decoded) {
            val offset = i * bytesPerSample
            buffer[i] = when (info.format) {
                WavFormat.PCM_16 -> {
                    val value = (bytes[offset].toInt() and 0xFF) or (bytes[offset + 1].toInt() shl 8)
                    value.toShort() / PCM_16_SCALE
                }

                WavFormat.FLOAT_32 -> Float.fromBits(
                    (bytes[offset].toInt() and 0xFF) or
                        ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                        ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
                        ((bytes[offset + 3].toInt() and 0xFF) shl 24),
                )
            }
        }
        // 終端より後は無音。ここで例外を投げるとオーディオスレッドが落ちる
        for (i in decoded until length) {
            buffer[i] = 0f
        }
    }

    override fun reset() {
        input.seek(WavHeader.SIZE.toLong())
        consumed = 0
    }

    override fun close() {
        input.close()
    }

    private companion object {
        const val DEFAULT_BUFFER_SAMPLES = 2048
        const val PCM_16_SCALE = 32768f
    }
}
