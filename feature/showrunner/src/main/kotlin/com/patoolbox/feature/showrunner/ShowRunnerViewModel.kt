package com.patoolbox.feature.showrunner

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patoolbox.core.audio.AudioCaptureEngine
import com.patoolbox.core.audio.AudioInputDevice
import com.patoolbox.core.audio.SoundCuePlayer
import com.patoolbox.core.billing.ProGate
import com.patoolbox.core.data.CalibrationRepository
import com.patoolbox.core.data.JobRepository
import com.patoolbox.core.data.ScheduleRepository
import com.patoolbox.core.data.ShowModeController
import com.patoolbox.core.data.SoundCueRepository
import com.patoolbox.core.data.UserPreferencesRepository
import com.patoolbox.core.model.Job
import com.patoolbox.core.dsp.FeedbackDetector
import com.patoolbox.core.dsp.OctaveSmoothing
import com.patoolbox.core.dsp.SpectrumPipeline
import com.patoolbox.core.dsp.amplitudeToDb
import com.patoolbox.core.dsp.rms
import com.patoolbox.core.model.AudioInputType
import com.patoolbox.core.model.CalibrationProfile
import com.patoolbox.core.model.ProStatus
import com.patoolbox.core.model.ScheduleTimeline
import com.patoolbox.core.model.ShowModeSettings
import com.patoolbox.core.model.SoundCue
import com.patoolbox.core.model.TimelineEntry
import com.patoolbox.core.ui.component.FeedbackAlert
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.Job as CoroutineJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.patoolbox.core.model.ScheduleItem as CoreScheduleItem

/**
 * 1本ぶんの予定。「何を」「何分で」を基本に持つ。
 *
 * 開始・終了の時刻を必須にしないのは、現場では押し巻きで前の項目が伸び縮みするため。
 * 常に「いま開いている項目から何分」で数える方が、後ろの項目がずれても壊れない。
 * 時刻表示（[ShowRunnerUiState.projectedTimeline]）はこの上に**あとから**載せる。
 *
 * @param fixedStartEpochMs 「本番は19:00固定」のような譲れない開始時刻。無ければ積み上げで計算する
 * @param linkedSoundCueId この項目が始まったときに鳴らす SE。[com.patoolbox.core.model.SoundCue.id]
 * @param cueDelayMs 項目が始まってから SE を鳴らすまでの遅延
 * @param extraMinutes 本番中に延長した分。[plannedMinutes] とは別に持つので、
 *   延長操作の履歴が「何分で組んだか」を消さない
 */
data class ScheduleItem(
    val id: Long,
    val title: String,
    val plannedMinutes: Int,
    val fixedStartEpochMs: Long? = null,
    val linkedSoundCueId: Long? = null,
    val cueDelayMs: Long = 0,
    val extraMinutes: Int = 0,
) {
    /** 延長込みの、いま持っている時間。 */
    val totalMinutes: Int get() = plannedMinutes + extraMinutes
}

