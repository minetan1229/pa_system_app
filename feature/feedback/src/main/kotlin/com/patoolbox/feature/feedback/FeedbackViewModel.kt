package com.patoolbox.feature.feedback

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patoolbox.core.audio.AudioCaptureEngine
import com.patoolbox.core.billing.ProGate
import com.patoolbox.core.dsp.FeedbackDetector
import com.patoolbox.core.dsp.FeedbackSensitivity
import com.patoolbox.core.dsp.FeedbackTracker
import com.patoolbox.core.dsp.OctaveSmoothing
import com.patoolbox.core.dsp.SpectrumPipeline
import com.patoolbox.core.model.ProStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedbackUiState(
    val isMeasuring: Boolean = false,
    /**
     * 更新のたびに増える。配列の中身が同じでも描き直したいので、
     * StateFlow の重複排除に引っかからないようにこれを入れている
     */
    val frame: Long = 0,
    /** いまこの瞬間に発振している成分 */
    val candidates: List<FeedbackDetector.Candidate> = emptyList(),
    /** 記録開始からの履歴。[sort] の順に並んでいる */
    val tracks: List<FeedbackTracker.Track> = emptyList(),
    val columnsDb: FloatArray = FloatArray(0),
    val peakHoldDb: FloatArray = FloatArray(0),
    val frequencies: DoubleArray = DoubleArray(0),
    val elapsedMs: Long = 0,
    val sort: FeedbackTracker.Sort = FeedbackTracker.Sort.TOTAL_TIME,
    val sensitivity: FeedbackSensitivity = FeedbackSensitivity.NORMAL,
    val proStatus: ProStatus = ProStatus.Free,
    val error: String? = null,
) {
    val hasReading: Boolean get() = columnsDb.isNotEmpty()

    /** いま鳴っているもの。図の印と大表示に使う */
    val activeTracks: List<FeedbackTracker.Track> get() = tracks.filter { it.isActive }

    /**
     * いちばん潰すべき1本。
     *
     * 「いま出ている中で一番大きいもの」ではなく **累計で一番長く鳴っているもの** を出す。
     * 一瞬の派手な発振より、鳴り続けている方が本番を壊す。
     */
    val worst: FeedbackTracker.Track?
        get() = tracks.maxWithOrNull(
            compareBy<FeedbackTracker.Track> { it.totalRingingMs }.thenBy { it.longestRunMs },
        )

    /** 累計で1秒以上鳴ったものの数。「何本相手にしているか」の目安 */
    val establishedCount: Int get() = tracks.count { it.totalRingingMs >= ESTABLISHED_MS }

    private companion object {
        const val ESTABLISHED_MS = 1_000L
    }
}

/**
 * ハウリング検出。
 *
 * 検出そのもの（[FeedbackDetector]）は「いま鳴っている成分」しか返さない。
 * それだけを画面に出していたのが以前の作りで、消えると何も残らなかった。
 * 実際に潰したいのは **鳴ったり止んだりしながら居座っている周波数** なので、
 * [FeedbackTracker] で時間方向に束ねて、累計時間と回数で並べ替えられるようにしている。
 *
 * 図（[SpectrumPipeline]）を並べて出すのは、数字の一覧だけだと
 * 「その周波数が全体のどこにあるか」「他の山とどれだけ差があるか」が分からないため。
 * ピーク保持を常に入れてあるので、線の形がそのまま「この現場で溜まりやすい帯域」になる。
 */
