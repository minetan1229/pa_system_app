package com.patoolbox.core.calc

import kotlin.math.sqrt

/** 配電方式。日本の現場で使うもの。 */
enum class WiringSystem(val label: String, val voltageDropCoefficient: Double) {
    /** 単相2線式 100V。仮設のコンセントはこれ */
    SINGLE_PHASE_2WIRE("単相2線式", 35.6),

    /** 単相3線式 100V/200V */
    SINGLE_PHASE_3WIRE("単相3線式", 17.8),

    /** 三相3線式 200V。大型のアンプラックや電源車 */
    THREE_PHASE_3WIRE("三相3線式", 30.8),
    ;

    val isThreePhase: Boolean get() = this == THREE_PHASE_3WIRE
}

/**
 * 電源の計算。
 *
 * 現場でブレーカーを落とすのは、計算していないか力率を見ていないかのどちらか。
 * 連続負荷は定格の80%までに収めるのが基本なので、その判定を必ず返す。
 */
object PowerCalculator {

    /** 連続負荷で使ってよいブレーカー定格の割合 */
    const val CONTINUOUS_LOAD_FACTOR = 0.8

    /** 幹線と分岐を合わせた電圧降下の目安（%） */
    const val ACCEPTABLE_VOLTAGE_DROP_PERCENT = 2.0

    /**
     * 消費電力から電流（A）を求める。
     *
     * @param powerFactor 力率。スイッチング電源のアンプは 0.9 前後、
     *   古いトランス式や照明の調光は 0.6〜0.8 まで落ちることがある
     */
    fun currentAmps(
        watts: Double,
        volts: Double,
        system: WiringSystem = WiringSystem.SINGLE_PHASE_2WIRE,
        powerFactor: Double = 1.0,
    ): Double {
        require(volts > 0.0) { "電圧は正の値でなければならない" }
        require(powerFactor > 0.0 && powerFactor <= 1.0) { "力率は 0 より大きく 1 以下" }

        return if (system.isThreePhase) {
            watts / (sqrt(3.0) * volts * powerFactor)
        } else {
            watts / (volts * powerFactor)
        }
    }

    /** 連続負荷として安全に使える電流（A）。ブレーカー定格の80%。 */
    fun continuousCapacityAmps(breakerAmps: Double): Double =
        breakerAmps * CONTINUOUS_LOAD_FACTOR

    /** その電流をこのブレーカーで連続して流してよいか。 */
    fun isWithinBreaker(currentAmps: Double, breakerAmps: Double): Boolean =
        currentAmps <= continuousCapacityAmps(breakerAmps)

    /** そのブレーカーで連続して使える電力（W）。 */
    fun usableWatts(
        breakerAmps: Double,
        volts: Double,
        system: WiringSystem = WiringSystem.SINGLE_PHASE_2WIRE,
        powerFactor: Double = 1.0,
    ): Double {
        val amps = continuousCapacityAmps(breakerAmps)
        return if (system.isThreePhase) {
            sqrt(3.0) * volts * amps * powerFactor
        } else {
            volts * amps * powerFactor
        }
    }

    /**
     * 電圧降下（V）。内線規程の簡易式。
     *   単相2線式 e = 35.6 × L × I / (1000 × A)
     *   単相3線式 e = 17.8 × L × I / (1000 × A)
     *   三相3線式 e = 30.8 × L × I / (1000 × A)
     *
     * @param lengthMeters ケーブルの片道長
     * @param crossSectionMm2 導体の断面積（mm²）
     */
    fun voltageDropVolts(
        lengthMeters: Double,
        currentAmps: Double,
        crossSectionMm2: Double,
        system: WiringSystem = WiringSystem.SINGLE_PHASE_2WIRE,
    ): Double {
        require(crossSectionMm2 > 0.0) { "断面積は正の値でなければならない" }
        return system.voltageDropCoefficient * lengthMeters * currentAmps /
            (1000.0 * crossSectionMm2)
    }

    /** 電圧降下の割合（%）。 */
    fun voltageDropPercent(
        lengthMeters: Double,
        currentAmps: Double,
        crossSectionMm2: Double,
        volts: Double,
        system: WiringSystem = WiringSystem.SINGLE_PHASE_2WIRE,
    ): Double {
        require(volts > 0.0)
        return voltageDropVolts(lengthMeters, currentAmps, crossSectionMm2, system) / volts * 100.0
    }

    /** 電圧降下が許容範囲に収まっているか。 */
    fun isVoltageDropAcceptable(dropPercent: Double): Boolean =
        dropPercent <= ACCEPTABLE_VOLTAGE_DROP_PERCENT

    /** 現場で使うケーブルの断面積（mm²）。 */
    val COMMON_CROSS_SECTIONS = listOf(1.25, 2.0, 3.5, 5.5, 8.0, 14.0, 22.0, 38.0)

    /** よくあるブレーカー定格（A）。 */
    val COMMON_BREAKERS = listOf(15.0, 20.0, 30.0, 50.0, 60.0, 75.0, 100.0)
}
