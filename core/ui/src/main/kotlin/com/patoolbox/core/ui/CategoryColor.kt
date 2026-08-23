package com.patoolbox.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.patoolbox.core.designsystem.theme.LocalPaThemeMode
import com.patoolbox.core.designsystem.theme.PaCategoryColors
import com.patoolbox.core.designsystem.theme.isDarkAppearance
import com.patoolbox.core.model.ThemeMode
import com.patoolbox.core.model.ToolCategory

/**
 * カテゴリの識別色。
 *
 * 暗所モードでは色で区別すると赤以外の光が出てしまうため、明度差だけで区別する。
 *
 * 暗色テーマでは明色用より淡い値に切り替える。この色はバッジの地になるので、
 * 明色用の濃い色を炭色のカードに置くと明度差が 2.5 まで落ち、
 * バッジがカードに溶けて「どの分類の道具か」が見分けられなくなる。
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
    if (LocalPaThemeMode.current.isDarkAppearance()) {
        return when (this) {
            ToolCategory.MEASURE -> PaCategoryColors.measureDark
            ToolCategory.CALC -> PaCategoryColors.calcDark
            ToolCategory.DOCUMENT -> PaCategoryColors.documentDark
            ToolCategory.BUSINESS -> PaCategoryColors.businessDark
        }
    }
    return when (this) {
        ToolCategory.MEASURE -> PaCategoryColors.measure
        ToolCategory.CALC -> PaCategoryColors.calc
        ToolCategory.DOCUMENT -> PaCategoryColors.document
        ToolCategory.BUSINESS -> PaCategoryColors.business
    }
}
