package com.patoolbox.core.dsp

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

class SignalSourcesTest {

    private val analyzer = SpectrumAnalyzer(TEST_SAMPLE_RATE, fftSize = 8192)

    @Test
    fun `dBFSの規約が解析側と一致する`() {
        // フルスケールのサイン（振幅1.0）は -3.01 dBFS
        assertThat(sineAmplitudeFor(-3.01)).isWithin(0.001).of(1.0)
        assertThat(rmsFor(0.0)).isWithin(0.001).of(1.0)
    }

    @Test
    fun `サイン波のレベルが指定どおり`() {
        val signal = SineSource(TEST_SAMPLE_RATE, frequencyHz = 1000.0, levelDbFs = -20.0)
            .render(TEST_SAMPLE_RATE)

        assertThat(levelDbFs(signal)).isWithin(0.05).of(-20.0)
    }

    @Test
    fun `サイン波の周波数が指定どおり`() {
        val signal = SineSource(TEST_SAMPLE_RATE, frequencyHz = 440.0).render(8192)
        val spectrum = analyzer.powerSpectrum(signal)

        val peakHz = analyzer.interpolatedPeakHz(spectrum, analyzer.peakBin(spectrum))
        assertThat(peakHz).isWithin(1.0).of(440.0)
    }

    @Test
    fun `矩形波のレベルが指定どおり`() {
        val signal = SquareSource(TEST_SAMPLE_RATE, frequencyHz = 1000.0, levelDbFs = -20.0)
            .render(TEST_SAMPLE_RATE)

        assertThat(levelDbFs(signal)).isWithin(0.1).of(-20.0)
    }

    @Test
    fun `白色ノイズのレベルが指定どおり`() {
        val signal = WhiteNoiseSource(TEST_SAMPLE_RATE, levelDbFs = -20.0, seed = 5)
            .render(TEST_SAMPLE_RATE)

        assertThat(levelDbFs(signal)).isWithin(0.3).of(-20.0)
    }

    @Test
    fun `ピンクノイズのレベルが指定どおり`() {
        val signal = PinkNoiseSource(TEST_SAMPLE_RATE, levelDbFs = -20.0, seed = 5)
            .render(TEST_SAMPLE_RATE * 2)

        assertThat(levelDbFs(signal)).isWithin(0.6).of(-20.0)
    }

    @Test
    fun `ピンクノイズは白色ノイズより低域寄り`() {
        val pink = PinkNoiseSource(TEST_SAMPLE_RATE, levelDbFs = -20.0, seed = 5).render(8192)
        val white = WhiteNoiseSource(TEST_SAMPLE_RATE, levelDbFs = -20.0, seed = 5).render(8192)

        val pinkSpectrum = analyzer.powerSpectrum(pink).copyOf()
        val whiteSpectrum = analyzer.powerSpectrum(white)

        // 100Hz 付近と 10kHz 付近の比を比べる
        fun ratio(spectrum: DoubleArray): Double {
            val lowBin = (100.0 / analyzer.binWidthHz).toInt()
            val highBin = (10000.0 / analyzer.binWidthHz).toInt()
            val low = (lowBin - 3..lowBin + 3).sumOf { spectrum[it] }
            val high = (highBin - 3..highBin + 3).sumOf { spectrum[it] }
            return powerToDb(low) - powerToDb(high)
        }

        assertThat(ratio(pinkSpectrum)).isGreaterThan(ratio(whiteSpectrum) + 10.0)
    }

    @Test
    fun `同じシードなら同じ波形になる`() {
        val a = WhiteNoiseSource(TEST_SAMPLE_RATE, seed = 99).render(4096)
        val b = WhiteNoiseSource(TEST_SAMPLE_RATE, seed = 99).render(4096)

        assertThat(a).isEqualTo(b)
    }

    @Test
    fun `resetで同じ波形が再生される`() {
        val source = WhiteNoiseSource(TEST_SAMPLE_RATE, seed = 99)
        val first = source.render(4096)
        source.reset()
        val second = source.render(4096)

        assertThat(first).isEqualTo(second)
    }

