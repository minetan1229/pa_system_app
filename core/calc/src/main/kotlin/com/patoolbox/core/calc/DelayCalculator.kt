package com.patoolbox.core.calc

/**
 * ディレイタイムと距離の換算。
 *
 * ディレイタワーやフロントフィルの時間合わせに使う。
 * 距離を入れれば ms、ms を入れれば距離が出る。
 */
object DelayCalculator {

    const val METERS_PER_FOOT = 0.3048

    /** 距離（m）→ 到達時間（ms）。 */
    fun millisecondsForDistance(
        meters: Double,
        speedOfSoundMPerSec: Double = SpeedOfSound.DEFAULT_M_PER_SEC,
    ): Double = meters / speedOfSoundMPerSec * 1000.0

    /** 時間（ms）→ 距離（m）。 */
    fun distanceForMilliseconds(
        milliseconds: Double,
        speedOfSoundMPerSec: Double = SpeedOfSound.DEFAULT_M_PER_SEC,
    ): Double = milliseconds / 1000.0 * speedOfSoundMPerSec

    /**
     * ディレイタワーに入れるディレイ量（ms）。
     *
     * メインスピーカーからタワーまでの距離ぶん遅らせるのが基本。
     * そこに [alignmentOffsetMs] を足して「わずかに遅らせる」調整をすることが多い
     * （タワーの音が先に来ると音像がタワー側に張り付くため、
     * 数ミリ秒遅らせてメイン側に寄せるのが定石）。
     */
    fun towerDelayMs(
        mainToTowerMeters: Double,
        speedOfSoundMPerSec: Double = SpeedOfSound.DEFAULT_M_PER_SEC,
        alignmentOffsetMs: Double = 0.0,
    ): Double = millisecondsForDistance(mainToTowerMeters, speedOfSoundMPerSec) + alignmentOffsetMs

    /** ms → サンプル数。卓のディレイをサンプル指定するとき用。 */
    fun samplesForMilliseconds(milliseconds: Double, sampleRate: Int): Double =
        milliseconds / 1000.0 * sampleRate

    fun millisecondsForSamples(samples: Double, sampleRate: Int): Double =
        samples / sampleRate * 1000.0

    fun feetToMeters(feet: Double): Double = feet * METERS_PER_FOOT

    fun metersToFeet(meters: Double): Double = meters / METERS_PER_FOOT

    /**
     * 2点間の到達時間差（ms）。位相干渉の確認に使う。
     * 差が正なら [farMeters] 側が遅れて届く。
     */
    fun arrivalDifferenceMs(
        nearMeters: Double,
        farMeters: Double,
        speedOfSoundMPerSec: Double = SpeedOfSound.DEFAULT_M_PER_SEC,
    ): Double = millisecondsForDistance(farMeters - nearMeters, speedOfSoundMPerSec)

    /**
     * その時間差で打ち消しが起きる最も低い周波数（Hz）。
     * 半波長ぶんずれると逆相になるので f = 1 / (2 * Δt)。
     */
    fun firstCancellationHz(differenceMs: Double): Double {
        if (differenceMs <= 0.0) return Double.POSITIVE_INFINITY
        return 1000.0 / (2.0 * differenceMs)
    }
}
