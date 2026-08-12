package com.patoolbox.feature.patch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patoolbox.core.data.PatchSheetRepository
import com.patoolbox.core.model.PatchRow
import com.patoolbox.core.model.PatchSheet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PatchSheetUiState(
    val sheet: PatchSheet? = null,
) {
    val rows: List<PatchRow> get() = sheet?.rows.orEmpty()

    /** 48V を使う ch 数。ステージで電源を入れる前に確認する数字。 */
    val phantomCount: Int get() = rows.count { it.phantom }

    /** 記入済みの ch 数。埋まっていない枠が残っているかの目安。 */
    val filledCount: Int get() = rows.count { !it.isEmpty }
}

/**
 * パッチ表の編集。
 *
 * 保存ボタンは置かず、入力のたびに即保存する。現場では「書いたのに保存し忘れた」
 * のほうが事故になるため。
 */
@HiltViewModel
class PatchSheetViewModel @Inject constructor(
    private val repository: PatchSheetRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val sheetId: Long = savedStateHandle.get<Long>(KEY_SHEET_ID) ?: 0L

    val uiState: StateFlow<PatchSheetUiState> = repository.observeWithRows(sheetId)
        .map { PatchSheetUiState(sheet = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = PatchSheetUiState(),
        )

    fun updateRow(row: PatchRow) {
        viewModelScope.launch { repository.saveRow(sheetId, row) }
    }

    fun deleteRow(row: PatchRow) {
        viewModelScope.launch { repository.deleteRow(sheetId, row) }
    }

    fun addChannel() {
        viewModelScope.launch { repository.appendChannel(sheetId) }
    }

    fun rename(name: String) {
        val sheet = uiState.value.sheet ?: return
        viewModelScope.launch { repository.rename(sheet, name) }
    }

    companion object {
        /** ナビゲーションの引数名。ルート側の property 名と一致させる必要がある */
        const val KEY_SHEET_ID = "sheetId"
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
