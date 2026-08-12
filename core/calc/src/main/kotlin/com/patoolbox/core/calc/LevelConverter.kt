package com.patoolbox.core.calc

import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * レベルの単位換算とゲインステージ。
 *
 * 「卓のアウトが +4dBu、アンプの入力感度が +4dBu、パワーが 8Ω 500W」
 * のような話を数字で確認するための道具。dBu と dBV を混同したまま
 * ゲインを組むのが現場でよくある事故なので、換算をここに集約している。
 */
object LevelConverter {

    /** 0 dBu = 0.7746 Vrms（600Ω で 1mW となる電圧が由来） */
    const val DBU_REFERENCE_VOLTS = 0.774_596_669

    /** 0 dBV = 1 Vrms */
    const val DBV_REFERENCE_VOLTS = 1.0

    /** dBu と dBV の差（dBu - dBV）。約 2.2dB */
    val DBU_TO_DBV_OFFSET: Double = 20.0 * log10(DBU_REFERENCE_VOLTS / DBV_REFERENCE_VOLTS)

    // --- 電圧 ---

    fun dbuToVolts(dbu: Double): Double = DBU_REFERENCE_VOLTS * 10.0.pow(dbu / 20.0)

    fun voltsToDbu(volts: Double): Double =
        20.0 * log10((volts / DBU_REFERENCE_VOLTS).coerceAtLeast(MIN_RATIO))

    fun dbvToVolts(dbv: Double): Double = DBV_REFERENCE_VOLTS * 10.0.pow(dbv / 20.0)

    fun voltsToDbv(volts: Double): Double =
        20.0 * log10((volts / DBV_REFERENCE_VOLTS).coerceAtLeast(MIN_RATIO))

    fun dbuToDbv(dbu: Double): Double = dbu + DBU_TO_DBV_OFFSET

    fun dbvToDbu(dbv: Double): Double = dbv - DBU_TO_DBV_OFFSET

    // --- 電力 ---

    /** 電圧と負荷から電力（W）。 */
    fun wattsFor(volts: Double, impedanceOhms: Double): Double {
        require(impedanceOhms > 0.0) { "インピーダンスは正の値でなければならない" }
        return volts * volts / impedanceOhms
    }

    /** 電力と負荷から必要な電圧（V）。 */
    fun voltsFor(watts: Double, impedanceOhms: Double): Double {
        require(watts >= 0.0 && impedanceOhms > 0.0)
        return sqrt(watts * impedanceOhms)
    }

    // --- 比 ---

    /** 電圧比 → dB（20log）。 */
    fun voltageRatioToDb(ratio: Double): Double =
        20.0 * log10(ratio.coerceAtLeast(MIN_RATIO))

    /** 電力比 → dB（10log）。 */
    fun powerRatioToDb(ratio: Double): Double =
        10.0 * log10(ratio.coerceAtLeast(MIN_RATIO))

    fun dbToVoltageRatio(db: Double): Double = 10.0.pow(db / 20.0)

    fun dbToPowerRatio(db: Double): Double = 10.0.pow(db / 10.0)

    // --- ゲインステージ ---

    /**
     * ヘッドルーム（dB）。正なら余裕がある。
     * @param peakDb 実際に来ているピークレベル
     * @param maxDb その機器が受けられる最大レベル
     */
    fun headroomDb(peakDb: Double, maxDb: Double): Double = maxDb - peakDb

    /**
     * 必要なパッド量（dB）。0以下ならパッドは不要。
     * 入力が最大許容を超えているときに何dB落とせばよいかを返す。
     */
    fun requiredPadDb(inputDb: Double, maxInputDb: Double): Double =
        (inputDb - maxInputDb).coerceAtLeast(0.0)

    /**
     * 距離が変わったときのSPL差（dB）。逆二乗則。
     * 距離が2倍なら -6dB。
     */
    fun distanceAttenuationDb(fromMeters: Double, toMeters: Double): Double {
        require(fromMeters > 0.0 && toMeters > 0.0) { "距離は正の値でなければならない" }
        return 20.0 * log10(fromMeters / toMeters)
    }

    private const val MIN_RATIO = 1e-12
}
