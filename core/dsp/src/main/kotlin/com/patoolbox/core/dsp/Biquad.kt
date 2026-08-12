package com.patoolbox.core.dsp

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * 2次IIRフィルタ（Direct Form II transposed）。
 *
 * 係数と内部状態を Double で持つのは、A特性の 20.6Hz 付近の極が
 * 48kHz サンプリングだと単位円にかなり近く、Float では丸め誤差で低域が崩れるため。
 */
class Biquad(
    val b0: Double,
    val b1: Double,
    val b2: Double,
    val a1: Double,
    val a2: Double,
) {
    private var z1 = 0.0
    private var z2 = 0.0

    fun process(x: Double): Double {
        val y = b0 * x + z1
        z1 = b1 * x - a1 * y + z2
        z2 = b2 * x - a2 * y
        return y
    }

    fun reset() {
        z1 = 0.0
        z2 = 0.0
    }

    /** 周波数 [frequencyHz] での振幅特性（倍率）。 */
    fun magnitudeAt(frequencyHz: Double, sampleRate: Int): Double {
        val w = 2.0 * Math.PI * frequencyHz / sampleRate
        val cos1 = cos(w)
        val sin1 = sin(w)
        val cos2 = cos(2.0 * w)
        val sin2 = sin(2.0 * w)

        // e^(-jw) = cos(w) - j*sin(w)
        val numRe = b0 + b1 * cos1 + b2 * cos2
        val numIm = -(b1 * sin1 + b2 * sin2)
        val denRe = 1.0 + a1 * cos1 + a2 * cos2
        val denIm = -(a1 * sin1 + a2 * sin2)

        return hypot(numRe, numIm) / hypot(denRe, denIm)
    }
}

/**
 * アナログの2次セクション
 * H(s) = (n2 s^2 + n1 s + n0) / (d2 s^2 + d1 s + d0)
 * を双一次変換でデジタルの biquad に落とす。
 *
 * プリワープは掛けていない。A特性の 12.2kHz の極は 48kHz では数十分の1dB ずれるが、
 * IEC 61672 のクラス1許容差に収まる範囲（テストで確認している）。
 */
internal fun bilinearTransform(
    n2: Double,
    n1: Double,
    n0: Double,
    d2: Double,
    d1: Double,
    d0: Double,
    sampleRate: Int,
): Biquad {
    val k = 2.0 * sampleRate
    val kk = k * k
    val a0 = d2 * kk + d1 * k + d0

    return Biquad(
        b0 = (n2 * kk + n1 * k + n0) / a0,
        b1 = (2.0 * n0 - 2.0 * n2 * kk) / a0,
        b2 = (n2 * kk - n1 * k + n0) / a0,
        a1 = (2.0 * d0 - 2.0 * d2 * kk) / a0,
        a2 = (d2 * kk - d1 * k + d0) / a0,
    )
}

/** 直列に繋いだ biquad 群。 */
class BiquadCascade(private val sections: List<Biquad>) {

    fun process(x: Double): Double {
        var value = x
        for (section in sections) {
            value = section.process(value)
        }
        return value
    }

    fun magnitudeAt(frequencyHz: Double, sampleRate: Int): Double {
        var magnitude = 1.0
        for (section in sections) {
            magnitude *= section.magnitudeAt(frequencyHz, sampleRate)
        }
        return magnitude
    }

    fun reset() {
        for (section in sections) {
            section.reset()
        }
    }

    val isEmpty: Boolean get() = sections.isEmpty()
}
