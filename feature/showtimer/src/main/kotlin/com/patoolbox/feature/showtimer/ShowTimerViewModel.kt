package com.patoolbox.feature.showtimer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

enum class TimerMode { COUNTDOWN, ELAPSED }

data class ShowTimerUiState(
    val mode: TimerMode = TimerMode.COUNTDOWN,
    val targetMinutes: Int = DEFAULT_MINUTES,
    val running: Boolean = false,
    val elapsedMillis: Long = 0,
) {
    /** カウントダウンの残り。マイナスは押し（超過）。 */
    val remainingMillis: Long get() = targetMinutes * MILLIS_PER_MINUTE - elapsedMillis

    val isOverrun: Boolean get() = mode == TimerMode.COUNTDOWN && remainingMillis < 0

    /** 画面に出す値（絶対値）。押しかどうかは [isOverrun] で色を変える。 */
    val displayMillis: Long
        get() = when (mode) {
            TimerMode.COUNTDOWN -> abs(remainingMillis)
            TimerMode.ELAPSED -> elapsedMillis
        }

    companion object {
        const val DEFAULT_MINUTES = 30
        const val MILLIS_PER_MINUTE = 60_000L
    }
}

/**
 * 本番の時間管理。
 *
 * 経過時間は tick の回数ではなく単調時計（nanoTime）の差分から出す。
 * tick を数える実装だと、画面が重いときや遅延が積もったときにずれていく。
 */
@HiltViewModel
class ShowTimerViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ShowTimerUiState())
    val uiState: StateFlow<ShowTimerUiState> = _uiState.asStateFlow()

    private var ticker: Job? = null
    private var runStartNanos = 0L
    private var accumulatedMillis = 0L

    fun toggle() {
        if (_uiState.value.running) pause() else start()
    }

    fun start() {
        if (_uiState.value.running) return
        runStartNanos = System.nanoTime()
        _uiState.update { it.copy(running = true) }

        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                val elapsed = accumulatedMillis + elapsedSinceStartMillis()
                _uiState.update { it.copy(elapsedMillis = elapsed) }
                delay(TICK_MS)
            }
        }
    }

    fun pause() {
        if (!_uiState.value.running) return
        accumulatedMillis += elapsedSinceStartMillis()
        ticker?.cancel()
        ticker = null
        _uiState.update { it.copy(running = false, elapsedMillis = accumulatedMillis) }
    }

    fun reset() {
        ticker?.cancel()
        ticker = null
        accumulatedMillis = 0
        runStartNanos = 0
        _uiState.update { it.copy(running = false, elapsedMillis = 0) }
    }

    fun setMode(mode: TimerMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    fun setTargetMinutes(minutes: Int) {
        _uiState.update { it.copy(targetMinutes = minutes.coerceIn(MIN_MINUTES, MAX_MINUTES)) }
    }

    override fun onCleared() {
        ticker?.cancel()
        super.onCleared()
    }

    private fun elapsedSinceStartMillis(): Long =
        (System.nanoTime() - runStartNanos) / NANOS_PER_MILLI

    companion object {
        const val MIN_MINUTES = 1
        const val MAX_MINUTES = 600

        /** よく使う持ち時間 */
        val PRESET_MINUTES = listOf(5, 10, 15, 20, 30, 45, 60, 90)

        private const val TICK_MS = 100L
        private const val NANOS_PER_MILLI = 1_000_000L
    }
}
