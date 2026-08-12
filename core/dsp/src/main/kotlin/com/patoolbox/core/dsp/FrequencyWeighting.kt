package com.patoolbox.core.dsp

/** 周波数重み付け。IEC 61672-1。 */
enum class FrequencyWeighting {
    /** 人の聴感に合わせる。騒音レベルの基本 */
    A,

    /** 低域まで拾う。ピーク値（LCpeak）や低域の確認に使う */
    C,

    /** 重み付けなし（フラット） */
    Z,
    ;

    val displayName: String
        get() = when (this) {
            A -> "dBA"
            C -> "dBC"
            Z -> "dBZ"
        }
}

/**
 * A/C 特性フィルタ。
 *
 * IEC 61672-1 が定めるアナログ極
 *   f1 = 20.598997 Hz（2重）, f2 = 107.65265 Hz, f3 = 737.86223 Hz, f4 = 12194.217 Hz（2重）
 * を双一次変換で biquad 3段（A）／2段（C）に落とし、1kHz で 0dB になるよう正規化する。
 */
class WeightingFilter private constructor(
    val weighting: FrequencyWeighting,
    val sampleRate: Int,
    private val cascade: BiquadCascade,
    private val gain: Double,
) {

    fun process(x: Double): Double = if (cascade.isEmpty) x else cascade.process(x) * gain

    /** ブロックをその場で重み付けする（割り当てを起こさない）。 */
    fun processInPlace(buffer: FloatArray, length: Int = buffer.size) {
        if (cascade.isEmpty) return
        for (i in 0 until length) {
            buffer[i] = process(buffer[i].toDouble()).toFloat()
        }
    }

    /** [frequencyHz] における重み付け量（dB）。 */
    fun magnitudeDbAt(frequencyHz: Double): Double {
        if (cascade.isEmpty) return 0.0
        return amplitudeToDb(cascade.magnitudeAt(frequencyHz, sampleRate) * gain)
    }

    fun reset() = cascade.reset()

    companion object {
        private const val F1 = 20.598997
        private const val F2 = 107.65265
        private const val F3 = 737.86223
        private const val F4 = 12194.217

        /** 正規化の基準周波数。ここで 0dB になる。 */
        const val REFERENCE_FREQUENCY_HZ = 1000.0

        fun create(weighting: FrequencyWeighting, sampleRate: Int): WeightingFilter {
            val cascade = BiquadCascade(sections(weighting, sampleRate))
            val gain = if (cascade.isEmpty) {
                1.0
            } else {
                1.0 / cascade.magnitudeAt(REFERENCE_FREQUENCY_HZ, sampleRate)
            }
            return WeightingFilter(weighting, sampleRate, cascade, gain)
        }

        private fun sections(weighting: FrequencyWeighting, sampleRate: Int): List<Biquad> {
            val w1 = 2.0 * Math.PI * F1
            val w2 = 2.0 * Math.PI * F2
            val w3 = 2.0 * Math.PI * F3
            val w4 = 2.0 * Math.PI * F4

            return when (weighting) {
                FrequencyWeighting.Z -> emptyList()

                // C: s^2 / ((s+w1)^2 (s+w4)^2)
                FrequencyWeighting.C -> listOf(
                    bilinearTransform(1.0, 0.0, 0.0, 1.0, 2.0 * w1, w1 * w1, sampleRate),
                    bilinearTransform(0.0, 0.0, 1.0, 1.0, 2.0 * w4, w4 * w4, sampleRate),
                )

                // A: s^4 / ((s+w1)^2 (s+w2)(s+w3)(s+w4)^2)
                FrequencyWeighting.A -> listOf(
                    bilinearTransform(1.0, 0.0, 0.0, 1.0, 2.0 * w1, w1 * w1, sampleRate),
                    bilinearTransform(1.0, 0.0, 0.0, 1.0, w2 + w3, w2 * w3, sampleRate),
                    bilinearTransform(0.0, 0.0, 1.0, 1.0, 2.0 * w4, w4 * w4, sampleRate),
                )
            }
        }
    }
}
