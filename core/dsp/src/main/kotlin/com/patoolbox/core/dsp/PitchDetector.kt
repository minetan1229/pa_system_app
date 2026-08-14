package com.patoolbox.core.dsp

/**
 * 基本周波数の検出（McLeod Pitch Method 相当）。
 *
 * 自己相関を FFT で求め、NSDF（正規化二乗差関数）に直してからピークを取る。
 * 素の自己相関だと倍音の多い楽器で1オクターブ下に誤検出しやすいが、
 * NSDF＋「最初の十分高いピーク」を採ることでそれを抑えている。
 */
class PitchDetector(
    val sampleRate: Int,
    val minFrequencyHz: Double = 40.0,
    val maxFrequencyHz: Double = 2000.0,
    val windowSize: Int = 4096,
) {
    data class Pitch(
        val frequencyHz: Double,
        /** 0..1。1に近いほど周期性がはっきりしている */
        val clarity: Double,
    )

    private val fftSize = nextPowerOfTwo(windowSize * 2)
    private val fft = Fft(fftSize)
    private val re = DoubleArray(fftSize)
    private val im = DoubleArray(fftSize)

    /** x^2 の累積和。NSDF の分母を O(1) で出すために使う */
    private val prefixSquares = DoubleArray(windowSize + 1)
    private val nsdf = DoubleArray(windowSize)

    private val minLag = (sampleRate / maxFrequencyHz).toInt().coerceAtLeast(2)
    private val maxLag = (sampleRate / minFrequencyHz).toInt().coerceAtMost(windowSize - 1)

    /**
     * @return 検出できなければ null（無音・非周期的な音）
     */
    fun detect(buffer: FloatArray, length: Int = buffer.size): Pitch? {
        val n = minOf(length, windowSize)
        if (n < maxLag + 2) return null

        // 直流を抜く。マイクのDCオフセットがあると自己相関が単調になって検出できない
        var mean = 0.0
        for (i in 0 until n) mean += buffer[i]
        mean /= n

        for (i in 0 until n) {
            re[i] = buffer[i] - mean
            im[i] = 0.0
        }
        for (i in n until fftSize) {
            re[i] = 0.0
            im[i] = 0.0
        }

        prefixSquares[0] = 0.0
        for (i in 0 until n) {
            prefixSquares[i + 1] = prefixSquares[i] + re[i] * re[i]
        }
        val totalPower = prefixSquares[n]
        if (totalPower <= SILENCE_POWER) return null

        // 自己相関 = IFFT(|FFT(x)|^2)
        fft.transform(re, im)
        for (i in 0 until fftSize) {
            re[i] = re[i] * re[i] + im[i] * im[i]
            im[i] = 0.0
        }
        fft.transform(re, im, inverse = true)

        // NSDF: n[tau] = 2 r[tau] / (Σx[j]^2 + Σx[j+tau]^2)
        for (lag in 0..maxLag) {
            val denominator = prefixSquares[n - lag] + (totalPower - prefixSquares[lag])
            nsdf[lag] = if (denominator > 0.0) 2.0 * re[lag] / denominator else 0.0
        }

        // 最初の負→正の切り替わり以降を探す（lag=0 の自明なピークを避ける）
        var searchStart = minLag
        var lag = 1
        while (lag <= maxLag && nsdf[lag] > 0.0) lag++
        if (lag > searchStart) searchStart = lag
        if (searchStart >= maxLag) return null

        // 局所ピークを集め、最大値の 90% を超える最初のものを採る（オクターブ誤りの抑制）
        var highest = -1.0
        for (i in searchStart until maxLag) {
            if (nsdf[i] > nsdf[i - 1] && nsdf[i] >= nsdf[i + 1] && nsdf[i] > highest) {
                highest = nsdf[i]
            }
        }
        if (highest < MIN_CLARITY) return null

        val threshold = highest * PEAK_ACCEPT_RATIO
        var chosen = -1
        for (i in searchStart until maxLag) {
            if (nsdf[i] > nsdf[i - 1] && nsdf[i] >= nsdf[i + 1] && nsdf[i] >= threshold) {
                chosen = i
                break
            }
        }
        if (chosen < 0) return null

        val refined = parabolicPeak(nsdf, chosen)
        if (refined <= 0.0) return null

        return Pitch(
            frequencyHz = sampleRate / refined,
            clarity = nsdf[chosen].coerceIn(0.0, 1.0),
        )
    }

    private companion object {
        const val SILENCE_POWER = 1e-9
        const val MIN_CLARITY = 0.3
        const val PEAK_ACCEPT_RATIO = 0.9
    }
}
