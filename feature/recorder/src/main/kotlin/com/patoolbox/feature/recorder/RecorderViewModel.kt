package com.patoolbox.feature.recorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patoolbox.core.audio.AudioCaptureEngine
import com.patoolbox.core.audio.AudioPlaybackEngine
import com.patoolbox.core.audio.WavFileSource
import com.patoolbox.core.billing.ProGate
import com.patoolbox.core.data.RecordingRepository
import com.patoolbox.core.data.di.IoDispatcher
import com.patoolbox.core.dsp.amplitudeToDb
import com.patoolbox.core.dsp.peakAmplitude
import com.patoolbox.core.dsp.rms
import com.patoolbox.core.export.WavFileWriter
import com.patoolbox.core.model.ProStatus
import com.patoolbox.core.model.Recording
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import javax.inject.Inject

data class RecorderUiState(
    val isRecording: Boolean = false,
    val elapsedSeconds: Double = 0.0,
    /** 直近ブロックのレベル。録音中のメーター表示 */
    val levelDb: Double = SILENCE_DB,
    val peakDb: Double = SILENCE_DB,
    val clipped: Boolean = false,
    val recordings: List<Recording> = emptyList(),
    val playingId: Long? = null,
    val proStatus: ProStatus = ProStatus.Free,
    val error: String? = null,
) {
    companion object {
        const val SILENCE_DB = -100.0
    }
}

/**
 * 現場の録音。
 *
 * 書き込みは取り込みスレッドから直接行う。[AudioCaptureEngine] が
 * 8ブロック（約170ms）ぶんの余裕を持っているので、内部ストレージへの
 * 数KBの書き込みが多少詰まっても取りこぼしにはならない。
 * 別スレッドとキューを挟む方が理屈は綺麗だが、受け渡しの分だけ
 * 落ちどころが増えるので、余裕で足りているうちは単純な方を採る。
 */
