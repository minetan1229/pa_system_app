package com.patoolbox.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.patoolbox.core.database.dao.JobDao
import com.patoolbox.core.database.entity.JobEntity

/**
 * 端末内の唯一のDB。測定ログ・パッチ表・機材台帳などをここに足していく。
 *
 * 測定ログ（MeasurementSample）は行数が膨大になるので、
 * Phase 4 で追加するときはバンドデータを BLOB に丸め、保持期間の自動削除も併せて入れる。
 *
 * スキーマは schemas/ にコミットしてマイグレーションをテストできるようにしている。
 */
@Database(
    entities = [
        JobEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class PaDatabase : RoomDatabase() {
    abstract fun jobDao(): JobDao

    companion object {
        const val NAME = "pa_toolbox.db"
    }
}
