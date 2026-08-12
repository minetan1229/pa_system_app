package com.patoolbox.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 案件（現場）。パッチ表・進行表・測定結果はすべてこれに紐づく。
 *
 * 時刻は epoch ミリ秒で持つ。タイムゾーンをまたぐ現場もあるので、
 * 表示側で端末のタイムゾーンに変換する。
 */
@Entity(tableName = "jobs")
data class JobEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val venueName: String = "",
    /** 本番日（epoch ミリ秒、日付のみ使う） */
    val eventDate: Long? = null,
    /** 搬入時刻 */
    val loadInAt: Long? = null,
    /** 開演時刻 */
    val showAt: Long? = null,
    val clientName: String = "",
    val contact: String = "",
    val notes: String = "",
    val createdAt: Long,
    val updatedAt: Long,
)
