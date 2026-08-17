package com.patoolbox.feature.business

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.ProStatus
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.ui.component.PaToolScaffold

/**
 * Phase 6 の運営ツールに共通する枠。
 *
 * 5画面（機材台帳・スナップショット・見積/請求・稼働記録・バックアップ）は
 * どれも「一覧して足して消す」形なので、外枠と Pro の案内をまとめてある。
 */
@Composable
internal fun BusinessScaffold(
    tool: ToolId,
    title: String,
    onBack: () -> Unit,
    proStatus: ProStatus,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (Modifier) -> Unit,
) {
    val dimens = LocalPaDimens.current

    PaToolScaffold(
        tool = tool,
        onBack = onBack,
        modifier = modifier,
        title = title,
        actions = { actions() },
        floatingActionButton = floatingActionButton,
    ) { innerPadding ->
        if (!proStatus.isPro) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(dimens.gutter),
            ) {
                Text(
                    text = stringResource(R.string.business_pro),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            return@PaToolScaffold
        }

        content(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = dimens.gutter),
        )
    }
}

@Composable
internal fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        keyboardOptions = if (numeric) {
            KeyboardOptions(keyboardType = KeyboardType.Number)
        } else {
            KeyboardOptions.Default
        },
        modifier = modifier,
    )
}

/** 円の表示。3桁区切りが無いと桁を読み違える。 */
internal fun formatYen(value: Long): String = "%,d 円".format(value)
