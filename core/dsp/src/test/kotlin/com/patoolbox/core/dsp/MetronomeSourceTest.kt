package com.patoolbox.core.dsp

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

class MetronomeSourceTest {

    /**
     * クリックの開始位置。
     * クリックはサイン波なので中でゼロを何度も横切る。単純なゼロ判定では
     * 半周期ごとに「開始」と誤検出するため、一定長の無音が続いた後だけを開始とみなす。
     */
    private fun clickStarts(signal: FloatArray, minSilenceSamples: Int = 200): List<Int> {
        val starts = mutableListOf<Int>()
        var silentRun = minSilenceSamples
        signal.forEachIndexed { index, value ->
            if (abs(value) < 1e-6f) {
                silentRun++
            } else {
                if (silentRun >= minSilenceSamples) starts += index
                silentRun = 0
            }
        }
        return starts
    }

    @Test
    fun `120BPMなら2秒で4拍`() {
        val source = MetronomeSource(TEST_SAMPLE_RATE, bpm = 120.0)
        val signal = source.render(TEST_SAMPLE_RATE * 2)

        assertThat(clickStarts(signal)).hasSize(4)
    }

    @Test
    fun `クリックの間隔がBPMどおり`() {
        val source = MetronomeSource(TEST_SAMPLE_RATE, bpm = 100.0)
        val signal = source.render(TEST_SAMPLE_RATE * 3)

        val starts = clickStarts(signal)
        val expectedInterval = 60.0 / 100.0 * TEST_SAMPLE_RATE

        starts.zipWithNext().forEach { (a, b) ->
            assertThat((b - a).toDouble()).isWithin(2.0).of(expectedInterval)
        }
    }

    @Test
    fun `小節の頭だけ音が高い`() {
        val source = MetronomeSource(TEST_SAMPLE_RATE, bpm = 120.0, beatsPerBar = 4)
        val signal = source.render(TEST_SAMPLE_RATE * 2)
        val starts = clickStarts(signal)

        val analyzer = SpectrumAnalyzer(TEST_SAMPLE_RATE, fftSize = 1024)
        fun dominantHz(from: Int): Double {
            val block = signal.copyOfRange(from, from + 1024)
            val spectrum = analyzer.powerSpectrum(block)
            return analyzer.interpolatedPeakHz(spectrum, analyzer.peakBin(spectrum))
        }

        // 1拍目はアクセント（1600Hz）、2拍目は通常（1000Hz）
        assertThat(dominantHz(starts[0])).isGreaterThan(1300.0)
        assertThat(dominantHz(starts[1])).isLessThan(1300.0)
    }

    @Test
    fun `アクセントを切ると全部同じ音になる`() {
        val source = MetronomeSource(
            TEST_SAMPLE_RATE,
            bpm = 120.0,
            accentFirstBeat = false,
        )
        val signal = source.render(TEST_SAMPLE_RATE)
        val starts = clickStarts(signal)

        val analyzer = SpectrumAnalyzer(TEST_SAMPLE_RATE, fftSize = 1024)
        starts.take(2).forEach { start ->
            val block = signal.copyOfRange(start, start + 1024)
            val spectrum = analyzer.powerSpectrum(block)
            val hz = analyzer.interpolatedPeakHz(spectrum, analyzer.peakBin(spectrum))
            assertThat(hz).isLessThan(1300.0)
        }
    }

    @Test
    fun `拍のカウンタが進む`() {
        val source = MetronomeSource(TEST_SAMPLE_RATE, bpm = 120.0, beatsPerBar = 3)
        source.render(TEST_SAMPLE_RATE * 2)

        assertThat(source.beatCounter).isEqualTo(4)
        // 4拍目 = 小節内の位置は 3 で割った余り
        assertThat(source.currentBeat).isEqualTo(0)
    }

    @Test
    fun `resetで先頭に戻る`() {
        val source = MetronomeSource(TEST_SAMPLE_RATE, bpm = 120.0)
        source.render(TEST_SAMPLE_RATE)

        source.reset()

        assertThat(source.beatCounter).isEqualTo(0)
        assertThat(source.currentBeat).isEqualTo(0)
    }

    @Test
    fun `クリックのレベルが指定どおり`() {
        val source = MetronomeSource(TEST_SAMPLE_RATE, bpm = 120.0, levelDbFs = -12.0)
        val signal = source.render(TEST_SAMPLE_RATE)

        // 減衰する短いバーストなのでピークで確認する
        assertThat(amplitudeToDb(peakAmplitude(signal))).isWithin(0.5).of(-12.0 + 3.01)
    }
}