    @Test
    fun `デュアルトーンは2つの成分を含む`() {
        val signal = DualToneSource(
            TEST_SAMPLE_RATE,
            lowHz = 250.0,
            highHz = 8000.0,
            levelDbFs = -20.0,
        ).render(8192)

        val spectrum = analyzer.powerSpectrum(signal)
        fun toneDbAt(hz: Double): Double {
            val bin = (hz / analyzer.binWidthHz).toInt()
            val peak = analyzer.peakBin(spectrum, bin - 4, bin + 4)
            return powerToDb(analyzer.toneMeanSquareAround(spectrum, peak))
        }

        // 各音は合計より 3dB 下
        assertThat(toneDbAt(250.0)).isWithin(0.5).of(-23.01)
        assertThat(toneDbAt(8000.0)).isWithin(0.5).of(-23.01)
        assertThat(levelDbFs(signal)).isWithin(0.3).of(-20.0)
    }

    @Test
    fun `対数スイープは開始と終了の周波数を通る`() {
        val duration = 2.0
        val source = LogSweepSource(
            TEST_SAMPLE_RATE,
            startHz = 100.0,
            endHz = 10000.0,
            durationSeconds = duration,
            levelDbFs = -20.0,
        )
        val signal = source.render((TEST_SAMPLE_RATE * duration).toInt())

        // 先頭ブロックの支配周波数は 100Hz 付近、末尾は 10kHz 付近
        val head = analyzer.powerSpectrum(signal.copyOfRange(0, 8192)).copyOf()
        val headHz = analyzer.interpolatedPeakHz(head, analyzer.peakBin(head))
        assertThat(headHz).isLessThan(300.0)

        val tail = analyzer.powerSpectrum(signal.copyOfRange(signal.size - 8192, signal.size))
        val tailHz = analyzer.interpolatedPeakHz(tail, analyzer.peakBin(tail))
        assertThat(tailHz).isGreaterThan(6000.0)

        assertThat(source.isFinished).isTrue()
    }

    @Test
    fun `リニアスイープは周波数が等間隔に上がる`() {
        val duration = 2.0
        val signal = LinearSweepSource(
            TEST_SAMPLE_RATE,
            startHz = 1000.0,
            endHz = 5000.0,
            durationSeconds = duration,
        ).render((TEST_SAMPLE_RATE * duration).toInt())

        // 中間地点では中央の周波数（3kHz）付近になる
        val middle = signal.size / 2
        val spectrum = analyzer.powerSpectrum(signal.copyOfRange(middle - 4096, middle + 4096))
        val hz = analyzer.interpolatedPeakHz(spectrum, analyzer.peakBin(spectrum))

        assertThat(abs(hz - 3000.0)).isLessThan(300.0)
    }

    @Test
    fun `バーストは休止区間で無音になる`() {
        val source = BurstSource(
            SineSource(TEST_SAMPLE_RATE, frequencyHz = 1000.0, levelDbFs = -20.0),
            onSeconds = 0.1,
            offSeconds = 0.1,
        )
        val signal = source.render(TEST_SAMPLE_RATE) // 1秒 = 5周期

        val zeros = signal.count { it == 0f }
        // 半分が無音になる（サインのゼロ交差ぶんの誤差は無視できる）
        assertThat(zeros).isGreaterThan(signal.size / 2 - 100)
        assertThat(zeros).isLessThan(signal.size / 2 + 100)
    }

    @Test
    fun `ブロック境界をまたいでも位相が連続する`() {
        // ブロックごとに位相をリセットしてしまうと、ここでクリック音が出る
        val source = SineSource(TEST_SAMPLE_RATE, frequencyHz = 997.0, levelDbFs = -20.0)
        val blocked = source.render(8192, blockSize = 333)

        source.reset()
        val single = source.render(8192, blockSize = 8192)

        for (i in blocked.indices) {
            assertThat(abs(blocked[i] - single[i])).isLessThan(1e-5f)
        }
    }
}
