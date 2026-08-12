package com.patoolbox.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 現場前提の寸法。
 * [minTouch] を 48dp 以上に保つのは、暗所・手袋・揺れる足場でも押せるようにするため。
 */
@Immutable
data class PaDimens(
    val minTouch: Dp = 48.dp,
    val gutter: Dp = 16.dp,
    val gutterSmall: Dp = 8.dp,
    val cardCorner: Dp = 16.dp,
    val toolCardMinHeight: Dp = 104.dp,
    val badgeSize: Dp = 40.dp,
)

val LocalPaDimens = staticCompositionLocalOf { PaDimens() }
