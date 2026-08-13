package com.patoolbox.feature.spl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patoolbox.core.audio.AudioCaptureEngine
import com.patoolbox.core.audio.AudioInputDevice
import com.patoolbox.core.billing.ProGate
import com.patoolbox.core.data.CalibrationRepository
import com.patoolbox.core.data.MeasurementRepository
import com.patoolbox.core.dsp.FrequencyWeighting
import com.patoolbox.core.dsp.SoundLevelMeter
import com.patoolbox.core.dsp.TimeWeighting
import com.patoolbox.core.model.AudioInputType
import com.patoolbox.core.model.CalibrationProfile
import com.patoolbox.core.model.Measurement
import com.patoolbox.core.model.MeasurementSample
import com.patoolbox.core.model.ProStatus
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
    val proStatus: ProStatus = ProStatus.Free,
    val isLogging: Boolean = false,
    val loggedSamples: Int = 0,
    val savedMessage: String? = null,
    val error: String? = null,
) {
    /** 記録は Pro 専用。測定そのものは無料で使える */
    val canLog: Boolean get() = proStatus.isPro && isMeasuring
}

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
    private val measurementRepository: MeasurementRepository,
    proGate: ProGate,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplUiState())
    val uiState: StateFlow<SplUiState> = _uiState.asStateFlow()

    private var meter: SoundLevelMeter? = null
    private var blockCounter = 0

    // 記録はオーディオスレッドからのみ触る。UI へは件数だけを流す
    private val loggedSamples = mutableListOf<MeasurementSample>()
    private var logStartedAtEpochMs = 0L
    private var nextSampleAtMs = 0L

    init {
        viewModelScope.launch {
            proGate.proStatus.collect { status ->
                _uiState.update { it.copy(proStatus = status) }
            }
        }
    }

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

    /** 記録開始。測定中でないと呼べない。 */
    fun startLogging() {
        if (!uiState.value.canLog || uiState.value.isLogging) return
        loggedSamples.clear()
        logStartedAtEpochMs = System.currentTimeMillis()
        nextSampleAtMs = 0L
        _uiState.update { it.copy(isLogging = true, loggedSamples = 0, savedMessage = null) }
    }

    /** 記録を止めて保存する。1件も無ければ何もしない。 */
    fun stopLoggingAndSave(title: String) {
        if (!uiState.value.isLogging) return
        val state = uiState.value
        val samples = loggedSamples.toList()
        _uiState.update { it.copy(isLogging = false) }
        if (samples.isEmpty()) return

        viewModelScope.launch {
            measurementRepository.save(
                measurement = Measurement(
                    title = title.ifBlank { DEFAULT_TITLE },
                    startedAtEpochMs = logStartedAtEpochMs,
                    endedAtEpochMs = System.currentTimeMillis(),
                    frequencyWeighting = state.frequencyWeighting.displayName,
                    timeWeighting = state.timeWeighting.label,
                    calibrationOffsetDb = state.calibration.offsetDb,
                    calibrationMethod = state.calibration.method,
                    leqDb = state.leqDb,
                    maxDb = state.maxDb,
                    minDb = state.minDb,
                    peakDb = state.peakDb,
                    l10Db = state.l10Db,
                    l50Db = state.l50Db,
                    l90Db = state.l90Db,
                    clipped = state.clipped,
                ),
                samples = samples,
            )
            _uiState.update { it.copy(savedMessage = MESSAGE_SAVED) }
        }
    }

    fun clearSavedMessage() {
        _uiState.update { it.copy(savedMessage = null) }
    }

    private fun publish(meter: SoundLevelMeter, reading: SoundLevelMeter.Reading) {
        if (_uiState.value.isLogging) {
            val elapsedMs = (reading.elapsedSeconds * 1000).toLong()
            if (elapsedMs >= nextSampleAtMs) {
                loggedSamples += MeasurementSample(
                    offsetMs = elapsedMs,
                    instantDb = reading.instantDb,
                    leqDb = reading.leqDb,
                )
                nextSampleAtMs = elapsedMs + SAMPLE_INTERVAL_MS
            }
        }

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
                loggedSamples = loggedSamples.size,
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
        const val DEFAULT_TITLE = "無題の測定"
        const val MESSAGE_SAVED = "saved"

        /** 記録の間隔。1秒ごとなら3時間で約1万行に収まる */
        const val SAMPLE_INTERVAL_MS = 1_000L

        const val UI_UPDATE_EVERY_BLOCKS = 2
        const val PERCENTILE_10 = 10.0
        const val PERCENTILE_50 = 50.0
        const val PERCENTILE_90 = 90.0
    }
}
