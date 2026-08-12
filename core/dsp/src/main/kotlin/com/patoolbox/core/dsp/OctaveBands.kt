package com.patoolbox.core.dsp

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/** バンド分解能。1オクターブあたりのバンド数。 */
enum class BandResolution(val bandsPerOctave: Int, val displayName: String) {
    FULL(1, "1/1 oct"),
    THIRD(3, "1/3 oct"),
    SIXTH(6, "1/6 oct"),
    TWELFTH(12, "1/12 oct"),
    ;

    /** 1/6・1/12 は Pro 専用。 */
    val requiresPro: Boolean get() = this == SIXTH || this == TWELFTH
}

data class FrequencyBand(
    /** 1kHz を 0 とした帯域番号 */
    val index: Int,
    val centerHz: Double,
    val lowerHz: Double,
    val upperHz: Double,
    /** 表示用の呼び値（1/3oct なら "31.5" "1k" など） */
    val label: String,
)

/**
 * IEC 61260 の 10 を底とする体系でバンドを作る。
 *   中心周波数 fm(n) = 1000 * 10^(3n / (10b))
 *   帯域端      fm * 10^(±3 / (20b))
 */
object OctaveBands {

    /** 呼び値で範囲指定できるようにするための余裕（2%）。 */
    private const val RANGE_SLACK = 1.02

    fun bands(
        resolution: BandResolution,
        minHz: Double = 20.0,
        maxHz: Double = 20000.0,
    ): List<FrequencyBand> {
        val b = resolution.bandsPerOctave
        val edgeFactor = 10.0.pow(3.0 / (20.0 * b))

        // 1000Hz を基準に、minHz..maxHz に収まる帯域番号の範囲を求める。
        //
        // 判定に RANGE_SLACK の余裕を持たせているのは、呼び値と計算値がずれるため。
        // 「20Hz から」と指定したときの 20Hz 帯は計算上 19.953Hz なので、
        // 素朴に中心 >= 20.0 で切ると最低帯域が丸ごと落ちてしまう。
        val exponent = 10.0 * b / 3.0
        val nMin = ceil(exponent * log10Safe(minHz / RANGE_SLACK / 1000.0)).toInt()
        val nMax = floor(exponent * log10Safe(maxHz * RANGE_SLACK / 1000.0)).toInt()

        return (nMin..nMax).map { n ->
            val center = 1000.0 * 10.0.pow(3.0 * n / (10.0 * b))
            FrequencyBand(
                index = n,
                centerHz = center,
                lowerHz = center / edgeFactor,
                upperHz = center * edgeFactor,
                label = label(center, resolution),
            )
        }
    }

    private fun log10Safe(x: Double): Double = ln(x.coerceAtLeast(1e-12)) / ln(10.0)

    /**
     * 1/1・1/3 オクターブは IEC 61260 の呼び値（25, 31.5, 40, ...）を使う。
     * RTA のスケール表示はこの呼び値で読まれるので、計算値の 31.62 ではなく 31.5 と出す。
     */
    private fun label(centerHz: Double, resolution: BandResolution): String {
        if (resolution == BandResolution.FULL || resolution == BandResolution.THIRD) {
            NOMINAL_THIRD_OCTAVE[nearestThirdOctaveIndex(centerHz)]?.let { return it }
        }
        return formatFrequency(centerHz)
    }

    /** 呼び値表を引くための 1/3oct 換算の帯域番号。 */
    private fun nearestThirdOctaveIndex(centerHz: Double): Int =
        (10.0 * log10Safe(centerHz / 1000.0)).roundToInt()

    private fun formatFrequency(hz: Double): String = when {
        hz >= 10000 -> "${(hz / 1000).roundToInt()}k"
        hz >= 1000 -> {
            val k = hz / 1000.0
            val rounded = (k * 10).roundToInt() / 10.0
            if (rounded % 1.0 == 0.0) "${rounded.toInt()}k" else "${rounded}k"
        }
        hz >= 100 -> hz.roundToInt().toString()
        hz >= 10 -> {
            val rounded = (hz * 10).roundToInt() / 10.0
            if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
        }
        else -> ((hz * 10).roundToInt() / 10.0).toString()
    }

    /** IEC 61260 の 1/3 オクターブ呼び値（キーは 1kHz を 0 とした帯域番号）。 */
    private val NOMINAL_THIRD_OCTAVE: Map<Int, String> = mapOf(
        -20 to "10", -19 to "12.5", -18 to "16", -17 to "20",
        -16 to "25", -15 to "31.5", -14 to "40", -13 to "50",
        -12 to "63", -11 to "80", -10 to "100", -9 to "125",
        -8 to "160", -7 to "200", -6 to "250", -5 to "315",
        -4 to "400", -3 to "500", -2 to "630", -1 to "800",
        0 to "1k", 1 to "1.25k", 2 to "1.6k", 3 to "2k",
        4 to "2.5k", 5 to "3.15k", 6 to "4k", 7 to "5k",
        8 to "6.3k", 9 to "8k", 10 to "10k", 11 to "12.5k",
        12 to "16k", 13 to "20k",
    )
}
