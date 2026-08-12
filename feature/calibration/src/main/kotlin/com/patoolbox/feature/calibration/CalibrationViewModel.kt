package com.patoolbox.feature.calibration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patoolbox.core.audio.AudioCaptureEngine
import com.patoolbox.core.audio.AudioInputDevice
import com.patoolbox.core.data.CalibrationRepository
import com.patoolbox.core.dsp.FrequencyWeighting
import com.patoolbox.core.dsp.SoundLevelMeter
import com.patoolbox.core.dsp.TimeWeighting
import com.patoolbox.core.model.AudioInputType
import com.patoolbox.core.model.CalibrationMethod
import com.patoolbox.core.model.CalibrationProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CalibrationUiState(
    val isMeasuring: Boolean = false,
    val hasReading: Boolean = false,
    /** 校正オフセットを掛けない生のレベル（dBFS） */
    val measuredDbFs: Double = 0.0,
    val profile: CalibrationProfile = CalibrationProfile.uncalibrated(
        AudioInputDevice.BUILTIN_KEY,
        AudioInputType.BUILTIN_MIC,
    ),
    val deviceLabel: String = "",
    val referenceInput: String = "",
    val message: String? = null,
    val error: String? = null,
)

/**
 * マイク校正。
 *
 * オフセットの定義は「0 dBFS が何 dB SPL に相当するか」なので、
 *   オフセット = 基準の読み − 現在の dBFS
 * で求まる。1kHz では A/C/Z どれも 0dB なので、生の値を取るために Z で測る。
 */
@HiltViewModel
class CalibrationViewModel @Inject constructor(
    private val captureEngine: AudioCaptureEngine,
    private val calibrationRepository: CalibrationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalibrationUiState())
    val uiState: StateFlow<CalibrationUiState> = _uiState.asStateFlow()

    private var meter: SoundLevelMeter? = null
    private var blockCounter = 0
    private var deviceKey: String = AudioInputDevice.BUILTIN_KEY
    private var inputType: AudioInputType = AudioInputType.BUILTIN_MIC

    fun start() {
        if (_uiState.value.isMeasuring) return
        if (!captureEngine.hasPermission()) {
            _uiState.update { it.copy(error = "マイクの許可がありません") }
            return
        }

        runCatching {
            // 校正中は生の dBFS が欲しいので、重み付けなし・オフセット0で測る。
            // 値を読みやすくするため時間重み付けは Slow。
            val newMeter = SoundLevelMeter(
                sampleRate = AudioCaptureEngine.DEFAULT_SAMPLE_RATE,
                frequencyWeighting = FrequencyWeighting.Z,
                timeWeighting = TimeWeighting.SLOW,
                calibrationOffsetDb = 0.0,
            )
            meter = newMeter
            blockCounter = 0

            captureEngine.start { buffer, length ->
                val reading = newMeter.process(buffer, length)
                if (++blockCounter % UI_UPDATE_EVERY_BLOCKS == 0) {
                    _uiState.update {
                        it.copy(hasReading = true, measuredDbFs = reading.instantDb)
                    }
                }
            }
        }.onSuccess { session ->
            deviceKey = session.calibrationKey
            inputType = session.inputType
            _uiState.update {
                it.copy(
                    isMeasuring = true,
                    error = null,
                    deviceLabel = session.device?.name ?: "内蔵マイク",
                )
            }
            observeProfile()
        }.onFailure { throwable ->
            meter = null
            _uiState.update { it.copy(isMeasuring = false, error = throwable.message) }
        }
    }

    fun stop() {
        captureEngine.stop()
        _uiState.update { it.copy(isMeasuring = false) }
    }

    fun onReferenceChange(text: String) {
        _uiState.update { it.copy(referenceInput = text, message = null) }
    }

    /** 基準の騒音計の読みに合わせる。 */
    fun applyManual() {
        val state = _uiState.value
        val reference = state.referenceInput.trim().toDoubleOrNull()
        if (reference == null) {
            _uiState.update { it.copy(message = MESSAGE_INVALID) }
            return
        }
        applyOffset(reference, CalibrationMethod.MANUAL)
    }

    /** 音響校正器（94dB / 114dB @1kHz）に合わせる。 */
    fun applyCalibrator(levelDb: Double) {
        applyOffset(levelDb, CalibrationMethod.CALIBRATOR)
    }

    fun clear() {
        viewModelScope.launch {
            calibrationRepository.clear(deviceKey, inputType)
            _uiState.update { it.copy(message = null) }
        }
    }

    override fun onCleared() {
        captureEngine.stop()
        super.onCleared()
    }

    private fun applyOffset(referenceDb: Double, method: CalibrationMethod) {
        val state = _uiState.value
        if (!state.hasReading) {
            _uiState.update { it.copy(message = MESSAGE_NEED_MEASURE) }
            return
        }

        val offset = referenceDb - state.measuredDbFs
        viewModelScope.launch {
            calibrationRepository.save(
                CalibrationProfile(
                    id = state.profile.id,
                    deviceKey = deviceKey,
                    inputType = inputType,
                    offsetDb = offset,
                    method = method,
                    calibratedAtEpochMs = System.currentTimeMillis(),
                ),
            )
            _uiState.update { it.copy(message = MESSAGE_SAVED) }
        }
    }

    private fun observeProfile() {
        viewModelScope.launch {
            calibrationRepository.observe(deviceKey, inputType).collect { profile ->
                _uiState.update { it.copy(profile = profile) }
            }
        }
    }

    companion object {
        const val CALIBRATOR_94_DB = 94.0
        const val CALIBRATOR_114_DB = 114.0

        const val MESSAGE_SAVED = "saved"
        const val MESSAGE_NEED_MEASURE = "need_measure"
        const val MESSAGE_INVALID = "invalid"

        private const val UI_UPDATE_EVERY_BLOCKS = 4
    }
}
