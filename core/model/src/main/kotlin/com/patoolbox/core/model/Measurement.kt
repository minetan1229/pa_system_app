package com.patoolbox.core.model

/**
 * 保存された測定。
 *
 * 測定条件（重み付け・校正）を必ず一緒に持つ。
 * どの設定で測ったか分からない記録は、後から見ても判断に使えないため。
 */
data class Measurement(
    val id: Long = 0,
    val jobId: Long? = null,
    val title: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long,
    val frequencyWeighting: String,
    val timeWeighting: String,
    val calibrationOffsetDb: Double,
    val calibrationMethod: CalibrationMethod,
    val leqDb: Double,
    val maxDb: Double,
    val minDb: Double,
    val peakDb: Double,
    val l10Db: Double,
    val l50Db: Double,
    val l90Db: Double,
    val clipped: Boolean,
) {
    val durationSeconds: Long
        get() = ((endedAtEpochMs - startedAtEpochMs) / 1000).coerceAtLeast(0)

    /** 未校正のまま記録されたか。書き出した資料に注記を出すために使う。 */
    val isUncalibrated: Boolean get() = calibrationMethod == CalibrationMethod.NONE

    val weightingLabel: String get() = "$frequencyWeighting / $timeWeighting"
}

/** 測定の1秒ごとの値。 */
data class MeasurementSample(
    val offsetMs: Long,
    val instantDb: Double,
    val leqDb: Double,
    val marker: String = "",
)

/**
 * 現場で撮った録音。
 *
 * 音声そのものはアプリ内部のファイルに置き、DB には所在と要約だけを持つ。
 * 数分の録音でも数十MBになるので、BLOB で DB に入れるとバックアップも
 * 一覧の読み込みも重くなる。
 */
data class Recording(
    val id: Long = 0,
    val jobId: Long? = null,
    val title: String,
    /** アプリ内部の録音フォルダからの相対名 */
    val fileName: String,
    val startedAtEpochMs: Long,
    val durationSeconds: Double,
    val sampleRate: Int,
    val sizeBytes: Long,
    val peakAmplitude: Float,
    val note: String = "",
) {
    val clipped: Boolean get() = peakAmplitude >= CLIP_THRESHOLD

    val durationLabel: String
        get() {
            val total = durationSeconds.toInt()
            return "%d:%02d".format(total / 60, total % 60)
        }

    private companion object {
        const val CLIP_THRESHOLD = 0.99f
    }
}
