package com.patoolbox.core.model

/**
 * 日付の入った進行表 1本。
 *
 * 案件（[Job]）に日付と搬入時刻が入っているので、進行表を「いつの現場のものか」
 * まで含めて 1つにまとめられる。本番万能コントローラーはこれを取り込み、
 * ホームは「今日のこれ、もう始まっていますよ」を出すのに使う。
 *
 * 時刻は保存せず [ScheduleTimeline] で積み上げて出す。現場では 1項目が伸びると
 * 以降が全部押すので、時刻を持たせると直す手間が現実的でなくなる。
 *
 * @param anchorEpochMs 先頭の開始時刻。搬入時刻 → 本番時刻 → 日付の0時 の順に拾う
 */
data class PlannedShow(
    val job: Job,
    val items: List<ScheduleItem>,
    val anchorEpochMs: Long,
    val entries: List<TimelineEntry>,
) {
    val startAtEpochMs: Long
        get() = entries.firstOrNull()?.startAtEpochMs ?: anchorEpochMs

    val endAtEpochMs: Long
        get() = entries.maxOfOrNull { it.endAtEpochMs } ?: anchorEpochMs

    val totalMinutes: Int get() = ScheduleTimeline.totalMinutes(items)

    /** 予定の開始時刻を過ぎているか。「もう始まっていますよ」の判定 */
    fun hasStarted(nowEpochMs: Long): Boolean = nowEpochMs >= startAtEpochMs

    /** いま進行中の時間帯に入っているか。終わった現場を蒸し返さないための条件 */
    fun isRunningAt(nowEpochMs: Long): Boolean =
        nowEpochMs >= startAtEpochMs && nowEpochMs < endAtEpochMs

    /** いまの時刻が当たっている項目。無ければ null（開始前か、全部終わった後） */
    fun entryAt(nowEpochMs: Long): TimelineEntry? = entries.firstOrNull { entry ->
        nowEpochMs >= entry.startAtEpochMs && nowEpochMs < entry.endAtEpochMs
    }

    companion object {

        /**
         * 案件と項目から組み立てる。項目が空なら進行表として成立しないので null。
         */
        fun from(job: Job, items: List<ScheduleItem>): PlannedShow? {
            if (items.isEmpty()) return null
            val anchor = job.loadInAtEpochMs
                ?: job.showAtEpochMs
                ?: job.eventDateEpochMs
                ?: return null
            return PlannedShow(
                job = job,
                items = items,
                anchorEpochMs = anchor,
                entries = ScheduleTimeline.build(anchor, items),
            )
        }

        /**
         * 「いま出すならどれか」を1本だけ選ぶ。
         *
         * 進行中のものが最優先。無ければこれから始まるいちばん近いもの。
         * それも無ければ（全部終わっている）null を返す——終わった現場を
         * 本番中の画面に出しても邪魔にしかならない。
         */
        fun pick(shows: List<PlannedShow>, nowEpochMs: Long): PlannedShow? =
            shows.firstOrNull { it.isRunningAt(nowEpochMs) }
                ?: shows.filter { it.startAtEpochMs > nowEpochMs }
                    .minByOrNull { it.startAtEpochMs }
    }
}
