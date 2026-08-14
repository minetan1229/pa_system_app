package com.patoolbox.feature.business

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patoolbox.core.billing.ProGate
import com.patoolbox.core.calc.InvoiceCalculator
import com.patoolbox.core.calc.InvoiceLine
import com.patoolbox.core.calc.InvoiceTotals
import com.patoolbox.core.calc.RateType
import com.patoolbox.core.calc.TaxMode
import com.patoolbox.core.calc.TaxRate
import com.patoolbox.core.calc.TaxRounding
import com.patoolbox.core.calc.WorkEntry
import com.patoolbox.core.calc.WorkLogCalculator
import com.patoolbox.core.calc.WorkSummary
import com.patoolbox.core.data.BackupRepository
import com.patoolbox.core.data.GearRepository
import com.patoolbox.core.data.InvoiceRepository
import com.patoolbox.core.data.RestoreResult
import com.patoolbox.core.data.SnapshotRepository
import com.patoolbox.core.data.WorkLogRepository
import com.patoolbox.core.data.di.IoDispatcher
import com.patoolbox.core.model.GearCategory
import com.patoolbox.core.model.GearItem
import com.patoolbox.core.model.GearStatus
import com.patoolbox.core.model.Invoice
import com.patoolbox.core.model.InvoiceLineItem
import com.patoolbox.core.model.ProStatus
import com.patoolbox.core.model.Snapshot
import com.patoolbox.core.model.SnapshotChannel
import com.patoolbox.core.model.WorkLogEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

// enum は名前で保存している。読めない名前が来ても既定値に落として画面を開けるようにする
private inline fun <reified T : Enum<T>> String.toEnumOr(fallback: T): T =
    runCatching { enumValueOf<T>(this) }.getOrDefault(fallback)

// --- 機材台帳 ---

data class GearUiState(
    val items: List<GearItem> = emptyList(),
    val filter: GearCategory? = null,
    val proStatus: ProStatus = ProStatus.Free,
) {
    val visible: List<GearItem>
        get() = filter?.let { category -> items.filter { it.category == category } } ?: items

    /** 現場に出せる本数の合計。台帳を開いて最初に見たい数字 */
    val usableCount: Int get() = items.filter { it.status.isUsable }.sumOf { it.quantity }
}

@HiltViewModel
class GearViewModel @Inject constructor(
    private val repository: GearRepository,
    proGate: ProGate,
) : ViewModel() {

    private val filter = MutableStateFlow<GearCategory?>(null)

    val uiState: StateFlow<GearUiState> = combine(
        repository.observeAll(),
        filter,
        proGate.proStatus,
    ) { items, category, proStatus ->
        GearUiState(items = items, filter = category, proStatus = proStatus)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), GearUiState())

    fun setFilter(category: GearCategory?) = filter.update { category }

    fun save(item: GearItem) {
        viewModelScope.launch { repository.save(item) }
    }

    fun delete(item: GearItem) {
        viewModelScope.launch { repository.delete(item.id) }
    }
}

// --- スナップショット ---

data class SnapshotListUiState(
    val snapshots: List<Snapshot> = emptyList(),
    val proStatus: ProStatus = ProStatus.Free,
)

@HiltViewModel
class SnapshotListViewModel @Inject constructor(
    private val repository: SnapshotRepository,
    proGate: ProGate,
) : ViewModel() {

    val uiState: StateFlow<SnapshotListUiState> = combine(
        repository.observeAll(),
        proGate.proStatus,
    ) { snapshots, proStatus ->
        SnapshotListUiState(snapshots, proStatus)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        SnapshotListUiState(),
    )

    fun create(title: String, consoleName: String, channelCount: Int, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            onCreated(
                repository.create(
                    title = title.ifBlank { "無題のスナップショット" },
                    consoleName = consoleName,
                    channelCount = channelCount.coerceIn(1, MAX_CHANNELS),
                ),
            )
        }
    }

    fun delete(snapshot: Snapshot) {
        viewModelScope.launch { repository.delete(snapshot.id) }
    }

    companion object {
        const val DEFAULT_CHANNELS = 16
        const val MAX_CHANNELS = 64
    }
}