data class ShowRunnerUiState(
    val schedule: List<ScheduleItem> = emptyList(),
    /** いまカウントダウン中の項目。null なら何も走っていない */
    val activeItemId: Long? = null,
    val running: Boolean = false,
    val elapsedMillis: Long = 0,

    /** 追加フォームの下書き */
    val draftTitle: String = "",
    val draftMinutes: Int = DEFAULT_DRAFT_MINUTES,

    /** 延長フォームの下書き。プリセットを押すと仮選択され、微調整してから適用する */
    val draftExtendMinutes: Int = DEFAULT_EXTEND_MINUTES,

    /** 進行表全体の開始予定時刻。null なら時刻は出さず、分表示だけにする */
    val anchorEpochMs: Long? = null,

    /** SE パッド・同期音源。[com.patoolbox.feature.sfx.SfxScreen] と同じ保存先を見ている */
    val pads: List<SoundCue> = emptyList(),
    val playingIds: Set<Long> = emptySet(),
    val importing: Boolean = false,
    val proStatus: ProStatus = ProStatus.Free,
    val error: String? = null,

    // --- 本番モード ---
    val showModeActive: Boolean = false,
    val showMode: ShowModeSettings = ShowModeSettings.Default,
    val hasNotificationPolicy: Boolean = false,

    // --- モニター（ハウリング測定・スペクトラムアナライザ） ---
    val monitoring: Boolean = false,
    /** 更新のたびに増える。配列の中身が変わっても等値と見なされないようにする */
    val monitorFrame: Long = 0,
    val levelDb: Double = SILENCE_DB,
    val maxLevelDb: Double = SILENCE_DB,
    val columnsDb: FloatArray = FloatArray(0),
    val peakHoldDb: FloatArray = FloatArray(0),
    val frequencies: DoubleArray = DoubleArray(0),
    val feedback: FeedbackAlert? = null,
    val lastFeedback: FeedbackAlert? = null,
    val calibration: CalibrationProfile = CalibrationProfile.uncalibrated(
        AudioInputDevice.BUILTIN_KEY,
        AudioInputType.BUILTIN_MIC,
    ),
    val monitorError: String? = null,

    /** 「案件から取り込む」ダイアログ用の案件一覧 */
    val availableJobs: List<Job> = emptyList(),
) {
    /** 無料版でこれ以上パッドを増やせるか。SE パッド画面と同じ上限を共有する */
    val canAddMorePads: Boolean
        get() = proStatus.isPro || pads.size < SoundCue.FREE_LIMIT

    val activeItem: ScheduleItem? get() = schedule.firstOrNull { it.id == activeItemId }

    /** 残り時間（ミリ秒）。マイナスは押し。延長ぶんを含む */
    val remainingMillis: Long
        get() = (activeItem?.totalMinutes ?: 0) * MILLIS_PER_MINUTE - elapsedMillis

    val isOverrun: Boolean get() = activeItem != null && remainingMillis < 0

    val displayMillis: Long get() = abs(remainingMillis)

    /** いま走っている項目の次。無ければ null（最後まで終わった） */
    val nextItem: ScheduleItem?
        get() {
            val current = activeItemId ?: return schedule.firstOrNull()
            val index = schedule.indexOfFirst { it.id == current }
            if (index < 0 || index + 1 >= schedule.size) return null
            return schedule[index + 1]
        }

    val hasReading: Boolean get() = columnsDb.isNotEmpty()

    /** 一度でも解析フレームが揃ったか */
    val hasLevel: Boolean get() = monitorFrame > 0 && columnsDb.isNotEmpty()

    /** 単位。未校正でもオフセット（既定 120dB）は乗っているので dBFS とは書けない */
    val unitLabel: String get() = if (calibration.isCalibrated) "dB SPL" else "dB(目安)"

    /** 通知を止める設定なのに許可が無い状態 */
    val needsNotificationPolicyGrant: Boolean
        get() = showMode.needsNotificationPolicy && !hasNotificationPolicy

    /** 全項目の合計時間（分）。進行表が空のときは 0 */
    val totalScheduleMinutes: Int get() = schedule.sumOf { it.totalMinutes }

    /**
     * 進行表に時刻を付けたもの。[anchorEpochMs] が無ければ空（分表示だけにする）。
     *
     * アクティブな項目より手前は元のアンカーからの静的な積み上げのまま見せる
     * （終わったものの時刻をいまさら動かしても現場の役に立たない）。
     * アクティブな項目とそれ以降は、**いま実際に始まった時刻**を新しいアンカーとして
     * 積み直す。押し・巻きが以降の予定時刻に自動で反映される。
     *
     * @param nowEpochMs 呼び出し時点の時刻。テストで固定できるよう引数にしている
     */
    fun projectedTimeline(nowEpochMs: Long): List<TimelineEntry> {
        val anchor = anchorEpochMs ?: return emptyList()
        val activeIndex = activeItemId?.let { id -> schedule.indexOfFirst { it.id == id } } ?: -1
        if (activeIndex < 0) {
            return ScheduleTimeline.build(anchor, schedule.map(ScheduleItem::toCoreItem))
        }

        val before = schedule.subList(0, activeIndex)
        val active = schedule[activeIndex]
        val after = schedule.subList(activeIndex + 1, schedule.size)

        val estimatedActiveStart = nowEpochMs - elapsedMillis
        val activeEnd = estimatedActiveStart + active.totalMinutes * MILLIS_PER_MINUTE

        val beforeEntries = ScheduleTimeline.build(anchor, before.map(ScheduleItem::toCoreItem))
        val activeEntry = TimelineEntry(
            item = active.toCoreItem(),
            startAtEpochMs = estimatedActiveStart,
            endAtEpochMs = activeEnd,
            isAnchor = false,
        )
        val afterEntries = ScheduleTimeline.build(activeEnd, after.map(ScheduleItem::toCoreItem))
        return beforeEntries + activeEntry + afterEntries
    }

    companion object {
        const val DEFAULT_DRAFT_MINUTES = 5
        const val DEFAULT_EXTEND_MINUTES = 5
        const val MILLIS_PER_MINUTE = 60_000L
        const val SILENCE_DB = -200.0
    }
}

