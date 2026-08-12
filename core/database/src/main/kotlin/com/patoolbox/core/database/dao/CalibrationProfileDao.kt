package com.patoolbox.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.patoolbox.core.database.entity.CalibrationProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalibrationProfileDao {

    @Query("SELECT * FROM calibration_profiles ORDER BY calibratedAtEpochMs DESC")
    fun observeAll(): Flow<List<CalibrationProfileEntity>>

    @Query(
        "SELECT * FROM calibration_profiles WHERE deviceKey = :deviceKey AND inputType = :inputType",
    )
    fun observeFor(deviceKey: String, inputType: String): Flow<CalibrationProfileEntity?>

    @Upsert
    suspend fun upsert(profile: CalibrationProfileEntity)

    @Query("DELETE FROM calibration_profiles WHERE deviceKey = :deviceKey AND inputType = :inputType")
    suspend fun delete(deviceKey: String, inputType: String)
}
