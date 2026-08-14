package com.patoolbox.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Phase 6 の運営まわりのテーブル。
 *
 * enum は序数ではなく **名前の文字列** で持つ。序数で保存すると、
 * enum に項目を足して並びが変わった瞬間に、既存データの意味が静かに入れ替わる。
 */
@Entity(tableName = "gear_items")
data class GearItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String,
    val name: String,
    val maker: String = "",
    val modelName: String = "",
    val serial: String = "",
    val quantity: Int = 1,
    val status: String,
    val note: String = "",
    val updatedAtEpochMs: Long,
)

@Entity(
    tableName = "snapshots",
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
data class SnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val jobId: Long? = null,
    val title: String,
    val consoleName: String = "",
    val takenAtEpochMs: Long,
    val note: String = "",
)

@Entity(
    tableName = "snapshot_channels",
    foreignKeys = [
        ForeignKey(
            entity = SnapshotEntity::class,
            parentColumns = ["id"],
            childColumns = ["snapshotId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("snapshotId")],
)
data class SnapshotChannelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val snapshotId: Long,
    val channel: Int,
    val name: String = "",
    val gain: String = "",
    val hpf: String = "",
    val eq: String = "",
    val send: String = "",
    val note: String = "",
)

@Entity(
    tableName = "invoices",
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
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val jobId: Long? = null,
    val number: String = "",
    val issueDateEpochMs: Long,
    val clientName: String = "",
    val subject: String = "",
    val issuerName: String = "",
    val registrationNumber: String = "",
    val taxModeName: String,
    val taxRoundingName: String,
    val note: String = "",
    val isEstimate: Boolean = false,
    val updatedAtEpochMs: Long,
)

@Entity(
    tableName = "invoice_lines",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("invoiceId")],
)
data class InvoiceLineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceId: Long,
    val description: String = "",
    val quantity: Double = 1.0,
    val unit: String = "式",
    val unitPrice: Long = 0,
    val taxRateName: String,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "work_logs",
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
data class WorkLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val jobId: Long? = null,
    val title: String = "",
    val dateEpochMs: Long,
    val startMinutesOfDay: Int,
    val endMinutesOfDay: Int,
    val breakMinutes: Int = 0,
    val rateTypeName: String,
    val rate: Long = 0,
    val multiplier: Double = 1.0,
    val transportFee: Long = 0,
    val note: String = "",
)
