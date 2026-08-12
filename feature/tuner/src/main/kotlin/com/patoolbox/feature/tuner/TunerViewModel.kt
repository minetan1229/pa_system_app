package com.patoolbox.feature.tuner

import androidx.lifecycle.ViewModel
import com.patoolbox.core.audio.AudioCaptureEngine
import com.patoolbox.core.dsp.FrameAccumulator
import com.patoolbox.core.dsp.NoteNames
import com.patoolbox.core.dsp.NoteReading
import com.patoolbox.core.dsp.PitchDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class TunerUiState(
    val isListening: Boolean = false,
    val note: NoteReading? = null,
    val clarity: Double = 0.0,
    val referenceAHz: Double = NoteNames.DEFAULT_REFERENCE_A_HZ,
    val error: String? = null,
) {
    val isInTune: Boolean
        get() = note != null && kotlin.math.abs(note.cents) <= NoteNames.IN_TUNE_CENTS
}

/**
 * チューナー。
 *
 * 4096サンプル（約85ms）ごとに基本周波数を推定する。
 * 検出できなかったフレームで表示を即座に消すとちらつくので、
 * [MISS_TOLERANCE] フレーム続けて外したときだけクリアする。
 */
@HiltViewModel
class TunerViewModel @Inject constructor(
    private val captureEngine: AudioCaptureEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TunerUiState())
    val uiState: StateFlow<TunerUiState> = _uiState.asStateFlow()

    private val sampleRate = AudioCaptureEngine.DEFAULT_SAMPLE_RATE
    private val detector = PitchDetector(sampleRate, windowSize = WINDOW_SIZE)
    private var accumulator = FrameAccumulator(WINDOW_SIZE, hopSize = WINDOW_SIZE / 2)
    private var missedFrames = 0

    fun toggle() {
        if (_uiState.value.isListening) stop() else start()
    }

    fun start() {
        if (_uiState.value.isListening) return
        if (!captureEngine.hasPermission()) {
            _uiState.update { it.copy(error = "マイクの許可がありません") }
            return
        }

        runCatching {
            accumulator = FrameAccumulator(WINDOW_SIZE, hopSize = WINDOW_SIZE / 2)
            missedFrames = 0
            captureEngine.start { buffer, length ->
                accumulator.add(buffer, length) { frame -> onFrame(frame) }
            }
        }.onSuccess {
            _uiState.update { it.copy(isListening = true, error = null) }
        }.onFailure { throwable ->
            _uiState.update { it.copy(isListening = false, error = throwable.message) }
        }
    }

    fun stop() {
        captureEngine.stop()
        _uiState.update { it.copy(isListening = false, note = null, clarity = 0.0) }
    }

    fun adjustReference(deltaHz: Double) {
        _uiState.update {
            it.copy(
                referenceAHz = (it.referenceAHz + deltaHz)
                    .coerceIn(MIN_REFERENCE_HZ, MAX_REFERENCE_HZ),
            )
        }
    }

    fun resetReference() {
        _uiState.update { it.copy(referenceAHz = NoteNames.DEFAULT_REFERENCE_A_HZ) }
    }

    override fun onCleared() {
        captureEngine.stop()
        super.onCleared()
    }

    private fun onFrame(frame: FloatArray) {
        val pitch = detector.detect(frame)
        if (pitch == null) {
            missedFrames++
            if (missedFrames >= MISS_TOLERANCE) {
                _uiState.update { it.copy(note = null, clarity = 0.0) }
            }
            return
        }

        missedFrames = 0
        val reference = _uiState.value.referenceAHz
        val note = NoteNames.fromFrequency(pitch.frequencyHz, reference)
        _uiState.update { it.copy(note = note, clarity = pitch.clarity) }
    }

    companion object {
        const val MIN_REFERENCE_HZ = 415.0
        const val MAX_REFERENCE_HZ = 465.0

        /** 48kHz で約85ms。ベースの最低音（41Hz）でも2周期以上入る長さ */
        private const val WINDOW_SIZE = 4096
        private const val MISS_TOLERANCE = 4
    }
}
