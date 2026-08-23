package com.patoolbox.feature.rta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patoolbox.core.audio.AudioCaptureEngine
import com.patoolbox.core.audio.AudioInputDevice
import com.patoolbox.core.billing.ProGate
import com.patoolbox.core.data.CalibrationRepository
import com.patoolbox.core.dsp.BandAnalyzer
import com.patoolbox.core.dsp.BandResolution
import com.patoolbox.core.dsp.FrameAccumulator
import com.patoolbox.core.dsp.FrequencyWeighting
import com.patoolbox.core.dsp.OctaveBands
import com.patoolbox.core.dsp.SpectrumAnalyzer
import com.patoolbox.core.dsp.WeightingFilter
import com.patoolbox.core.dsp.energySumDb
import com.patoolbox.core.dsp.powerToDb
import com.patoolbox.core.model.AudioInputType
import com.patoolbox.core.model.CalibrationProfile
import com.patoolbox.core.model.ProStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 平均化の強さ。現場では「速い」で追い、詰めるときは「遅い」で見る。 */
enum class RtaAveraging(val coefficient: Double) {
    FAST(0.5),
    NORMAL(0.2),
    SLOW(0.07),
}

/**
 * ピークホールドの保持秒数。
 *
 * 無期限（ずっと最大値のまま）にしないのは、リハと本番の間に空くと
 * 数十分前の一発が残ったままになり、いまの音量と見分けが付かなくなるため。
 * 短すぎると目で追えないので、下限は5秒にしてある。
 */
enum class RtaPeakHoldDuration(val seconds: Double) {
    SHORT(5.0),
    LONG(10.0),
}

data class RtaBand(
    val label: String,
    val levelDb: Double,
    val peakDb: Double,
)

data class RtaUiState(
    val isMeasuring: Boolean = false,
    val hasReading: Boolean = false,
    val bands: List<RtaBand> = emptyList(),
    val totalDb: Double = 0.0,
    val resolution: BandResolution = BandResolution.THIRD,
    val weighting: FrequencyWeighting = FrequencyWeighting.Z,
    val averaging: RtaAveraging = RtaAveraging.NORMAL,
    val peakHold: Boolean = true,
    val peakHoldDuration: RtaPeakHoldDuration = RtaPeakHoldDuration.LONG,
    val calibration: CalibrationProfile = CalibrationProfile.uncalibrated(
        AudioInputDevice.BUILTIN_KEY,
        AudioInputType.BUILTIN_MIC,
    ),
    val proStatus: ProStatus = ProStatus.Free,
    val error: String? = null,
)

/**
 * リアルタイムアナライザ。
 *
 * 録音ブロック（1024）を [FrameAccumulator] で FFT フレーム（8192、50%オーバーラップ）に
 * 組み直して解析する。48kHz では約85msごと（毎秒約12回）の更新になる。
 *
 * 重み付けフィルタは状態を持つので、フレーム単位ではなく入力ブロックの流れに対して
 * 連続して掛ける。フレームごとにリセットすると低域が崩れる。
 */
