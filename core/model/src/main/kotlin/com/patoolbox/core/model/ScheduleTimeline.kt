package com.patoolbox.core.model

/** 進行表の1行に計算済みの時刻を付けたもの。 */
data class TimelineEntry(
    val item: ScheduleItem,
    val startAtEpochMs: Long,
    val endAtEpochMs: Long,
    /** この項目が固定時刻（アンカー）で始まるか */
    val isAnchor: Boolean,
)

/**
 * 進行表の時刻を積み上げで計算する。
 *
 * 現場の進行表は「搬入 9:00 から順に積む」形が基本で、リハが伸びたら以降が全部後ろへ
 * ずれる（押す）。だから各項目に時刻を手入力させるのではなく、**長さだけを入れて
 * 時刻は計算する**のが正しい。1つ直せば以降が自動で追従する。
 *
 * ただし「本番は19:00固定」のような譲れない時刻もあるので、
 * [ScheduleItem.startAtEpochMs] が入っている項目はそこで積み直す（アンカー）。
 */
object ScheduleTimeline {

    private const val MILLIS_PER_MINUTE = 60_000L

    /**
     * @param anchorEpochMs 先頭の開始時刻（案件の搬入時刻など）
     * @param items sortOrder 順に並んだ項目
     */
    fun build(anchorEpochMs: Long, items: List<ScheduleItem>): List<TimelineEntry> {
        var cursor = anchorEpochMs

        return items.map { item ->
            val fixedStart = item.startAtEpochMs
            val isAnchor = fixedStart != null
            val start = fixedStart ?: cursor
            val end = start + item.durationMinutes * MILLIS_PER_MINUTE
            cursor = end

            TimelineEntry(
                item = item,
                startAtEpochMs = start,
                endAtEpochMs = end,
                isAnchor = isAnchor,
            )
        }
    }

    /** 進行表全体の所要時間（分）。 */
    fun totalMinutes(items: List<ScheduleItem>): Int = items.sumOf { it.durationMinutes }

    /**
     * アンカーに対して手前の積み上げが食い込んでいる項目を返す。
     *
     * 「リハが伸びて本番の固定時刻に間に合わない」状態を検出する。
     * 現場で一番知りたいのはこれなので、警告として出す。
     */
    fun overrunningAnchors(entries: List<TimelineEntry>): List<TimelineEntry> {
        val result = mutableListOf<TimelineEntry>()
        for (index in 1 until entries.size) {
            val entry = entries[index]
            if (entry.isAnchor && entries[index - 1].endAtEpochMs > entry.startAtEpochMs) {
                result += entry
            }
        }
        return result
    }
}
