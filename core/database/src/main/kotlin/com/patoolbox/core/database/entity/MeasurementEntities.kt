package com.patoolbox.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 1回の測定の要約。
 * 測定条件（重み付けと校正値）も一緒に残す。後から見て
 * 「どの設定で測った値か」が分からない記録は使えないため。
 */
@Entity(
    tableName = "measurements",
    foreignKeys = [
        ForeignKey(
            entity = JobEntity::class,
            parentColumns = ["id"],
            childColumns = ["jobId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("jobId")],
)
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val jobId: Long? = null,
    val title: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long,
    /** FrequencyWeighting の名前 */
    val frequencyWeighting: String,
    /** TimeWeighting の名前 */
    val timeWeighting: String,
    val calibrationOffsetDb: Double,
    val calibrationMethod: String,
    val leqDb: Double,
    val maxDb: Double,
    val minDb: Double,
    val peakDb: Double,
    val l10Db: Double,
    val l50Db: Double,
    val l90Db: Double,
    val clipped: Boolean,
)

/**
 * 測定の時系列。1秒ごとに1行。
 *
 * バンドごとのデータは持たない（3時間の測定で31バンド×1万行は重すぎる）。
 * RTA の記録が要るようになったら、別テーブルに BLOB で入れる。
 */
@Entity(
    tableName = "measurement_samples",
    foreignKeys = [
        ForeignKey(
            entity = MeasurementEntity::class,
            parentColumns = ["id"],
            childColumns = ["measurementId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("measurementId")],
)
data class MeasurementSampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val measurementId: Long,
    /** 測定開始からの経過（ミリ秒） */
    val offsetMs: Long,
    val instantDb: Double,
    val leqDb: Double,
    /** 曲名などの目印。無ければ空 */
    val marker: String = "",
)

/**
 * 録音。音声そのものはアプリ内部のファイルで、ここには所在と要約だけを持つ。
 * 案件が消えたら録音の行も消えるが、**ファイルは自動では消えない**ので、
 * リポジトリ側で孤児ファイルを掃除する。
 */
@Entity(
    tableName = "recordings",
    foreignKeys = [
        ForeignKey(
            entity = JobEntity::class,
            parentColumns = ["id"],
            childColumns = ["jobId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("jobId")],
)
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val jobId: Long? = null,
    val title: String,
    val fileName: String,
    val startedAtEpochMs: Long,
    val durationSeconds: Double,
    val sampleRate: Int,
    val sizeBytes: Long,
    val peakAmplitude: Float,
    val note: String = "",
)
