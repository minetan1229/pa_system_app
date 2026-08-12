package com.patoolbox.core.data

import com.patoolbox.core.database.dao.JobDao
import com.patoolbox.core.database.entity.JobEntity
import com.patoolbox.core.model.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** 案件（現場）。パッチ表・進行表の親になる。 */
interface JobRepository {

    fun observeAll(): Flow<List<Job>>

    fun observeById(id: Long): Flow<Job?>

    suspend fun count(): Int

    /** @return 追加された案件のID */
    suspend fun create(job: Job): Long

    suspend fun update(job: Job)

    suspend fun delete(job: Job)
}

@Singleton
class RoomJobRepository @Inject constructor(
    private val dao: JobDao,
) : JobRepository {

    override fun observeAll(): Flow<List<Job>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    override fun observeById(id: Long): Flow<Job?> =
        dao.observeById(id).map { it?.toModel() }

    override suspend fun count(): Int = dao.count()

    override suspend fun create(job: Job): Long {
        val now = System.currentTimeMillis()
        return dao.insert(job.toEntity(createdAt = now, updatedAt = now))
    }

    override suspend fun update(job: Job) {
        dao.update(
            job.toEntity(
                createdAt = job.createdAtEpochMs,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun delete(job: Job) {
        dao.delete(job.toEntity(job.createdAtEpochMs, job.updatedAtEpochMs))
    }
}

// エンティティの列名は v1 のまま（eventDate など）。
// 列名を変えると手書きマイグレーションが必要になるので、ここで名前を吸収している。
private fun JobEntity.toModel() = Job(
    id = id,
    name = name,
    venueName = venueName,
    eventDateEpochMs = eventDate,
    loadInAtEpochMs = loadInAt,
    showAtEpochMs = showAt,
    clientName = clientName,
    contact = contact,
    notes = notes,
    createdAtEpochMs = createdAt,
    updatedAtEpochMs = updatedAt,
)

private fun Job.toEntity(createdAt: Long, updatedAt: Long) = JobEntity(
    id = id,
    name = name,
    venueName = venueName,
    eventDate = eventDateEpochMs,
    loadInAt = loadInAtEpochMs,
    showAt = showAtEpochMs,
    clientName = clientName,
    contact = contact,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
