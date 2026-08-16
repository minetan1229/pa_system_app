package com.patoolbox.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * SE パッド。音声ファイル本体はアプリ内部ストレージに置き、ここには参照だけを持つ。
 * 録音（recordings）と同じ分け方。
 */
@Entity(tableName = "sound_cues")
data class SoundCueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val fileName: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val position: Int,
    val loop: Boolean = false,
    val gain: Float = 1f,
    val colorIndex: Int = 0,
    val importedAtEpochMs: Long,
)
