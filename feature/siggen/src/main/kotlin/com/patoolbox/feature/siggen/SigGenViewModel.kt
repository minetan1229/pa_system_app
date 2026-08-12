package com.patoolbox.feature.siggen

import androidx.lifecycle.ViewModel
import com.patoolbox.core.audio.AudioCaptureEngine
import com.patoolbox.core.audio.AudioPlaybackEngine
import com.patoolbox.core.dsp.BurstSource
import com.patoolbox.core.dsp.DualToneSource
import com.patoolbox.core.dsp.LinearSweepSource
import com.patoolbox.core.dsp.LogSweepSource
import com.patoolbox.core.dsp.PinkNoiseSource
import com.patoolbox.core.dsp.SignalSource
import com.patoolbox.core.dsp.SineSource
import com.patoolbox.core.dsp.SquareSource
import com.patoolbox.core.dsp.WhiteNoiseSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

enum class Waveform {
    SINE,
    PINK_NOISE,
    WHITE_NOISE,
    SQUARE,
    LOG_SWEEP,
    LINEAR_SWEEP,
    DUAL_TONE,
    ;

    /** 周波数の指定が意味を持つ波形か */
    val hasFrequency: Boolean get() = this == SINE || this == SQUARE
}

data class SigGenUiState(
    val isPlaying: Boolean = false,
    val waveform: Waveform = Waveform.SINE,
    val frequencyHz: Double = 1000.0,
    val levelDbFs: Double = DEFAULT_LEVEL,
    val burst: Boolean = false,
    val sweepSeconds: Double = 5.0,
    val error: String? = null,
) {
    companion object {
        const val DEFAULT_LEVEL = -20.0
        const val MIN_LEVEL = -60.0
        const val MAX_LEVEL = -3.0
        const val MIN_FREQUENCY = 20.0
        const val MAX_FREQUENCY = 20000.0
    }
}

/**
 * テスト信号の出力。
 *
 * 再生中のパラメータ変更は、信号源を作り直さずに @Volatile なフィールドを書き換える。
 * 作り直すと位相がリセットされてクリック音が出るため。
 */
@HiltViewModel
class SigGenViewModel @Inject constructor(
    private val playbackEngine: AudioPlaybackEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SigGenUiState())
    val uiState: StateFlow<SigGenUiState> = _uiState.asStateFlow()

    private val sampleRate = AudioCaptureEngine.DEFAULT_SAMPLE_RATE

    // 波形ごとに作り置きし、切り替えても位相や乱数系列が続くようにする
    private val sine = SineSource(sampleRate)
    private val square = SquareSource(sampleRate)
    private val pink = PinkNoiseSource(sampleRate)
    private val white = WhiteNoiseSource(sampleRate)
    private val logSweep = LogSweepSource(sampleRate, loop = true)
    private val linearSweep = LinearSweepSource(sampleRate, loop = true)
    private val dualTone = DualToneSource(sampleRate)

    fun togglePlay() {
        if (_uiState.value.isPlaying) stop() else play()
    }

    fun play() {
        if (_uiState.value.isPlaying) return
        runCatching {
            applyParameters()
            playbackEngine.start(currentSource())
        }.onSuccess {
            _uiState.update { it.copy(isPlaying = true, error = null) }
        }.onFailure { throwable ->
            _uiState.update { it.copy(isPlaying = false, error = throwable.message) }
        }
    }

    fun stop() {
        playbackEngine.stop()
        _uiState.update { it.copy(isPlaying = false) }
    }

    /** 波形の切り替えは信号源そのものが変わるので、鳴っていれば作り直す。 */
    fun setWaveform(waveform: Waveform) {
        if (_uiState.value.waveform == waveform) return
        val wasPlaying = _uiState.value.isPlaying
        if (wasPlaying) stop()
        _uiState.update { it.copy(waveform = waveform) }
        if (wasPlaying) play()
    }

    fun setFrequency(hz: Double) {
        _uiState.update { it.copy(frequencyHz = hz) }
        applyParameters()
    }

    fun setLevel(dbFs: Double) {
        _uiState.update { it.copy(levelDbFs = dbFs) }
        applyParameters()
    }

    fun setBurst(enabled: Boolean) {
        if (_uiState.value.burst == enabled) return
        val wasPlaying = _uiState.value.isPlaying
        if (wasPlaying) stop()
        _uiState.update { it.copy(burst = enabled) }
        if (wasPlaying) play()
    }

    fun applyPreset1kHz() {
        setWaveform(Waveform.SINE)
        setFrequency(1000.0)
        setLevel(SigGenUiState.DEFAULT_LEVEL)
    }

    fun applyPresetPink() {
        setWaveform(Waveform.PINK_NOISE)
        setLevel(SigGenUiState.DEFAULT_LEVEL)
    }

    override fun onCleared() {
        playbackEngine.stop()
        super.onCleared()
    }

    /** 再生中でも安全に反映できるパラメータを書き込む。 */
    private fun applyParameters() {
        val state = _uiState.value
        sine.frequencyHz = state.frequencyHz
        sine.levelDbFs = state.levelDbFs
        square.frequencyHz = state.frequencyHz
        square.levelDbFs = state.levelDbFs
        pink.levelDbFs = state.levelDbFs
        white.levelDbFs = state.levelDbFs
        logSweep.levelDbFs = state.levelDbFs
        logSweep.durationSeconds = state.sweepSeconds
        linearSweep.levelDbFs = state.levelDbFs
        linearSweep.durationSeconds = state.sweepSeconds
        dualTone.levelDbFs = state.levelDbFs
    }

    private fun currentSource(): SignalSource {
        val base = when (_uiState.value.waveform) {
            Waveform.SINE -> sine
            Waveform.SQUARE -> square
            Waveform.PINK_NOISE -> pink
            Waveform.WHITE_NOISE -> white
            Waveform.LOG_SWEEP -> logSweep
            Waveform.LINEAR_SWEEP -> linearSweep
            Waveform.DUAL_TONE -> dualTone
        }
        return if (_uiState.value.burst) BurstSource(base) else base
    }
}
