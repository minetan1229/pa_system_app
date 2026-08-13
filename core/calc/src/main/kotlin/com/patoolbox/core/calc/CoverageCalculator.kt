package com.patoolbox.core.calc

import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.tan

/**
 * スピーカーの距離減衰とカバー範囲。
 *
 * 「最後列で何dB出るか」「この開き角で何m幅をカバーできるか」を出す。
 * 仕込み前に台数と吊り位置を決めるための道具。
 */
object CoverageCalculator {

    /**
     * 距離 [meters] における音圧（dB SPL）。
     *
     * @param sensitivityDb スピーカーの感度（1W/1m）または実測の 1m 値
     * @param inputWatts 入力電力。感度が 1W 基準のとき使う
     */
    fun splAtDistance(
        sensitivityDb: Double,
        inputWatts: Double,
        meters: Double,
    ): Double {
        require(meters > 0.0) { "距離は正の値でなければならない" }
        require(inputWatts > 0.0) { "電力は正の値でなければならない" }
        return sensitivityDb + 10.0 * log10(inputWatts) - 20.0 * log10(meters)
    }

    /** 1m の音圧から距離 [meters] での音圧（dB）。逆二乗則のみ。 */
    fun splFromOneMeter(splAtOneMeterDb: Double, meters: Double): Double {
        require(meters > 0.0)
        return splAtOneMeterDb - 20.0 * log10(meters)
    }

    /** 目標の音圧を距離 [meters] で得るために、1m で必要な音圧（dB）。 */
    fun requiredSplAtOneMeter(targetDb: Double, meters: Double): Double {
        require(meters > 0.0)
        return targetDb + 20.0 * log10(meters)
    }

    /**
     * 目標音圧に必要な入力電力（W）。
     * アンプ選定の目安に使う（ヘッドルームは別途見込むこと）。
     */
    fun requiredWatts(sensitivityDb: Double, targetDb: Double, meters: Double): Double {
        val requiredAtOneMeter = requiredSplAtOneMeter(targetDb, meters)
        return Math.pow(10.0, (requiredAtOneMeter - sensitivityDb) / 10.0)
    }

    /**
     * 開き角 [dispersionDegrees] のスピーカーが距離 [meters] でカバーする幅（m）。
     * 幅 = 2 × 距離 × tan(角度 / 2)
     */
    fun coverageWidthMeters(dispersionDegrees: Double, meters: Double): Double {
        require(dispersionDegrees > 0.0 && dispersionDegrees < 180.0) {
            "開き角は 0 より大きく 180 未満"
        }
        require(meters > 0.0)
        val halfAngleRadians = dispersionDegrees / 2.0 * PI / 180.0
        return 2.0 * meters * tan(halfAngleRadians)
    }

    /** その幅をカバーするのに必要な距離（m）。 */
    fun distanceForWidth(dispersionDegrees: Double, widthMeters: Double): Double {
        require(dispersionDegrees > 0.0 && dispersionDegrees < 180.0)
        require(widthMeters > 0.0)
        val halfAngleRadians = dispersionDegrees / 2.0 * PI / 180.0
        return widthMeters / (2.0 * tan(halfAngleRadians))
    }

    /**
     * 同じスピーカーを [count] 台足したときの音圧の増分（dB）。
     *
     * @param coherent true なら同相で加算（+6dB/倍）。サブの重ねなど。
     *   false なら無相関の加算（+3dB/倍）。離れた位置のスピーカーはこちらに近い
     */
    fun gainFromMultipleSources(count: Int, coherent: Boolean = false): Double {
        require(count >= 1) { "台数は1以上" }
        val factor = if (coherent) 20.0 else 10.0
        return factor * log10(count.toDouble())
    }

    /**
     * 最前列と最後列の音圧差（dB）。
     * 差が大きいほどディレイタワーやフィルが要る。10dB を超えたら検討の目安。
     */
    fun frontToBackDifferenceDb(nearMeters: Double, farMeters: Double): Double {
        require(nearMeters > 0.0 && farMeters > 0.0)
        return 20.0 * log10(farMeters / nearMeters)
    }

    /** 現場でよくあるスピーカーの水平開き角（度）。 */
    val COMMON_DISPERSIONS = listOf(60.0, 75.0, 80.0, 90.0, 100.0, 110.0, 120.0)
}
