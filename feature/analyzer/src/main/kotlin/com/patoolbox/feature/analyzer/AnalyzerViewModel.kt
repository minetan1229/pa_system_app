package com.patoolbox.feature.analyzer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patoolbox.core.audio.AudioCaptureEngine
import com.patoolbox.core.audio.AudioInputDevice
import com.patoolbox.core.billing.ProGate
import com.patoolbox.core.data.CalibrationRepository
import com.patoolbox.core.dsp.LogSpectrumMapper
import com.patoolbox.core.dsp.NoteNames
import com.patoolbox.core.dsp.OctaveSmoothing
import com.patoolbox.core.dsp.SpectrogramBuffer
import com.patoolbox.core.dsp.SpectrumPeak
import com.patoolbox.core.dsp.SpectrumPipeline
import com.patoolbox.core.dsp.WindowFunction
import com.patoolbox.core.model.AudioInputType
import com.patoolbox.core.model.CalibrationProfile
import com.patoolbox.core.model.ProStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

/** ピークホールドの保持秒数。無期限にしないのは [feature.rta.RtaPeakHoldDuration] と同じ理由。 */
enum class AnalyzerPeakHoldDuration(val seconds: Double, val label: String) {
    SHORT(5.0, "5秒"),
    LONG(10.0, "10秒"),
}

/**
 * 縦軸の幅。上端はレベルに追従させるので、利用者が決めるのは「何dB分を見るか」だけ。
 *
 * 50dB は1本の山の形を見るとき、90dB は暗騒音まで含めて全体を見るとき。
 */
enum class AnalyzerSpan(val db: Double, val label: String) {
    NARROW(50.0, "50dB"),
    NORMAL(70.0, "70dB"),
    WIDE(90.0, "90dB"),
}

/**
 * 選べる窓関数。
 *
 * [WindowFunction] のうち計測で使い分ける3つだけを出す。
 * 矩形と Hamming は、この画面での使いどころが説明できないので出さない。
 */
enum class AnalyzerWindow(val function: WindowFunction, val label: String, val detail: String) {
    HANN(WindowFunction.HANN, "Hann", "汎用。迷ったらこれ"),
    BLACKMAN_HARRIS(WindowFunction.BLACKMAN_HARRIS, "B-Harris", "近い2本を分けたいとき"),
    FLAT_TOP(WindowFunction.FLAT_TOP, "Flat-top", "レベルを正確に読みたいとき"),
}

/** 表に出す山。音名は表示のためだけに付ける（PA では「250Hz あたり」より "B3" が通じる場面がある） */
data class AnalyzerPeak(
    val frequencyHz: Double,
    val levelDb: Double,
    val noteName: String?,
)

/**
 * 控えておいた読み。
 *
 * リハでハウった周波数を本番前に見返す、卓の EQ に入れる値をメモする、
 * といった使い方を想定している。画面を離れると消えるので、
 * 残したいものは書き写してもらう前提（保存先を作るとどこに何があるか分からなくなる）。
 *
 * @param label 何回目の保存か。並べたときに順番が分かればよいので番号だけ持つ
 */
