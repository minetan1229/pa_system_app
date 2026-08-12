package com.patoolbox.feature.schedule

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patoolbox.core.data.JobRepository
import com.patoolbox.core.data.ScheduleRepository
import com.patoolbox.core.model.Job
import com.patoolbox.core.model.ScheduleItem
import com.patoolbox.core.model.ScheduleTimeline
import com.patoolbox.core.model.TimelineEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScheduleUiState(
    val job: Job? = null,
    val items: List<ScheduleItem> = emptyList(),
    val entries: List<TimelineEntry> = emptyList(),
    val overrunning: List<TimelineEntry> = emptyList(),
    val anchorEpochMs: Long = 0,
    val hasAnchor: Boolean = false,
) {
    val totalMinutes: Int get() = ScheduleTimeline.totalMinutes(items)
}

/**
 * 進行表。
 *
 * 時刻は保存せず、案件の搬入時刻を起点に長さから計算する（[ScheduleTimeline]）。
 * 現場では1項目が伸びると以降が全部押すので、時刻を手入力させると
 * 直す手間が現実的でなくなる。
 */
@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    jobRepository: JobRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val jobId: Long = savedStateHandle.get<Long>(KEY_JOB_ID) ?: 0L

    val uiState: StateFlow<ScheduleUiState> = combine(
        jobRepository.observeById(jobId),
        scheduleRepository.observeForJob(jobId),
    ) { job, items ->
        val anchor = job?.loadInAtEpochMs
        val entries = ScheduleTimeline.build(
            anchorEpochMs = anchor ?: 0L,
            items = items,
        )
        ScheduleUiState(
            job = job,
            items = items,
            entries = entries,
            overrunning = ScheduleTimeline.overrunningAnchors(entries),
            anchorEpochMs = anchor ?: 0L,
            hasAnchor = anchor != null,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = ScheduleUiState(),
    )

    fun add(title: String, durationMinutes: Int, owner: String, fixedStartEpochMs: Long?) {
        viewModelScope.launch {
            scheduleRepository.add(
                ScheduleItem(
                    jobId = jobId,
                    title = title.ifBlank { DEFAULT_TITLE },
                    durationMinutes = durationMinutes.coerceIn(0, MAX_MINUTES),
                    owner = owner,
                    startAtEpochMs = fixedStartEpochMs,
                ),
            )
        }
    }

    fun update(item: ScheduleItem) {
        viewModelScope.launch { scheduleRepository.update(item) }
    }

    fun delete(item: ScheduleItem) {
        viewModelScope.launch { scheduleRepository.delete(item) }
    }

    /** 1つ上/下へ動かす。ドラッグ操作は現場の手袋では扱いにくいのでボタンにしている。 */
    fun move(item: ScheduleItem, offset: Int) {
        val items = uiState.value.items.toMutableList()
        val index = items.indexOfFirst { it.id == item.id }
        val target = index + offset
        if (index < 0 || target !in items.indices) return

        items.add(target, items.removeAt(index))
        viewModelScope.launch { scheduleRepository.reorder(items) }
    }

    companion object {
        /** ナビゲーションの引数名。ルートの property 名と一致させる */
        const val KEY_JOB_ID = "jobId"
        const val MAX_MINUTES = 24 * 60
        private const val DEFAULT_TITLE = "無題"
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
