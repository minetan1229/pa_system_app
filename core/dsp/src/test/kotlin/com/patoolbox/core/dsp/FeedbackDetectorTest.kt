package com.patoolbox.core.dsp

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FeedbackDetectorTest {

    private val fftSize = 8192

    private fun detector() = FeedbackDetector(TEST_SAMPLE_RATE, fftSize)

    /** 信号をフレームに切って流し、最後の結果を返す。 */
    private fun run(signal: FloatArray, detector: FeedbackDetector): List<FeedbackDetector.Candidate> {
        var result = emptyList<FeedbackDetector.Candidate>()
        var offset = 0
        while (offset + fftSize <= signal.size) {
            result = detector.process(signal.copyOfRange(offset, offset + fftSize))
            offset += fftSize / 2
        }
        return result
    }

    /** ノイズに正弦を重ねる。ハウリングが乗った状態の模擬 */
    private fun noiseWithTone(toneHz: Double, toneLevelDbFs: Double): FloatArray {
        val length = TEST_SAMPLE_RATE
        val noise = PinkNoiseSource(TEST_SAMPLE_RATE, levelDbFs = -30.0, seed = 5).render(length)
        val tone = sineAtLevel(toneHz, toneLevelDbFs, length)
        return FloatArray(length) { noise[it] + tone[it] }
    }

    @Test
    fun `ノイズだけでは検出しない`() {
        val noise = PinkNoiseSource(TEST_SAMPLE_RATE, levelDbFs = -30.0, seed = 3)
            .render(TEST_SAMPLE_RATE)

        assertThat(run(noise, detector())).isEmpty()
    }

    @Test
    fun `無音では検出しない`() {
        assertThat(run(FloatArray(TEST_SAMPLE_RATE), detector())).isEmpty()
    }

    @Test
    fun `鳴り続ける正弦を検出する`() {
        val result = run(noiseWithTone(2000.0, -12.0), detector())

        assertThat(result).isNotEmpty()
        assertThat(result.first().frequencyHz).isWithin(15.0).of(2000.0)
    }

    @Test
    fun `低い周波数でも検出する`() {
        val result = run(noiseWithTone(160.0, -12.0), detector())

        assertThat(result).isNotEmpty()
        assertThat(result.first().frequencyHz).isWithin(10.0).of(160.0)
    }

    @Test
    fun `高い周波数でも検出する`() {
        val result = run(noiseWithTone(6300.0, -12.0), detector())

        assertThat(result).isNotEmpty()
        assertThat(result.first().frequencyHz).isWithin(30.0).of(6300.0)
    }

    @Test
    fun `検出範囲外は無視する`() {
        // 既定は 80Hz〜10kHz
        val result = run(noiseWithTone(40.0, -12.0), detector())

        result.forEach { assertThat(it.frequencyHz).isAtLeast(70.0) }
    }

    @Test
    fun `1フレームだけでは検出しない`() {
        // 継続していないものはハウリングではない
        val detector = detector()
        val signal = noiseWithTone(2000.0, -12.0)

        val firstFrame = detector.process(signal.copyOfRange(0, fftSize))

        assertThat(firstFrame).isEmpty()
    }

    @Test
    fun `本当に埋もれている成分は拾わない`() {
        // ノイズは全帯域に広がるのでビンあたりの密度は低い。
        // -30dBFS のピンクノイズに対して -45dBFS の正弦でも 16dB 突出して検出される
        // （ハウリングは小さいうちから見つけたいので、これは意図どおり）。
        // 閾値を下回るのは -60dBFS まで落としたとき。
        assertThat(run(noiseWithTone(2000.0, -60.0), detector())).isEmpty()
    }

    @Test
    fun `小さい段階のハウリングも見つける`() {
        // 大きくなってからでは遅いので、埋もれる直前のレベルでも拾えること
        val result = run(noiseWithTone(2000.0, -45.0), detector())

        assertThat(result).isNotEmpty()
        assertThat(result.first().frequencyHz).isWithin(15.0).of(2000.0)
    }

    @Test
    fun `突出量が返る`() {
        val result = run(noiseWithTone(2000.0, -12.0), detector())

        assertThat(result.first().prominenceDb).isAtLeast(12.0)
        assertThat(result.first().sustainedFrames).isAtLeast(4)
    }

    @Test
    fun `隣接ビンの重複を1つにまとめる`() {
        // 窓の広がりで数ビンにまたがるが、報告は1件であってほしい
        val result = run(noiseWithTone(2000.0, -10.0), detector())

        val around2k = result.filter { it.frequencyHz in 1900.0..2100.0 }
        assertThat(around2k).hasSize(1)
    }

    @Test
    fun `2箇所で鳴っていれば2件返す`() {
        val length = TEST_SAMPLE_RATE
        val noise = PinkNoiseSource(TEST_SAMPLE_RATE, levelDbFs = -30.0, seed = 5).render(length)
        val toneA = sineAtLevel(500.0, -12.0, length)
        val toneB = sineAtLevel(3150.0, -12.0, length)
        val mixed = FloatArray(length) { noise[it] + toneA[it] + toneB[it] }

        val result = run(mixed, detector())

        assertThat(result.size).isAtLeast(2)
        assertThat(result.any { kotlin.math.abs(it.frequencyHz - 500.0) < 15.0 }).isTrue()
        assertThat(result.any { kotlin.math.abs(it.frequencyHz - 3150.0) < 25.0 }).isTrue()
    }

    @Test
    fun `突出量の大きい順に返る`() {
        val length = TEST_SAMPLE_RATE
        val noise = PinkNoiseSource(TEST_SAMPLE_RATE, levelDbFs = -30.0, seed = 5).render(length)
        val weak = sineAtLevel(500.0, -20.0, length)
        val strong = sineAtLevel(3150.0, -6.0, length)
        val mixed = FloatArray(length) { noise[it] + weak[it] + strong[it] }

        val result = run(mixed, detector())

        assertThat(result.first().frequencyHz).isWithin(25.0).of(3150.0)
    }

    @Test
    fun `帯域ラベルと音名が付く`() {
        val result = run(noiseWithTone(1000.0, -12.0), detector())
        val candidate = result.first()

        assertThat(candidate.bandLabel).isEqualTo("1k")
        assertThat(candidate.noteName).isNotEmpty()
    }

    @Test
    fun `resetで検出状態が消える`() {
        val detector = detector()
        run(noiseWithTone(2000.0, -12.0), detector)

        detector.reset()
        val afterReset = detector.process(
            PinkNoiseSource(TEST_SAMPLE_RATE, levelDbFs = -30.0, seed = 9).render(fftSize),
        )

        assertThat(afterReset).isEmpty()
    }
}
