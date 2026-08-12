package com.patoolbox.feature.metronome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patoolbox.core.audio.AudioCaptureEngine
import com.patoolbox.core.audio.AudioPlaybackEngine
import com.patoolbox.core.dsp.MetronomeSource
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
import kotlin.math.roundToInt

data class MetronomeUiState(
    val isPlaying: Boolean = false,
    val bpm: Int = 120,
    val beatsPerBar: Int = 4,
    val accentFirstBeat: Boolean = true,
    val levelDbFs: Double = DEFAULT_LEVEL,
    /** いま鳴っている拍（0 始まり）。UI の点灯に使う */
    val currentBeat: Int = 0,
    val error: String? = null,
) {
    companion object {
        const val DEFAULT_LEVEL = -12.0
        const val MIN_LEVEL = -40.0
        const val MAX_LEVEL = -3.0
    }
}

/**
 * メトロノーム。
 *
 * 拍の点灯は経過時間からの推測ではなく、出力スレッドが実際に鳴らした拍
 * （[MetronomeSource.beatCounter]）を読んで反映する。音と表示がずれないようにするため。
 */
@HiltViewModel
class MetronomeViewModel @Inject constructor(
    private val playbackEngine: AudioPlaybackEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MetronomeUiState())
    val uiState: StateFlow<MetronomeUiState> = _uiState.asStateFlow()

    private val source = MetronomeSource(AudioCaptureEngine.DEFAULT_SAMPLE_RATE)
    private var beatWatcher: Job? = null
    private val tapTimestamps = ArrayDeque<Long>()

    fun toggle() {
        if (_uiState.value.isPlaying) stop() else start()
    }

    fun start() {
        if (_uiState.value.isPlaying) return
        runCatching {
            applyParameters()
            source.reset()
            playbackEngine.start(source)
        }.onSuccess {
            _uiState.update { it.copy(isPlaying = true, error = null) }
            watchBeats()
        }.onFailure { throwable ->
            _uiState.update { it.copy(isPlaying = false, error = throwable.message) }
        }
    }

    fun stop() {
        beatWatcher?.cancel()
        beatWatcher = null
        playbackEngine.stop()
        _uiState.update { it.copy(isPlaying = false) }
    }

    fun adjustBpm(delta: Int) {
        setBpm(_uiState.value.bpm + delta)
    }

    fun setBpm(bpm: Int) {
        val clamped = bpm.coerceIn(
            MetronomeSource.MIN_BPM.toInt(),
            MetronomeSource.MAX_BPM.toInt(),
        )
        _uiState.update { it.copy(bpm = clamped) }
        applyParameters()
    }

    fun setBeatsPerBar(beats: Int) {
        _uiState.update { it.copy(beatsPerBar = beats.coerceIn(MIN_BEATS, MAX_BEATS)) }
        applyParameters()
    }

    fun setAccent(enabled: Boolean) {
        _uiState.update { it.copy(accentFirstBeat = enabled) }
        applyParameters()
    }

    fun setLevel(dbFs: Double) {
        _uiState.update { it.copy(levelDbFs = dbFs) }
        applyParameters()
    }

    /**
     * タップでテンポを決める。直近の間隔を平均する。
     * 前のタップから離れすぎたら測り直し（打ち直しのつもりで叩くため）。
     */
    fun tap() {
        val now = System.nanoTime()
        val last = tapTimestamps.lastOrNull()
        if (last != null && now - last > TAP_RESET_NANOS) {
            tapTimestamps.clear()
        }

        tapTimestamps.addLast(now)
        while (tapTimestamps.size > MAX_TAPS) {
            tapTimestamps.removeFirst()
        }

        if (tapTimestamps.size < 2) return

        val intervals = tapTimestamps.zipWithNext { a, b -> (b - a).toDouble() }
        val averageNanos = intervals.average()
        if (averageNanos <= 0.0) return

        setBpm((NANOS_PER_MINUTE / averageNanos).roundToInt())
    }

    override fun onCleared() {
        beatWatcher?.cancel()
        playbackEngine.stop()
        super.onCleared()
    }

    private fun applyParameters() {
        val state = _uiState.value
        source.bpm = state.bpm.toDouble()
        source.beatsPerBar = state.beatsPerBar
        source.accentFirstBeat = state.accentFirstBeat
        source.levelDbFs = state.levelDbFs
    }

    private fun watchBeats() {
        beatWatcher?.cancel()
        beatWatcher = viewModelScope.launch {
            var lastCounter = -1L
            while (isActive) {
                val counter = source.beatCounter
                if (counter != lastCounter) {
                    lastCounter = counter
                    val beat = source.currentBeat
                    _uiState.update { it.copy(currentBeat = beat) }
                }
                delay(BEAT_POLL_MS)
            }
        }
    }

    companion object {
        const val MIN_BEATS = 1
        const val MAX_BEATS = 12

        private const val MAX_TAPS = 5
        private const val TAP_RESET_NANOS = 2_000_000_000L
        private const val NANOS_PER_MINUTE = 60_000_000_000.0
        private const val BEAT_POLL_MS = 16L
    }
}
