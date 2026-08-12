package com.patoolbox.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.patoolbox.core.database.entity.JobEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {

    @Query("SELECT * FROM jobs ORDER BY COALESCE(eventDate, createdAt) DESC")
    fun observeAll(): Flow<List<JobEntity>>

    @Query("SELECT * FROM jobs WHERE id = :id")
    fun observeById(id: Long): Flow<JobEntity?>

    @Query("SELECT COUNT(*) FROM jobs")
    suspend fun count(): Int

    @Insert
    suspend fun insert(job: JobEntity): Long

    @Update
    suspend fun update(job: JobEntity)

    @Delete
    suspend fun delete(job: JobEntity)
}
