package com.patoolbox.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 現場前提の寸法。
 *
 * 余白は 4dp グリッドに乗せている。画面ごとに 6dp / 10dp / 14dp と散らすと、
 * 一つ一つは正しく見えても並べたときに揃わず、雑な印象だけが残る。
 * [minTouch] を 48dp 以上に保つのは、暗所・手袋・揺れる足場でも押せるようにするため。
 */
@Immutable
data class PaDimens(
    val minTouch: Dp = 48.dp,

    // --- 余白（4dp グリッド） ---
    val spaceXs: Dp = 4.dp,
    val spaceSm: Dp = 8.dp,
    val spaceMd: Dp = 12.dp,
    val space: Dp = 16.dp,
    val spaceLg: Dp = 24.dp,
    val spaceXl: Dp = 32.dp,

    /** 画面の左右余白 */
    val gutter: Dp = 16.dp,
    val gutterSmall: Dp = 8.dp,

    // --- 角丸 ---
    val cornerSmall: Dp = 10.dp,
    val cardCorner: Dp = 16.dp,
    val cornerLarge: Dp = 24.dp,

    val toolCardMinHeight: Dp = 116.dp,
    val badgeSize: Dp = 40.dp,

    /** 枠線。影ではなく線で面を分けるので、太らせないこと */
    val hairline: Dp = 1.dp,
)

val LocalPaDimens = staticCompositionLocalOf { PaDimens() }
