package com.patoolbox.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// surfaceContainer 系まで明示的に指定している。指定を省くと Material のベースライン
// （紫がかったグレー）が残り、特に NIGHT_RED でカードだけ灰色に浮くため。
//
// 面の分け方は「影」ではなく「明度の段差 + 髪の毛一本の枠線」に統一している。
// 暗所モードでは影が一切見えず、屋外モードでは影が飛ぶ。4つのテーマすべてで
// 同じ作りが成立するのは、影に頼らない方だけ。

internal val PaLightColors = lightColorScheme(
    primary = Color(0xFF0B5FD0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE8FF),
    onPrimaryContainer = Color(0xFF001A44),
    secondary = Color(0xFF44566B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCE6F2),
    onSecondaryContainer = Color(0xFF0A1722),
    tertiary = Color(0xFF00808C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC6F1F5),
    onTertiaryContainer = Color(0xFF00272C),
    error = Color(0xFFC0182B),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDDDF),
    onErrorContainer = Color(0xFF41000A),
    background = Color(0xFFF7F9FC),
    onBackground = Color(0xFF0E1116),
    surface = Color(0xFFF7F9FC),
    onSurface = Color(0xFF0E1116),
    surfaceVariant = Color(0xFFE4E9F1),
    onSurfaceVariant = Color(0xFF4A5462),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFCFDFE),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF1F4F9),
    surfaceContainerHighest = Color(0xFFE8EDF4),
    outline = Color(0xFF6B7684),
    outlineVariant = Color(0xFFDDE3EC),
)

/**
 * 暗色。FOH の卓まわりで一番長く見る画面なので、ここを基準に作ってある。
 * 背景をほぼ黒に落とし、面は明度をわずかに上げるだけで分ける。
 */
internal val PaDarkColors = darkColorScheme(
    primary = Color(0xFF7FB0FF),
    onPrimary = Color(0xFF00274F),
    primaryContainer = Color(0xFF0B3D85),
    onPrimaryContainer = Color(0xFFDCE8FF),
    secondary = Color(0xFFAFC2D6),
    onSecondary = Color(0xFF1B2836),
    secondaryContainer = Color(0xFF2C3B4B),
    onSecondaryContainer = Color(0xFFDCE6F2),
    tertiary = Color(0xFF5AD8E4),
    onTertiary = Color(0xFF00363C),
    tertiaryContainer = Color(0xFF004F58),
    onTertiaryContainer = Color(0xFFC6F1F5),
    error = Color(0xFFFF9A9F),
    onError = Color(0xFF5C0011),
    errorContainer = Color(0xFF8C0018),
    onErrorContainer = Color(0xFFFFDDDF),
    background = Color(0xFF0A0C10),
    onBackground = Color(0xFFE7EBF1),
    surface = Color(0xFF0A0C10),
    onSurface = Color(0xFFE7EBF1),
    surfaceVariant = Color(0xFF2A313C),
    onSurfaceVariant = Color(0xFFA9B3C1),
    surfaceContainerLowest = Color(0xFF05070A),
    surfaceContainerLow = Color(0xFF0F1319),
    surfaceContainer = Color(0xFF141920),
    surfaceContainerHigh = Color(0xFF1C222B),
    surfaceContainerHighest = Color(0xFF252C37),
    outline = Color(0xFF7D8797),
    outlineVariant = Color(0xFF262D38),
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
    val measure = Color(0xFF00B3C4)
    val calc = Color(0xFF5B8DEF)
    val document = Color(0xFFE8A33D)
    val business = Color(0xFF9B7BEA)
}
