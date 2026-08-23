package com.patoolbox.feature.sfx

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.patoolbox.core.designsystem.theme.LocalPaThemeMode
import com.patoolbox.core.model.ThemeMode

/**
 * パッドの色。
 *
 * 位置ではなく色で覚えられるようにするためのもの。暗い袖で「左から3番目」を
 * 数えるより、「青いやつ」の方が速い。
 *
 * 暗所モード（[ThemeMode.NIGHT_RED]）では赤以外の光を一切出さない約束なので、
 * 色相ではなく明度だけで6段階に分ける。区別はしにくくなるが、
 * 暗順応を壊さないことの方が優先される。
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

// 彩度は落としてある。純度の高い6色を並べると、
// 生成りの地の上でパッドだけが別のアプリのように浮く。
// 色相の間隔は保ってあるので、暗い袖で「青いやつ」と覚える使い方は変わらない。
private val STANDARD = listOf(
    Color(0xFF5F82BF), // 青
    Color(0xFF4E9179), // 緑
    Color(0xFFD9A362), // 橙
    Color(0xFFC4738F), // 桃
    Color(0xFF8E7BC4), // 紫
    Color(0xFF4E96A5), // 水
)

private val NIGHT_RED = listOf(
    Color(0xFFFF6B4A),
    Color(0xFFE04B2B),
    Color(0xFFC23A1F),
    Color(0xFFA33520),
    Color(0xFF802B18),
    Color(0xFF662211),
)
