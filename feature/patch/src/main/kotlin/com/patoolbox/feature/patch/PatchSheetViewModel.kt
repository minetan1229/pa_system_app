package com.patoolbox.feature.patch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patoolbox.core.billing.ProGate
import com.patoolbox.core.data.PatchSheetRepository
import com.patoolbox.core.data.di.IoDispatcher
import com.patoolbox.core.export.DocumentTables
import com.patoolbox.core.export.PdfTableWriter
import com.patoolbox.core.model.PatchRow
import com.patoolbox.core.model.PatchSheet
import com.patoolbox.core.model.ProStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject

data class PatchSheetUiState(
    val sheet: PatchSheet? = null,
    val proStatus: ProStatus = ProStatus.Free,
) {
    /** PDF出力は Pro 専用。 */
    val canExport: Boolean get() = proStatus.isPro && sheet != null

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
    private val pdfWriter: PdfTableWriter,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    proGate: ProGate,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val sheetId: Long = savedStateHandle.get<Long>(KEY_SHEET_ID) ?: 0L

    val uiState: StateFlow<PatchSheetUiState> = combine(
        repository.observeWithRows(sheetId),
        proGate.proStatus,
    ) { sheet, proStatus ->
        PatchSheetUiState(sheet = sheet, proStatus = proStatus)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = PatchSheetUiState(),
    )

    /**
     * PDF を書き出す。ストリームは画面側が SAF から取得したもので、ここで閉じる。
     * 出力先の選択（SAF）は画面が持ち、ファイル生成はここが持つ。
     */
    fun exportPdf(output: OutputStream) {
        val sheet = uiState.value.sheet ?: return
        if (!uiState.value.canExport) return

        viewModelScope.launch {
            withContext(ioDispatcher) {
                output.use { stream ->
                    pdfWriter.write(
                        table = DocumentTables.patchSheet(
                            sheet = sheet,
                            jobName = "",
                            generatedAt = dateFormat.format(Date()),
                        ),
                        output = stream,
                    )
                }
            }
        }
    }

    /** 保存ダイアログに出す既定のファイル名。 */
    fun suggestedFileName(): String {
        val name = uiState.value.sheet?.name?.ifBlank { "patch" } ?: "patch"
        return "$name.pdf"
    }

    private val dateFormat: DateFormat =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)

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
