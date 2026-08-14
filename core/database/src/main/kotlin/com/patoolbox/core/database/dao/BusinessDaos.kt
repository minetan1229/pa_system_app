package com.patoolbox.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.patoolbox.core.database.entity.GearItemEntity
import com.patoolbox.core.database.entity.InvoiceEntity
import com.patoolbox.core.database.entity.InvoiceLineEntity
import com.patoolbox.core.database.entity.SnapshotChannelEntity
import com.patoolbox.core.database.entity.SnapshotEntity
import com.patoolbox.core.database.entity.WorkLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GearDao {

    @Query("SELECT * FROM gear_items ORDER BY category, name")
    fun observeAll(): Flow<List<GearItemEntity>>

    @Query("SELECT * FROM gear_items ORDER BY category, name")
    suspend fun all(): List<GearItemEntity>

    @Query("SELECT COUNT(*) FROM gear_items")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(item: GearItemEntity): Long

    @Query("DELETE FROM gear_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM gear_items")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(items: List<GearItemEntity>)
}

@Dao
interface SnapshotDao {

    @Query("SELECT * FROM snapshots ORDER BY takenAtEpochMs DESC")
    fun observeAll(): Flow<List<SnapshotEntity>>

    @Query("SELECT * FROM snapshots ORDER BY takenAtEpochMs DESC")
    suspend fun all(): List<SnapshotEntity>

    @Query("SELECT * FROM snapshots WHERE id = :id")
    fun observeById(id: Long): Flow<SnapshotEntity?>

    @Query("SELECT * FROM snapshot_channels WHERE snapshotId = :snapshotId ORDER BY channel")
    fun observeChannels(snapshotId: Long): Flow<List<SnapshotChannelEntity>>

    @Query("SELECT * FROM snapshot_channels ORDER BY snapshotId, channel")
    suspend fun allChannels(): List<SnapshotChannelEntity>

    @Query("SELECT COUNT(*) FROM snapshots")
    suspend fun count(): Int

    @Insert
    suspend fun insert(snapshot: SnapshotEntity): Long

    @Query(
        "UPDATE snapshots SET title = :title, consoleName = :console, note = :note WHERE id = :id",
    )
    suspend fun updateDetails(id: Long, title: String, console: String, note: String)

    @Upsert
    suspend fun upsertChannel(channel: SnapshotChannelEntity)

    @Query("DELETE FROM snapshots WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM snapshots")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(snapshots: List<SnapshotEntity>)

    @Insert
    suspend fun insertAllChannels(channels: List<SnapshotChannelEntity>)

    /** 空のスナップショットを作り、[channelCount] 行ぶんの枠を用意する。 */
    @Transaction
    suspend fun createWithChannels(snapshot: SnapshotEntity, channelCount: Int): Long {
        val id = insert(snapshot)
        for (channel in 1..channelCount) {
            upsertChannel(SnapshotChannelEntity(snapshotId = id, channel = channel))
        }
        return id
    }
}

@Dao
interface InvoiceDao {

    @Query("SELECT * FROM invoices ORDER BY issueDateEpochMs DESC")
    fun observeAll(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices ORDER BY issueDateEpochMs DESC")
    suspend fun all(): List<InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE id = :id")
    fun observeById(id: Long): Flow<InvoiceEntity?>

    @Query("SELECT * FROM invoice_lines WHERE invoiceId = :invoiceId ORDER BY sortOrder, id")
    fun observeLines(invoiceId: Long): Flow<List<InvoiceLineEntity>>

    @Query("SELECT * FROM invoice_lines ORDER BY invoiceId, sortOrder")
    suspend fun allLines(): List<InvoiceLineEntity>

    @Query("SELECT COUNT(*) FROM invoices")
    suspend fun count(): Int

    @Insert
    suspend fun insert(invoice: InvoiceEntity): Long

    @Upsert
    suspend fun upsert(invoice: InvoiceEntity)

    @Upsert
    suspend fun upsertLine(line: InvoiceLineEntity)

    @Query("DELETE FROM invoice_lines WHERE id = :id")
    suspend fun deleteLineById(id: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM invoice_lines WHERE invoiceId = :id")
    suspend fun nextSortOrder(id: Long): Int

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM invoices")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(invoices: List<InvoiceEntity>)

    @Insert
    suspend fun insertAllLines(lines: List<InvoiceLineEntity>)
}

@Dao
interface WorkLogDao {

    @Query("SELECT * FROM work_logs ORDER BY dateEpochMs DESC")
    fun observeAll(): Flow<List<WorkLogEntity>>

    @Query("SELECT * FROM work_logs ORDER BY dateEpochMs DESC")
    suspend fun all(): List<WorkLogEntity>

    @Query("SELECT COUNT(*) FROM work_logs")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(entry: WorkLogEntity): Long

    @Query("DELETE FROM work_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM work_logs")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(entries: List<WorkLogEntity>)
}