private fun ScheduleItem.toCoreItem(): CoreScheduleItem = CoreScheduleItem(
    id = id,
    jobId = 0L,
    title = title,
    startAtEpochMs = fixedStartEpochMs,
    durationMinutes = totalMinutes,
)

/**
 * 本番万能コントローラー。
 *
 * 本番タイマー（[com.patoolbox.feature.showtimer]）は「今どれだけ経ったか／
 * あと何分か」だけを見る道具として残し、こちらは**あらかじめ組んだ進行表を
 * 順番に消化しながら、SE の自動再生とハウリング/スペクトラムのモニターまで
 * 1画面にまとめた**道具にしてある。本番中に画面を持ち替えさせない、という
 * 本番タイマーと同じ考え方を、機能を増やしても崩さない。
 *
 * SE パッドは [SoundCueRepository] を [com.patoolbox.feature.sfx] と共有している。
 * モニターは新しい feature モジュールに依存せず、[com.patoolbox.feature.showtimer] と同じく
 * core:audio / core:dsp を直接使う（feature 同士は依存しない、というこのアプリの原則）。
 */
@HiltViewModel
class ShowRunnerViewModel @Inject constructor(
    private val soundCueRepository: SoundCueRepository,
    private val player: SoundCuePlayer,
    private val captureEngine: AudioCaptureEngine,
    private val calibrationRepository: CalibrationRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val showModeController: ShowModeController,
    private val jobRepository: JobRepository,
    private val scheduleRepository: ScheduleRepository,
    proGate: ProGate,
) : ViewModel() {

    private val local = MutableStateFlow(ShowRunnerUiState())

    val uiState: StateFlow<ShowRunnerUiState> = combine(
        local,
        soundCueRepository.observeAll(),
        proGate.proStatus,
    ) { state, pads, proStatus ->
        state.copy(pads = pads, proStatus = proStatus)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = ShowRunnerUiState(),
    )

    private var ticker: CoroutineJob? = null
    private var runStartNanos = 0L
    private var accumulatedMillis = 0L
    private var nextId = 1L

    /** 開始した項目に紐付いた SE を、遅延ぶん待ってから鳴らすジョブ。項目切り替えで必ずキャンセルする */
    private var pendingCueJob: CoroutineJob? = null

    private val pipeline = SpectrumPipeline(sampleRate = AudioCaptureEngine.DEFAULT_SAMPLE_RATE)
    private val feedbackDetector = FeedbackDetector(
        sampleRate = AudioCaptureEngine.DEFAULT_SAMPLE_RATE,
        fftSize = pipeline.fftSize,
    )
    private var framesSinceFeedback = Int.MAX_VALUE

    @Volatile
    private var lastLevelDb = ShowRunnerUiState.SILENCE_DB

    @Volatile
    private var runningMaxDb = ShowRunnerUiState.SILENCE_DB

    init {
        player.onFinished = { cueId ->
            local.update { it.copy(playingIds = it.playingIds - cueId) }
        }
        // SE パッド画面と同じ設定を見る。ここだけ違う挙動にすると、
        // 本番モードで「他アプリの音は止めない」にしていてもこの画面だけ止まる事故になる
        viewModelScope.launch {
            preferencesRepository.preferences.collect { prefs ->
                player.interruptOtherApps = !prefs.showMode.allowOtherAppAudio
                local.update { it.copy(showMode = prefs.showMode) }
            }
        }
        local.update { it.copy(frequencies = pipeline.frequencies) }
        refreshNotificationPolicy()

        viewModelScope.launch {
            jobRepository.observeAll().collect { jobs ->
                local.update { it.copy(availableJobs = jobs) }
            }
        }
    }

    // --- 進行表 ---

    fun setDraftTitle(value: String) {
        local.update { it.copy(draftTitle = value) }
    }

    fun setDraftMinutes(minutes: Int) {
        local.update { it.copy(draftMinutes = minutes.coerceIn(0, 600)) }
    }

    /**
     * 案件管理の進行表をまとめて取り込む。
     * 既存の項目はそのまま残し、末尾に追加する。
     */
    fun importFromJob(jobId: Long) {
        viewModelScope.launch {
            val items = scheduleRepository.observeForJob(jobId).first()
            if (items.isEmpty()) return@launch
            val newItems = items.map { coreItem ->
                ScheduleItem(
                    id = nextId++,
                    title = coreItem.title,
                    plannedMinutes = coreItem.durationMinutes,
                    fixedStartEpochMs = coreItem.startAtEpochMs,
                )
            }
            local.update { it.copy(schedule = it.schedule + newItems) }
        }
    }

    fun addScheduleItem() {
        val title = local.value.draftTitle.trim()
        if (title.isEmpty()) return
        val item = ScheduleItem(id = nextId++, title = title, plannedMinutes = local.value.draftMinutes)
        local.update { it.copy(schedule = it.schedule + item, draftTitle = "") }
    }

    fun removeScheduleItem(id: Long) {
        local.update { state ->
            val wasActive = state.activeItemId == id
            state.copy(
                schedule = state.schedule.filterNot { it.id == id },
                activeItemId = if (wasActive) null else state.activeItemId,
                running = if (wasActive) false else state.running,
            )
        }
        if (local.value.activeItemId == null) {
            stopTicker()
            cancelPendingCue()
        }
    }

    /** 1つ前と入れ替える。先頭なら何もしない。 */
    fun moveUp(id: Long) = swap(id, -1)

    /** 1つ後ろと入れ替える。末尾なら何もしない。 */
    fun moveDown(id: Long) = swap(id, +1)

    private fun swap(id: Long, delta: Int) {
        local.update { state ->
            val list = state.schedule.toMutableList()
            val index = list.indexOfFirst { it.id == id }
            val target = index + delta
            if (index < 0 || target < 0 || target >= list.size) return@update state
            val item = list.removeAt(index)
            list.add(target, item)
            state.copy(schedule = list)
        }
    }

    /** 進行表全体の開始予定時刻。null を渡すと時刻表示自体をやめる。 */
    fun setAnchorTime(epochMs: Long?) {
        local.update { it.copy(anchorEpochMs = epochMs) }
    }

    // --- SE 連動・遅延 ---

    /** 「本番は19:00固定」のような、この項目だけ譲れない開始時刻。null で解除する。 */
    fun setItemFixedTime(itemId: Long, epochMs: Long?) {
        updateItem(itemId) { it.copy(fixedStartEpochMs = epochMs) }
    }

    fun setCueLink(itemId: Long, soundCueId: Long?) {
        updateItem(itemId) { it.copy(linkedSoundCueId = soundCueId, cueDelayMs = if (soundCueId == null) 0 else it.cueDelayMs) }
    }

    fun setCueDelayMs(itemId: Long, delayMs: Long) {
        updateItem(itemId) { it.copy(cueDelayMs = delayMs.coerceIn(0, MAX_CUE_DELAY_MS)) }
    }

    fun renameItem(itemId: Long, title: String) {
        if (title.isBlank()) return
        updateItem(itemId) { it.copy(title = title.trim()) }
    }

    fun setItemDuration(itemId: Long, minutes: Int) {
        updateItem(itemId) { it.copy(plannedMinutes = minutes.coerceIn(0, 600)) }
    }

    private fun updateItem(itemId: Long, transform: (ScheduleItem) -> ScheduleItem) {
        local.update { state ->
            state.copy(schedule = state.schedule.map { if (it.id == itemId) transform(it) else it })
        }
    }

    // --- 延長 ---

    fun setDraftExtendMinutes(minutes: Int) {
        local.update { it.copy(draftExtendMinutes = minutes.coerceIn(1, 180)) }
    }

    /** 下書きの延長分を、いまアクティブな項目にそのまま加算する。 */
    fun applyExtend() {
        val id = local.value.activeItemId ?: return
        val extend = local.value.draftExtendMinutes
        updateItem(id) { it.copy(extraMinutes = it.extraMinutes + extend) }
    }

    // --- カウントダウン ---

    /** この項目からカウントダウンを開始する。走っていた別の項目があれば止めて切り替える。 */
    fun startItem(id: Long) {
        stopTicker()
        cancelPendingCue()
        accumulatedMillis = 0
        runStartNanos = System.nanoTime()
        local.update { it.copy(activeItemId = id, running = true, elapsedMillis = 0) }
        startTicker()
        scheduleLinkedCue(id)
    }

    /** 次の項目へ進む。無ければ止めて終わる。 */
    fun startNext() {
        val next = local.value.nextItem ?: run { pause(); return }
        startItem(next.id)
    }

    fun togglePause() {
        if (local.value.running) pause() else resume()
    }

    private fun resume() {
        if (local.value.activeItemId == null) return
        runStartNanos = System.nanoTime()
        local.update { it.copy(running = true) }
        startTicker()
    }

    private fun pause() {
        accumulatedMillis += elapsedSinceStartMillis()
        stopTicker()
        local.update { it.copy(running = false, elapsedMillis = accumulatedMillis) }
    }

    fun resetActive() {
        val id = local.value.activeItemId ?: return
        startItem(id)
    }

    fun stopAll() {
        stopTicker()
        cancelPendingCue()
        local.update { it.copy(activeItemId = null, running = false, elapsedMillis = 0) }
    }

    // --- 本番モード ---

    fun toggleShowMode() {
        val current = local.value
        if (current.showModeActive) {
            showModeController.exit()
            local.update { it.copy(showModeActive = false) }
        } else {
            showModeController.enter(current.showMode)
            local.update { it.copy(showModeActive = true) }
        }
    }

    fun setShowMode(settings: ShowModeSettings) {
        viewModelScope.launch { preferencesRepository.setShowMode(settings) }
    }

    fun refreshNotificationPolicy() {
        local.update { it.copy(hasNotificationPolicy = showModeController.hasNotificationPolicyAccess()) }
    }

    fun notificationPolicySettingsIntent() = showModeController.notificationPolicySettingsIntent()

    private fun startTicker() {
        ticker = viewModelScope.launch {
            while (isActive) {
                val elapsed = accumulatedMillis + elapsedSinceStartMillis()
                local.update { it.copy(elapsedMillis = elapsed) }
                delay(TICK_MS)
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private fun elapsedSinceStartMillis(): Long =
        (System.nanoTime() - runStartNanos) / NANOS_PER_MILLI

    /** 項目に紐付いた SE を、設定された遅延ぶん待ってから鳴らす。 */
    private fun scheduleLinkedCue(itemId: Long) {
        val item = local.value.schedule.firstOrNull { it.id == itemId } ?: return
        val cueId = item.linkedSoundCueId ?: return
        pendingCueJob = viewModelScope.launch {
            if (item.cueDelayMs > 0) delay(item.cueDelayMs)
            val cue = local.value.pads.firstOrNull { it.id == cueId } ?: return@launch
            startCue(cue)
        }
    }

    private fun cancelPendingCue() {
        pendingCueJob?.cancel()
        pendingCueJob = null
    }

    // --- SE パッド・同期音源 ---

    fun import(uri: Uri) {
        if (!uiState.value.canAddMorePads) {
            local.update {
                it.copy(error = "無料版で持てるパッドは${SoundCue.FREE_LIMIT}枚までです")
            }
            return
        }
        local.update { it.copy(importing = true, error = null) }
        viewModelScope.launch {
            val cue = soundCueRepository.import(uri)
            local.update {
                it.copy(
                    importing = false,
                    error = if (cue == null) "取り込めませんでした" else null,
                )
            }
        }
    }

    /** 同期音源として使う＝鳴らし続ける想定なのでループを付けて登録し直す */
    fun markAsSync(cue: SoundCue) {
        viewModelScope.launch { soundCueRepository.update(cue.copy(loop = true)) }
    }

    fun markAsOneShot(cue: SoundCue) {
        viewModelScope.launch { soundCueRepository.update(cue.copy(loop = false)) }
    }

    fun togglePad(cue: SoundCue) {
        if (player.isPlaying(cue.id)) {
            player.stop(cue.id)
            local.update { it.copy(playingIds = it.playingIds - cue.id) }
            return
        }
        startCue(cue)
    }

    /** 頭から鳴らす。手動再生とスケジュール連動の両方から呼ぶ */
    private fun startCue(cue: SoundCue) {
        val started = player.play(
            cueId = cue.id,
            file = soundCueRepository.fileOf(cue),
            gain = cue.gain,
            loop = cue.loop,
        )
        if (started) {
            local.update { it.copy(playingIds = it.playingIds + cue.id) }
        } else {
            local.update { it.copy(error = "再生できませんでした（${cue.title}）") }
        }
    }

    fun stopAllPads() {
        player.stopAll()
        local.update { it.copy(playingIds = emptySet()) }
    }

    // --- モニター（ハウリング測定・スペクトラムアナライザ） ---

    fun hasMicPermission(): Boolean = captureEngine.hasPermission()

    fun startMonitor() {
        if (local.value.monitoring) return
        if (!captureEngine.hasPermission()) {
            local.update { it.copy(monitorError = "マイクの許可がありません") }
            return
        }

        pipeline.reset()
        feedbackDetector.reset()
        framesSinceFeedback = Int.MAX_VALUE
        lastLevelDb = ShowRunnerUiState.SILENCE_DB
        runningMaxDb = ShowRunnerUiState.SILENCE_DB
        local.update { it.copy(feedback = null) }

        runCatching {
            captureEngine.start { buffer, length ->
                val level = amplitudeToDb(rms(buffer, length))
                lastLevelDb = level
                if (level > runningMaxDb) runningMaxDb = level
                pipeline.accumulator.add(buffer, length) { frame -> onMonitorFrame(frame) }
            }
        }.onSuccess { session ->
            local.update { it.copy(monitoring = true, monitorError = null) }
            observeCalibration(session.calibrationKey, session.inputType)
        }.onFailure { throwable ->
            local.update { it.copy(monitoring = false, monitorError = throwable.message) }
        }
    }

    fun stopMonitor() {
        captureEngine.stop()
        local.update { it.copy(monitoring = false, feedback = null) }
    }

    fun toggleMonitor() {
        if (local.value.monitoring) stopMonitor() else startMonitor()
    }

    fun resetMaxLevel() {
        runningMaxDb = lastLevelDb
    }

    fun clearLastFeedback() {
        local.update { it.copy(lastFeedback = null) }
    }

    private fun onMonitorFrame(frame: FloatArray) {
        val offset = local.value.calibration.offsetDb
        val snapshot = pipeline.analyze(
            frame = frame,
            smoothing = OctaveSmoothing.SIXTH,
            offsetDb = offset,
            peakHold = true,
        )
        val alert = detectFeedback(frame)

        local.update {
            it.copy(
                monitorFrame = it.monitorFrame + 1,
                levelDb = lastLevelDb + offset,
                maxLevelDb = runningMaxDb + offset,
                columnsDb = snapshot.columnsDb,
                peakHoldDb = snapshot.peakHoldDb,
                frequencies = pipeline.frequencies,
                feedback = alert,
                lastFeedback = alert ?: it.lastFeedback,
            )
        }
    }

    /** 一番突出している1本だけ出す。3本並べても打つ手は変わらないし、表示が増えるほど読みにくい */
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
            return local.value.feedback
        }
        return null
    }

    private fun observeCalibration(deviceKey: String, inputType: AudioInputType) {
        viewModelScope.launch {
            calibrationRepository.observe(deviceKey, inputType).collect { profile ->
                local.update { it.copy(calibration = profile) }
            }
        }
    }

    override fun onCleared() {
        stopTicker()
        cancelPendingCue()
        player.releaseAll()
        captureEngine.stop()
        if (local.value.showModeActive) showModeController.exit()
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val TICK_MS = 100L
        const val NANOS_PER_MILLI = 1_000_000L
        const val MAX_CUE_DELAY_MS = 60_000L

        /** 検出が途切れてから表示を消すまでのフレーム数。1フレーム約85msなので12で約1秒 */
        const val FEEDBACK_HOLD_FRAMES = 12
    }
}
