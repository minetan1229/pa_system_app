package com.patoolbox.feature.wireless

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patoolbox.core.billing.ProGate
import com.patoolbox.core.calc.Intermodulation
import com.patoolbox.core.data.di.DefaultDispatcher
import com.patoolbox.core.model.ProStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class WirelessUiState(
    val frequenciesKHz: List<Long> = emptyList(),
    val guardKHz: Long = Intermodulation.DEFAULT_GUARD_KHZ,
    val minSpacingKHz: Long = Intermodulation.DEFAULT_MIN_SPACING_KHZ,
    val includeFifthOrder: Boolean = false,
    val report: Intermodulation.Report = Intermodulation.Report(emptyList(), emptyList()),
    val planFromMHz: String = "470.000",
    val planToMHz: String = "490.000",
    val planStepKHz: String = "25",
    val planCount: String = "8",
    /** 生成で足りなかった本数。0 なら要求どおり取れた */
    val planShortfall: Int? = null,
    val isPlanning: Boolean = false,
    val proStatus: ProStatus = ProStatus.Free,
    val error: String? = null,
) {
    val orders: Set<Intermodulation.Order>
        get() = if (includeFifthOrder) {
            Intermodulation.DEFAULT_ORDERS + Intermodulation.Order.FIFTH_TWO_TONE
        } else {
            Intermodulation.DEFAULT_ORDERS
        }
}

/**
 * ワイヤレスの周波数調整。
 *
 * **法令上どの周波数を使えるかは判定しない。** 使用可能な周波数は電波法と
 * 免許の条件で決まり、TVホワイトスペースに至っては運用地点ごとに違う。
 * ここで扱うのは「入力された組み合わせで混変調が起きるか」だけで、
 * どの周波数を入力するかは機材の取扱説明書と免許の条件に従ってもらう。
 */
@HiltViewModel
class WirelessViewModel @Inject constructor(
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    proGate: ProGate,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WirelessUiState())
    val uiState: StateFlow<WirelessUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            proGate.proStatus.collect { status ->
                _uiState.update { it.copy(proStatus = status) }
            }
        }
    }

    /** @param text "470.425" のような MHz 表記 */
    fun addFrequency(text: String) {
        val mhz = text.trim().toDoubleOrNull()
        if (mhz == null || mhz <= 0.0) {
            _uiState.update { it.copy(error = "周波数を MHz で入力してください（例 470.425）") }
            return
        }
        val khz = Intermodulation.mhzToKHz(mhz)
        _uiState.update { state ->
            if (khz in state.frequenciesKHz) {
                state.copy(error = "すでに入っています")
            } else {
                state.copy(
                    frequenciesKHz = (state.frequenciesKHz + khz).sorted(),
                    error = null,
                )
            }
        }
        reanalyze()
    }

    fun removeFrequency(khz: Long) {
        _uiState.update { it.copy(frequenciesKHz = it.frequenciesKHz - khz) }
        reanalyze()
    }

    fun clearAll() {
        _uiState.update {
            it.copy(
                frequenciesKHz = emptyList(),
                report = Intermodulation.Report(emptyList(), emptyList()),
                planShortfall = null,
            )
        }
    }

    fun setGuard(khz: Long) {
        _uiState.update { it.copy(guardKHz = khz) }
        reanalyze()
    }

    fun setMinSpacing(khz: Long) {
        _uiState.update { it.copy(minSpacingKHz = khz) }
        reanalyze()
    }

    fun toggleFifthOrder() {
        _uiState.update { it.copy(includeFifthOrder = !it.includeFifthOrder) }
        reanalyze()
    }

    fun setPlanFrom(value: String) = _uiState.update { it.copy(planFromMHz = value) }
    fun setPlanTo(value: String) = _uiState.update { it.copy(planToMHz = value) }
    fun setPlanStep(value: String) = _uiState.update { it.copy(planStepKHz = value) }
    fun setPlanCount(value: String) = _uiState.update { it.copy(planCount = value) }

    /**
     * 干渉しない組み合わせを生成して置き換える。
     *
     * すでに入力されている周波数は「動かせないもの」として扱う。
     * 他社の運用や、機材の都合で固定したい本数がある現場を想定している。
     */
    fun generatePlan(keepExisting: Boolean) {
        val state = _uiState.value
        val from = state.planFromMHz.trim().toDoubleOrNull()
        val to = state.planToMHz.trim().toDoubleOrNull()
        val step = state.planStepKHz.trim().toLongOrNull()
        val count = state.planCount.trim().toIntOrNull()

        if (from == null || to == null || step == null || count == null) {
            _uiState.update { it.copy(error = "生成の条件を数値で入力してください") }
            return
        }
        if (to <= from || step <= 0 || count <= 0) {
            _uiState.update { it.copy(error = "範囲・刻み・本数を確認してください") }
            return
        }

        _uiState.update { it.copy(isPlanning = true, error = null) }

        viewModelScope.launch {
            val fixed = if (keepExisting) state.frequenciesKHz else emptyList()
            val result = withContext(defaultDispatcher) {
                Intermodulation.plan(
                    Intermodulation.PlanRequest(
                        fromKHz = Intermodulation.mhzToKHz(from),
                        toKHz = Intermodulation.mhzToKHz(to),
                        stepKHz = step,
                        count = count,
                        guardKHz = state.guardKHz,
                        minSpacingKHz = state.minSpacingKHz,
                        orders = state.orders,
                        fixedKHz = fixed,
                    ),
                )
            }
            val combined = (fixed + result.frequenciesKHz).distinct().sorted()
            val report = withContext(defaultDispatcher) {
                analyze(combined, state)
            }
            _uiState.update {
                it.copy(
                    frequenciesKHz = combined,
                    report = report,
                    planShortfall = result.shortfall,
                    isPlanning = false,
                )
            }
        }
    }

    private fun reanalyze() {
        val state = _uiState.value
        viewModelScope.launch {
            val report = withContext(defaultDispatcher) {
                analyze(state.frequenciesKHz, state)
            }
            _uiState.update { it.copy(report = report, planShortfall = null) }
        }
    }

    private fun analyze(frequencies: List<Long>, state: WirelessUiState) =
        Intermodulation.analyze(
            frequenciesKHz = frequencies,
            guardKHz = state.guardKHz,
            minSpacingKHz = state.minSpacingKHz,
            orders = state.orders,
        )
}
