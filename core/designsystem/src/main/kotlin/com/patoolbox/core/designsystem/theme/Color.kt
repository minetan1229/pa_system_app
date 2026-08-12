package com.patoolbox.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// surfaceContainer 系まで明示的に指定している。指定を省くと Material のベースライン
// （紫がかったグレー）が残り、特に NIGHT_RED でカードだけ灰色に浮くため。

internal val PaLightColors = lightColorScheme(
    primary = Color(0xFF00658F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC7E7FF),
    onPrimaryContainer = Color(0xFF001E2F),
    secondary = Color(0xFF4F616E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD2E5F5),
    onSecondaryContainer = Color(0xFF0B1D29),
    tertiary = Color(0xFF63597C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE9DDFF),
    onTertiaryContainer = Color(0xFF1F1635),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF7F9FF),
    onBackground = Color(0xFF191C1E),
    surface = Color(0xFFF7F9FF),
    onSurface = Color(0xFF191C1E),
    surfaceVariant = Color(0xFFDDE3EA),
    onSurfaceVariant = Color(0xFF41484D),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F4F9),
    surfaceContainer = Color(0xFFEBEEF3),
    surfaceContainerHigh = Color(0xFFE5E8EE),
    surfaceContainerHighest = Color(0xFFDFE3E8),
    outline = Color(0xFF71787E),
    outlineVariant = Color(0xFFC1C7CE),
)

internal val PaDarkColors = darkColorScheme(
    primary = Color(0xFF87CEFF),
    onPrimary = Color(0xFF00344D),
    primaryContainer = Color(0xFF004C6D),
    onPrimaryContainer = Color(0xFFC7E7FF),
    secondary = Color(0xFFB6C9D8),
    onSecondary = Color(0xFF21323E),
    secondaryContainer = Color(0xFF374955),
    onSecondaryContainer = Color(0xFFD2E5F5),
    tertiary = Color(0xFFCDC1E9),
    onTertiary = Color(0xFF342B4B),
    tertiaryContainer = Color(0xFF4B4163),
    onTertiaryContainer = Color(0xFFE9DDFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF101416),
    onBackground = Color(0xFFE1E2E5),
    surface = Color(0xFF101416),
    onSurface = Color(0xFFE1E2E5),
    surfaceVariant = Color(0xFF41484D),
    onSurfaceVariant = Color(0xFFC1C7CE),
    surfaceContainerLowest = Color(0xFF0A0E10),
    surfaceContainerLow = Color(0xFF171B1D),
    surfaceContainer = Color(0xFF1B1F21),
    surfaceContainerHigh = Color(0xFF262A2C),
    surfaceContainerHighest = Color(0xFF313537),
    outline = Color(0xFF8B9198),
    outlineVariant = Color(0xFF41484D),
)

/**
 * 暗所（FOH）用。赤系だけで描画して暗順応を壊さない。
 * 青系の光を一切出さないのが目的なので、彩度のある色を足さないこと。
 */
internal val PaNightRedColors = darkColorScheme(
    primary = Color(0xFFFF5533),
    onPrimary = Color(0xFF1A0000),
    primaryContainer = Color(0xFF3D0F00),
    onPrimaryContainer = Color(0xFFFF8566),
    secondary = Color(0xFFCC4629),
    onSecondary = Color(0xFF1A0000),
    secondaryContainer = Color(0xFF2E0A00),
    onSecondaryContainer = Color(0xFFFF7A5C),
    tertiary = Color(0xFFB33D22),
    onTertiary = Color(0xFF1A0000),
    tertiaryContainer = Color(0xFF260800),
    onTertiaryContainer = Color(0xFFFF6B4A),
    error = Color(0xFFFFAA8F),
    onError = Color(0xFF330500),
    errorContainer = Color(0xFF5C1200),
    onErrorContainer = Color(0xFFFFC7B5),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE04B2B),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFE04B2B),
    surfaceVariant = Color(0xFF260800),
    onSurfaceVariant = Color(0xFFB33D22),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF080000),
    surfaceContainer = Color(0xFF120000),
    surfaceContainerHigh = Color(0xFF1C0300),
    surfaceContainerHighest = Color(0xFF260500),
    outline = Color(0xFF802B18),
    outlineVariant = Color(0xFF4D1A0E),
)

/** 屋外・直射日光用。コントラスト最大。 */
internal val PaOutdoorColors = lightColorScheme(
    primary = Color(0xFF00344D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF00344D),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF1A1A1A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E0E0),
    onSecondaryContainer = Color(0xFF000000),
    tertiary = Color(0xFF3D2E00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE08A),
    onTertiaryContainer = Color(0xFF000000),
    error = Color(0xFF8B0000),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFD6D6),
    onErrorContainer = Color(0xFF000000),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFE8E8E8),
    onSurfaceVariant = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFAFA),
    surfaceContainer = Color(0xFFF2F2F2),
    surfaceContainerHigh = Color(0xFFEAEAEA),
    surfaceContainerHighest = Color(0xFFE0E0E0),
    outline = Color(0xFF000000),
    outlineVariant = Color(0xFF8A8A8A),
)

/**
 * カテゴリの識別色。
 * アイコンを使わない方針なので、色 + 文字バッジでツールを見分ける。
 */
object PaCategoryColors {
    val measure = Color(0xFF00A0B0)
    val calc = Color(0xFF7B8FA1)
    val document = Color(0xFFE0A32E)
    val business = Color(0xFF8E7CC3)
}
