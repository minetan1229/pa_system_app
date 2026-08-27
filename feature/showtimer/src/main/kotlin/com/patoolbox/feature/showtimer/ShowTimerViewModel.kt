package com.patoolbox.feature.showtimer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patoolbox.core.audio.AudioCaptureEngine
import com.patoolbox.core.audio.AudioInputDevice
import com.patoolbox.core.data.CalibrationRepository
import com.patoolbox.core.data.ShowModeController
import com.patoolbox.core.data.UserPreferencesRepository
import com.patoolbox.core.dsp.FeedbackDetector
import com.patoolbox.core.dsp.LogSpectrumMapper
import com.patoolbox.core.dsp.OctaveSmoothing
import com.patoolbox.core.dsp.SpectrumPipeline
import com.patoolbox.core.dsp.amplitudeToDb
import com.patoolbox.core.dsp.rms
import com.patoolbox.core.model.AudioInputType
import com.patoolbox.core.model.CalibrationProfile
import com.patoolbox.core.model.ShowModeSettings
import com.patoolbox.core.ui.component.FeedbackAlert
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class TimerMode { COUNTDOWN, ELAPSED }

data class ShowTimerUiState(
    val mode: TimerMode = TimerMode.COUNTDOWN,
    val targetMinutes: Int = DEFAULT_MINUTES,
    val running: Boolean = false,
    val elapsedMillis: Long = 0,

    // --- 本番中のモニタ ---
    val monitoring: Boolean = false,
    /** 更新のたびに増える。配列の中身が変わっても等値と見なされないようにする */
    val frame: Long = 0,
    val levelDb: Double = SILENCE_DB,
    /** モニタを開始してからの最大値。「今日の一番大きいところ」を後で言えるように */
    val maxLevelDb: Double = SILENCE_DB,
    val columnsDb: FloatArray = FloatArray(0),
    val frequencies: DoubleArray = DoubleArray(0),
    /** いま鳴っていると判断したハウリング。null なら検出なし */
    val feedback: FeedbackAlert? = null,
    /** 直近で見つけたもの。いま鳴っていなくても「さっき出た」を残す */
    val lastFeedback: FeedbackAlert? = null,
    val calibration: CalibrationProfile = CalibrationProfile.uncalibrated(
        AudioInputDevice.BUILTIN_KEY,
        AudioInputType.BUILTIN_MIC,
    ),
    val monitorError: String? = null,

    // --- 本番モード ---
    val showModeActive: Boolean = false,
    val showMode: ShowModeSettings = ShowModeSettings.Default,
    /** おやすみモードを触る許可があるか。無ければ設定画面へ案内する */
    val hasNotificationPolicy: Boolean = false,
) {
    /** カウントダウンの残り。マイナスは押し（超過）。 */
    val remainingMillis: Long get() = targetMinutes * MILLIS_PER_MINUTE - elapsedMillis

    val isOverrun: Boolean get() = mode == TimerMode.COUNTDOWN && remainingMillis < 0

    /** 画面に出す値（絶対値）。押しかどうかは [isOverrun] で色を変える。 */
    val displayMillis: Long
        get() = when (mode) {
            TimerMode.COUNTDOWN -> abs(remainingMillis)
            TimerMode.ELAPSED -> elapsedMillis
        }

    /** 一度でも解析フレームが揃ったか。レベルには校正オフセットが乗るので値では判定できない */
    val hasLevel: Boolean get() = frame > 0 && columnsDb.isNotEmpty()

    /**
     * 単位。未校正でもオフセット（既定 120dB）は乗っているので dBFS とは書けない。
     * 校正状態はバッジでも出す。
     */
    val unitLabel: String get() = if (calibration.isCalibrated) "dB SPL" else "dB(目安)"

    /** 通知を止める設定なのに許可が無い状態。本番モードに入る前に案内する */
    val needsNotificationPolicyGrant: Boolean
        get() = showMode.needsNotificationPolicy && !hasNotificationPolicy

    /** 本番モード中に画面を消さないか。モードに入っていないときは既存どおり常時点灯 */
    val shouldKeepScreenOn: Boolean
        get() = !showModeActive || showMode.keepScreenOn

    companion object {
        const val DEFAULT_MINUTES = 30
        const val MILLIS_PER_MINUTE = 60_000L
        const val SILENCE_DB = -200.0
    }
}

/**
 * 本番の時間管理と、その場のレベル/スペクトラム監視。
 *
 * 経過時間は tick の回数ではなく単調時計（nanoTime）の差分から出す。
 * tick を数える実装だと、画面が重いときや遅延が積もったときにずれていく。
 *
 * モニタを同じ画面に載せているのは、本番中に卓から離れられないため。
 * 時間を見るために画面を切り替えると音が見えなくなる、という往復が起きると
 * どちらも見なくなる。
 */
