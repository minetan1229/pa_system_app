package com.patoolbox.feature.job

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patoolbox.core.billing.ProGate
import com.patoolbox.core.data.JobRepository
import com.patoolbox.core.model.Job
import com.patoolbox.core.model.ProStatus
import com.patoolbox.core.model.saveLimit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JobListUiState(
    val jobs: List<Job> = emptyList(),
    val proStatus: ProStatus = ProStatus.Free,
) {
    val saveLimit: Int? get() = proStatus.saveLimit()
    val canCreate: Boolean get() = saveLimit?.let { jobs.size < it } ?: true
}

@HiltViewModel
class JobListViewModel @Inject constructor(
    private val repository: JobRepository,
    proGate: ProGate,
) : ViewModel() {

    val uiState: StateFlow<JobListUiState> = combine(
        repository.observeAll(),
        proGate.proStatus,
    ) { jobs, proStatus ->
        JobListUiState(jobs = jobs, proStatus = proStatus)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = JobListUiState(),
    )

    fun create(name: String, onCreated: (Long) -> Unit) {
        if (!uiState.value.canCreate) return
        viewModelScope.launch {
            val id = repository.create(Job(name = name.ifBlank { DEFAULT_NAME }))
            onCreated(id)
        }
    }

    fun delete(job: Job) {
        viewModelScope.launch { repository.delete(job) }
    }

    private companion object {
        const val DEFAULT_NAME = "無題の案件"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

data class JobDetailUiState(
    val job: Job? = null,
    val saved: Boolean = false,
)

/**
 * 案件の編集。
 *
 * 入力中の値は画面側が持ち、保存ボタンで確定する。パッチ表と違って
 * 日付・時刻のパースが絡むため、打ちかけの文字列をそのまま保存すると
 * 意図しない値が入るのを避けたい。
 */
@HiltViewModel
class JobDetailViewModel @Inject constructor(
    private val repository: JobRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val jobId: Long = savedStateHandle.get<Long>(KEY_JOB_ID) ?: 0L

    private val savedFlag = MutableStateFlow(false)

    val uiState: StateFlow<JobDetailUiState> = combine(
        repository.observeById(jobId),
        savedFlag,
    ) { job, saved ->
        JobDetailUiState(job = job, saved = saved)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = JobDetailUiState(),
    )

    val jobIdValue: Long get() = jobId

    fun save(job: Job) {
        viewModelScope.launch {
            repository.update(job.copy(id = jobId))
            savedFlag.update { true }
        }
    }

    fun clearSaved() {
        savedFlag.update { false }
    }

    companion object {
        /** ナビゲーションの引数名。ルートの property 名と一致させる */
        const val KEY_JOB_ID = "jobId"
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
