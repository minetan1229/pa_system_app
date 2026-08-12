package com.patoolbox.core.model

/** 入力の種類。内蔵マイクか外部マイクかで信頼度が大きく変わる。 */
enum class AudioInputType {
    BUILTIN_MIC,
    WIRED_HEADSET,
    /** USB Audio Class の外部マイク。測定マイクを繋げる場合これ */
    USB,
    BLUETOOTH,
    UNKNOWN,
    ;

    /** 測定向きの入力か（Bluetooth は圧縮と自動ゲインが入るので不適） */
    val isMeasurementCapable: Boolean
        get() = this == BUILTIN_MIC || this == WIRED_HEADSET || this == USB
}

/** 校正の取り方。UI に「どう校正されたか」を出すために区別する。 */
enum class CalibrationMethod {
    /** 未校正。仮のオフセットで動いている */
    NONE,

    /** 基準となる騒音計と並べて差分を手入力した */
    MANUAL,

    /** 音響校正器（94dB / 114dB @1kHz）を当てた */
    CALIBRATOR,
}

/**
 * 入力デバイスごとの校正値。
 *
 * [offsetDb] は「0 dBFS が何 dB SPL に相当するか」。
 * 測定値 = 信号の dBFS + offsetDb になる。
 */
data class CalibrationProfile(
    val id: Long = 0,
    /** 端末モデルや USB 機器名。デバイスを差し替えても値を取り違えないためのキー */
    val deviceKey: String,
    val inputType: AudioInputType,
    val offsetDb: Double,
    val method: CalibrationMethod,
    val calibratedAtEpochMs: Long? = null,
) {
    val isCalibrated: Boolean get() = method != CalibrationMethod.NONE

    /**
     * 表示してよい精度の目安。
     * 有料で売る以上、どの状態で測っているかを隠さないための情報。
     */
    val confidence: CalibrationConfidence
        get() = when {
            method == CalibrationMethod.CALIBRATOR -> CalibrationConfidence.GOOD
            method == CalibrationMethod.MANUAL && inputType == AudioInputType.USB ->
                CalibrationConfidence.GOOD
            method == CalibrationMethod.MANUAL -> CalibrationConfidence.FAIR
            else -> CalibrationConfidence.UNCALIBRATED
        }

    companion object {
        /**
         * 未校正時の仮オフセット。端末によって10dB以上違うので参考値にすぎない。
         * この値のまま数字を信用させないよう、UI には必ず未校正バッジを出す。
         */
        const val DEFAULT_OFFSET_DB = 120.0

        fun uncalibrated(deviceKey: String, inputType: AudioInputType) = CalibrationProfile(
            deviceKey = deviceKey,
            inputType = inputType,
            offsetDb = DEFAULT_OFFSET_DB,
            method = CalibrationMethod.NONE,
        )
    }
}

enum class CalibrationConfidence {
    /** 未校正。数値は目安 */
    UNCALIBRATED,

    /** 手動校正済み。内蔵マイクなので周波数特性の癖は残る */
    FAIR,

    /** 校正器または外部測定マイク */
    GOOD,
}
