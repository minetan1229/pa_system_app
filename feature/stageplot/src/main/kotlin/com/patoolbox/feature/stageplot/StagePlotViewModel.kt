package com.patoolbox.feature.stageplot

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patoolbox.core.billing.ProGate
import com.patoolbox.core.data.StagePlotRepository
import com.patoolbox.core.data.di.IoDispatcher
import com.patoolbox.core.export.StagePlotPdfWriter
import com.patoolbox.core.model.ProStatus
import com.patoolbox.core.model.StageItem
import com.patoolbox.core.model.StagePlot
import com.patoolbox.core.model.StageSymbol
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import javax.inject.Inject

data class StagePlotUiState(
    val plot: StagePlot? = null,
    val selectedItemId: Long? = null,
    val proStatus: ProStatus = ProStatus.Free,
) {
    val selectedItem: StageItem?
        get() = plot?.items?.firstOrNull { it.id == selectedItemId }
}

/**
 * ステージプロットの編集。
 *
 * 記号の移動はドラッグ中ずっと発生するので、DB には座標だけを書く
 * （[StagePlotRepository.moveItem]）。エンティティ全体を書き戻すと、
 * 同時に編集中のラベルを古い値で踏み潰す。
 */
@HiltViewModel
class StagePlotViewModel @Inject constructor(
    private val repository: StagePlotRepository,
    private val pdfWriter: StagePlotPdfWriter,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    savedStateHandle: SavedStateHandle,
    proGate: ProGate,
) : ViewModel() {

    private val plotId: Long = savedStateHandle.get<Long>(KEY_PLOT_ID) ?: 0L
    private val selection = MutableStateFlow<Long?>(null)
    private val pro = MutableStateFlow(ProStatus.Free)

    val uiState: StateFlow<StagePlotUiState> = combine(
        repository.observeWithItems(plotId),
        selection,
        pro,
    ) { plot, selected, proStatus ->
        StagePlotUiState(
            plot = plot,
            // 消された記号を選択したままにしない
            selectedItemId = selected?.takeIf { id -> plot?.items?.any { it.id == id } == true },
            proStatus = proStatus,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = StagePlotUiState(),
    )

    init {
        viewModelScope.launch {
            proGate.proStatus.collect { status -> pro.update { status } }
        }
    }

    fun select(itemId: Long?) {
        selection.update { itemId }
    }

    /** パレットから追加する。置く場所はステージの中央にして、そこから動かしてもらう。 */
    fun addItem(symbol: StageSymbol) {
        viewModelScope.launch {
            val id = repository.addItem(
                plotId,
                StageItem(symbol = symbol, x = NEW_ITEM_X, y = NEW_ITEM_Y),
            )
            selection.update { id }
        }
    }

    fun moveItem(itemId: Long, x: Float, y: Float) {
        viewModelScope.launch { repository.moveItem(plotId, itemId, x, y) }
    }

    fun renameSelected(label: String) {
        val item = uiState.value.selectedItem ?: return
        viewModelScope.launch { repository.renameItem(plotId, item, label) }
    }

    fun recolorSelected(colorIndex: Int) {
        val itemId = uiState.value.selectedItemId ?: return
        viewModelScope.launch { repository.recolorItem(plotId, itemId, colorIndex) }
    }

    fun deleteSelected() {
        val itemId = uiState.value.selectedItemId ?: return
        selection.update { null }
        viewModelScope.launch { repository.deleteItem(plotId, itemId) }
    }

    fun updateDetails(name: String, widthMeters: Double, depthMeters: Double, notes: String) {
        val plot = uiState.value.plot ?: return
        viewModelScope.launch {
            repository.updateDetails(
                plot.copy(
                    name = name,
                    stageWidthMeters = widthMeters,
                    stageDepthMeters = depthMeters,
                    notes = notes,
                ),
            )
        }
    }

    fun suggestedFileName(): String = "${uiState.value.plot?.name.orEmpty()}.pdf"

    fun exportPdf(output: OutputStream) {
        val plot = uiState.value.plot ?: return
        viewModelScope.launch {
            withContext(ioDispatcher) {
                output.use { pdfWriter.write(plot, it) }
            }
        }
    }

    companion object {
        /** ナビゲーションの引数名と一致させること */
        const val KEY_PLOT_ID = "plotId"

        private const val STOP_TIMEOUT_MS = 5_000L
        private const val NEW_ITEM_X = 0.5f
        private const val NEW_ITEM_Y = 0.5f
    }
}
