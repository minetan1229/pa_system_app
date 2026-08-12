package com.patoolbox.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.patoolbox.core.designsystem.theme.LocalPaThemeMode
import com.patoolbox.core.designsystem.theme.PaCategoryColors
import com.patoolbox.core.model.ThemeMode
import com.patoolbox.core.model.ToolCategory

/**
 * カテゴリの識別色。
 * 暗所モードでは色で区別すると赤以外の光が出てしまうため、明度差だけで区別する。
 */
@Composable
fun ToolCategory.accentColor(): Color {
    if (LocalPaThemeMode.current == ThemeMode.NIGHT_RED) {
        return when (this) {
            ToolCategory.MEASURE -> Color(0xFFFF6B4A)
            ToolCategory.CALC -> Color(0xFFCC4629)
            ToolCategory.DOCUMENT -> Color(0xFFA33520)
            ToolCategory.BUSINESS -> Color(0xFF802B18)
        }
    }
    return when (this) {
        ToolCategory.MEASURE -> PaCategoryColors.measure
        ToolCategory.CALC -> PaCategoryColors.calc
        ToolCategory.DOCUMENT -> PaCategoryColors.document
        ToolCategory.BUSINESS -> PaCategoryColors.business
    }
}
