package com.patoolbox.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patoolbox.core.designsystem.theme.ReadoutTextStyle

/**
 * SPL 値のような主計測値を、離れた場所からでも読める大きさで表示する。
 *
 * 桁数が増えたら自動で縮小する（"104.7" と "97.2" で表示位置が跳ねないよう等幅）。
 *
 * @param value 表示する数値の文字列。フォーマットは呼び出し側の責任（丸めは計測側で決める）
 * @param unit 単位（dB, ms など）
 * @param label 上に出す見出し（LAeq, T30 など）
 * @param caption 下に出す補足（校正状態や測定条件）
 */
@Composable
fun BigReadout(
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    label: String? = null,
    caption: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.primary,
    maxFontSize: TextUnit = 88.sp,
    minFontSize: TextUnit = 32.sp,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center,
        ) {
            AutoShrinkText(
                text = value,
                color = valueColor,
                maxFontSize = maxFontSize,
                minFontSize = minFontSize,
            )
            if (unit != null) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                )
            }
        }

        if (caption != null) {
            Text(
                text = caption,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * 幅に収まるまでフォントサイズを段階的に下げる。
 * Compose の自動縮小 API に頼らず自前で行っているのは、
 * 1行固定・等幅で「桁が増えても左右に暴れない」挙動を確実にしたいため。
 */
@Composable
private fun AutoShrinkText(
    text: String,
    color: Color,
    maxFontSize: TextUnit,
    minFontSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    var fontSizeValue by remember(text, maxFontSize) { mutableStateOf(maxFontSize.value) }

    Text(
        text = text,
        modifier = modifier,
        style = ReadoutTextStyle.copy(fontSize = fontSizeValue.sp),
        color = color,
        maxLines = 1,
        softWrap = false,
        onTextLayout = { result ->
            if (result.didOverflowWidth && fontSizeValue > minFontSize.value) {
                fontSizeValue = (fontSizeValue * 0.92f).coerceAtLeast(minFontSize.value)
            }
        },
    )
}
