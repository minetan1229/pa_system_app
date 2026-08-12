package com.patoolbox.core.dsp

/**
 * FFT による片側パワースペクトラム。
 *
 * 正規化は Parseval に合わせてある：全ビンの合計 = 信号の平均二乗値。
 * これによりオクターブバンドへの合成（[BandAnalyzer]）がそのまま正しい帯域レベルになる。
 *
 * 純音のレベルを読みたいときはビンのパワーをそのまま dB にせず、
 * [toneMeanSquare] で窓の等価ノイズ帯域幅ぶんを戻すこと。窓で1本のピークが
 * 複数ビンに広がるため、ピークビンだけ見ると Hann で約1.76dB 低く出る。
 */
class SpectrumAnalyzer(
    val sampleRate: Int,
    val fftSize: Int = DEFAULT_FFT_SIZE,
    windowFunction: WindowFunction = WindowFunction.HANN,
) {
    val binCount: Int = fftSize / 2 + 1
    val binWidthHz: Double = sampleRate.toDouble() / fftSize

    private val window = Window(windowFunction, fftSize)
    private val fft = Fft(fftSize)
    private val re = DoubleArray(fftSize)
    private val im = DoubleArray(fftSize)
    private val power = DoubleArray(binCount)

    val windowFunction: WindowFunction get() = window.function
    val equivalentNoiseBandwidthBins: Double get() = window.equivalentNoiseBandwidthBins

    /** 窓の等価ノイズ帯域幅（Hz）。 */
    val equivalentNoiseBandwidthHz: Double
        get() = window.equivalentNoiseBandwidthBins * binWidthHz

    fun binCenterHz(bin: Int): Double = bin * binWidthHz

    /**
     * [block] を解析して片側パワースペクトラム（ビンごとの平均二乗値）を返す。
     *
     * 返す配列は内部バッファなので、次の呼び出しで上書きされる。
     * 保持したい場合は呼び出し側でコピーすること（毎フレームの割り当てを避けるための設計）。
     */
    fun powerSpectrum(block: FloatArray, length: Int = block.size): DoubleArray {
        val n = minOf(length, fftSize)

        for (i in 0 until n) {
            re[i] = block[i].toDouble() * window.coefficients[i]
            im[i] = 0.0
        }
        // ブロックが短いときはゼロ詰め（窓も掛からないので実効的に短い窓になる）
        for (i in n until fftSize) {
            re[i] = 0.0
            im[i] = 0.0
        }

        fft.transform(re, im)

        val norm = 1.0 / (fftSize * window.sumOfSquares)
        for (bin in 0 until binCount) {
            val magnitudeSquared = re[bin] * re[bin] + im[bin] * im[bin]
            // DC とナイキストは折り返しの相手がいないので2倍しない
            val twoSided = if (bin == 0 || bin == fftSize / 2) 1.0 else 2.0
            power[bin] = magnitudeSquared * norm * twoSided
        }
        return power
    }

    /**
     * ビンのパワー → 純音の平均二乗値。
     *
     * **純音の周波数がビン中心にある場合のみ正確**。ビン中心から外れると
     * スキャロップロス（Hann で最大 1.4dB）ぶん低く出る。実測では周波数が
     * ビン中心に乗ることはまずないので、レベルを読む用途では
     * [toneMeanSquareAround] を使うこと。
     */
    fun toneMeanSquare(binPower: Double): Double =
        binPower * window.equivalentNoiseBandwidthBins

    /**
     * ピーク周辺のメインローブを合成した純音の平均二乗値。
     *
     * パワースペクトラムが Parseval 正規化されているので、メインローブを足すだけで
     * 窓の種類やビン中心からのずれに関係なく正しいレベルになる。
     * FFTアナライザのカーソル表示やチューナーのレベル表示はこちらを使う。
     */
    fun toneMeanSquareAround(
        spectrum: DoubleArray,
        peakBin: Int,
        halfWidthBins: Int = TONE_HALF_WIDTH_BINS,
    ): Double {
        var sum = 0.0
        val from = (peakBin - halfWidthBins).coerceAtLeast(0)
        val to = (peakBin + halfWidthBins).coerceAtMost(binCount - 1)
        for (bin in from..to) {
            sum += spectrum[bin]
        }
        return sum
    }

    /** 最大のビンを返す。純音の周波数推定に使う（放物線補間つき）。 */
    fun peakBin(spectrum: DoubleArray, fromBin: Int = 1, toBin: Int = binCount - 1): Int {
        var best = fromBin
        var bestValue = spectrum[fromBin]
        for (bin in fromBin + 1..toBin) {
            if (spectrum[bin] > bestValue) {
                bestValue = spectrum[bin]
                best = bin
            }
        }
        return best
    }

    /**
     * ピーク周辺を放物線補間した周波数。
     * ビン幅より細かい周波数を読むために使う（FFTアナライザのカーソル表示など）。
     */
    fun interpolatedPeakHz(spectrum: DoubleArray, peakBin: Int): Double {
        if (peakBin <= 0 || peakBin >= binCount - 1) return binCenterHz(peakBin)
        val left = powerToDb(spectrum[peakBin - 1])
        val center = powerToDb(spectrum[peakBin])
        val right = powerToDb(spectrum[peakBin + 1])
        val denominator = left - 2.0 * center + right
        if (denominator == 0.0) return binCenterHz(peakBin)
        val offset = 0.5 * (left - right) / denominator
        return (peakBin + offset) * binWidthHz
    }

    companion object {
        /**
         * 48kHz で約 5.9Hz のビン幅。
         * 1/3オクターブの最低帯域（25Hz 帯 = 幅 5.8Hz）を1ビンで拾える下限として選んだ。
         */
        const val DEFAULT_FFT_SIZE = 8192

        /**
         * 純音のレベルを読むときに合成するビン数（片側）。
         * Hann のメインローブは片側2ビンなので、3本あればサイドローブまで拾える。
         */
        const val TONE_HALF_WIDTH_BINS = 3
    }
}
