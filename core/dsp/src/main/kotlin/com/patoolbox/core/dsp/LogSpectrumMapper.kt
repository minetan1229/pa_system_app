package com.patoolbox.core.dsp

import kotlin.math.pow
import kotlin.math.roundToInt

/** 表示のならし幅。生の FFT は音楽を入れると読めないので、現場では必ず何かしら掛ける。 */
enum class OctaveSmoothing(val denominator: Int?, val label: String) {
    NONE(null, "生"),
    TWELFTH(12, "1/12"),
    SIXTH(6, "1/6"),
    THIRD(3, "1/3"),
    ;

    val requiresPro: Boolean get() = false
}

/**
 * リニアな FFT ビンを、対数周波数の表示カラムに畳み込む。
 *
 * 8192点 FFT だとビンは 4097 本あり、しかも 48kHz では等間隔（5.86Hz）。
 * そのまま対数軸に置くと、100Hz 以下には 17 本しか無いのに 10kHz 以上に 2400 本が
 * 潰れて詰まる。音の判断は対数の周波数感覚で行うので、表示側で組み替える必要がある。
 *
 * ならし無しのときは各カラムの **最大** を取る。平均で潰すと、狭いピーク
 * （ハウリングの芽や電源ハム）が高域ほど薄まって見えなくなる。
 * 一番見つけたいものが一番消えるのでは意味がない。
 */
class LogSpectrumMapper(
    val binCount: Int,
    val binWidthHz: Double,
    val columns: Int = DEFAULT_COLUMNS,
    val minHz: Double = 20.0,
    maxHz: Double = 20000.0,
) {
    /** 実際の上限。ナイキストを超える指定は切り詰める */
    val maxHz: Double = maxHz.coerceAtMost((binCount - 1) * binWidthHz)

    /** 各カラムの中心周波数。目盛りの描画にも使う */
    val frequencies: DoubleArray

    /**
     * 1オクターブぶんのカラム数。
     * 「1/3オクターブ以上離れた山だけ拾う」のように、
     * 周波数の間隔をカラム数に直すときに使う。
     */
    val columnsPerOctave: Double

    /** 隣り合うカラムの周波数比。カラムの幅を出すのに使う */
    private val ratio: Double

    private val prefix = DoubleArray(binCount + 1)

    init {
        require(binCount >= 2) { "ビンが少なすぎる: $binCount" }
        require(this.maxHz > minHz) { "周波数範囲が不正: $minHz..${this.maxHz}" }
        require(columns >= 2) { "カラムが少なすぎる: $columns" }

        ratio = (this.maxHz / minHz).pow(1.0 / (columns - 1))
        frequencies = DoubleArray(columns) { minHz * ratio.pow(it) }
        columnsPerOctave = kotlin.math.ln(2.0) / kotlin.math.ln(ratio)
    }

    /**
     * パワースペクトラムを表示カラムに落とす。
     *
     * @param out 長さ [columns] の出力先。毎フレーム呼ぶので割り当てを避けている
     * @return [out]（ビンのパワーと同じ単位）
     */
    fun map(
        spectrum: DoubleArray,
        smoothing: OctaveSmoothing = OctaveSmoothing.NONE,
        out: DoubleArray = DoubleArray(columns),
    ): DoubleArray {
        require(out.size >= columns) { "出力配列が短い: ${out.size} < $columns" }

        val fraction = smoothing.denominator
        if (fraction != null) buildPrefix(spectrum)
        // ならしの窓は「その周波数を中心とした 1/n オクターブ」
        val halfWidth = if (fraction == null) 0.0 else 2.0.pow(1.0 / (2.0 * fraction))
        val halfColumn = kotlin.math.sqrt(ratio)

        for (column in 0 until columns) {
            val center = frequencies[column]
            val lowHz: Double
            val highHz: Double
            if (fraction == null) {
                lowHz = center / halfColumn
                highHz = center * halfColumn
            } else {
                lowHz = center / halfWidth
                highHz = center * halfWidth
            }

            val fromBin = binOf(lowHz)
            val toBin = binOf(highHz).coerceAtLeast(fromBin)

            out[column] = if (fraction == null) {
                maxBetween(spectrum, fromBin, toBin)
            } else {
                (prefix[toBin + 1] - prefix[fromBin]) / (toBin - fromBin + 1)
            }
        }
        return out
    }

    /** 表示カラムの周波数 → カラム番号。カーソル表示の逆引きに使う */
    fun columnOf(frequencyHz: Double): Int {
        if (frequencyHz <= minHz) return 0
        if (frequencyHz >= maxHz) return columns - 1
        val steps = kotlin.math.ln(frequencyHz / minHz) / kotlin.math.ln(ratio)
        return steps.roundToInt().coerceIn(0, columns - 1)
    }

    private fun binOf(hz: Double): Int =
        (hz / binWidthHz).roundToInt().coerceIn(0, binCount - 1)

    private fun maxBetween(spectrum: DoubleArray, fromBin: Int, toBin: Int): Double {
        var peak = spectrum[fromBin]
        for (bin in fromBin + 1..toBin) {
            if (spectrum[bin] > peak) peak = spectrum[bin]
        }
        return peak
    }

    private fun buildPrefix(spectrum: DoubleArray) {
        prefix[0] = 0.0
        for (i in 0 until binCount) {
            prefix[i + 1] = prefix[i] + spectrum[i]
        }
    }

    companion object {
        /**
         * 既定のカラム数。実機の横解像度（1080前後）に対して、
         * 1カラム4pxで足りる程度。増やしても目には見えないうえ、
         * スペクトログラムの画素数がそのまま増える
         */
        const val DEFAULT_COLUMNS = 256
    }
}
