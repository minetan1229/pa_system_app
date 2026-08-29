package com.patoolbox.feature.sfx

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SfxUiState(
    val cues: List<SoundCue> = emptyList(),
    /** いま鳴っているパッド */
    val playingIds: Set<Long> = emptySet(),
    val importing: Boolean = false,
    /** 編集中のパッド。null なら編集シートを出さない */
    val editing: SoundCue? = null,
    val proStatus: ProStatus = ProStatus.Free,
    val error: String? = null,
) {
    /** 無料版でこれ以上増やせるか。Pro なら無制限 */
    val canAddMore: Boolean
        get() = proStatus.isPro || cues.size < SoundCue.FREE_LIMIT

    val isAnyPlaying: Boolean get() = playingIds.isNotEmpty()
}

/**
 * SE パッド。
 *
 * 取り込みはアプリ内へのコピーで完結させ（[SoundCueRepository]）、
 * 再生は端末のデコーダに任せる（[SoundCuePlayer]）。
 * この画面自体はネットワークに一切触れないので、圏外の会場でもそのまま動く。
 */
@HiltViewModel
class SfxViewModel @Inject constructor(
    private val repository: SoundCueRepository,
    private val player: SoundCuePlayer,
    preferencesRepository: UserPreferencesRepository,
    proGate: ProGate,
) : ViewModel() {

    private val local = MutableStateFlow(SfxUiState())

    val uiState: StateFlow<SfxUiState> = combine(
        local,
        repository.observeAll(),
        proGate.proStatus,
    ) { state, cues, proStatus ->
        state.copy(cues = cues, proStatus = proStatus)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = SfxUiState(),
    )

    init {
        // 鳴り終わったパッドの点灯を消す。ループ以外は勝手に終わる
        player.onFinished = { cueId ->
            local.update { it.copy(playingIds = it.playingIds - cueId) }
        }

        // 本番モードの「他アプリの音を止めない」をここで効かせる。
        // 設定した場所（本番タイマー）と効く場所（SE の再生）が違うので、
        // 設定だけあって何も起きない状態にならないよう必ず繋いでおく
        viewModelScope.launch {
            preferencesRepository.preferences.collect { prefs ->
                player.interruptOtherApps = !prefs.showMode.allowOtherAppAudio
            }
        }
    }

    fun import(uri: Uri) {
        if (!uiState.value.canAddMore) {
            local.update { it.copy(error = "無料版で持てるパッドは${SoundCue.FREE_LIMIT}枚までです") }
            return
        }

        local.update { it.copy(importing = true, error = null) }
        viewModelScope.launch {
            val imported = repository.import(uri)
            local.update {
                it.copy(
                    importing = false,
                    error = if (imported == null) "この音声は取り込めませんでした" else null,
                )
            }
        }
    }

    /** パッドを叩く。鳴っていれば止める（トグル） */
    fun trigger(cue: SoundCue) {
        if (cue.id in uiState.value.playingIds) {
            stop(cue)
            return
        }

        val started = player.play(
            cueId = cue.id,
            file = repository.fileOf(cue),
            gain = cue.gain,
            loop = cue.loop,
        )
        if (started) {
            local.update { it.copy(playingIds = it.playingIds + cue.id, error = null) }
        } else {
            local.update { it.copy(error = "「${cue.title}」を再生できませんでした（ファイルが見つからないか、対応していない形式です）") }
        }
    }

    fun stop(cue: SoundCue) {
        player.stop(cue.id)
        // フェードが終わるまで待たずに消灯する。押した手応えが遅れる方が現場では困る
        local.update { it.copy(playingIds = it.playingIds - cue.id) }
    }

    fun stopAll() {
        player.stopAll()
        local.update { it.copy(playingIds = emptySet()) }
    }

    fun edit(cue: SoundCue) {
        local.update { it.copy(editing = cue) }
    }

    fun dismissEdit() {
        local.update { it.copy(editing = null) }
    }

    fun save(cue: SoundCue) {
        viewModelScope.launch { repository.update(cue) }
        local.update { it.copy(editing = null) }
    }

    fun delete(cue: SoundCue) {
        player.stop(cue.id)
        viewModelScope.launch { repository.delete(cue) }
        local.update { it.copy(editing = null, playingIds = it.playingIds - cue.id) }
    }

    /** 並べ替え。[from] のパッドを [to] の位置へ動かす */
    fun move(from: Int, to: Int) {
        val cues = uiState.value.cues
        if (from !in cues.indices || to !in cues.indices || from == to) return
        val reordered = cues.toMutableList().apply { add(to, removeAt(from)) }
        viewModelScope.launch { repository.reorder(reordered) }
    }

    fun dismissError() {
        local.update { it.copy(error = null) }
    }

    override fun onCleared() {
        // onFinished のコールバックだけ外す。
        // SoundCuePlayer は @Singleton で ShowRunnerViewModel とも共有している。
        // ここで releaseAll() を呼ぶと「SE パッドは別ページでも流し続ける」要件を壊すので、
        // 音の管理は ShowRunnerViewModel.onCleared() に委ねる。
        player.onFinished = null
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
