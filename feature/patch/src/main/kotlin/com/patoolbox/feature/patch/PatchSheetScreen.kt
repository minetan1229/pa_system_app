package com.patoolbox.feature.patch

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.PatchRow
import com.patoolbox.core.model.StandTypes
import com.patoolbox.core.ui.R as CoreUiR

/**
 * パッチ表の編集。
 *
 * 行は折りたたみ表示にしている。1画面に8項目×16chを並べても現場では読めないので、
 * 一覧では ch / 音源 / マイク / 48V だけを出し、タップで編集欄を開く。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatchSheetScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PatchSheetViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dimens = LocalPaDimens.current
    val context = LocalContext.current
    var expandedRowId by rememberSaveable { mutableStateOf<Long?>(null) }

    // 保存先の選択は SAF に任せる。書き込み権限が不要になり、
    // ユーザーが Drive でも端末内でも選べる
    val createPdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(PDF_MIME_TYPE),
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.let(viewModel::exportPdf)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(uiState.sheet?.name ?: stringResource(R.string.patch_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(CoreUiR.string.back))
                    }
                },
                actions = {
                    // PDF は Pro 専用。押せない理由が分かるよう、隠さず無効で出す
                    TextButton(
                        onClick = { createPdfLauncher.launch(viewModel.suggestedFileName()) },
                        enabled = uiState.canExport,
                    ) {
                        Text(stringResource(R.string.patch_export_pdf))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = dimens.gutter),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(
                        R.string.patch_filled_count,
                        uiState.filledCount,
                        uiState.rows.size,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.patch_phantom_count, uiState.phantomCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (uiState.phantomCount > 0) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Text(
                text = stringResource(R.string.patch_expand_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = dimens.gutterSmall),
            ) {
                items(uiState.rows, key = { it.id }) { row ->
                    PatchRowCard(
                        row = row,
                        expanded = expandedRowId == row.id,
                        onToggle = {
                            expandedRowId = if (expandedRowId == row.id) null else row.id
                        },
                        onChange = viewModel::updateRow,
                        onDelete = {
                            expandedRowId = null
                            viewModel.deleteRow(row)
                        },
                        minTouch = dimens.minTouch,
                    )
                }
                item {
                    OutlinedButton(
                        onClick = viewModel::addChannel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = dimens.gutterSmall, bottom = dimens.gutter),
                    ) {
                        Text(stringResource(R.string.patch_add_channel))
                    }
                }
            }
        }
    }
}

@Composable
private fun PatchRowCard(
    row: PatchRow,
    expanded: Boolean,
    onToggle: () -> Unit,
    onChange: (PatchRow) -> Unit,
    onDelete: () -> Unit,
    minTouch: Dp,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = row.channel.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(28.dp),
            )
            Text(
                text = row.source.ifBlank { "—" },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.2f),
            )
            Text(
                text = row.micModel.ifBlank { "—" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (row.phantom) {
                Text(
                    text = stringResource(R.string.patch_phantom),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }

        if (!expanded) return@Card

        Column(
            modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = row.source,
                onValueChange = { onChange(row.copy(source = it)) },
                label = { Text(stringResource(R.string.patch_source)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = row.micModel,
                onValueChange = { onChange(row.copy(micModel = it)) },
                label = { Text(stringResource(R.string.patch_mic)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = row.multiNumber,
                    onValueChange = { onChange(row.copy(multiNumber = it)) },
                    label = { Text(stringResource(R.string.patch_multi)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.patch_phantom),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = row.phantom,
                        onCheckedChange = { onChange(row.copy(phantom = it)) },
                    )
                }
            }

            Text(
                text = stringResource(R.string.patch_stand),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // スタンドは自由入力より選択のほうが早い（現場で毎回同じ数種類しか使わない）
            StandSelector(
                selected = row.standType,
                onSelect = { onChange(row.copy(standType = it)) },
                minTouch = minTouch,
            )

            OutlinedTextField(
                value = row.notes,
                onValueChange = { onChange(row.copy(notes = it)) },
                label = { Text(stringResource(R.string.patch_notes)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            TextButton(onClick = onDelete) {
                Text(
                    text = stringResource(R.string.patch_delete_row),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun StandSelector(
    selected: String,
    onSelect: (String) -> Unit,
    minTouch: Dp,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StandTypes.ALL.forEach { stand ->
            val isSelected = stand == selected
            OutlinedButton(
                // もう一度押したら選択解除（間違えて押したときに戻せる）
                onClick = { onSelect(if (isSelected) "" else stand) },
                modifier = Modifier.heightIn(min = minTouch),
                colors = if (isSelected) {
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else {
                    ButtonDefaults.outlinedButtonColors()
                },
            ) { Text(stand) }
        }
    }
}

private const val PDF_MIME_TYPE = "application/pdf"
