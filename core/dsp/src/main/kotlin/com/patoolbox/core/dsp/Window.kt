package com.patoolbox.core.dsp

import kotlin.math.cos

enum class WindowFunction(val displayName: String) {
    /** 窓なし。インパルス応答の切り出しなど、時間波形をそのまま扱うとき */
    RECTANGULAR("矩形"),

    /** 汎用。RTA の既定 */
    HANN("Hann"),

    HAMMING("Hamming"),

    /** サイドローブが小さい。近接した成分を分離したいとき */
    BLACKMAN_HARRIS("Blackman-Harris"),

    /** 振幅の読み取り精度が高い。純音のレベル測定向け */
    FLAT_TOP("Flat-top"),
}

/**
 * 窓関数の係数と、スペクトラムの正規化に必要な統計量。
 *
 * 係数は periodic（2πn/N）で作る。DFT でのスペクトル解析では
 * symmetric（2πn/(N-1)）より漏れが小さい。
 */
class Window(val function: WindowFunction, val size: Int) {

    val coefficients: DoubleArray = DoubleArray(size) { n ->
        val x = 2.0 * Math.PI * n / size
        when (function) {
            WindowFunction.RECTANGULAR -> 1.0
            WindowFunction.HANN -> 0.5 - 0.5 * cos(x)
            WindowFunction.HAMMING -> 0.54 - 0.46 * cos(x)
            WindowFunction.BLACKMAN_HARRIS ->
                0.35875 - 0.48829 * cos(x) + 0.14128 * cos(2 * x) - 0.01168 * cos(3 * x)
            WindowFunction.FLAT_TOP ->
                0.21557895 - 0.41663158 * cos(x) + 0.277263158 * cos(2 * x) -
                    0.083578947 * cos(3 * x) + 0.006947368 * cos(4 * x)
        }
    }

    /** 係数の総和。純音の振幅換算に使う */
    val sum: Double = coefficients.sum()

    /** 係数の二乗和。ノイズのパワー換算に使う */
    val sumOfSquares: Double = coefficients.sumOf { it * it }

    /** コヒーレントゲイン（純音の振幅がどれだけ下がるか） */
    val coherentGain: Double = sum / size

    /** 等価ノイズ帯域幅（ビン数）。Hann なら約1.5 */
    val equivalentNoiseBandwidthBins: Double = size * sumOfSquares / (sum * sum)
}
