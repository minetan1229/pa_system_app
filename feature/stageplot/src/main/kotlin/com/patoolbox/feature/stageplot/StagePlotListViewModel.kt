package com.patoolbox.feature.stageplot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patoolbox.core.billing.ProGate
import com.patoolbox.core.data.StagePlotRepository
import com.patoolbox.core.model.ProStatus
import com.patoolbox.core.model.StagePlot
import com.patoolbox.core.model.saveLimit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StagePlotListUiState(
    val plots: List<StagePlot> = emptyList(),
    val proStatus: ProStatus = ProStatus.Free,
) {
    /** 無料版の保存件数上限。Pro は null（無制限） */
    val saveLimit: Int? get() = proStatus.saveLimit()

    val canCreate: Boolean get() = saveLimit?.let { plots.size < it } ?: true
}

@HiltViewModel
class StagePlotListViewModel @Inject constructor(
    private val repository: StagePlotRepository,
    proGate: ProGate,
) : ViewModel() {

    val uiState: StateFlow<StagePlotListUiState> = combine(
        repository.observeAll(),
        proGate.proStatus,
    ) { plots, proStatus ->
        StagePlotListUiState(plots = plots, proStatus = proStatus)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = StagePlotListUiState(),
    )

    /** 画面側でもボタンを無効化しているが、こちらでも弾いて二重に守る。 */
    fun create(name: String, onCreated: (Long) -> Unit) {
        if (!uiState.value.canCreate) return
        viewModelScope.launch {
            onCreated(repository.create(name = name.ifBlank { DEFAULT_NAME }, jobId = null))
        }
    }

    fun delete(plot: StagePlot) {
        viewModelScope.launch { repository.delete(plot.id) }
    }

    private companion object {
        const val DEFAULT_NAME = "無題の配置図"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
