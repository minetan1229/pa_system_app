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
        ToolId.SPL_METER -> Color(0xFF00B3C4)
        ToolId.SPL_LOGGER -> Color(0xFF0E8C99)

        // --- 計測: 周波数系（青〜紫） ---
        ToolId.RTA -> Color(0xFF2FA36B)
        ToolId.FFT -> Color(0xFF5B8DEF)
        ToolId.SPECTROGRAM -> Color(0xFF8E6BE8)
        ToolId.FEEDBACK_FINDER -> Color(0xFFE0483C)

        // --- 計測: 音を出して測る（橙〜赤） ---
        ToolId.SIGNAL_GENERATOR -> Color(0xFFE8A33D)
        ToolId.DELAY_FINDER -> Color(0xFFE07B39)
        ToolId.POLARITY_CHECK -> Color(0xFFD65C8A)
        ToolId.ROOM_MEASURE -> Color(0xFFB05CD6)

        // --- 計測: 音楽の道具（黄緑） ---
        ToolId.TUNER -> Color(0xFF8FBF3F)
        ToolId.METRONOME -> Color(0xFF6FA8A0)
        ToolId.RECORDER -> Color(0xFFCC3355)

        // --- リファレンス: 劣化だけ色を分ける（症状から引く画面で、他のリファレンスと役割が違う） ---
        ToolId.SIGNAL_QUALITY -> Color(0xFFB0453C)

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
        this == ToolId.RECORDER