@HiltViewModel
class RtaViewModel @Inject constructor(
    private val captureEngine: AudioCaptureEngine,
    private val calibrationRepository: CalibrationRepository,
    proGate: ProGate,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RtaUiState())
    val uiState: StateFlow<RtaUiState> = _uiState.asStateFlow()

    private val sampleRate = AudioCaptureEngine.DEFAULT_SAMPLE_RATE
    private val analyzer = SpectrumAnalyzer(sampleRate, fftSize = FFT_SIZE)

    private var accumulator = FrameAccumulator(FFT_SIZE, hopSize = FFT_SIZE / 2)
    private var bandAnalyzer = createBandAnalyzer(BandResolution.THIRD)
    private var filter = WeightingFilter.create(FrequencyWeighting.Z, sampleRate)

    private var smoothedPowers = DoubleArray(bandAnalyzer.bands.size)
    private var peakPowers = DoubleArray(bandAnalyzer.bands.size)
    /** 各帯域のピークを更新した時刻（[elapsedSeconds] 基準） */
    private var peakSetAtSeconds = DoubleArray(bandAnalyzer.bands.size)
    private var elapsedSeconds = 0.0
    private var framePowers = DoubleArray(bandAnalyzer.bands.size)
    private var weighted = FloatArray(AudioCaptureEngine.DEFAULT_BLOCK_SIZE)
    private var hasFrame = false

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
            rebuild(_uiState.value.resolution, _uiState.value.weighting)
            captureEngine.start { buffer, length -> onBlock(buffer, length) }
        }.onSuccess { session ->
            _uiState.update { it.copy(isMeasuring = true, error = null) }
            observeCalibration(session.calibrationKey, session.inputType)
        }.onFailure { throwable ->
            _uiState.update { it.copy(isMeasuring = false, error = throwable.message) }
        }
    }

    fun stop() {
        captureEngine.stop()
        _uiState.update { it.copy(isMeasuring = false) }
    }

    fun setResolution(resolution: BandResolution) {
        if (resolution.requiresPro && !_uiState.value.proStatus.isPro) return
        if (_uiState.value.resolution == resolution) return

        val wasMeasuring = _uiState.value.isMeasuring
        stop()
        _uiState.update { it.copy(resolution = resolution, hasReading = false, bands = emptyList()) }
        if (wasMeasuring) start()
    }

    fun setWeighting(weighting: FrequencyWeighting) {
        if (_uiState.value.weighting == weighting) return
        val wasMeasuring = _uiState.value.isMeasuring
        stop()
        _uiState.update { it.copy(weighting = weighting, hasReading = false) }
        if (wasMeasuring) start()
    }

    fun setAveraging(averaging: RtaAveraging) {
        _uiState.update { it.copy(averaging = averaging) }
    }

    fun togglePeakHold() {
        val enabled = !_uiState.value.peakHold
        if (!enabled) clearPeaks()
        _uiState.update { it.copy(peakHold = enabled) }
    }

    /** ピークホールドの保持秒数。次に更新されるカラムから効く。 */
    fun setPeakHoldDuration(duration: RtaPeakHoldDuration) {
        _uiState.update { it.copy(peakHoldDuration = duration) }
    }

    fun clearPeaks() {
        peakPowers.fill(0.0)
        peakSetAtSeconds.fill(elapsedSeconds)
    }

    override fun onCleared() {
        captureEngine.stop()
        super.onCleared()
    }

    private fun onBlock(buffer: FloatArray, length: Int) {
        if (weighted.size < length) weighted = FloatArray(length)

        // 重み付けは入力の流れに対して連続に掛ける
        for (i in 0 until length) {
            weighted[i] = filter.process(buffer[i].toDouble()).toFloat()
        }

        accumulator.add(weighted, length) { frame ->
            bandAnalyzer.bandPowers(analyzer.powerSpectrum(frame), framePowers)

            val alpha = _uiState.value.averaging.coefficient
            val holdSeconds = _uiState.value.peakHoldDuration.seconds
            elapsedSeconds += HOP_SECONDS
            for (b in framePowers.indices) {
                smoothedPowers[b] = if (hasFrame) {
                    smoothedPowers[b] + alpha * (framePowers[b] - smoothedPowers[b])
                } else {
                    framePowers[b]
                }
                when {
                    smoothedPowers[b] > peakPowers[b] -> {
                        peakPowers[b] = smoothedPowers[b]
                        peakSetAtSeconds[b] = elapsedSeconds
                    }
                    elapsedSeconds - peakSetAtSeconds[b] >= holdSeconds -> {
                        peakPowers[b] = smoothedPowers[b]
                        peakSetAtSeconds[b] = elapsedSeconds
                    }
                }
            }
            hasFrame = true
            publish()
        }
    }

    private fun publish() {
        val offset = _uiState.value.calibration.offsetDb
        val bands = bandAnalyzer.bands
        val levels = ArrayList<RtaBand>(bands.size)
        val levelsDb = DoubleArray(bands.size)

        for (b in bands.indices) {
            val levelDb = powerToDb(smoothedPowers[b]) + offset
            levelsDb[b] = levelDb
            levels += RtaBand(
                label = bands[b].label,
                levelDb = levelDb,
                peakDb = powerToDb(peakPowers[b]) + offset,
            )
        }

        _uiState.update {
            it.copy(hasReading = true, bands = levels, totalDb = energySumDb(levelsDb))
        }
    }

    private fun rebuild(resolution: BandResolution, weighting: FrequencyWeighting) {
        bandAnalyzer = createBandAnalyzer(resolution)
        filter = WeightingFilter.create(weighting, sampleRate)
        accumulator = FrameAccumulator(FFT_SIZE, hopSize = FFT_SIZE / 2)
        smoothedPowers = DoubleArray(bandAnalyzer.bands.size)
        peakPowers = DoubleArray(bandAnalyzer.bands.size)
        peakSetAtSeconds = DoubleArray(bandAnalyzer.bands.size)
        elapsedSeconds = 0.0
        framePowers = DoubleArray(bandAnalyzer.bands.size)
        hasFrame = false
    }

    private fun createBandAnalyzer(resolution: BandResolution) = BandAnalyzer(
        bands = OctaveBands.bands(resolution),
        binWidthHz = analyzer.binWidthHz,
        binCount = analyzer.binCount,
    )

    private fun observeCalibration(deviceKey: String, inputType: AudioInputType) {
        viewModelScope.launch {
            calibrationRepository.observe(deviceKey, inputType).collect { profile ->
                _uiState.update { it.copy(calibration = profile) }
            }
        }
    }

    private companion object {
        /** 48kHz でビン幅約5.9Hz。1/3オクターブの最低帯域を拾える下限 */
        const val FFT_SIZE = 8192

        /** FFT_SIZE/2（50%オーバーラップのホップ幅）÷ サンプルレート */
        const val HOP_SECONDS = (FFT_SIZE / 2).toDouble() / AudioCaptureEngine.DEFAULT_SAMPLE_RATE
    }
}
