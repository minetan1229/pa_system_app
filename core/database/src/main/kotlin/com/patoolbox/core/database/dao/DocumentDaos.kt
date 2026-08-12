package com.patoolbox.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.patoolbox.core.database.entity.PatchRowEntity
import com.patoolbox.core.database.entity.PatchSheetEntity
import com.patoolbox.core.database.entity.ScheduleItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PatchSheetDao {

    @Query("SELECT * FROM patch_sheets ORDER BY updatedAtEpochMs DESC")
    fun observeAll(): Flow<List<PatchSheetEntity>>

    @Query("SELECT * FROM patch_sheets WHERE jobId = :jobId ORDER BY updatedAtEpochMs DESC")
    fun observeForJob(jobId: Long): Flow<List<PatchSheetEntity>>

    @Query("SELECT * FROM patch_sheets WHERE id = :id")
    fun observeById(id: Long): Flow<PatchSheetEntity?>

    @Query("SELECT * FROM patch_rows WHERE patchSheetId = :sheetId ORDER BY channel")
    fun observeRows(sheetId: Long): Flow<List<PatchRowEntity>>

    /** 無料版の保存件数制限の判定に使う。 */
    @Query("SELECT COUNT(*) FROM patch_sheets")
    suspend fun count(): Int

    @Insert
    suspend fun insertSheet(sheet: PatchSheetEntity): Long

    /** 名前だけ更新する。エンティティ全体を書き戻すと createdAt を壊すので使わない。 */
    @Query("UPDATE patch_sheets SET name = :name, updatedAtEpochMs = :now WHERE id = :id")
    suspend fun renameSheet(id: Long, name: String, now: Long)

    /** 行を編集したときに一覧の並び（更新順）を保つため親の更新時刻を進める。 */
    @Query("UPDATE patch_sheets SET updatedAtEpochMs = :now WHERE id = :id")
    suspend fun touchSheet(id: Long, now: Long)

    @Query("DELETE FROM patch_sheets WHERE id = :id")
    suspend fun deleteSheetById(id: Long)

    @Upsert
    suspend fun upsertRow(row: PatchRowEntity)

    @Delete
    suspend fun deleteRow(row: PatchRowEntity)

    @Query("SELECT COALESCE(MAX(channel), 0) FROM patch_rows WHERE patchSheetId = :sheetId")
    suspend fun maxChannel(sheetId: Long): Int

    /**
     * 空のパッチ表を作り、1..[channelCount] の行を一気に用意する。
     * 現場では「まず16ch分の枠を出して埋めていく」使い方になるため。
     */
    @Transaction
    suspend fun createWithChannels(sheet: PatchSheetEntity, channelCount: Int): Long {
        val sheetId = insertSheet(sheet)
        for (channel in 1..channelCount) {
            upsertRow(PatchRowEntity(patchSheetId = sheetId, channel = channel))
        }
        return sheetId
    }
}

@Dao
interface ScheduleItemDao {

    @Query("SELECT * FROM schedule_items WHERE jobId = :jobId ORDER BY sortOrder, startAtEpochMs")
    fun observeForJob(jobId: Long): Flow<List<ScheduleItemEntity>>

    @Upsert
    suspend fun upsert(item: ScheduleItemEntity)

    @Delete
    suspend fun delete(item: ScheduleItemEntity)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM schedule_items WHERE jobId = :jobId")
    suspend fun nextSortOrder(jobId: Long): Int
}
