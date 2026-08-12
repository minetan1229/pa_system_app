package com.patoolbox.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.patoolbox.core.database.dao.CalibrationProfileDao
import com.patoolbox.core.database.dao.JobDao
import com.patoolbox.core.database.entity.CalibrationProfileEntity
import com.patoolbox.core.database.entity.JobEntity

/**
 * 端末内の唯一のDB。測定ログ・パッチ表・機材台帳などをここに足していく。
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
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        // v2: calibration_profiles テーブルを追加
        AutoMigration(from = 1, to = 2),
    ],
)
abstract class PaDatabase : RoomDatabase() {
    abstract fun jobDao(): JobDao
    abstract fun calibrationProfileDao(): CalibrationProfileDao

    companion object {
        const val NAME = "pa_toolbox.db"
    }
}
