package com.patoolbox.feature.showrunner

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patoolbox.core.audio.SoundCuePlayer
import com.patoolbox.core.billing.ProGate
import com.patoolbox.core.data.SoundCueRepository
import com.patoolbox.core.data.UserPreferencesRepository
import com.patoolbox.core.model.ProStatus
import com.patoolbox.core.model.SoundCue
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 1本ぶんの予定。「何を」「何分で」の2つだけ持つ。
 *
 * 開始・終了の時刻を持たせないのは、現場では押し巻きで前の項目が伸び縮みするため。
 * 常に「いま開いている項目から何分」で数える方が、後ろの項目がずれても壊れない。
 */
data class ScheduleItem(
    val id: Long,
    val title: String,
    val plannedMinutes: Int,
)

data class ShowRunnerUiState(
    val schedule: List<ScheduleItem> = emptyList(),
    /** いまカウントダウン中の項目。null なら何も走っていない */
    val activeItemId: Long? = null,
    val running: Boolean = false,
    val elapsedMillis: Long = 0,

    /** 追加フォームの下書き */
    val draftTitle: String = "",
    val draftMinutes: Int = DEFAULT_DRAFT_MINUTES,

    /** SE パッド・同期音源。[com.patoolbox.feature.sfx.SfxScreen] と同じ保存先を見ている */
    val pads: List<SoundCue> = emptyList(),
    val playingIds: Set<Long> = emptySet(),
    val importing: Boolean = false,
    val proStatus: ProStatus = ProStatus.Free,
    val error: String? = null,
) {
    /** 無料版でこれ以上パッドを増やせるか。SE パッド画面と同じ上限を共有する */
    val canAddMorePads: Boolean
        get() = proStatus.isPro || pads.size < SoundCue.FREE_LIMIT

    val activeItem: ScheduleItem? get() = schedule.firstOrNull { it.id == activeItemId }

    /** 残り時間（ミリ秒）。マイナスは押し。 */
    val remainingMillis: Long
        get() = (activeItem?.plannedMinutes ?: 0) * MILLIS_PER_MINUTE - elapsedMillis

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

    companion object {
        const val DEFAULT_DRAFT_MINUTES = 5
        const val MILLIS_PER_MINUTE = 60_000L
    }
}

/**
 * 本番進行コントローラー。
 *
 * 本番タイマー（[com.patoolbox.feature.showtimer]）は「今どれだけ経ったか／
 * あと何分か」だけを見る道具として残し、こちらは**あらかじめ組んだ進行表を
 * 順番に消化していく**ための道具にしてある。1本のカウントダウンだけでは
 * 「MCコメント→1曲目→転換→2曲目」のような進行を追えないため。
 *
 * SE パッドは [SoundCueRepository] を [com.patoolbox.feature.sfx] と共有している。
 * 別の保存先を持たせなかったのは、SE パッド画面で作った音を
 * この画面からも同じボタンで鳴らせないと、結局2回取り込む羽目になるため。
 * 「同期音源」を専用の入力にしなかったのも同じ理由——ループを付けたパッドを
 * そのまま流し続ければ、バッキング音源として同じ仕組みで鳴らせる。
 */
@HiltViewModel
class ShowRunnerViewModel @Inject constructor(
    private val soundCueRepository: SoundCueRepository,
    private val player: SoundCuePlayer,
    preferencesRepository: UserPreferencesRepository,
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

    private var ticker: Job? = null
    private var runStartNanos = 0L
    private var accumulatedMillis = 0L
    private var nextId = 1L

    init {
        player.onFinished = { cueId ->
            local.update { it.copy(playingIds = it.playingIds - cueId) }
        }
        // SE パッド画面と同じ設定を見る。ここだけ違う挙動にすると、
        // 本番モードで「他アプリの音は止めない」にしていてもこの画面だけ止まる事故になる
        viewModelScope.launch {
            preferencesRepository.preferences.collect { prefs ->
                player.interruptOtherApps = !prefs.showMode.allowOtherAppAudio
            }
        }
    }

    // --- 進行表 ---

    fun setDraftTitle(value: String) {
        local.update { it.copy(draftTitle = value) }
    }

    fun setDraftMinutes(minutes: Int) {
        local.update { it.copy(draftMinutes = minutes.coerceIn(1, 600)) }
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
        if (local.value.activeItemId == null) stopTicker()
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

    // --- カウントダウン ---

    /** この項目からカウントダウンを開始する。走っていた別の項目があれば止めて切り替える。 */
    fun startItem(id: Long) {
        stopTicker()
        accumulatedMillis = 0
        runStartNanos = System.nanoTime()
        local.update { it.copy(activeItemId = id, running = true, elapsedMillis = 0) }
        startTicker()
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
        local.update { it.copy(activeItemId = null, running = false, elapsedMillis = 0) }
    }

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

    override fun onCleared() {
        stopTicker()
        player.releaseAll()
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val TICK_MS = 100L
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
