package com.patoolbox.feature.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patoolbox.core.audio.AudioCaptureEngine
import com.patoolbox.core.billing.ProGate
import com.patoolbox.core.dsp.FeedbackDetector
import com.patoolbox.core.dsp.FrameAccumulator
import com.patoolbox.core.model.ProStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedbackUiState(
    val isMeasuring: Boolean = false,
    val candidates: List<FeedbackDetector.Candidate> = emptyList(),
    val proStatus: ProStatus = ProStatus.Free,
    val error: String? = null,
)

/**
 * ハウリング検出。
 *
 * 解析はオーディオスレッドで回し、UI にはフレームごとの結果だけを渡す。
 * 検出は「周りより突出していて鳴り続けている」成分を探すので、
 * 楽器の音では基本的に反応しない（[FeedbackDetector] のテスト参照）。
 */
@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val captureEngine: AudioCaptureEngine,
    proGate: ProGate,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

    private val sampleRate = AudioCaptureEngine.DEFAULT_SAMPLE_RATE
    private val detector = FeedbackDetector(sampleRate, FFT_SIZE)
    private var accumulator = FrameAccumulator(FFT_SIZE, hopSize = FFT_SIZE / 2)

    init {
        viewModelScope.launch {
            proGate.proStatus.collect { status ->
                _uiState.update { it.copy(proStatus = status) }
            }
        }
    }

    fun toggle() {
        if (_uiState.value.isMeasuring) stop() else start()
    }

    fun start() {
        if (_uiState.value.isMeasuring) return
        if (!_uiState.value.proStatus.isPro) return
        if (!captureEngine.hasPermission()) {
            _uiState.update { it.copy(error = "マイクの許可がありません") }
            return
        }

        runCatching {
            detector.reset()
            accumulator = FrameAccumulator(FFT_SIZE, hopSize = FFT_SIZE / 2)
            captureEngine.start { buffer, length ->
                accumulator.add(buffer, length) { frame ->
                    val found = detector.process(frame)
                    _uiState.update { it.copy(candidates = found) }
                }
            }
        }.onSuccess {
            _uiState.update { it.copy(isMeasuring = true, error = null) }
        }.onFailure { throwable ->
            _uiState.update { it.copy(isMeasuring = false, error = throwable.message) }
        }
    }

    fun stop() {
        captureEngine.stop()
        _uiState.update { it.copy(isMeasuring = false, candidates = emptyList()) }
    }

    override fun onCleared() {
        captureEngine.stop()
        super.onCleared()
    }

    private companion object {
        const val FFT_SIZE = 8192
    }
}
