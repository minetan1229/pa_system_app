package com.patoolbox.feature.showrunner

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.patoolbox.core.designsystem.theme.LocalPaThemeMode
import com.patoolbox.core.model.ThemeMode

/**
 * パッドの色。[com.patoolbox.feature.sfx] の同名関数と揃えてある
 * （同じ [com.patoolbox.core.model.SoundCue.colorIndex] を見せるので、色も同じにしないと
 * 「SE パッド画面では青いのに、こちらでは違う色」になって混乱する）。
 */
@Composable
fun padColor(colorIndex: Int): Color {
    val index = ((colorIndex % PALETTE_SIZE) + PALETTE_SIZE) % PALETTE_SIZE
    return if (LocalPaThemeMode.current == ThemeMode.NIGHT_RED) {
        NIGHT_RED[index]
    } else {
        STANDARD[index]
    }
}

private const val PALETTE_SIZE = 6

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
