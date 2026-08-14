package com.patoolbox.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * パッチ表。案件が削除されたら一緒に消える。
 * [jobId] が null のものはテンプレートとして残る。
 */
@Entity(
    tableName = "patch_sheets",
    foreignKeys = [
        ForeignKey(
            entity = JobEntity::class,
            parentColumns = ["id"],
            childColumns = ["jobId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("jobId")],
)
data class PatchSheetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val jobId: Long? = null,
    val name: String,
    val isTemplate: Boolean = false,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

/** パッチ表の1行。 */
@Entity(
    tableName = "patch_rows",
    foreignKeys = [
        ForeignKey(
            entity = PatchSheetEntity::class,
            parentColumns = ["id"],
            childColumns = ["patchSheetId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("patchSheetId")],
)
data class PatchRowEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patchSheetId: Long,
    val channel: Int,
    val source: String = "",
    val micModel: String = "",
    val standType: String = "",
    val phantom: Boolean = false,
    val multiNumber: String = "",
    val notes: String = "",
)

/** ステージプロット（配置図）。案件が削除されたら一緒に消える。 */
@Entity(
    tableName = "stage_plots",
    foreignKeys = [
        ForeignKey(
            entity = JobEntity::class,
            parentColumns = ["id"],
            childColumns = ["jobId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("jobId")],
)
data class StagePlotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val jobId: Long? = null,
    val name: String,
    val stageWidthMeters: Double,
    val stageDepthMeters: Double,
    val notes: String = "",
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

/**
 * 配置図の1記号。
 *
 * [symbol] は enum 名を文字列で持つ。将来 enum に項目を足しても、
 * 序数で持っている場合のような並び替え事故が起きない。
 * 読めない名前が入っていた場合は読み出し側で既定の記号に落とす。
 */
@Entity(
    tableName = "stage_items",
    foreignKeys = [
        ForeignKey(
            entity = StagePlotEntity::class,
            parentColumns = ["id"],
            childColumns = ["stagePlotId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("stagePlotId")],
)
data class StageItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val stagePlotId: Long,
    val symbol: String,
    val label: String = "",
    val x: Float,
    val y: Float,
)

/** 進行表の1項目。 */
@Entity(
    tableName = "schedule_items",
    foreignKeys = [
        ForeignKey(
            entity = JobEntity::class,
            parentColumns = ["id"],
            childColumns = ["jobId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("jobId")],
)
data class ScheduleItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val jobId: Long,
    val title: String,
    val startAtEpochMs: Long? = null,
    val durationMinutes: Int = 0,
    val owner: String = "",
    val sortOrder: Int = 0,
)
