package com.patoolbox.feature.stageplot

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.StagePlot
import com.patoolbox.core.model.StageSymbol
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.ui.component.PaToolScaffold

/**
 * ステージプロットの編集。
 *
 * 図をタップして選び、そのままドラッグで動かす。長押しやモード切替を挟まないのは、
 * 現場で片手で触ることが多く、手数が増えると使われなくなるため。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StagePlotEditorScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StagePlotViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dimens = LocalPaDimens.current
    val context = LocalContext.current
    var showDetails by remember { mutableStateOf(false) }

    val createPdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(PDF_MIME_TYPE),
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.let { viewModel.exportPdf(it) }
        }
    }

    PaToolScaffold(
        tool = ToolId.STAGE_PLOT,
        onBack = onBack,
        modifier = modifier,
        title = uiState.plot?.name ?: stringResource(R.string.stageplot_title),
        actions = {
            TextButton(onClick = { showDetails = true }) {
                Text(stringResource(R.string.stageplot_details))
            }
            if (uiState.proStatus.isPro) {
                TextButton(
                    onClick = { createPdfLauncher.launch(viewModel.suggestedFileName()) },
                ) {
                    Text(stringResource(R.string.stageplot_export))
                }
            }
        },
    ) { innerPadding ->
        val plot = uiState.plot
        if (plot == null) {
            // 削除された図を開いたまま戻ってきた場合
            LaunchedEffect(Unit) { onBack() }
            return@PaToolScaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = dimens.gutter),
            verticalArrangement = Arrangement.spacedBy(dimens.gutterSmall),
        ) {
            StagePlotCanvas(
                plot = plot,
                selectedItemId = uiState.selectedItemId,
                onSelect = viewModel::select,
                onMove = viewModel::moveItem,
                modifier = Modifier.weight(1f),
            )

            uiState.selectedItem?.let { item ->
                var label by remember(item.id) { mutableStateOf(item.label) }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = label,
                        onValueChange = {
                            label = it
                            viewModel.renameSelected(it)
                        },
                        label = { Text(stringResource(R.string.stageplot_label)) },
                        placeholder = { Text(item.symbol.defaultLabel) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = viewModel::deleteSelected) {
                        Text(
                            text = stringResource(R.string.stageplot_delete_item),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            } ?: Text(
                text = stringResource(R.string.stageplot_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = stringResource(R.string.stageplot_palette),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = dimens.gutter),
            ) {
                for (symbol in StageSymbol.PALETTE) {
                    AssistChip(
                        onClick = { viewModel.addItem(symbol) },
                        label = { Text(symbol.defaultLabel) },
                        modifier = Modifier.height(dimens.minTouch),
                    )
                }
            }
        }

        if (showDetails) {
            DetailsDialog(
                plot = plot,
                onDismiss = { showDetails = false },
                onSave = { name, width, depth, notes ->
                    viewModel.updateDetails(name, width, depth, notes)
                    showDetails = false
                },
            )
        }
    }
}

@Composable
private fun DetailsDialog(
    plot: StagePlot,
    onDismiss: () -> Unit,
    onSave: (name: String, width: Double, depth: Double, notes: String) -> Unit,
) {
    var name by remember { mutableStateOf(plot.name) }
    var width by remember { mutableStateOf(plot.stageWidthMeters.toString()) }
    var depth by remember { mutableStateOf(plot.stageDepthMeters.toString()) }
    var notes by remember { mutableStateOf(plot.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.stageplot_details)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.stageplot_name)) },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = width,
                        onValueChange = { width = it },
                        label = { Text(stringResource(R.string.stageplot_width)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = depth,
                        onValueChange = { depth = it },
                        label = { Text(stringResource(R.string.stageplot_depth)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.stageplot_notes)) },
                    minLines = 2,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        name.ifBlank { plot.name },
                        width.toDoubleOrNull() ?: plot.stageWidthMeters,
                        depth.toDoubleOrNull() ?: plot.stageDepthMeters,
                        notes,
                    )
                },
            ) {
                Text(stringResource(R.string.stageplot_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.stageplot_cancel))
            }
        },
    )
}

private const val PDF_MIME_TYPE = "application/pdf"