data class SavedPeakSet(
    val label: Int,
    val peaks: List<AnalyzerPeak>,
)

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
    val topPeaks: List<AnalyzerPeak> = emptyList(),
    /** 「ピーク保存」で控えた読み。新しいものが先頭 */
    val savedPeaks: List<SavedPeakSet> = emptyList(),
    /** 利用者が置いたカーソル。null なら置いていない */
    val cursorHz: Double? = null,
    val cursorLevelDb: Double = Double.NEGATIVE_INFINITY,
    val fftSize: AnalyzerFftSize = AnalyzerFftSize.MEDIUM,
    val window: AnalyzerWindow = AnalyzerWindow.HANN,
    val smoothing: OctaveSmoothing = OctaveSmoothing.SIXTH,
    val averaging: AnalyzerAveraging = AnalyzerAveraging.NORMAL,
    val span: AnalyzerSpan = AnalyzerSpan.NORMAL,
    val peakHold: Boolean = false,
    val peakHoldDuration: AnalyzerPeakHoldDuration = AnalyzerPeakHoldDuration.LONG,
    /** カーソルの倍音を重ねるか。ハムや共振の次数を追うときに使う */
    val showHarmonics: Boolean = false,
    val calibration: CalibrationProfile = CalibrationProfile.uncalibrated(
        AudioInputDevice.BUILTIN_KEY,
        AudioInputType.BUILTIN_MIC,
    ),
    val proStatus: ProStatus = ProStatus.Free,
    val error: String? = null,
) {
    val hasReading: Boolean get() = columnsDb.isNotEmpty()

    /**
     * 単位。
     *
     * 表示している数値には常に [CalibrationProfile.offsetDb] が乗っている。
     * 未校正でもオフセットは 120dB（[CalibrationProfile.DEFAULT_OFFSET_DB]）が入るので、
     * 「dBFS」と書くと 120dB ずれた数字に嘘の単位を付けることになる。
     * 未校正であることは [com.patoolbox.core.ui.component.CalibrationBadge] でも出す。
     */
    val unitLabel: String get() = if (calibration.isCalibrated) "dB SPL" else "dB(目安)"

    /** カーソルの音名。ハウリングの帯域を人に伝えるときに使う */
    val cursorNote: String? get() = cursorHz?.let { NoteNames.fromFrequency(it)?.displayName }

    /** 一番出ている成分の音名。大表示の下に添える */
    val peakNote: String?
        get() = if (hasReading) NoteNames.fromFrequency(peakFrequencyHz)?.displayName else null
}

