package com.patoolbox.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.patoolbox.core.database.dao.CalibrationProfileDao
import com.patoolbox.core.database.dao.JobDao
import com.patoolbox.core.database.dao.PatchSheetDao
import com.patoolbox.core.database.dao.ScheduleItemDao
import com.patoolbox.core.database.entity.CalibrationProfileEntity
import com.patoolbox.core.database.entity.JobEntity
import com.patoolbox.core.database.entity.PatchRowEntity
import com.patoolbox.core.database.entity.PatchSheetEntity
import com.patoolbox.core.database.entity.ScheduleItemEntity

/**
 * 端末内の唯一のDB。
 *
 * 測定ログ（MeasurementSample）は行数が膨大になるので、
 * Phase 4 で追加するときはバンドデータを BLOB に丸め、保持期間の自動削除も併せて入れる。
 *
 * スキーマは schemas/ にコミットしている。テーブル追加だけの変更は
 * AutoMigration で済むので、破壊的マイグレーションは使わない。
 */
@Database(
    entities = [
        JobEntity::class,
        CalibrationProfileEntity::class,
        PatchSheetEntity::class,
        PatchRowEntity::class,
        ScheduleItemEntity::class,
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        // v2: calibration_profiles を追加
        AutoMigration(from = 1, to = 2),
        // v3: patch_sheets / patch_rows / schedule_items を追加
        AutoMigration(from = 2, to = 3),
    ],
)
abstract class PaDatabase : RoomDatabase() {
    abstract fun jobDao(): JobDao
    abstract fun calibrationProfileDao(): CalibrationProfileDao
    abstract fun patchSheetDao(): PatchSheetDao
    abstract fun scheduleItemDao(): ScheduleItemDao

    companion object {
        const val NAME = "pa_toolbox.db"
    }
}
