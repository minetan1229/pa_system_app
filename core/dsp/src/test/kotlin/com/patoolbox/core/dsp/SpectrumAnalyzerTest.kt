package com.patoolbox.core.dsp

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SpectrumAnalyzerTest {

    private val analyzer = SpectrumAnalyzer(TEST_SAMPLE_RATE, fftSize = 8192)

    @Test
    fun `Hann窓の等価ノイズ帯域幅は約15ビン`() {
        assertThat(analyzer.equivalentNoiseBandwidthBins).isWithin(0.01).of(1.5)
    }

    @Test
    fun `フルスケールのサインは-3_01dBFSと読める`() {
        // 振幅1.0のサイン波の RMS は 0.707 → -3.01 dBFS
        val block = sine(1000.0, amplitude = 1.0, lengthSamples = 8192)
        val spectrum = analyzer.powerSpectrum(block)

        val peak = analyzer.peakBin(spectrum)
        val toneDb = powerToDb(analyzer.toneMeanSquareAround(spectrum, peak))

        assertThat(toneDb).isWithin(0.1).of(-3.01)
    }

    @Test
    fun `指定レベルのサインがそのレベルで読める`() {
        val block = sineAtLevel(1000.0, levelDbFs = -20.0, lengthSamples = 8192)
        val spectrum = analyzer.powerSpectrum(block)

        val peak = analyzer.peakBin(spectrum)
        val toneDb = powerToDb(analyzer.toneMeanSquareAround(spectrum, peak))

        assertThat(toneDb).isWithin(0.1).of(-20.0)
    }

    @Test
    fun `ビン中心ちょうどなら単一ビンからでも正しく読める`() {
        // toneMeanSquare（1ビンだけ・等価ノイズ帯域幅で戻す方式）が正確なのはこの場合だけ。
        // 実測の周波数がビン中心に乗ることはまずないので、通常は
        // toneMeanSquareAround を使う前提になっている。
        val onBinHz = 170 * analyzer.binWidthHz
        val block = sineAtLevel(onBinHz, levelDbFs = -20.0, lengthSamples = 8192)
        val spectrum = analyzer.powerSpectrum(block)
        val peak = analyzer.peakBin(spectrum)

        assertThat(powerToDb(analyzer.toneMeanSquare(spectrum[peak]))).isWithin(0.1).of(-20.0)
    }

    @Test
    fun `ビン中心から外れた純音でもレベルを読める`() {
        // 半ビンずれ = Hann のスキャロップロスが最大になる位置
        val worstCaseHz = (170 + 0.5) * analyzer.binWidthHz
        val block = sineAtLevel(worstCaseHz, levelDbFs = -20.0, lengthSamples = 8192)
        val spectrum = analyzer.powerSpectrum(block)
        val peak = analyzer.peakBin(spectrum)

        // 1ビンだけだと 1.4dB 近く低く出るが、メインローブを合成すれば正しい
        assertThat(powerToDb(analyzer.toneMeanSquare(spectrum[peak]))).isLessThan(-20.8)
        assertThat(powerToDb(analyzer.toneMeanSquareAround(spectrum, peak)))
            .isWithin(0.1)
            .of(-20.0)
    }

    @Test
    fun `全ビンの合計が平均二乗値に一致する`() {
        // この正規化のおかげでオクターブバンドへの合成がそのまま帯域レベルになる
        val block = sineAtLevel(1000.0, levelDbFs = -12.0, lengthSamples = 8192)
        val spectrum = analyzer.powerSpectrum(block)

        val sumDb = powerToDb(spectrum.sum())

        assertThat(sumDb).isWithin(0.2).of(-12.0)
    }

    @Test
    fun `白色ノイズでも合計が平均二乗値に一致する`() {
        val noise = WhiteNoiseSource(TEST_SAMPLE_RATE, levelDbFs = -20.0, seed = 42)
            .render(8192)
        val spectrum = analyzer.powerSpectrum(noise)

        val expected = amplitudeToDb(rms(noise))

        assertThat(powerToDb(spectrum.sum())).isWithin(0.5).of(expected)
    }

    @Test
    fun `ビンの間の周波数を放物線補間で読める`() {
        // ビン幅は約5.86Hz。ビン中心から外れた1234Hzを±1Hzで当てる
        val block = sine(1234.0, lengthSamples = 8192)
        val spectrum = analyzer.powerSpectrum(block)
        val peak = analyzer.peakBin(spectrum)

        assertThat(analyzer.interpolatedPeakHz(spectrum, peak)).isWithin(1.0).of(1234.0)
    }

    @Test
    fun `ビン幅とビン数がサンプリング周波数と整合する`() {
        assertThat(analyzer.binCount).isEqualTo(4097)
        assertThat(analyzer.binWidthHz).isWithin(0.001).of(48000.0 / 8192)
        assertThat(analyzer.binCenterHz(1000)).isWithin(0.001).of(1000 * 48000.0 / 8192)
    }

    @Test
    fun `Flat-top窓は純音の振幅読み取りが正確`() {
        // ビン中心から最も外れた位置（半ビンずれ）でも誤差が小さいことを確認する
        val flatTop = SpectrumAnalyzer(
            TEST_SAMPLE_RATE,
            fftSize = 8192,
            windowFunction = WindowFunction.FLAT_TOP,
        )
        val offBinHz = (1000 + 0.5) * flatTop.binWidthHz
        val block = sineAtLevel(offBinHz, levelDbFs = -20.0, lengthSamples = 8192)

        val spectrum = flatTop.powerSpectrum(block)
        val peak = flatTop.peakBin(spectrum)

        // Flat-top は1ビンだけでもスキャロップロスが小さい（これが Flat-top の存在理由）
        assertThat(powerToDb(flatTop.toneMeanSquare(spectrum[peak]))).isWithin(0.5).of(-20.0)
    }
}
