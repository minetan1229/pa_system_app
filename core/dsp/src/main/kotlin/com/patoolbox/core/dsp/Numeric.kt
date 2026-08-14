package com.patoolbox.core.dsp

/** [value] 以上で最小の2のべき乗。FFT サイズを決めるのに使う。 */
internal fun nextPowerOfTwo(value: Int): Int {
    var result = 1
    while (result < value) result = result shl 1
    return result
}

/**
 * ピーク周辺を放物線で補間して、サンプル間の位置を推定する。
 *
 * 遅延測定ではこれが効く。48kHz の1サンプルは 20.8us ＝ 音速で約 7mm なので
 * 整数サンプルのままでも十分に思えるが、実際の到来波形は帯域制限されて
 * なだらかなので、離散ピークは真のピークから最大 0.5 サンプルずれる。
 * 3点補間するとその偏りが消える。
 */
internal fun parabolicPeak(values: DoubleArray, peak: Int): Double {
    if (peak <= 0 || peak >= values.size - 1) return peak.toDouble()
    val a = values[peak - 1]
    val b = values[peak]
    val c = values[peak + 1]
    val denominator = 2.0 * (2.0 * b - a - c)
    if (denominator == 0.0) return peak.toDouble()
    return peak + (c - a) / denominator
}

/**
 * 最小二乗法による直線あてはめ。
 *
 * @return 傾き・切片・相関係数。残響時間の減衰直線と、そのあてはまりの良さに使う。
 */
internal fun linearFit(x: DoubleArray, y: DoubleArray): LinearFit {
    require(x.size == y.size) { "x と y の長さが違う" }
    val n = x.size
    require(n >= 2) { "あてはめには2点以上必要" }

    var sumX = 0.0
    var sumY = 0.0
    for (i in 0 until n) {
        sumX += x[i]
        sumY += y[i]
    }
    val meanX = sumX / n
    val meanY = sumY / n

    var sxx = 0.0
    var syy = 0.0
    var sxy = 0.0
    for (i in 0 until n) {
        val dx = x[i] - meanX
        val dy = y[i] - meanY
        sxx += dx * dx
        syy += dy * dy
        sxy += dx * dy
    }
    if (sxx == 0.0) return LinearFit(slope = 0.0, intercept = meanY, correlation = 0.0)

    val slope = sxy / sxx
    val correlation = if (syy == 0.0) 0.0 else sxy / kotlin.math.sqrt(sxx * syy)
    return LinearFit(
        slope = slope,
        intercept = meanY - slope * meanX,
        correlation = correlation,
    )
}

internal data class LinearFit(
    val slope: Double,
    val intercept: Double,
    /** -1..1。減衰が直線からどれだけ外れているかの指標 */
    val correlation: Double,
)
