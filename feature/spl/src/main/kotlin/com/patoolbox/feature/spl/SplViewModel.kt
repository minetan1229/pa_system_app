package com.patoolbox.feature.spl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patoolbox.core.audio.AudioCaptureEngine
import com.patoolbox.core.audio.AudioInputDevice
import com.patoolbox.core.data.CalibrationRepository
import com.patoolbox.core.dsp.FrequencyWeighting
import com.patoolbox.core.dsp.SoundLevelMeter
import com.patoolbox.core.dsp.TimeWeighting
import com.patoolbox.core.model.AudioInputType
import com.patoolbox.core.model.CalibrationProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SplUiState(
    val isMeasuring: Boolean = false,
    val hasReading: Boolean = false,
    val instantDb: Double = 0.0,
    val leqDb: Double = 0.0,
    val maxDb: Double = 0.0,
    val minDb: Double = 0.0,
    val peakDb: Double = 0.0,
    val l10Db: Double = 0.0,
    val l50Db: Double = 0.0,
    val l90Db: Double = 0.0,
    val elapsedSeconds: Double = 0.0,
    val clipped: Boolean = false,
    val frequencyWeighting: FrequencyWeighting = FrequencyWeighting.A,
    val timeWeighting: TimeWeighting = TimeWeighting.FAST,
    val calibration: CalibrationProfile = CalibrationProfile.uncalibrated(
        AudioInputDevice.BUILTIN_KEY,
        AudioInputType.BUILTIN_MIC,
    ),
    val inputSourceLabel: String = "",
    val inputSourceIsMeasurementGrade: Boolean = true,
    val error: String? = null,
)

/**
 * SPLメーターの状態管理。
 *
 * DSP はオーディオスレッド（[AudioCaptureEngine.BlockListener] の中）で回し、
 * UI へは計算済みの数値だけを流す。UI 更新は [UI_UPDATE_EVERY_BLOCKS] ブロックごとに
 * 間引いていて、48kHz / 1024サンプルなら毎秒約23回になる。
 */
@HiltViewModel
class SplViewModel @Inject constructor(
    private val captureEngine: AudioCaptureEngine,
    private val calibrationRepository: CalibrationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplUiState())
    val uiState: StateFlow<SplUiState> = _uiState.asStateFlow()

    private var meter: SoundLevelMeter? = null
    private var blockCounter = 0

    fun start() {
        if (_uiState.value.isMeasuring) return

        if (!captureEngine.hasPermission()) {
            _uiState.update { it.copy(error = "マイクの許可がありません") }
            return
        }

        runCatching {
            val state = _uiState.value
            val newMeter = SoundLevelMeter(
                sampleRate = AudioCaptureEngine.DEFAULT_SAMPLE_RATE,
                frequencyWeighting = state.frequencyWeighting,
                timeWeighting = state.timeWeighting,
                calibrationOffsetDb = state.calibration.offsetDb,
            )
            meter = newMeter
            blockCounter = 0

            val session = captureEngine.start { buffer, length ->
                val reading = newMeter.process(buffer, length)
                if (++blockCounter % UI_UPDATE_EVERY_BLOCKS == 0) {
                    publish(newMeter, reading)
                }
            }
            session
        }.onSuccess { session ->
            _uiState.update {
                it.copy(
                    isMeasuring = true,
                    error = null,
                    inputSourceLabel = session.inputSource.label,
                    inputSourceIsMeasurementGrade = session.inputSource.isMeasurementGrade,
                )
            }
            observeCalibration(session.calibrationKey, session.inputType)
        }.onFailure { throwable ->
            meter = null
            _uiState.update { it.copy(isMeasuring = false, error = throwable.message) }
        }
    }

    fun stop() {
        captureEngine.stop()
        _uiState.update { it.copy(isMeasuring = false) }
    }

    fun reset() {
        meter?.reset()
        _uiState.update {
            it.copy(
                hasReading = false,
                instantDb = 0.0,
                leqDb = 0.0,
                maxDb = 0.0,
                minDb = 0.0,
                peakDb = 0.0,
                l10Db = 0.0,
                l50Db = 0.0,
                l90Db = 0.0,
                elapsedSeconds = 0.0,
                clipped = false,
            )
        }
    }

    /** 重み付けを変えたら測定はやり直しになる（フィルタと積分をリセットするため）。 */
    fun setFrequencyWeighting(weighting: FrequencyWeighting) {
        if (_uiState.value.frequencyWeighting == weighting) return
        val wasMeasuring = _uiState.value.isMeasuring
        stop()
        _uiState.update { it.copy(frequencyWeighting = weighting, hasReading = false) }
        if (wasMeasuring) start()
    }

    fun setTimeWeighting(weighting: TimeWeighting) {
        if (_uiState.value.timeWeighting == weighting) return
        val wasMeasuring = _uiState.value.isMeasuring
        stop()
        _uiState.update { it.copy(timeWeighting = weighting, hasReading = false) }
        if (wasMeasuring) start()
    }

    override fun onCleared() {
        captureEngine.stop()
        super.onCleared()
    }

    private fun publish(meter: SoundLevelMeter, reading: SoundLevelMeter.Reading) {
        _uiState.update { state ->
            state.copy(
                hasReading = true,
                instantDb = reading.instantDb,
                leqDb = reading.leqDb,
                maxDb = reading.maxDb,
                minDb = reading.minDb,
                peakDb = reading.peakDb,
                l10Db = meter.percentileDb(PERCENTILE_10),
                l50Db = meter.percentileDb(PERCENTILE_50),
                l90Db = meter.percentileDb(PERCENTILE_90),
                elapsedSeconds = reading.elapsedSeconds,
                clipped = reading.clipped,
            )
        }
    }

    private fun observeCalibration(deviceKey: String, inputType: AudioInputType) {
        viewModelScope.launch {
            calibrationRepository.observe(deviceKey, inputType).collect { profile ->
                meter?.calibrationOffsetDb = profile.offsetDb
                _uiState.update { it.copy(calibration = profile) }
            }
        }
    }

    private companion object {
        const val UI_UPDATE_EVERY_BLOCKS = 2
        const val PERCENTILE_10 = 10.0
        const val PERCENTILE_50 = 50.0
        const val PERCENTILE_90 = 90.0
    }
}