@HiltViewModel
class SnapshotDetailViewModel @Inject constructor(
    private val repository: SnapshotRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val snapshotId: Long = savedStateHandle.get<Long>(KEY_SNAPSHOT_ID) ?: 0L

    val snapshot: StateFlow<Snapshot?> = repository.observeWithChannels(snapshotId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    fun saveChannel(channel: SnapshotChannel) {
        viewModelScope.launch { repository.saveChannel(snapshotId, channel) }
    }

    fun updateDetails(title: String, consoleName: String, note: String) {
        viewModelScope.launch { repository.updateDetails(snapshotId, title, consoleName, note) }
    }

    companion object {
        const val KEY_SNAPSHOT_ID = "snapshotId"
    }
}

// --- 請求書・見積書 ---

data class InvoiceListUiState(
    val invoices: List<Invoice> = emptyList(),
    val proStatus: ProStatus = ProStatus.Free,
)

@HiltViewModel
class InvoiceListViewModel @Inject constructor(
    private val repository: InvoiceRepository,
    proGate: ProGate,
) : ViewModel() {

    val uiState: StateFlow<InvoiceListUiState> = combine(
        repository.observeAll(),
        proGate.proStatus,
    ) { invoices, proStatus -> InvoiceListUiState(invoices, proStatus) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            InvoiceListUiState(),
        )

    fun create(isEstimate: Boolean, clientName: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch { onCreated(repository.create(isEstimate, clientName)) }
    }

    fun delete(invoice: Invoice) {
        viewModelScope.launch { repository.delete(invoice.id) }
    }
}

data class InvoiceDetailUiState(
    val invoice: Invoice? = null,
    val totals: InvoiceTotals = InvoiceTotals(emptyList(), emptyList()),
) {
    val taxMode: TaxMode get() = invoice?.taxModeName?.toEnumOr(TaxMode.EXCLUSIVE) ?: TaxMode.EXCLUSIVE
    val rounding: TaxRounding
        get() = invoice?.taxRoundingName?.toEnumOr(TaxRounding.DOWN) ?: TaxRounding.DOWN

    /** 登録番号が入っていて、形が間違っている場合だけ警告する */
    val registrationLooksWrong: Boolean
        get() = invoice?.registrationNumber?.isNotBlank() == true &&
            !InvoiceCalculator.isRegistrationNumberShaped(invoice.registrationNumber)
}

@HiltViewModel
class InvoiceDetailViewModel @Inject constructor(
    private val repository: InvoiceRepository,
    private val documentWriter: InvoiceDocumentWriter,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val invoiceId: Long = savedStateHandle.get<Long>(KEY_INVOICE_ID) ?: 0L

    val uiState: StateFlow<InvoiceDetailUiState> = repository.observeWithLines(invoiceId)
        .map { invoice ->
            InvoiceDetailUiState(
                invoice = invoice,
                totals = invoice?.let { calculate(it) }
                    ?: InvoiceTotals(emptyList(), emptyList()),
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            InvoiceDetailUiState(),
        )

    fun save(invoice: Invoice) {
        viewModelScope.launch { repository.save(invoice) }
    }

    fun addLine() {
        viewModelScope.launch { repository.addLine(invoiceId) }
    }

    fun saveLine(line: InvoiceLineItem) {
        viewModelScope.launch { repository.saveLine(invoiceId, line) }
    }

    fun deleteLine(line: InvoiceLineItem) {
        viewModelScope.launch { repository.deleteLine(line.id) }
    }

    fun suggestedFileName(): String {
        val invoice = uiState.value.invoice ?: return "invoice.pdf"
        val number = invoice.number.ifBlank { invoice.id.toString() }
        return "${invoice.documentLabel}_$number.pdf"
    }

    fun exportPdf(output: OutputStream) {
        val state = uiState.value
        val invoice = state.invoice ?: return
        viewModelScope.launch {
            withContext(ioDispatcher) {
                output.use { documentWriter.write(invoice, state.totals, it) }
            }
        }
    }

    private fun calculate(invoice: Invoice): InvoiceTotals = InvoiceCalculator.calculate(
        lines = invoice.lines.map { it.toCalcLine() },
        mode = invoice.taxModeName.toEnumOr(TaxMode.EXCLUSIVE),
        rounding = invoice.taxRoundingName.toEnumOr(TaxRounding.DOWN),
    )

    companion object {
        const val KEY_INVOICE_ID = "invoiceId"
    }
}

internal fun InvoiceLineItem.toCalcLine() = InvoiceLine(
    description = description,
    quantity = quantity,
    unit = unit,
    unitPrice = unitPrice,
    taxRate = taxRateName.toEnumOr(TaxRate.STANDARD),
)

// --- 稼働記録 ---

data class WorkLogUiState(
    val entries: List<WorkLogEntry> = emptyList(),
    val proStatus: ProStatus = ProStatus.Free,
) {
    val total: WorkSummary get() = WorkLogCalculator.total(entries.map { it.toCalcEntry() })

    fun summaryOf(entry: WorkLogEntry): WorkSummary =
        WorkLogCalculator.summarize(entry.toCalcEntry())
}

internal fun WorkLogEntry.toCalcEntry() = WorkEntry(
    dateEpochMs = dateEpochMs,
    startMinutesOfDay = startMinutesOfDay,
    endMinutesOfDay = endMinutesOfDay,
    breakMinutes = breakMinutes,
    rateType = rateTypeName.toEnumOr(RateType.DAILY),
    rate = rate,
    multiplier = multiplier,
    transportFee = transportFee,
)

@HiltViewModel
class WorkLogViewModel @Inject constructor(
    private val repository: WorkLogRepository,
    proGate: ProGate,
) : ViewModel() {

    val uiState: StateFlow<WorkLogUiState> = combine(
        repository.observeAll(),
        proGate.proStatus,
    ) { entries, proStatus -> WorkLogUiState(entries, proStatus) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), WorkLogUiState())

    fun save(entry: WorkLogEntry) {
        viewModelScope.launch { repository.save(entry) }
    }

    fun delete(entry: WorkLogEntry) {
        viewModelScope.launch { repository.delete(entry.id) }
    }
}

// --- バックアップ ---

data class BackupUiState(
    val isWorking: Boolean = false,
    val message: String? = null,
    val restoreSucceeded: Boolean = false,
    val excluded: List<String> = emptyList(),
    val proStatus: ProStatus = ProStatus.Free,
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repository: BackupRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    proGate: ProGate,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState(excluded = repository.excluded))
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            proGate.proStatus.collect { status -> _uiState.update { it.copy(proStatus = status) } }
        }
    }

    fun suggestedFileName(): String = repository.suggestedFileName()

    fun export(output: OutputStream) {
        _uiState.update { it.copy(isWorking = true, message = null) }
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { repository.export(output) } }
                .onSuccess {
                    _uiState.update { it.copy(isWorking = false, message = "書き出しました") }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isWorking = false, message = throwable.message ?: "失敗しました")
                    }
                }
        }
    }

    fun restore(input: InputStream) {
        _uiState.update { it.copy(isWorking = true, message = null) }
        viewModelScope.launch {
            val result = withContext(ioDispatcher) { repository.restore(input) }
            _uiState.update {
                when (result) {
                    is RestoreResult.Success -> it.copy(
                        isWorking = false,
                        restoreSucceeded = true,
                        message = "復元しました。アプリを一度終了して開き直してください",
                    )

                    is RestoreResult.TooNew -> it.copy(
                        isWorking = false,
                        message = "このバックアップは新しいアプリで作られています" +
                            "（バックアップ v${result.backupVersion} / アプリ v${result.appVersion}）。" +
                            "アプリを更新してからやり直してください",
                    )

                    RestoreResult.NotADatabase -> it.copy(
                        isWorking = false,
                        message = "このファイルはバックアップではありません",
                    )

                    is RestoreResult.Failed -> it.copy(
                        isWorking = false,
                        message = result.message,
                    )
                }
            }
        }
    }
}
