package com.patoolbox.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.patoolbox.core.database.entity.MeasurementEntity
import com.patoolbox.core.database.entity.MeasurementSampleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {

    @Query("SELECT * FROM measurements ORDER BY startedAtEpochMs DESC")
    fun observeAll(): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE id = :id")
    suspend fun findById(id: Long): MeasurementEntity?

    @Query("SELECT * FROM measurement_samples WHERE measurementId = :id ORDER BY offsetMs")
    suspend fun samplesOf(id: Long): List<MeasurementSampleEntity>

    @Query("SELECT COUNT(*) FROM measurements")
    suspend fun count(): Int

    @Insert
    suspend fun insert(measurement: MeasurementEntity): Long

    @Insert
    suspend fun insertSamples(samples: List<MeasurementSampleEntity>)

    @Query("DELETE FROM measurements WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 要約と時系列をまとめて保存する。
     * 途中で落ちて要約だけ残る（＝グラフが出ない記録）のを避けるためトランザクションにする。
     */
    @Transaction
    suspend fun save(
        measurement: MeasurementEntity,
        samples: List<MeasurementSampleEntity>,
    ): Long {
        val id = insert(measurement)
        insertSamples(samples.map { it.copy(measurementId = id) })
        return id
    }
}
