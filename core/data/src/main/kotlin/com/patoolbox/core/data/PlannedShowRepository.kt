package com.patoolbox.core.data

import com.patoolbox.core.model.Job
import com.patoolbox.core.model.PlannedShow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 「今日の進行表」。
 *
 * 案件（[JobRepository]）と進行表（[ScheduleRepository]）は別々に持っているので、
 * 日付で今日のものだけを拾って 1本にまとめる係をここに置く。
 * 本番万能コントローラーの自動取り込みと、ホームの「もう始まっています」は
 * どちらもこの同じ結果を見る——別々に判定を書くと、片方だけ出る事故になる。
 */
interface PlannedShowRepository {

    /**
     * 今日の日付が入っている進行表を、開始時刻の早い順に返す。
     *
     * 「いまが当たっているか」の判定はここでは持たない（時計が進んでも Flow は
     * 流れないため）。時刻の判定は [PlannedShow.isRunningAt] などを使って
     * 画面側で行う。
     */
    fun observeToday(): Flow<List<PlannedShow>>
}

@Singleton
class DefaultPlannedShowRepository @Inject constructor(
    private val jobRepository: JobRepository,
    private val scheduleRepository: ScheduleRepository,
) : PlannedShowRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeToday(): Flow<List<PlannedShow>> =
        jobRepository.observeAll().flatMapLatest { jobs ->
            val today = jobs.filter { it.isOn(LocalDate.now()) }
            if (today.isEmpty()) return@flatMapLatest flowOf(emptyList())

            combine(
                today.map { job ->
                    scheduleRepository.observeForJob(job.id).map { items -> job to items }
                },
            ) { pairs ->
                pairs.mapNotNull { (job, items) -> PlannedShow.from(job, items) }
                    .sortedBy { it.startAtEpochMs }
            }
        }
}

/**
 * この案件がその日のものか。
 *
 * 搬入・本番・開催日のどれか1つでも当日なら拾う。現場によっては
 * 開催日だけ、搬入時刻だけ、という入れ方をするため。
 */
private fun Job.isOn(date: LocalDate): Boolean =
    listOfNotNull(eventDateEpochMs, loadInAtEpochMs, showAtEpochMs)
        .any { it.toLocalDate() == date }

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
