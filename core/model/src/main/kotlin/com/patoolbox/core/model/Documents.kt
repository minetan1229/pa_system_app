package com.patoolbox.core.model

/**
 * 案件（現場）。パッチ表・進行表・測定結果はすべてこれに紐づく。
 *
 * 時刻は epoch ミリ秒で持ち、表示側で端末のタイムゾーンに変換する。
 */
data class Job(
    val id: Long = 0,
    val name: String,
    val venueName: String = "",
    val eventDateEpochMs: Long? = null,
    val loadInAtEpochMs: Long? = null,
    val showAtEpochMs: Long? = null,
    val clientName: String = "",
    val contact: String = "",
    val notes: String = "",
    val createdAtEpochMs: Long = 0,
    val updatedAtEpochMs: Long = 0,
)

/**
 * パッチ表（インプットリスト）。
 * [jobId] が null なら案件に紐づかないテンプレート。
 */
data class PatchSheet(
    val id: Long = 0,
    val jobId: Long? = null,
    val name: String,
    val isTemplate: Boolean = false,
    val rows: List<PatchRow> = emptyList(),
    val updatedAtEpochMs: Long = 0,
)

/**
 * パッチ表の1行 = 卓の1ch。
 *
 * ファンタムは行ごとに持つ。コンデンサマイクとDIが混ざる現場で
 * 「どのchに48Vを入れるか」が一目で分かることが実用上重要。
 */
data class PatchRow(
    val id: Long = 0,
    val channel: Int,
    val source: String = "",
    val micModel: String = "",
    val standType: String = "",
    val phantom: Boolean = false,
    val multiNumber: String = "",
    val notes: String = "",
) {
    val isEmpty: Boolean
        get() = source.isBlank() && micModel.isBlank() && notes.isBlank()
}

/** 進行表の1項目。 */
data class ScheduleItem(
    val id: Long = 0,
    val jobId: Long,
    val title: String,
    val startAtEpochMs: Long? = null,
    val durationMinutes: Int = 0,
    val owner: String = "",
) {
    /** 開始時刻と長さから終了時刻を出す。 */
    val endAtEpochMs: Long?
        get() = startAtEpochMs?.let { it + durationMinutes * MILLIS_PER_MINUTE }

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
    }
}

/** スタンドの種類。パッチ表で選ぶ頻度が高いので定型を用意する。 */
object StandTypes {
    const val TALL_BOOM = "ブーム(背高)"
    const val SHORT_BOOM = "ブーム(卓上)"
    const val STRAIGHT = "ストレート"
    const val CLIP = "クリップ"
    const val NONE = "なし"

    val ALL = listOf(TALL_BOOM, SHORT_BOOM, STRAIGHT, CLIP, NONE)
}
