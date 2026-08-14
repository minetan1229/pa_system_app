package com.patoolbox.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.patoolbox.core.database.dao.CalibrationProfileDao
import com.patoolbox.core.database.dao.JobDao
import com.patoolbox.core.database.dao.MeasurementDao
import com.patoolbox.core.database.dao.RecordingDao
import com.patoolbox.core.database.dao.PatchSheetDao
import com.patoolbox.core.database.dao.ScheduleItemDao
import com.patoolbox.core.database.dao.StagePlotDao
import com.patoolbox.core.database.entity.CalibrationProfileEntity
import com.patoolbox.core.database.entity.JobEntity
import com.patoolbox.core.database.entity.MeasurementEntity
import com.patoolbox.core.database.entity.MeasurementSampleEntity
import com.patoolbox.core.database.entity.RecordingEntity
import com.patoolbox.core.database.entity.PatchRowEntity
import com.patoolbox.core.database.entity.PatchSheetEntity
import com.patoolbox.core.database.entity.ScheduleItemEntity
import com.patoolbox.core.database.entity.StageItemEntity
import com.patoolbox.core.database.entity.StagePlotEntity

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
        MeasurementEntity::class,
        MeasurementSampleEntity::class,
        StagePlotEntity::class,
        StageItemEntity::class,
        RecordingEntity::class,
    ],
    version = 6,
    exportSchema = true,
    autoMigrations = [
        // v2: calibration_profiles を追加
        AutoMigration(from = 1, to = 2),
        // v3: patch_sheets / patch_rows / schedule_items を追加
        AutoMigration(from = 2, to = 3),
        // v4: measurements / measurement_samples を追加
        AutoMigration(from = 3, to = 4),
        // v5: stage_plots / stage_items を追加
        AutoMigration(from = 4, to = 5),
        // v6: recordings を追加
        AutoMigration(from = 5, to = 6),
    ],
)
abstract class PaDatabase : RoomDatabase() {
    abstract fun jobDao(): JobDao
    abstract fun calibrationProfileDao(): CalibrationProfileDao
    abstract fun patchSheetDao(): PatchSheetDao
    abstract fun scheduleItemDao(): ScheduleItemDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun stagePlotDao(): StagePlotDao
    abstract fun recordingDao(): RecordingDao

    companion object {
        const val NAME = "pa_toolbox.db"
    }
}
