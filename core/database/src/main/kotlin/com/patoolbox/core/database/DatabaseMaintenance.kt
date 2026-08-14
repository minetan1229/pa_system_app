package com.patoolbox.core.database

import javax.inject.Inject
import javax.inject.Singleton

/**
 * データベースのファイルそのものを扱う操作。
 *
 * バックアップと復元のために用意している。Room の型を上位モジュールに
 * 露出させたくないので、必要な操作だけをここに集めた。
 */
@Singleton
class DatabaseMaintenance @Inject constructor(
    private val database: PaDatabase,
) {
    val databaseName: String get() = PaDatabase.NAME

    val schemaVersion: Int get() = PaDatabase.VERSION

    /**
     * WAL に溜まっている変更を本体のファイルに書き戻す。
     * 複製の前にこれを行わないと、直前の変更が入っていないファイルが出来上がる。
     */
    fun checkpoint() {
        runCatching {
            database.openHelper.writableDatabase
                .query("PRAGMA wal_checkpoint(FULL)")
                .use { it.moveToFirst() }
        }
    }

    /** 復元でファイルを差し替える前に閉じる。以降の利用にはプロセスの再起動が要る。 */
    fun close() {
        runCatching { database.close() }
    }
}
