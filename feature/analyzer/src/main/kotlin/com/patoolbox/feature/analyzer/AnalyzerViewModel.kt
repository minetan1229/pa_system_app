package com.patoolbox.feature.analyzer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patoolbox.core.audio.AudioCaptureEngine
import com.patoolbox.core.audio.AudioInputDevice
import com.patoolbox.core.billing.ProGate
import com.patoolbox.core.data.CalibrationRepository
import com.patoolbox.core.dsp.FrameAccumulator
import com.patoolbox.core.dsp.LogSpectrumMapper
import com.patoolbox.core.dsp.OctaveSmoothing
import com.patoolbox.core.dsp.SpectrogramBuffer
import com.patoolbox.core.dsp.SpectrumAnalyzer
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

/**
 * FFT の点数。細かく見るほど時間分解能が落ちるという、動かせない交換関係がある。
 * 48kHz での更新間隔とビン幅を label に出して、選ぶときに迷わないようにしてある。
 */
enum class AnalyzerFftSize(val size: Int, val label: String, val detail: String) {
    SMALL(4096, "4k", "11.7Hz / 43ms"),
    MEDIUM(8192, "8k", "5.9Hz / 85ms"),
    LARGE(16384, "16k", "2.9Hz / 171ms"),
}

enum class AnalyzerAveraging(val coefficient: Double, val label: String) {
    FAST(0.6, "速い"),
    NORMAL(0.25, "普通"),
    SLOW(0.08, "遅い"),
}

data class AnalyzerUiState(
    val isMeasuring: Boolean = false,
    /**
     * 更新のたびに増える。表示の配列は中身が同じでも描き直したいので、
     * StateFlow の重複排除に引っかからないようにこれを入れている
     */
    val frame: Long = 0,
    val columnsDb: FloatArray = FloatArray(0),
    val peakHoldDb: FloatArray = FloatArray(0),
    val frequencies: DoubleArray = DoubleArray(0),
    val peakFrequencyHz: Double = 0.0,
    val peakLevelDb: Double = 0.0,
    val fftSize: AnalyzerFftSize = AnalyzerFftSize.MEDIUM,
    val smoothing: OctaveSmoothing = OctaveSmoothing.SIXTH,
    val averaging: AnalyzerAveraging = AnalyzerAveraging.NORMAL,
    val peakHold: Boolean = false,
    val calibration: CalibrationProfile = CalibrationProfile.uncalibrated(
        AudioInputDevice.BUILTIN_KEY,
        AudioInputType.BUILTIN_MIC,
    ),
    val proStatus: ProStatus = ProStatus.Free,
    val error: String? = null,
) {
    val hasReading: Boolean get() = columnsDb.isNotEmpty()

    /** 校正済みなら dB SPL、未校正なら dBFS。単位を偽らないために出し分ける */
    val unitLabel: String get() = if (calibration.isCalibrated) "dB SPL" else "dBFS"
}

/**
 * FFT アナライザとスペクトログラムの共通部分。
 *
 * 2つの画面は「同じ解析結果を別の見せ方で出している」だけなので、
 * 取り込みと解析はここ1箇所にまとめてある。
 * 画面ごとに別インスタンスになる（Nav の行き先が別なので）。
 */
