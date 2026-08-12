package com.patoolbox.core.data

import com.patoolbox.core.database.dao.PatchSheetDao
import com.patoolbox.core.database.entity.PatchRowEntity
import com.patoolbox.core.database.entity.PatchSheetEntity
import com.patoolbox.core.model.PatchRow
import com.patoolbox.core.model.PatchSheet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** パッチ表（インプットリスト）。 */
interface PatchSheetRepository {

    /** 一覧。行は含まない（一覧では使わないので読み込まない）。 */
    fun observeAll(): Flow<List<PatchSheet>>

    fun observeForJob(jobId: Long): Flow<List<PatchSheet>>

    /** 1件を行つきで観測する。 */
    fun observeWithRows(sheetId: Long): Flow<PatchSheet?>

    suspend fun count(): Int

    /**
     * 新規作成。[channelCount] 行ぶんの空の枠を同時に作る。
     * @return 作成したパッチ表のID
     */
    suspend fun create(name: String, jobId: Long?, channelCount: Int): Long

    suspend fun rename(sheet: PatchSheet, name: String)

    suspend fun delete(sheet: PatchSheet)

    suspend fun saveRow(sheetId: Long, row: PatchRow)

    suspend fun deleteRow(sheetId: Long, row: PatchRow)

    /** 末尾に1ch追加する。 */
    suspend fun appendChannel(sheetId: Long)
}

@Singleton
class RoomPatchSheetRepository @Inject constructor(
    private val dao: PatchSheetDao,
) : PatchSheetRepository {

    override fun observeAll(): Flow<List<PatchSheet>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    override fun observeForJob(jobId: Long): Flow<List<PatchSheet>> =
        dao.observeForJob(jobId).map { list -> list.map { it.toModel() } }

    override fun observeWithRows(sheetId: Long): Flow<PatchSheet?> =
        combine(dao.observeById(sheetId), dao.observeRows(sheetId)) { sheet, rows ->
            sheet?.toModel()?.copy(rows = rows.map { it.toModel() })
        }

    override suspend fun count(): Int = dao.count()

    override suspend fun create(name: String, jobId: Long?, channelCount: Int): Long {
        val now = System.currentTimeMillis()
        return dao.createWithChannels(
            sheet = PatchSheetEntity(
                jobId = jobId,
                name = name,
                isTemplate = jobId == null,
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
            ),
            channelCount = channelCount,
        )
    }

    override suspend fun rename(sheet: PatchSheet, name: String) {
        dao.renameSheet(sheet.id, name, now())
    }

    override suspend fun delete(sheet: PatchSheet) {
        // 行は外部キーの CASCADE で消える
        dao.deleteSheetById(sheet.id)
    }

    override suspend fun saveRow(sheetId: Long, row: PatchRow) {
        dao.upsertRow(row.toEntity(sheetId))
        touch(sheetId)
    }

    override suspend fun deleteRow(sheetId: Long, row: PatchRow) {
        dao.deleteRow(row.toEntity(sheetId))
        touch(sheetId)
    }

    override suspend fun appendChannel(sheetId: Long) {
        val nextChannel = dao.maxChannel(sheetId) + 1
        dao.upsertRow(PatchRowEntity(patchSheetId = sheetId, channel = nextChannel))
        touch(sheetId)
    }

    /** 一覧の並び（更新順）を正しく保つため、行を編集したら親の更新時刻も動かす。 */
    private suspend fun touch(sheetId: Long) {
        dao.touchSheet(sheetId, now())
    }

    private fun now() = System.currentTimeMillis()
}

private fun PatchSheetEntity.toModel() = PatchSheet(
    id = id,
    jobId = jobId,
    name = name,
    isTemplate = isTemplate,
    updatedAtEpochMs = updatedAtEpochMs,
)

private fun PatchRowEntity.toModel() = PatchRow(
    id = id,
    channel = channel,
    source = source,
    micModel = micModel,
    standType = standType,
    phantom = phantom,
    multiNumber = multiNumber,
    notes = notes,
)

private fun PatchRow.toEntity(sheetId: Long) = PatchRowEntity(
    id = id,
    patchSheetId = sheetId,
    channel = channel,
    source = source,
    micModel = micModel,
    standType = standType,
    phantom = phantom,
    multiNumber = multiNumber,
    notes = notes,
)
