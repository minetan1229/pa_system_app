package com.patoolbox.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.patoolbox.core.model.ThemeMode

/** 現在のテーマモード。屋外モードで線を太くするなど、コンポーネント側の分岐に使う。 */
val LocalPaThemeMode = staticCompositionLocalOf { ThemeMode.SYSTEM }

@Composable
fun PaTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val colorScheme = when (themeMode) {
        ThemeMode.SYSTEM -> if (systemDark) PaDarkColors else PaLightColors
        ThemeMode.LIGHT -> PaLightColors
        ThemeMode.DARK -> PaDarkColors
        ThemeMode.NIGHT_RED -> PaNightRedColors
        ThemeMode.OUTDOOR -> PaOutdoorColors
    }

    CompositionLocalProvider(
        LocalPaDimens provides PaDimens(),
        LocalPaThemeMode provides themeMode,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PaTypography,
            content = content,
        )
    }
}

/** テーマモードが暗所系か（ステータスバーのアイコン色などに使う）。 */
@Composable
fun ThemeMode.isDarkAppearance(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT, ThemeMode.OUTDOOR -> false
    ThemeMode.DARK, ThemeMode.NIGHT_RED -> true
}