/**
 * スペクトラムアナライザとスペクトログラムの共通部分。
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

    private var pipeline = createPipeline(AnalyzerFftSize.MEDIUM, AnalyzerWindow.HANN)

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

    /** スペクトログラム1行あたりの時間。時間軸の目盛りに使う */
    val hopSeconds: Double get() = pipeline.hopSeconds

    /** 履歴として遡れる長さ（秒）。FFT の点数を変えると変わるので計算で出す */
    val historySeconds: Double get() = spectrogram.historySize * pipeline.hopSeconds

    init {
        viewModelScope.launch {
            proGate.proStatus.collect { status ->
                _uiState.update { it.copy(proStatus = status) }
            }
        }
        // 未計測でも軸だけは描けるようにしておく。
        // 真っさらな画面より「20Hz〜20kHz の図が待っている」方が何をする画面か分かる
        _uiState.update { it.copy(frequencies = pipeline.frequencies) }
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
            rebuild(current.fftSize, current.window)
            captureEngine.start { buffer, length ->
                pipeline.accumulator.add(buffer, length) { frame -> onFrame(frame) }
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
        restartWith { it.copy(fftSize = fftSize) }
    }

    fun setWindow(window: AnalyzerWindow) {
        if (_uiState.value.window == window) return
        restartWith { it.copy(window = window) }
    }

    /**
     * 解析器を作り直す設定の変更。
     * 窓も点数も [SpectrumPipeline] の生成時に決まるので、測定中なら一度止めて開き直す。
     */
    private fun restartWith(change: (AnalyzerUiState) -> AnalyzerUiState) {
        val wasMeasuring = _uiState.value.isMeasuring
        stop()
        _uiState.update(change)
        val state = _uiState.value
        if (wasMeasuring) start() else rebuild(state.fftSize, state.window)
    }

    fun setSmoothing(smoothing: OctaveSmoothing) {
        _uiState.update { it.copy(smoothing = smoothing) }
    }

    fun setAveraging(averaging: AnalyzerAveraging) {
        _uiState.update { it.copy(averaging = averaging) }
    }

    fun setSpan(span: AnalyzerSpan) {
        _uiState.update { it.copy(span = span) }
    }

    /** 図を触ったときの周波数。カーソルのレベルは次のフレームで埋まる */
    fun setCursor(frequencyHz: Double) {
        _uiState.update { state ->
            state.copy(
                cursorHz = frequencyHz,
                cursorLevelDb = levelAt(frequencyHz, state.columnsDb),
            )
        }
    }

    fun clearCursor() {
        _uiState.update { it.copy(cursorHz = null, cursorLevelDb = Double.NEGATIVE_INFINITY) }
    }

    fun togglePeakHold() {
        val enabled = !_uiState.value.peakHold
        if (!enabled) pipeline.clearPeakHold()
        _uiState.update { it.copy(peakHold = enabled) }
    }

    /** ピークホールドの保持秒数。次のフレームから効く。 */
    fun setPeakHoldDuration(duration: AnalyzerPeakHoldDuration) {
        _uiState.update { it.copy(peakHoldDuration = duration) }
    }

    fun toggleHarmonics() {
        _uiState.update { it.copy(showHarmonics = !it.showHarmonics) }
    }

    fun clearPeaks() {
        pipeline.clearPeakHold()
    }

    /**
     * いま出ている山を控える。
     *
     * ピークホールドが入っていればその線の頂点、入っていなければ現在の山を残す。
     * 何も読めていないときは何もしない（空の行が増えるだけで、後から見て意味が無い）。
     */
    fun savePeaks() {
        val state = _uiState.value
        if (state.topPeaks.isEmpty()) return
        val entry = SavedPeakSet(label = state.savedPeaks.size + 1, peaks = state.topPeaks)
        _uiState.update { it.copy(savedPeaks = listOf(entry) + it.savedPeaks) }
    }

    fun clearSavedPeaks() {
        _uiState.update { it.copy(savedPeaks = emptyList()) }
    }

    override fun onCleared() {
        captureEngine.stop()
    }

    private fun onFrame(frame: FloatArray) {
        val state = _uiState.value
        val snapshot = pipeline.analyze(
            frame = frame,
            smoothing = state.smoothing,
            averagingCoefficient = state.averaging.coefficient,
            offsetDb = state.calibration.offsetDb,
            peakHold = state.peakHold,
            peakHoldSeconds = state.peakHoldDuration.seconds,
        )

        spectrogram.push(snapshot.columnsDb)

        frameCounter++
        _uiState.update {
            it.copy(
                frame = frameCounter,
                columnsDb = snapshot.columnsDb,
                peakHoldDb = snapshot.peakHoldDb,
                frequencies = pipeline.frequencies,
                peakFrequencyHz = snapshot.peakFrequencyHz,
                peakLevelDb = snapshot.peakLevelDb,
                topPeaks = snapshot.topPeaks.map(::toAnalyzerPeak),
                cursorLevelDb = it.cursorHz?.let { hz -> levelAt(hz, snapshot.columnsDb) }
                    ?: Double.NEGATIVE_INFINITY,
            )
        }
    }

    private fun toAnalyzerPeak(peak: SpectrumPeak) = AnalyzerPeak(
        frequencyHz = peak.frequencyHz,
        levelDb = peak.levelDb,
        noteName = NoteNames.fromFrequency(peak.frequencyHz)?.displayName,
    )

    private fun levelAt(frequencyHz: Double, columnsDb: FloatArray): Double {
        if (columnsDb.isEmpty()) return Double.NEGATIVE_INFINITY
        val column = pipeline.columnOf(frequencyHz).coerceIn(0, columnsDb.size - 1)
        return columnsDb[column].toDouble()
    }

    private fun rebuild(fftSize: AnalyzerFftSize, window: AnalyzerWindow) {
        pipeline = createPipeline(fftSize, window)
        spectrogram.clear()
        _uiState.update { it.copy(frequencies = pipeline.frequencies) }
    }

    private fun createPipeline(fftSize: AnalyzerFftSize, window: AnalyzerWindow) = SpectrumPipeline(
        sampleRate = sampleRate,
        fftSize = fftSize.size,
        columns = LogSpectrumMapper.DEFAULT_COLUMNS,
        windowFunction = window.function,
    )

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
