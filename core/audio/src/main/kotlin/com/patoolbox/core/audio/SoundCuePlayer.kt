package com.patoolbox.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SE パッドの再生。
 *
 * [AudioPlaybackEngine]（測定用の信号発生）とは別物として置いている。
 * あちらは自前で PCM を生成して AudioTrack に流す作りで、mp3 や m4a は扱えない。
 * SE は利用者が持ち込んだ任意の形式なので、端末のデコーダに任せる [MediaPlayer] を使う。
 *
 * **同時発音できることが要件**。拍手とジングルが重なる、SE の尻を切らずに次を出す、
 * といった使い方が本番では普通に起きる。パッドごとに MediaPlayer を持つ。
 *
 * 停止は必ずフェードアウトさせる。PA に繋いだ状態で切ると「ボツッ」が
 * 大音量で出るので、[AudioPlaybackEngine] と同じ扱いにしている。
 */
@Singleton
class SoundCuePlayer @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    /** 鳴っている1枚。[gain] を持つのは、フェードをその音量から始めるため */
    private class Playing(val player: MediaPlayer, val gain: Float)

    /** 再生中のパッド。キーは SoundCue.id */
    private val players = ConcurrentHashMap<Long, Playing>()

    private val handler = Handler(Looper.getMainLooper())

    private val audioManager: AudioManager?
        get() = context.getSystemService(AudioManager::class.java)

    private var focusRequest: AudioFocusRequest? = null

    /**
     * SE を鳴らすときに他アプリの音を止めるか。
     *
     * 既定は false（止めない）。BGM を別アプリで出しながら SE を重ねる使い方が
     * あるので、勝手に相手を黙らせない方を既定にしている。
     * 本番モードの設定から切り替える。
     */
    @Volatile
    var interruptOtherApps: Boolean = false

    /** 再生が終わった / 止まったときに呼ばれる。UI の点灯を消すのに使う */
    @Volatile
    var onFinished: ((Long) -> Unit)? = null

    val playingIds: Set<Long> get() = players.keys.toSet()

    fun isPlaying(cueId: Long): Boolean = players.containsKey(cueId)

    /**
     * 頭から鳴らす。すでに鳴っていたら**鳴らし直す**。
     *
     * 連打で重ねる作りにしていないのは、同じ SE が二重に鳴るのは事故だから。
     * 意図して重ねたい場合はパッドを2枚作る。
     *
     * @param gain 0.0〜1.0。素材ごとの録音レベル差を吸収する
     * @return 開けたかどうか。壊れたファイルや対応外のコーデックでは false
     */
    fun play(cueId: Long, file: File, gain: Float = 1f, loop: Boolean = false): Boolean {
        stopImmediately(cueId)
        if (!file.exists()) return false

        val level = gain.coerceIn(0f, 1f)
        val player = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                setDataSource(file.absolutePath)
                isLooping = loop
                setVolume(level, level)
                prepare()
            }
        }.getOrElse { return false }

        player.setOnCompletionListener {
            // ループ中は呼ばれない。ここに来るのは最後まで鳴りきったときだけ
            release(cueId)
        }
        player.setOnErrorListener { _, _, _ ->
            release(cueId)
            true
        }

        requestFocusIfNeeded()
        players[cueId] = Playing(player, level)
        player.start()
        return true
    }

    /**
     * 音声フォーカスを取る。取れなくても鳴らす。
     *
     * 「他アプリを止める」という設定が効かないより、SE が鳴らない方が現場では困る。
     * フォーカスを取れないのは他アプリが手放さない場合で、そのときも
     * 音自体は重なって出る（Android はフォーカス無しの再生を禁止していない）。
     */
    private fun requestFocusIfNeeded() {
        if (!interruptOtherApps || focusRequest != null) return
        val manager = audioManager ?: return

        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            // SE の途中で他アプリに主導権を渡さない。本番中に音が飛ぶ方が事故
            .setWillPauseWhenDucked(false)
            .build()

        runCatching { manager.requestAudioFocus(request) }
        focusRequest = request
    }

    /** 最後の1枚が止まったらフォーカスを返す。握ったままだと他アプリが復帰できない */
    private fun abandonFocusIfIdle() {
        if (players.isNotEmpty()) return
        val request = focusRequest ?: return
        focusRequest = null
        runCatching { audioManager?.abandonAudioFocusRequest(request) }
    }

    /** フェードアウトして止める。 */
    fun stop(cueId: Long) {
        val playing = players[cueId] ?: return
        fadeOutAndRelease(cueId, playing)
    }

    fun stopAll() {
        for (cueId in players.keys.toList()) stop(cueId)
    }

    /** 画面を離れるときなど、音を残してはいけない場面で使う。フェードしない */
    fun releaseAll() {
        for (cueId in players.keys.toList()) stopImmediately(cueId)
    }

    private fun stopImmediately(cueId: Long) {
        val playing = players.remove(cueId) ?: return
        runCatching { playing.player.stop() }
        playing.player.release()
        abandonFocusIfIdle()
    }

    /**
     * 音量を段階的に下げてから解放する。
     *
     * MediaPlayer にフェードの機能は無いので、[FADE_STEPS] 回に分けて
     * setVolume を呼ぶ。合計 [FADE_MS] ミリ秒。メインスレッドの Handler で
     * 回しているが、1回あたりの仕事は setVolume 1回だけなので描画は詰まらない。
     *
     * 予約を token で取り消す `postDelayed(Runnable, Object, Long)` は API 28 からで、
     * minSdk は 26。代わりに各ステップで「players の中身がまだこの MediaPlayer か」を
     * 確かめることで、フェード中に鳴らし直された新しい方を巻き込まないようにしている。
     */
    private fun fadeOutAndRelease(cueId: Long, playing: Playing) {
        val stepMs = FADE_MS / FADE_STEPS

        for (step in 1..FADE_STEPS) {
            handler.postDelayed(
                {
                    if (players[cueId] !== playing) return@postDelayed
                    if (step == FADE_STEPS) {
                        players.remove(cueId)
                        runCatching { playing.player.stop() }
                        playing.player.release()
                        abandonFocusIfIdle()
                        onFinished?.invoke(cueId)
                    } else {
                        val level = playing.gain * (1f - step.toFloat() / FADE_STEPS)
                        runCatching { playing.player.setVolume(level, level) }
                    }
                },
                stepMs * step,
            )
        }
    }

    private fun release(cueId: Long) {
        val playing = players.remove(cueId) ?: return
        runCatching { playing.player.stop() }
        playing.player.release()
        abandonFocusIfIdle()
        onFinished?.invoke(cueId)
    }

    private companion object {
        /** 立ち下がりの時間。これ以上短いとクリックが残り、長いと切ったつもりで鳴り続ける */
        const val FADE_MS = 180L
        const val FADE_STEPS = 6
    }
}
