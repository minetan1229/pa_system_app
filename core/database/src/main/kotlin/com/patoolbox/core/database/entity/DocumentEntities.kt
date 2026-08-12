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