@HiltViewModel
class ShowTimerViewModel @Inject constructor(
    private val captureEngine: AudioCaptureEngine,
    private val calibrationRepository: CalibrationRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val showModeController: ShowModeController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShowTimerUiState())
    val uiState: StateFlow<ShowTimerUiState> = _uiState.asStateFlow()

    private var ticker: Job? = null
    private var runStartNanos = 0L
    private var accumulatedMillis = 0L

    private val pipeline = SpectrumPipeline(
        sampleRate = AudioCaptureEngine.DEFAULT_SAMPLE_RATE,
        columns = LogSpectrumMapper.DEFAULT_COLUMNS,
    )

    /**
     * ハウリングの検出。
     *
     * スペクトラムと同じフレームを食わせている。取り込みを2系統に分けると
     * 同じ音を2回 FFT することになるうえ、表示とずれた時刻の判定が出る。
     * [SpectrumPipeline] と点数を揃えてあるのはそのため。
     */
    private val feedbackDetector = FeedbackDetector(
        sampleRate = AudioCaptureEngine.DEFAULT_SAMPLE_RATE,
        fftSize = pipeline.fftSize,
    )

    /** 検出が途切れてから何フレーム経ったか。1フレームの空振りで表示を消さないための猶予 */
    private var framesSinceFeedback = Int.MAX_VALUE

    /**
     * 直近ブロックのレベルと、フレーム間の最大値。
     *
     * 取り込みは約21msごとに来るが、UI をその速さで更新しても読めないうえに
     * 描画が詰まる。FFT のフレーム（約85ms）が揃ったときにまとめて出す。
     */
    @Volatile
    private var lastLevelDb = ShowTimerUiState.SILENCE_DB

    @Volatile
    private var runningMaxDb = ShowTimerUiState.SILENCE_DB

    init {
        _uiState.update { it.copy(frequencies = pipeline.frequencies) }
        viewModelScope.launch {
            preferencesRepository.preferences.collect { prefs ->
                _uiState.update { it.copy(showMode = prefs.showMode) }
            }
        }
        refreshNotificationPolicy()
    }

    /**
     * 許可の状態を読み直す。
     * 設定アプリで許可してから戻ってきたときに呼ぶ（画面側の再開時）。
     */
    fun refreshNotificationPolicy() {
        _uiState.update {
            it.copy(hasNotificationPolicy = showModeController.hasNotificationPolicyAccess())
        }
    }

    fun toggle() {
        if (_uiState.value.running) pause() else start()
    }

    fun start() {
        if (_uiState.value.running) return
        runStartNanos = System.nanoTime()
        _uiState.update { it.copy(running = true) }

        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                val elapsed = accumulatedMillis + elapsedSinceStartMillis()
                _uiState.update { it.copy(elapsedMillis = elapsed) }
                delay(TICK_MS)
            }
        }
    }

    fun pause() {
        if (!_uiState.value.running) return
        accumulatedMillis += elapsedSinceStartMillis()
        ticker?.cancel()
        ticker = null
        _uiState.update { it.copy(running = false, elapsedMillis = accumulatedMillis) }
    }

    fun reset() {
        ticker?.cancel()
        ticker = null
        accumulatedMillis = 0
        runStartNanos = 0
        _uiState.update { it.copy(running = false, elapsedMillis = 0) }
    }

    fun setMode(mode: TimerMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    fun setTargetMinutes(minutes: Int) {
        _uiState.update { it.copy(targetMinutes = minutes.coerceIn(MIN_MINUTES, MAX_MINUTES)) }
    }

    // --- モニタ ---

    fun hasMicPermission(): Boolean = captureEngine.hasPermission()

    fun startMonitor() {
        if (_uiState.value.monitoring) return
        if (!captureEngine.hasPermission()) {
            _uiState.update { it.copy(monitorError = "マイクの許可がありません") }
            return
        }

        pipeline.reset()
        feedbackDetector.reset()
        framesSinceFeedback = Int.MAX_VALUE
        lastLevelDb = ShowTimerUiState.SILENCE_DB
        runningMaxDb = ShowTimerUiState.SILENCE_DB
        _uiState.update { it.copy(feedback = null) }

        runCatching {
            captureEngine.start { buffer, length ->
                val level = amplitudeToDb(rms(buffer, length))
                lastLevelDb = level
                if (level > runningMaxDb) runningMaxDb = level
                pipeline.accumulator.add(buffer, length) { frame -> onFrame(frame) }
            }
        }.onSuccess { session ->
            _uiState.update { it.copy(monitoring = true, monitorError = null) }
            observeCalibration(session.calibrationKey, session.inputType)
        }.onFailure { throwable ->
            _uiState.update { it.copy(monitoring = false, monitorError = throwable.message) }
        }
    }

    fun stopMonitor() {
        captureEngine.stop()
        _uiState.update { it.copy(monitoring = false, feedback = null) }
    }

    fun toggleMonitor() {
        if (_uiState.value.monitoring) stopMonitor() else startMonitor()
    }

    /** 最大値だけを捨てる。曲の切れ目で「ここから測り直す」ときに使う */
    fun resetMaxLevel() {
        runningMaxDb = lastLevelDb
    }

    /** 「さっき出た」の記録を消す。対処が済んだときに押す */
    fun clearLastFeedback() {
        _uiState.update { it.copy(lastFeedback = null) }
    }

    // --- 本番モード ---

    fun toggleShowMode() {
        if (_uiState.value.showModeActive) exitShowMode() else enterShowMode()
    }

    fun enterShowMode() {
        val state = _uiState.value
        if (state.showModeActive) return

        showModeController.enter(state.showMode)
        // 止められなかった場合は hasNotificationPolicy が false のままになり、
        // 画面側が許可への導線（needsNotificationPolicyGrant）を出す。
        // ここで別のエラー文言を足すと、同じことを2箇所で言うことになる
        _uiState.update {
            it.copy(
                showModeActive = true,
                hasNotificationPolicy = showModeController.hasNotificationPolicyAccess(),
            )
        }
    }

    fun exitShowMode() {
        showModeController.exit()
        _uiState.update { it.copy(showModeActive = false) }
    }

    /**
     * 設定を変える。本番モード中なら即座に反映し直す
     * （「アラームは通す」に変えたのに次の本番まで効かない、では意味がない）。
     */
    fun setShowMode(settings: ShowModeSettings) {
        viewModelScope.launch { preferencesRepository.setShowMode(settings) }
        _uiState.update { it.copy(showMode = settings) }
        if (_uiState.value.showModeActive) {
            showModeController.exit()
            showModeController.enter(settings)
        }
    }

    /** おやすみモードの許可を取りに行くための Intent。画面側が startActivity する */
    fun notificationPolicySettingsIntent() =
        showModeController.notificationPolicySettingsIntent()

    override fun onCleared() {
        ticker?.cancel()
        captureEngine.stop()
        // 端末を黙らせたまま画面を閉じさせない。
        // ここを忘れると「通知が来なくなった」と気づくのが翌日になる
        if (_uiState.value.showModeActive) showModeController.exit()
    }

    private fun onFrame(frame: FloatArray) {
        val offset = _uiState.value.calibration.offsetDb
        val snapshot = pipeline.analyze(
            frame = frame,
            smoothing = OctaveSmoothing.SIXTH,
            offsetDb = offset,
        )

        val alert = detectFeedback(frame)

        _uiState.update {
            it.copy(
                frame = it.frame + 1,
                levelDb = lastLevelDb + offset,
                maxLevelDb = runningMaxDb + offset,
                columnsDb = snapshot.columnsDb,
                frequencies = pipeline.frequencies,
                feedback = alert,
                lastFeedback = alert ?: it.lastFeedback,
            )
        }
    }

    /**
     * ハウリングの判定。
     *
     * 一番突出しているものを1つだけ出す。本番中に3本並べても打つ手は変わらないし、
     * 表示が増えるほど時間表示が押し出される。

     * 見つからなくなってもすぐには消さない。ハウリングは揺れるので、
     * 1フレーム外しただけで表示が点滅すると読めなくなる。
     */
    private fun detectFeedback(frame: FloatArray): FeedbackAlert? {
        val best = feedbackDetector.process(frame).firstOrNull()

        if (best != null) {
            framesSinceFeedback = 0
            return FeedbackAlert(
                frequencyHz = best.frequencyHz,
                noteName = best.noteName,
                bandLabel = best.bandLabel,
                prominenceDb = best.prominenceDb,
            )
        }

        if (framesSinceFeedback < FEEDBACK_HOLD_FRAMES) {
            framesSinceFeedback++
            return _uiState.value.feedback
        }
        return null
    }

    private fun observeCalibration(deviceKey: String, inputType: AudioInputType) {
        viewModelScope.launch {
            calibrationRepository.observe(deviceKey, inputType).collect { profile ->
                _uiState.update { it.copy(calibration = profile) }
            }
        }
    }

    private fun elapsedSinceStartMillis(): Long =
        (System.nanoTime() - runStartNanos) / NANOS_PER_MILLI

    companion object {
        const val MIN_MINUTES = 1
        const val MAX_MINUTES = 600

        /** よく使う持ち時間 */
        val PRESET_MINUTES = listOf(5, 10, 15, 20, 30, 45, 60, 90)

        private const val TICK_MS = 100L
        private const val NANOS_PER_MILLI = 1_000_000L

        /**
         * 検出が途切れてから表示を消すまでのフレーム数。
         * 1フレームは約85ms なので、12 で約1秒。
         * これより短いと表示が点滅し、長いと収まった後も出たままになる
         */
        private const val FEEDBACK_HOLD_FRAMES = 12
    }
}
