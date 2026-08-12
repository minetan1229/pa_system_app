package com.patoolbox.core.data

import com.patoolbox.core.database.dao.ScheduleItemDao
import com.patoolbox.core.database.entity.ScheduleItemEntity
import com.patoolbox.core.model.ScheduleItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** 進行表。並び順は sortOrder で持ち、時刻は長さから計算する。 */
interface ScheduleRepository {

    fun observeForJob(jobId: Long): Flow<List<ScheduleItem>>

    suspend fun add(item: ScheduleItem)

    suspend fun update(item: ScheduleItem)

    suspend fun delete(item: ScheduleItem)

    /** 並びを入れ替える（1つ上/下へ）。 */
    suspend fun reorder(items: List<ScheduleItem>)
}

@Singleton
class RoomScheduleRepository @Inject constructor(
    private val dao: ScheduleItemDao,
) : ScheduleRepository {

    override fun observeForJob(jobId: Long): Flow<List<ScheduleItem>> =
        dao.observeForJob(jobId).map { list -> list.map { it.toModel() } }

    override suspend fun add(item: ScheduleItem) {
        dao.upsert(item.toEntity(sortOrder = dao.nextSortOrder(item.jobId)))
    }

    override suspend fun update(item: ScheduleItem) {
        // sortOrder は並べ替え専用に扱うので、ここでは触らない
        dao.upsert(item.toEntity(sortOrder = dao.sortOrderOf(item.id) ?: 0))
    }

    override suspend fun delete(item: ScheduleItem) {
        dao.delete(item.toEntity(sortOrder = 0))
    }

    override suspend fun reorder(items: List<ScheduleItem>) {
        items.forEachIndexed { index, item ->
            dao.updateSortOrder(item.id, index)
        }
    }
}

private fun ScheduleItemEntity.toModel() = ScheduleItem(
    id = id,
    jobId = jobId,
    title = title,
    startAtEpochMs = startAtEpochMs,
    durationMinutes = durationMinutes,
    owner = owner,
)

private fun ScheduleItem.toEntity(sortOrder: Int) = ScheduleItemEntity(
    id = id,
    jobId = jobId,
    title = title,
    startAtEpochMs = startAtEpochMs,
    durationMinutes = durationMinutes,
    owner = owner,
    sortOrder = sortOrder,
)
