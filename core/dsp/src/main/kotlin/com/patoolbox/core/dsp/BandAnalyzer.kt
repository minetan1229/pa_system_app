package com.patoolbox.core.dsp

/**
 * パワースペクトラムをオクターブバンドに合成する。
 *
 * ビンを「中心が入っているバンド」に丸投げせず、ビンの幅と帯域端の重なりで按分する。
 * 低域では 1/3 オクターブ帯域の幅がビン幅より狭くなる（48kHz/8192 でビン 5.9Hz、
 * 25Hz 帯は幅 5.8Hz）ため、丸投げだと空のバンドが出てしまう。
 * 按分ならエネルギーが保存され、帯域が細くてもその付近のスペクトル密度から妥当な値になる。
 */
class BandAnalyzer(
    val bands: List<FrequencyBand>,
    private val binWidthHz: Double,
    private val binCount: Int,
) {
    // バンドごとの (ビン番号, 重み) を作り置きする
    private val binIndices: Array<IntArray>
    private val binWeights: Array<DoubleArray>

    init {
        val indices = ArrayList<IntArray>(bands.size)
        val weights = ArrayList<DoubleArray>(bands.size)

        for (band in bands) {
            val firstBin = ((band.lowerHz / binWidthHz) - 0.5).toInt().coerceAtLeast(0)
            val lastBin = (((band.upperHz / binWidthHz) + 0.5).toInt() + 1)
                .coerceAtMost(binCount - 1)

            val idx = ArrayList<Int>()
            val wgt = ArrayList<Double>()
            for (bin in firstBin..lastBin) {
                val binLow = (bin - 0.5) * binWidthHz
                val binHigh = (bin + 0.5) * binWidthHz
                val overlap = minOf(binHigh, band.upperHz) - maxOf(binLow, band.lowerHz)
                if (overlap > 0.0) {
                    idx += bin
                    wgt += overlap / binWidthHz
                }
            }
            indices += idx.toIntArray()
            weights += wgt.toDoubleArray()
        }

        binIndices = indices.toTypedArray()
        binWeights = weights.toTypedArray()
    }

    /**
     * 各バンドの平均二乗値を [out] に書き込む。
     * @param out サイズは [bands].size
     */
    fun bandPowers(powerSpectrum: DoubleArray, out: DoubleArray) {
        require(out.size >= bands.size) { "出力配列がバンド数より小さい" }
        for (b in bands.indices) {
            val idx = binIndices[b]
            val wgt = binWeights[b]
            var sum = 0.0
            for (i in idx.indices) {
                sum += powerSpectrum[idx[i]] * wgt[i]
            }
            out[b] = sum
        }
    }

    /**
     * 各バンドのレベル（dB）を [out] に書き込む。
     * @param offsetDb 校正オフセット。dBFS から dB SPL へ持ち上げるのに使う
     */
    fun bandLevelsDb(powerSpectrum: DoubleArray, out: DoubleArray, offsetDb: Double = 0.0) {
        bandPowers(powerSpectrum, out)
        for (b in bands.indices) {
            out[b] = powerToDb(out[b]) + offsetDb
        }
    }
}
