package com.patoolbox.core.dsp

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BiquadTest {

    /** 1極ローパス 1/(s + w) を双一次変換したもの。 */
    private fun onePoleLowpass(cutoffHz: Double): Biquad {
        val w = 2.0 * Math.PI * cutoffHz
        return bilinearTransform(0.0, 0.0, w, 0.0, 1.0, w, TEST_SAMPLE_RATE)
    }

    @Test
    fun `1極ローパスはカットオフで3dB落ちる`() {
        val filter = onePoleLowpass(1000.0)

        assertThat(amplitudeToDb(filter.magnitudeAt(1000.0, TEST_SAMPLE_RATE)))
            .isWithin(0.05)
            .of(-3.01)
    }

    @Test
    fun `1極ローパスは直流を通す`() {
        val filter = onePoleLowpass(1000.0)

        assertThat(filter.magnitudeAt(0.0, TEST_SAMPLE_RATE)).isWithin(0.001).of(1.0)
    }

    @Test
    fun `1極ローパスは1オクターブごとに6dB落ちる`() {
        val filter = onePoleLowpass(100.0)

        val at1k = amplitudeToDb(filter.magnitudeAt(1000.0, TEST_SAMPLE_RATE))
        val at2k = amplitudeToDb(filter.magnitudeAt(2000.0, TEST_SAMPLE_RATE))

        assertThat(at1k - at2k).isWithin(0.3).of(6.02)
    }

    @Test
    fun `解析的な振幅特性と時間波形の実測が一致する`() {
        val cutoff = 500.0
        val testHz = 2000.0
        val filter = onePoleLowpass(cutoff)

        val input = sineAtLevel(testHz, levelDbFs = -20.0, lengthSamples = TEST_SAMPLE_RATE)
        val output = FloatArray(input.size) { filter.process(input[it].toDouble()).toFloat() }

        // 立ち上がりを除いた後半で比較
        val tail = output.copyOfRange(output.size / 2, output.size)
        val measured = levelDbFs(tail) - (-20.0)
        val predicted = amplitudeToDb(
            onePoleLowpass(cutoff).magnitudeAt(testHz, TEST_SAMPLE_RATE),
        )

        assertThat(measured).isWithin(0.1).of(predicted)
    }

    @Test
    fun `resetで内部状態が消える`() {
        val filter = onePoleLowpass(1000.0)
        repeat(100) { filter.process(1.0) }

        filter.reset()

        // 状態が残っていれば最初の出力が 0 から離れる
        assertThat(filter.process(0.0)).isWithin(1e-12).of(0.0)
    }

    @Test
    fun `カスケードは各段のゲインの積になる`() {
        val single = onePoleLowpass(1000.0)
        val cascade = BiquadCascade(listOf(onePoleLowpass(1000.0), onePoleLowpass(1000.0)))

        val singleDb = amplitudeToDb(single.magnitudeAt(4000.0, TEST_SAMPLE_RATE))
        val cascadeDb = amplitudeToDb(cascade.magnitudeAt(4000.0, TEST_SAMPLE_RATE))

        assertThat(cascadeDb).isWithin(0.001).of(singleDb * 2)
    }

    @Test
    fun `空のカスケードは素通し`() {
        val cascade = BiquadCascade(emptyList())

        assertThat(cascade.isEmpty).isTrue()
        assertThat(cascade.process(0.5)).isEqualTo(0.5)
        assertThat(cascade.magnitudeAt(1000.0, TEST_SAMPLE_RATE)).isEqualTo(1.0)
    }
}
