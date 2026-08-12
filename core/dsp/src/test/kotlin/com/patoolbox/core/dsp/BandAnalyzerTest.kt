package com.patoolbox.core.dsp

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * RTA の表示値そのものを検証する。
 * ピンクノイズがフラットに、白色ノイズが +1dB/バンドで出ることが最低条件。
 */
class BandAnalyzerTest {

    private val fftSize = 8192
    private val analyzer = SpectrumAnalyzer(TEST_SAMPLE_RATE, fftSize)
    private val bands = OctaveBands.bands(BandResolution.THIRD)
    private val bandAnalyzer = BandAnalyzer(bands, analyzer.binWidthHz, analyzer.binCount)

    /** 信号全体を重ねながら解析し、バンドごとのパワーを平均する。 */
    private fun averageBandLevels(signal: FloatArray): DoubleArray {
        val accumulated = DoubleArray(bands.size)
        val frame = DoubleArray(bands.size)
        var frames = 0
        var offset = 0
        val hop = fftSize / 2

        while (offset + fftSize <= signal.size) {
            val block = signal.copyOfRange(offset, offset + fftSize)
            bandAnalyzer.bandPowers(analyzer.powerSpectrum(block), frame)
            for (b in bands.indices) accumulated[b] += frame[b]
            frames++
            offset += hop
        }

        return DoubleArray(bands.size) { powerToDb(accumulated[it] / frames) }
    }

    private fun levelAt(levels: DoubleArray, label: String): Double =
        levels[bands.indexOfFirst { it.label == label }]

    @Test
    fun `ピンクノイズは1_3オクターブでフラットになる`() {
        val pink = PinkNoiseSource(TEST_SAMPLE_RATE, levelDbFs = -20.0, seed = 7)
            .render(TEST_SAMPLE_RATE * 8)
        val levels = averageBandLevels(pink)

        // 端の帯域は窓の分解能と Kellet フィルタの適用範囲外なので 50Hz..10kHz で見る
        val inRange = bands.indices.filter {
            bands[it].centerHz in 50.0..10_000.0
        }
        val mean = inRange.map { levels[it] }.average()

        inRange.forEach { index ->
            assertWithMessage("%s Hz 帯", bands[index].label)
                .that(levels[index])
                .isWithin(1.5)
                .of(mean)
        }
    }

    @Test
    fun `白色ノイズは1バンドあたり1dB上がる`() {
        val white = WhiteNoiseSource(TEST_SAMPLE_RATE, levelDbFs = -20.0, seed = 7)
            .render(TEST_SAMPLE_RATE * 4)
        val levels = averageBandLevels(white)

        // 800Hz と 8kHz は 1/3oct で10バンド離れている → 帯域幅が10倍 → +10dB
        val difference = levelAt(levels, "8k") - levelAt(levels, "800")

        assertThat(difference).isWithin(0.5).of(10.0)
    }

    @Test
    fun `純音はその帯域に集まる`() {
        val tone = sineAtLevel(1000.0, levelDbFs = -20.0, lengthSamples = fftSize * 4)
        val levels = averageBandLevels(tone)

        val oneK = levelAt(levels, "1k")
        assertThat(oneK).isWithin(0.5).of(-20.0)

        // 隣の帯域には漏れない
        assertThat(levelAt(levels, "800")).isLessThan(oneK - 20.0)
        assertThat(levelAt(levels, "1.25k")).isLessThan(oneK - 20.0)
    }

    @Test
    fun `純音は全バンドの合計が信号レベルに一致する`() {
        // 帯域内に全エネルギーがある信号なら、バンド合成の合計が元のレベルと一致する
        val tone = sineAtLevel(1000.0, levelDbFs = -15.0, lengthSamples = fftSize * 4)
        val levels = averageBandLevels(tone)

        assertThat(energySumDb(levels)).isWithin(0.3).of(-15.0)
    }

    @Test
    fun `ピンクノイズは帯域外のぶんだけ合計が低く出る`() {
        // ピンクノイズは 20Hz 未満にもオクターブあたり同じエネルギーを持つので、
        // 20Hz..20kHz の合成は元の信号レベルより低くなるのが正しい挙動。
        // ここが「一致してしまう」場合は正規化がどこかで二重に掛かっている。
        val pink = PinkNoiseSource(TEST_SAMPLE_RATE, levelDbFs = -15.0, seed = 3)
            .render(TEST_SAMPLE_RATE * 4)
        val levels = averageBandLevels(pink)

        val total = energySumDb(levels)
        assertThat(total).isLessThan(-15.0)
        assertThat(total).isGreaterThan(-18.0)
    }

    @Test
    fun `校正オフセットがバンドレベルに反映される`() {
        val tone = sineAtLevel(1000.0, levelDbFs = -20.0, lengthSamples = fftSize)
        val out = DoubleArray(bands.size)

        bandAnalyzer.bandLevelsDb(analyzer.powerSpectrum(tone), out, offsetDb = 120.0)

        val oneK = out[bands.indexOfFirst { it.label == "1k" }]
        assertThat(oneK).isWithin(0.5).of(100.0)
    }

    @Test
    fun `低域の細い帯域も空にならない`() {
        // 25Hz 帯の幅は約5.8Hz でビン幅（5.86Hz）より狭い。
        // 按分していないと空のバンドが出る。
        val pink = PinkNoiseSource(TEST_SAMPLE_RATE, levelDbFs = -20.0, seed = 11)
            .render(TEST_SAMPLE_RATE * 2)
        val levels = averageBandLevels(pink)

        val low = levelAt(levels, "25")
        assertThat(low).isGreaterThan(-80.0)
        assertThat(low.isFinite()).isTrue()
    }
}