@HiltViewModel
class AnalyzerViewModel @Inject constructor(
    private val captureEngine: AudioCaptureEngine,
    private val calibrationRepository: CalibrationRepository,
    proGate: ProGate,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyzerUiState())
    val uiState: StateFlow<AnalyzerUiState> = _uiState.asStateFlow()

    private val sampleRate = AudioCaptureEngine.DEFAULT_SAMPLE_RATE

    private var analyzer = SpectrumAnalyzer(sampleRate, AnalyzerFftSize.MEDIUM.size)
    private var mapper = createMapper(analyzer)
    private var accumulator = createAccumulator(AnalyzerFftSize.MEDIUM.size)

    private var smoothedPowers = DoubleArray(mapper.columns)
    private var peakPowers = DoubleArray(mapper.columns)
    private var scratch = DoubleArray(mapper.columns)
    private var hasFrame = false
    private var frameCounter = 0L

    /**
     * スペクトログラムの履歴。
     *
     * 書き込みはオーディオスレッド、読み出しは描画スレッドで、ロックを取っていない。
     * float 単体の読み書きは分割されないので、最悪でも1行の中に新旧が混ざるだけで、
     * 熱マップの見た目には現れない。ここでロックを取ると録音側が待たされる方が困る。
     */
    val spectrogram = SpectrogramBuffer(
        columns = LogSpectrumMapper.DEFAULT_COLUMNS,
        historySize = SPECTROGRAM_HISTORY,
    )

    init {
        viewModelScope.launch {
            proGate.proStatus.collect { status ->
                _uiState.update { it.copy(proStatus = status) }
            }
        }
    }

    fun start() {
        val current = _uiState.value
        if (current.isMeasuring) return
        if (!current.proStatus.isPro) return
        if (!captureEngine.hasPermission()) {
            _uiState.update { it.copy(error = "マイクの許可がありません") }
            return
        }

        runCatching {
            rebuild(current.fftSize)
            captureEngine.start { buffer, length ->
                accumulator.add(buffer, length) { frame -> onFrame(frame) }
            }
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

    fun toggle() {
        if (_uiState.value.isMeasuring) stop() else start()
    }

    fun setFftSize(fftSize: AnalyzerFftSize) {
        if (_uiState.value.fftSize == fftSize) return
        val wasMeasuring = _uiState.value.isMeasuring
        stop()
        _uiState.update { it.copy(fftSize = fftSize) }
        if (wasMeasuring) start() else rebuild(fftSize)
    }

    fun setSmoothing(smoothing: OctaveSmoothing) {
        _uiState.update { it.copy(smoothing = smoothing) }
    }

    fun setAveraging(averaging: AnalyzerAveraging) {
        _uiState.update { it.copy(averaging = averaging) }
    }

    fun togglePeakHold() {
        val enabled = !_uiState.value.peakHold
        if (!enabled) peakPowers.fill(0.0)
        _uiState.update { it.copy(peakHold = enabled) }
    }

    fun clearPeaks() {
        peakPowers.fill(0.0)
    }

    override fun onCleared() {
        captureEngine.stop()
    }

    private fun onFrame(frame: FloatArray) {
        val state = _uiState.value
        val spectrum = analyzer.powerSpectrum(frame)

        // ピーク周波数は表示カラムではなく生のビンから読む。
        // カラムに畳んだ後だと分解能がカラム幅（1オクターブの1/25程度）まで落ちる
        val peakBin = analyzer.peakBin(spectrum)
        val peakHz = analyzer.interpolatedPeakHz(spectrum, peakBin)
        val peakPower = analyzer.toneMeanSquareAround(spectrum, peakBin)

        mapper.map(spectrum, state.smoothing, scratch)

        val alpha = state.averaging.coefficient
        for (i in scratch.indices) {
            smoothedPowers[i] = if (hasFrame) {
                smoothedPowers[i] + alpha * (scratch[i] - smoothedPowers[i])
            } else {
                scratch[i]
            }
            if (smoothedPowers[i] > peakPowers[i]) peakPowers[i] = smoothedPowers[i]
        }
        hasFrame = true

        val offset = state.calibration.offsetDb
        val columnsDb = FloatArray(smoothedPowers.size) {
            (powerToDb(smoothedPowers[it]) + offset).toFloat()
        }
        val peaksDb = if (state.peakHold) {
            FloatArray(peakPowers.size) { (powerToDb(peakPowers[it]) + offset).toFloat() }
        } else {
            FloatArray(0)
        }

        spectrogram.push(columnsDb)

        frameCounter++
        _uiState.update {
            it.copy(
                frame = frameCounter,
                columnsDb = columnsDb,
                peakHoldDb = peaksDb,
                frequencies = mapper.frequencies,
                peakFrequencyHz = peakHz,
                peakLevelDb = powerToDb(peakPower) + offset,
            )
        }
    }

    private fun rebuild(fftSize: AnalyzerFftSize) {
        analyzer = SpectrumAnalyzer(sampleRate, fftSize.size)
        mapper = createMapper(analyzer)
        accumulator = createAccumulator(fftSize.size)
        smoothedPowers = DoubleArray(mapper.columns)
        peakPowers = DoubleArray(mapper.columns)
        scratch = DoubleArray(mapper.columns)
        hasFrame = false
        spectrogram.clear()
    }

    private fun createMapper(analyzer: SpectrumAnalyzer) = LogSpectrumMapper(
        binCount = analyzer.binCount,
        binWidthHz = analyzer.binWidthHz,
        columns = LogSpectrumMapper.DEFAULT_COLUMNS,
    )

    /** 50% オーバーラップ。点数を上げても更新が飛び飛びに見えないようにする */
    private fun createAccumulator(fftSize: Int) =
        FrameAccumulator(fftSize, hopSize = fftSize / 2)

    private fun observeCalibration(deviceKey: String, inputType: AudioInputType) {
        viewModelScope.launch {
            calibrationRepository.observe(deviceKey, inputType).collect { profile ->
                _uiState.update { it.copy(calibration = profile) }
            }
        }
    }

    private companion object {
        /** 8k/50%重なりで約85msごとなので、300行で約25秒ぶん */
        const val SPECTROGRAM_HISTORY = 300
    }
}