@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val captureEngine: AudioCaptureEngine,
    proGate: ProGate,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

    private val sampleRate = AudioCaptureEngine.DEFAULT_SAMPLE_RATE

    private var detector = createDetector(FeedbackSensitivity.NORMAL)
    private val pipeline = SpectrumPipeline(sampleRate, fftSize = FFT_SIZE)

    /**
     * 履歴は測定を止めても消さない。
     * リハで拾った周波数を本番前に見返す、というのがこの画面の主な使い道になる。
     */
    private val tracker = FeedbackTracker()

    private var frameCounter = 0L

    init {
        viewModelScope.launch {
            proGate.proStatus.collect { status ->
                _uiState.update { it.copy(proStatus = status) }
            }
        }
        // 測定前でも軸だけは描いておく。何をする画面か図で分かる方がよい
        _uiState.update { it.copy(frequencies = pipeline.frequencies) }
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
            pipeline.reset()
            captureEngine.start { buffer, length ->
                pipeline.accumulator.add(buffer, length) { frame -> onFrame(frame) }
            }
        }.onSuccess {
            _uiState.update { it.copy(isMeasuring = true, error = null) }
        }.onFailure { throwable ->
            _uiState.update { it.copy(isMeasuring = false, error = throwable.message) }
        }
    }

    fun stop() {
        captureEngine.stop()
        // 履歴と図は残す。止めた瞬間に画面が空になると、何を見ていたか分からなくなる。
        // ただし一覧は取り直す。最後のフレームの「発振中」が残ったままだと、
        // 止めた後もまだ鳴っているように見える
        // 途切れの許容時間ぶん先の時刻で締める。こうすると連続時間が確定し、
        // 全部が「停止」として並ぶ（空の更新なので累計時間は増えない）
        val closedAt = now() + tracker.gapToleranceMs + 1
        tracker.update(emptyList(), closedAt)

        _uiState.update { state ->
            state.copy(
                isMeasuring = false,
                candidates = emptyList(),
                tracks = tracker.snapshot(closedAt, state.sort, MIN_TOTAL_MS),
            )
        }
    }

    fun setSort(sort: FeedbackTracker.Sort) {
        _uiState.update { state ->
            state.copy(sort = sort, tracks = tracker.snapshot(now(), sort, MIN_TOTAL_MS))
        }
    }

    /**
     * 感度の変更。
     *
     * 検出器は作り直すが、履歴は残す。感度を上げ下げしながら同じ現場を見るのが普通で、
     * そのたびに集計が消えると比べられない。
     */
    fun setSensitivity(sensitivity: FeedbackSensitivity) {
        if (_uiState.value.sensitivity == sensitivity) return
        detector = createDetector(sensitivity)
        _uiState.update { it.copy(sensitivity = sensitivity, candidates = emptyList()) }
    }

    /** 履歴を捨てる。転換のあと、次のバンドで取り直すときに使う */
    fun clearHistory() {
        tracker.reset()
        detector.reset()
        pipeline.clearPeakHold()
        _uiState.update {
            it.copy(
                tracks = emptyList(),
                candidates = emptyList(),
                peakHoldDb = FloatArray(0),
                elapsedMs = 0,
            )
        }
    }

    override fun onCleared() {
        captureEngine.stop()
        super.onCleared()
    }

    private fun onFrame(frame: FloatArray) {
        val state = _uiState.value
        val found = detector.process(frame)
        val now = now()

        // 空のフレームも必ず渡す。渡さないと鳴り止んだことが分からず、連続時間が伸び続ける
        tracker.update(found, now)

        val snapshot = pipeline.analyze(
            frame = frame,
            // ならしを掛けない。1本の細い山が消えては、この画面の意味が無い
            smoothing = OctaveSmoothing.NONE,
            averagingCoefficient = AVERAGING,
            peakHold = true,
        )

        frameCounter++
        _uiState.update {
            it.copy(
                frame = frameCounter,
                candidates = found,
                tracks = tracker.snapshot(now, state.sort, MIN_TOTAL_MS),
                columnsDb = snapshot.columnsDb,
                peakHoldDb = snapshot.peakHoldDb,
                frequencies = pipeline.frequencies,
                elapsedMs = tracker.elapsedMs(now),
            )
        }
    }

    private fun createDetector(sensitivity: FeedbackSensitivity) = FeedbackDetector(
        sampleRate = sampleRate,
        fftSize = FFT_SIZE,
        prominenceThresholdDb = sensitivity.prominenceDb,
        sustainFrames = sensitivity.sustainFrames,
    )

    private fun now(): Long = SystemClock.elapsedRealtime()

    private companion object {
        const val FFT_SIZE = 8192

        /**
         * 図のならし。ハウリングは1本の細い山なので、
         * 追従は速く（値を大きく）しておかないと立ち上がりを見逃す
         */
        const val AVERAGING = 0.5

        /**
         * 一覧に残す最低の累計時間。
         * これ未満は拍手や物音で1〜2フレーム立っただけの成分として扱う
         * （いま鳴っているものは時間に関係なく出る）
         */
        const val MIN_TOTAL_MS = 200L
    }
}
