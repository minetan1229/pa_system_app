package com.patoolbox.feature.calc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 計算機の入力欄。
 * 空欄や入力途中（"-" や "1." など）を弾かずに受け取り、
 * 数値として読めないときは結果側を「--」にする方針にしている。
 * 入力中にエラー表示が出るのは現場で邪魔なので。
 */
@Composable
internal fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = imeAction,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/** 見出し。 */
@Composable
internal fun CalcSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

/** 計算結果。読みやすさを優先して枠付きで出す。 */
@Composable
internal fun CalcResult(
    text: String,
    modifier: Modifier = Modifier,
    emphasis: Boolean = false,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (emphasis) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
    ) {
        Text(
            text = text,
            style = if (emphasis) {
                MaterialTheme.typography.headlineMedium
            } else {
                MaterialTheme.typography.titleMedium
            },
            color = if (emphasis) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.padding(12.dp),
        )
    }
}

/** 補足説明。 */
@Composable
internal fun CalcNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** 選択肢の横並び。 */
@Composable
internal fun <T> CalcSelector(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    minTouch: Dp,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (value, label) ->
            if (value == selected) {
                Button(
                    onClick = { onSelect(value) },
                    modifier = Modifier.heightIn(min = minTouch),
                ) { Text(label) }
            } else {
                OutlinedButton(
                    onClick = { onSelect(value) },
                    modifier = Modifier.heightIn(min = minTouch),
                ) { Text(label) }
            }
        }
    }
}

/** 入力欄の横並び。 */
@Composable
internal fun CalcFieldRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

internal fun String.toDoubleOrNullLenient(): Double? =
    trim().replace(',', '.').toDoubleOrNull()

/** 数値を桁数指定で整形する。読めない値は "--"。 */
internal fun Double?.format(decimals: Int = 1): String =
    if (this == null || !isFinite()) "--" else "%.${decimals}f".format(this)

@Composable
internal fun CalcColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}
