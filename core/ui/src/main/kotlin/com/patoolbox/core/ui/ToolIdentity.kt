package com.patoolbox.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.patoolbox.core.designsystem.theme.LocalPaThemeMode
import com.patoolbox.core.model.ThemeMode
import com.patoolbox.core.model.ToolId

/**
 * ツール1つ1つの識別色。
 *
 * カテゴリ色（[accentColor]）だけだと、計測ツール13個が全部同じ色になり、
 * 「同じような図と同じようなボタンが並んだ画面」に見えてしまう。
 * どの画面を開いているかが色だけで分かるよう、計測は1ツール1色にしている。
 *
 * 色相は役割で並べてある。
 * 　レベルを測る＝青緑、周波数を見る＝青〜紫、音を出して測る＝橙〜赤、
 * 　音楽の道具＝黄緑。
 * 危険（大音量が出る）ものを暖色に寄せているのは、
 * 一覧の中で先に目に入るようにするため。
 *
 * 彩度は落としてある。地が生成りの紙（#F0EEE6）なので、
 * 純度の高い色を上に置くと、その画面だけ別のアプリのように浮く。
 * どれも [com.patoolbox.core.designsystem.component.contrastingInk] で
 * 白黒どちらかの文字が 4.5:1 以上で乗ることを確認済み。
 */
@Composable
fun ToolId.identityColor(): Color {
    // 暗所モードでは赤以外の光を出さない。色相で区別せず明度だけで分ける
    if (LocalPaThemeMode.current == ThemeMode.NIGHT_RED) {
        return when (ordinal % 4) {
            0 -> Color(0xFFFF6B4A)
            1 -> Color(0xFFD9502F)
            2 -> Color(0xFFB33D22)
            else -> Color(0xFF8C2F1A)
        }
    }
    return when (this) {
        // --- 計測: レベル系（青緑） ---
        ToolId.SPL_METER -> Color(0xFF4E9AA6)
        ToolId.SPL_LOGGER -> Color(0xFF2F6A75)

        // --- 計測: 周波数系（青〜紫） ---
        ToolId.RTA -> Color(0xFF5A9B72)
        ToolId.FFT -> Color(0xFF6F8FCB)
        ToolId.SPECTROGRAM -> Color(0xFF9084C9)
        ToolId.FEEDBACK_FINDER -> Color(0xFFD2604F)

        // --- 計測: 音を出して測る（橙〜赤） ---
        ToolId.SIGNAL_GENERATOR -> Color(0xFFD9A362)
        ToolId.DELAY_FINDER -> Color(0xFFD97757)
        ToolId.POLARITY_CHECK -> Color(0xFFC4738F)
        ToolId.ROOM_MEASURE -> Color(0xFFA97BC0)

        // --- 計測: 音楽の道具（黄緑） ---
        ToolId.TUNER -> Color(0xFF9BAE63)
        ToolId.METRONOME -> Color(0xFF7FA79E)
        ToolId.RECORDER -> Color(0xFFC15D6E)

        // --- リファレンス: 劣化だけ色を分ける（症状から引く画面で、他のリファレンスと役割が違う） ---
        ToolId.SIGNAL_QUALITY -> Color(0xFFA34E41)

        // --- それ以外はカテゴリ色に従う ---
        else -> category.accentColor()
    }
}

/**
 * この画面はスピーカーから音を出すか。
 *
 * 出す画面には必ず表示を付ける。PA に繋いだ状態で不意に大音量が出るのが、
 * この app でいちばん実害の大きい失敗なので、開いた瞬間に分かるようにしておく。
 */
val ToolId.emitsSound: Boolean
    get() = this == ToolId.SIGNAL_GENERATOR ||
        this == ToolId.DELAY_FINDER ||
        this == ToolId.POLARITY_CHECK ||
        this == ToolId.ROOM_MEASURE ||
        this == ToolId.METRONOME ||
        this == ToolId.SFX_PADS

/** マイクで拾って測る画面か。校正の効き方が変わるので、読み手に区別させる */
val ToolId.usesMicrophone: Boolean
    get() = this == ToolId.SPL_METER ||
        this == ToolId.SPL_LOGGER ||
        this == ToolId.RTA ||
        this == ToolId.FFT ||
        this == ToolId.SPECTROGRAM ||
        this == ToolId.FEEDBACK_FINDER ||
        this == ToolId.DELAY_FINDER ||
        this == ToolId.POLARITY_CHECK ||
        this == ToolId.ROOM_MEASURE ||
        this == ToolId.TUNER ||
        this == ToolId.RECORDER ||
        this == ToolId.SHOW_RUNNER ||
        this == ToolId.SHOW_TIMER
