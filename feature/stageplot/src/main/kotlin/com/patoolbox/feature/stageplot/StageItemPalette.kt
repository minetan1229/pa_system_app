package com.patoolbox.feature.stageplot

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.patoolbox.core.designsystem.component.contrastingInk
import com.patoolbox.core.designsystem.theme.LocalPaThemeMode
import com.patoolbox.core.model.StageItemColor
import com.patoolbox.core.model.ThemeMode

/**
 * 記号1個ずつの色（[com.patoolbox.core.model.StageItem.colorIndex]）。
 *
 * 暗所モードでは赤以外の光を出せないので、色相ではなく明度だけで
 * [StageItemColor.COUNT] 段に分ける。区別のしやすさは落ちるが、
 * 「赤しか出さない」という約束の方を優先する。
 */
@Composable
fun stageItemPalette(): List<Color> =
    if (LocalPaThemeMode.current == ThemeMode.NIGHT_RED) NIGHT_RED else STANDARD

/** [stageItemPalette] それぞれに乗せる文字色。しきい値ではなくコントラスト比で選ぶ。 */
@Composable
fun stageItemTextPalette(): List<Color> = stageItemPalette().map { contrastingInk(it) }

// StageItemColor.COUNT（6）ぶん。増やすときは両方の配列を揃えて足すこと
private val STANDARD = listOf(
    Color(0xFF5F82BF),
    Color(0xFF4E9179),
    Color(0xFFD9A362),
    Color(0xFFC4738F),
    Color(0xFF8E7BC4),
    Color(0xFF4E96A5),
)

private val NIGHT_RED = listOf(
    Color(0xFFFF6B4A),
    Color(0xFFE04B2B),
    Color(0xFFC23A1F),
    Color(0xFFA33520),
    Color(0xFF802B18),
    Color(0xFF662211),
)