@HiltViewModel
class RecorderViewModel @Inject constructor(
    private val captureEngine: AudioCaptureEngine,
    private val playbackEngine: AudioPlaybackEngine,
    private val repository: RecordingRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    proGate: ProGate,
) : ViewModel() {

    private val local = MutableStateFlow(RecorderUiState())

    val uiState: StateFlow<RecorderUiState> = combine(
        local,
        repository.observeAll(),
        proGate.proStatus,
    ) { state, recordings, proStatus ->
        state.copy(recordings = recordings, proStatus = proStatus)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = RecorderUiState(),
    )

    private var writer: WavFileWriter? = null
    private var startedAtEpochMs = 0L
    private var playbackSource: WavFileSource? = null
    private var blocksSincePublish = 0

    init {
        // 前回の録音が中断していると、行の無いファイルが残る
        viewModelScope.launch { withContext(ioDispatcher) { repository.pruneOrphans() } }
    }

    fun toggleRecording() {
        if (local.value.isRecording) stopRecording() else startRecording()
    }

    fun startRecording() {
        val state = local.value
        if (state.isRecording) return
        if (!state.proStatus.isPro) return
        if (!captureEngine.hasPermission()) {
            local.update { it.copy(error = "マイクの許可がありません") }
            return
        }
        stopPlayback()

        runCatching {
            startedAtEpochMs = System.currentTimeMillis()
            blocksSincePublish = 0
            val file = repository.newFile(startedAtEpochMs)
            val newWriter = WavFileWriter(file, AudioCaptureEngine.DEFAULT_SAMPLE_RATE)
            writer = newWriter

            captureEngine.start { buffer, length -> onBlock(newWriter, buffer, length) }
        }.onSuccess {
            local.update {
                it.copy(isRecording = true, elapsedSeconds = 0.0, clipped = false, error = null)
            }
        }.onFailure { throwable ->
            closeWriter()
            local.update { it.copy(isRecording = false, error = throwable.message) }
        }
    }

    /**
     * 録音を止めて保存する。
     *
     * 取り込みを先に止めてから閉じる。逆にすると、閉じたファイルに
     * 書き込もうとして例外がオーディオスレッドで飛ぶ。
     */
    fun stopRecording() {
        if (!local.value.isRecording) return
        captureEngine.stop()

        val finished = writer
        closeWriter()
        local.update { it.copy(isRecording = false) }
        if (finished == null || finished.sampleCount == 0) return

        val recording = Recording(
            title = defaultTitle(startedAtEpochMs),
            fileName = repository.newFile(startedAtEpochMs).name,
            startedAtEpochMs = startedAtEpochMs,
            durationSeconds = finished.durationSeconds,
            sampleRate = AudioCaptureEngine.DEFAULT_SAMPLE_RATE,
            sizeBytes = repository.newFile(startedAtEpochMs).length(),
            peakAmplitude = finished.peakAmplitude,
        )
        viewModelScope.launch { repository.save(recording) }
    }

    fun play(recording: Recording) {
        stopPlayback()
        val file = repository.fileOf(recording)
        if (!file.exists()) {
            local.update { it.copy(error = "音声ファイルが見つかりません") }
            return
        }

        runCatching {
            val source = WavFileSource(file)
            playbackSource = source
            playbackEngine.start(source)
            source
        }.onSuccess { source ->
            local.update { it.copy(playingId = recording.id, error = null) }
            // 終端で自動的に止める。放置すると無音を流し続ける
            viewModelScope.launch {
                val durationMs = (source.durationSeconds * 1000).toLong()
                kotlinx.coroutines.delay(durationMs + PLAYBACK_TAIL_MS)
                if (local.value.playingId == recording.id) stopPlayback()
            }
        }.onFailure { throwable ->
            local.update { it.copy(error = throwable.message) }
        }
    }

    fun stopPlayback() {
        if (playbackEngine.isPlaying) playbackEngine.stop()
        playbackSource?.close()
        playbackSource = null
        local.update { it.copy(playingId = null) }
    }

    fun rename(recording: Recording, title: String) {
        viewModelScope.launch {
            repository.updateDetails(recording, title.ifBlank { recording.title }, recording.note)
        }
    }

    fun delete(recording: Recording) {
        if (local.value.playingId == recording.id) stopPlayback()
        viewModelScope.launch { withContext(ioDispatcher) { repository.delete(recording) } }
    }

    fun suggestedFileName(recording: Recording): String = "${recording.title}.wav"

    fun exportTo(recording: Recording, output: OutputStream) {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                val file = repository.fileOf(recording)
                output.use { stream -> file.inputStream().use { it.copyTo(stream) } }
            }
        }
    }

    override fun onCleared() {
        captureEngine.stop()
        closeWriter()
        stopPlayback()
    }

    private fun onBlock(writer: WavFileWriter, buffer: FloatArray, length: Int) {
        writer.write(buffer, length)

        val peak = peakAmplitude(buffer, length)
        val clippedNow = peak >= CLIP_THRESHOLD
        // ブロックは 21ms ごとに来る。そのたびに画面を更新すると毎秒47回の
        // 再構成になり、録音一覧まで作り直される。表示は 100ms ごとで足りる。
        // ただしクリップだけは1ブロックでも見逃さずに上げる
        blocksSincePublish++
        if (blocksSincePublish < BLOCKS_PER_PUBLISH && !clippedNow) return
        blocksSincePublish = 0

        val level = amplitudeToDb(rms(buffer, length))
        local.update {
            it.copy(
                elapsedSeconds = writer.durationSeconds,
                levelDb = level,
                peakDb = amplitudeToDb(writer.peakAmplitude.toDouble()),
                clipped = it.clipped || clippedNow,
            )
        }
    }

    /** 閉じ忘れるとヘッダの長さが0のまま残り、再生できないファイルになる。 */
    private fun closeWriter() {
        runCatching { writer?.close() }
        writer = null
    }

    private fun defaultTitle(epochMs: Long): String {
        val formatter = java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.JAPAN)
        return "録音 ${formatter.format(java.util.Date(epochMs))}"
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val CLIP_THRESHOLD = 0.99f

        /** フェードアウトのぶん少し待ってから止める */
        const val PLAYBACK_TAIL_MS = 200L

        /** 1024サンプル/48kHz = 約21ms。5ブロックで約100msごとの更新になる */
        const val BLOCKS_PER_PUBLISH = 5
    }
}
