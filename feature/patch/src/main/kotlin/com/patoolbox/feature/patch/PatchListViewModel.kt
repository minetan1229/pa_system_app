package com.patoolbox.feature.patch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patoolbox.core.billing.ProGate
import com.patoolbox.core.data.PatchSheetRepository
import com.patoolbox.core.model.PatchSheet
import com.patoolbox.core.model.ProStatus
import com.patoolbox.core.model.saveLimit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PatchListUiState(
    val sheets: List<PatchSheet> = emptyList(),
    val proStatus: ProStatus = ProStatus.Free,
) {
    /** 無料版の保存件数上限。Pro は null（無制限） */
    val saveLimit: Int? get() = proStatus.saveLimit()

    /** これ以上作れるか。 */
    val canCreate: Boolean get() = saveLimit?.let { sheets.size < it } ?: true
}

@HiltViewModel
class PatchListViewModel @Inject constructor(
    private val repository: PatchSheetRepository,
    proGate: ProGate,
) : ViewModel() {

    val uiState: StateFlow<PatchListUiState> = combine(
        repository.observeAll(),
        proGate.proStatus,
    ) { sheets, proStatus ->
        PatchListUiState(sheets = sheets, proStatus = proStatus)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = PatchListUiState(),
    )

    /**
     * 新規作成。件数制限に達していたら何もしない。
     * 画面側でボタンを無効化しているが、こちらでも弾いて二重に守る。
     */
    fun create(name: String, channelCount: Int, onCreated: (Long) -> Unit) {
        if (!uiState.value.canCreate) return
        viewModelScope.launch {
            val id = repository.create(
                name = name.ifBlank { DEFAULT_NAME },
                jobId = null,
                channelCount = channelCount.coerceIn(MIN_CHANNELS, MAX_CHANNELS),
            )
            onCreated(id)
        }
    }

    fun delete(sheet: PatchSheet) {
        viewModelScope.launch { repository.delete(sheet) }
    }

    companion object {
        const val MIN_CHANNELS = 1
        const val MAX_CHANNELS = 64
        const val DEFAULT_CHANNELS = 16
        private const val DEFAULT_NAME = "無題のパッチ表"
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
