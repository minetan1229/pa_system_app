package com.patoolbox.core.dsp

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

/** これ以下のパワーは -inf 扱いにする（log10(0) を避ける）。 */
private const val POWER_FLOOR = 1e-20

/** 平均二乗値 → dB。 */
fun powerToDb(meanSquare: Double): Double = 10.0 * log10(meanSquare.coerceAtLeast(POWER_FLOOR))

/** 振幅 → dB。 */
fun amplitudeToDb(amplitude: Double): Double =
    20.0 * log10(abs(amplitude).coerceAtLeast(sqrt(POWER_FLOOR)))

fun dbToAmplitude(db: Double): Double = Math.pow(10.0, db / 20.0)

fun dbToPower(db: Double): Double = Math.pow(10.0, db / 10.0)

/** ブロックの平均二乗値。 */
fun meanSquare(buffer: FloatArray, length: Int = buffer.size): Double {
    if (length <= 0) return 0.0
    var sum = 0.0
    for (i in 0 until length) {
        val v = buffer[i].toDouble()
        sum += v * v
    }
    return sum / length
}

fun rms(buffer: FloatArray, length: Int = buffer.size): Double = sqrt(meanSquare(buffer, length))

/** ブロック内の絶対値最大。ピーク値・クリップ検出に使う。 */
fun peakAmplitude(buffer: FloatArray, length: Int = buffer.size): Double {
    var peak = 0.0
    for (i in 0 until length) {
        val v = abs(buffer[i].toDouble())
        if (v > peak) peak = v
    }
    return peak
}

/**
 * dB 値の集合をエネルギー平均する。
 * dB のまま算術平均すると誤るので、必ずこちらを使う。
 */
fun energyAverageDb(values: DoubleArray): Double {
    if (values.isEmpty()) return Double.NEGATIVE_INFINITY
    var sum = 0.0
    for (db in values) {
        sum += dbToPower(db)
    }
    return powerToDb(sum / values.size)
}

/** dB 値の集合をエネルギー加算する（帯域の合成レベルなど）。 */
fun energySumDb(values: DoubleArray): Double {
    if (values.isEmpty()) return Double.NEGATIVE_INFINITY
    var sum = 0.0
    for (db in values) {
        sum += dbToPower(db)
    }
    return powerToDb(sum)
}
