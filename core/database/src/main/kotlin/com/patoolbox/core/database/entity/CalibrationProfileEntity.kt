package com.patoolbox.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 入力デバイスごとの校正値。
 * 内蔵マイクと USB マイクで別に持てるよう、deviceKey + inputType で一意にする。
 */
@Entity(
    tableName = "calibration_profiles",
    indices = [Index(value = ["deviceKey", "inputType"], unique = true)],
)
data class CalibrationProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deviceKey: String,
    /** AudioInputType の名前 */
    val inputType: String,
    val offsetDb: Double,
    /** CalibrationMethod の名前 */
    val method: String,
    val calibratedAtEpochMs: Long?,
)
