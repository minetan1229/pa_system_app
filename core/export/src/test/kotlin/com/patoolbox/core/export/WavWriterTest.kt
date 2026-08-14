package com.patoolbox.core.export

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavWriterTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun ByteArray.ascii(from: Int, length: Int) =
        String(this, from, length, Charsets.US_ASCII)

    private fun ByteArray.int32(offset: Int) =
        ByteBuffer.wrap(this, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int

    private fun ByteArray.int16(offset: Int) =
        ByteBuffer.wrap(this, offset, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()

    @Test
    fun `ヘッダはRIFFとWAVEで始まる`() {
        val header = WavHeader.build(48000, 1, WavFormat.PCM_16, dataBytes = 1000)

        assertThat(header).hasLength(WavHeader.SIZE)
        assertThat(header.ascii(0, 4)).isEqualTo("RIFF")
        assertThat(header.ascii(8, 4)).isEqualTo("WAVE")
        assertThat(header.ascii(12, 4)).isEqualTo("fmt ")
        assertThat(header.ascii(36, 4)).isEqualTo("data")
    }

    @Test
    fun `長さはリトルエンディアンで入る`() {
        // ここを間違えると「再生はできるが長さがおかしい」という
        // 気づきにくい壊れ方をする
        val header = WavHeader.build(48000, 1, WavFormat.PCM_16, dataBytes = 1000)

        assertThat(header.int32(WavHeader.RIFF_SIZE_OFFSET)).isEqualTo(WavHeader.SIZE - 8 + 1000)
        assertThat(header.int32(WavHeader.DATA_SIZE_OFFSET)).isEqualTo(1000)
    }

    @Test
    fun `16bitのfmtチャンクが規格どおり`() {
        val header = WavHeader.build(48000, 1, WavFormat.PCM_16, dataBytes = 0)

        assertThat(header.int32(16)).isEqualTo(16) // fmt チャンクの長さ
        assertThat(header.int16(20)).isEqualTo(1) // リニアPCM
        assertThat(header.int16(22)).isEqualTo(1) // モノラル
        assertThat(header.int32(24)).isEqualTo(48000)
        assertThat(header.int32(28)).isEqualTo(48000 * 2) // byteRate
        assertThat(header.int16(32)).isEqualTo(2) // blockAlign
        assertThat(header.int16(34)).isEqualTo(16)
    }

    @Test
    fun `32bit floatはフォーマットコード3になる`() {
        val header = WavHeader.build(48000, 1, WavFormat.FLOAT_32, dataBytes = 0)

        assertThat(header.int16(20)).isEqualTo(3)
        assertThat(header.int16(34)).isEqualTo(32)
        assertThat(header.int32(28)).isEqualTo(48000 * 4)
    }

    @Test
    fun `ステレオのbyteRateは倍になる`() {
        val header = WavHeader.build(48000, 2, WavFormat.PCM_16, dataBytes = 0)

        assertThat(header.int16(22)).isEqualTo(2)
        assertThat(header.int16(32)).isEqualTo(4)
        assertThat(header.int32(28)).isEqualTo(48000 * 4)
    }

    @Test
    fun `不正な引数は拒否する`() {
        assertThat(
            runCatching { WavHeader.build(0, 1, WavFormat.PCM_16, 0) }.isFailure,
        ).isTrue()
        assertThat(
            runCatching { WavHeader.build(48000, 0, WavFormat.PCM_16, 0) }.isFailure,
        ).isTrue()
    }

    @Test
    fun `閉じたあとのヘッダに実際の長さが入る`() {
        // 録音中は全体長が分からないので0で書いておき、close で書き直す。
        // ここが抜けると再生できないファイルが残る
        val file = folder.newFile("take.wav")
        val samples = FloatArray(1000) { 0f }

        WavFileWriter(file, sampleRate = 48000).use { it.write(samples) }

        val bytes = file.readBytes()
        assertThat(bytes.int32(WavHeader.DATA_SIZE_OFFSET)).isEqualTo(2000)
        assertThat(bytes.int32(WavHeader.RIFF_SIZE_OFFSET)).isEqualTo(WavHeader.SIZE - 8 + 2000)
        assertThat(file.length()).isEqualTo(WavHeader.SIZE + 2000L)
    }

    @Test
    fun `書いた波形が読み返せる`() {
        val file = folder.newFile("tone.wav")
        val input = floatArrayOf(0f, 0.5f, -0.5f, 1f, -1f)

        WavFileWriter(file, sampleRate = 48000).use { it.write(input) }

        val bytes = file.readBytes()
        val read = FloatArray(input.size) { i ->
            bytes.int16(WavHeader.SIZE + i * 2).toShort() / 32768f
        }
        // -1.0 は正確に、+1.0 は 32767/32768 に収まる
        assertThat(read[0]).isWithin(1e-4f).of(0f)
        assertThat(read[1]).isWithin(1e-4f).of(0.5f)
        assertThat(read[2]).isWithin(1e-4f).of(-0.5f)
        assertThat(read[3]).isWithin(1e-4f).of(1f)
        assertThat(read[4]).isWithin(1e-9f).of(-1f)
    }

    @Test
    fun `32bit floatは値がそのまま残る`() {
        val file = folder.newFile("float.wav")
        val input = floatArrayOf(0.123456f, -0.987654f, 1.5f)

        WavFileWriter(file, 48000, WavFormat.FLOAT_32).use { it.write(input) }

        val bytes = file.readBytes()
        val read = FloatArray(input.size) { i ->
            Float.fromBits(bytes.int32(WavHeader.SIZE + i * 4))
        }
        assertThat(read.toList()).containsExactly(0.123456f, -0.987654f, 1.5f).inOrder()
    }

    @Test
    fun `過大入力は潰して記録する`() {
        // 巻き返す（wrap する）と大音量が小さい音に化けて、聞くまで気づけない。
        // 潰れていれば波形を見た時点で分かる
        val file = folder.newFile("clip.wav")

        WavFileWriter(file, 48000).use { it.write(floatArrayOf(3f, -3f)) }

        val bytes = file.readBytes()
        assertThat(bytes.int16(WavHeader.SIZE).toShort()).isEqualTo(32767.toShort())
        assertThat(bytes.int16(WavHeader.SIZE + 2).toShort()).isEqualTo((-32768).toShort())
    }

    @Test
    fun `ピークと長さを記録する`() {
        val file = folder.newFile("meta.wav")

        val writer = WavFileWriter(file, sampleRate = 48000)
        writer.use {
            it.write(FloatArray(48000) { 0.1f })
            it.write(floatArrayOf(0.8f, -0.9f))
        }

        assertThat(writer.sampleCount).isEqualTo(48002)
        assertThat(writer.peakAmplitude).isWithin(1e-6f).of(0.9f)
        assertThat(writer.durationSeconds).isWithin(0.001).of(1.0000417)
    }

    @Test
    fun `長さ0の録音でも壊れたファイルにしない`() {
        val file = folder.newFile("empty.wav")

        WavFileWriter(file, sampleRate = 48000).close()

        val bytes = file.readBytes()
        assertThat(bytes).hasLength(WavHeader.SIZE)
        assertThat(bytes.int32(WavHeader.DATA_SIZE_OFFSET)).isEqualTo(0)
    }

    @Test
    fun `複数回に分けて書いても連続する`() {
        val file = folder.newFile("chunks.wav")

        WavFileWriter(file, sampleRate = 48000).use { writer ->
            repeat(10) { writer.write(FloatArray(100) { 0.25f }) }
        }

        assertThat(file.length()).isEqualTo(WavHeader.SIZE + 2000L)
    }

    @Test
    fun `書いたヘッダを読み返せる`() {
        val header = WavHeader.build(44100, 2, WavFormat.FLOAT_32, dataBytes = 800)

        val info = WavHeader.read(header)

        assertThat(info).isNotNull()
        assertThat(info!!.sampleRate).isEqualTo(44100)
        assertThat(info.channels).isEqualTo(2)
        assertThat(info.format).isEqualTo(WavFormat.FLOAT_32)
        assertThat(info.sampleCount).isEqualTo(100)
        assertThat(info.durationSeconds).isWithin(1e-9).of(100.0 / 44100)
    }

    @Test
    fun `WAVでないものは読めないと返す`() {
        // 読めたふりをして違う位置から再生する方が害が大きい
        val notWav = ByteArray(WavHeader.SIZE) { 0 }

        assertThat(WavHeader.read(notWav)).isNull()
        assertThat(WavHeader.read(ByteArray(10))).isNull()
    }

    @Test
    fun `録音したファイルのヘッダから長さが分かる`() {
        val file = folder.newFile("read.wav")
        WavFileWriter(file, sampleRate = 48000).use { it.write(FloatArray(24000)) }

        val info = WavHeader.read(file.readBytes().copyOf(WavHeader.SIZE))

        assertThat(info!!.durationSeconds).isWithin(1e-6).of(0.5)
    }

    @Test
    fun `既にあるファイルは上書きする`() {
        val file: File = folder.newFile("reuse.wav")
        file.writeBytes(ByteArray(9999))

        WavFileWriter(file, sampleRate = 48000).use { it.write(FloatArray(10)) }

        assertThat(file.length()).isEqualTo(WavHeader.SIZE + 20L)
    }
}
