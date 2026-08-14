package com.patoolbox.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.patoolbox.core.database.dao.CalibrationProfileDao
import com.patoolbox.core.database.dao.JobDao
import com.patoolbox.core.database.dao.MeasurementDao
import com.patoolbox.core.database.dao.GearDao
import com.patoolbox.core.database.dao.InvoiceDao
import com.patoolbox.core.database.dao.RecordingDao
import com.patoolbox.core.database.dao.SnapshotDao
import com.patoolbox.core.database.dao.WorkLogDao
import com.patoolbox.core.database.dao.PatchSheetDao
import com.patoolbox.core.database.dao.ScheduleItemDao
import com.patoolbox.core.database.dao.StagePlotDao
import com.patoolbox.core.database.entity.CalibrationProfileEntity
import com.patoolbox.core.database.entity.JobEntity
import com.patoolbox.core.database.entity.MeasurementEntity
import com.patoolbox.core.database.entity.MeasurementSampleEntity
import com.patoolbox.core.database.entity.GearItemEntity
import com.patoolbox.core.database.entity.InvoiceEntity
import com.patoolbox.core.database.entity.InvoiceLineEntity
import com.patoolbox.core.database.entity.RecordingEntity
import com.patoolbox.core.database.entity.SnapshotChannelEntity
import com.patoolbox.core.database.entity.SnapshotEntity
import com.patoolbox.core.database.entity.WorkLogEntity
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
        GearItemEntity::class,
        SnapshotEntity::class,
        SnapshotChannelEntity::class,
        InvoiceEntity::class,
        InvoiceLineEntity::class,
        WorkLogEntity::class,
    ],
    version = 7,
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
        // v7: gear_items / snapshots / invoices / work_logs を追加
        AutoMigration(from = 6, to = 7),
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
    abstract fun gearDao(): GearDao
    abstract fun snapshotDao(): SnapshotDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun workLogDao(): WorkLogDao

    companion object {
        const val NAME = "pa_toolbox.db"

        /** @Database の version と必ず一致させること。バックアップの版数照合で使う */
        const val VERSION = 7
    }
}
