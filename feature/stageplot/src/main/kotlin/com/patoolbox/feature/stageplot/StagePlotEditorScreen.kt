package com.patoolbox.feature.stageplot

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.component.PaCard
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.StageItem
import com.patoolbox.core.model.StageItemColor
import com.patoolbox.core.model.StagePlot
import com.patoolbox.core.model.StageShape
import com.patoolbox.core.model.StageSymbol
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.ui.component.PaToolScaffold

/**
 * ステージプロットの編集。
 *
 * キャンバスを画面のほぼ全部に使わせ、記号の追加パレットと選択中の記号の編集は
 * 下からのシートに退避してある。常時表示のパレット行がキャンバスの取り分を
 * 圧迫していたのが「画面が小さくて触れない」の一因だったため。
 *
 * 図をタップして選ぶと編集シートが自動で開く。長押しやモード切替を挟まないのは、
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
    var showAddSymbol by remember { mutableStateOf(false) }

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
                .padding(innerPadding),
        ) {
            StagePlotCanvas(
                plot = plot,
                selectedItemId = uiState.selectedItemId,
                onSelect = viewModel::select,
                onMove = viewModel::moveItem,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = dimens.gutter),
            )

            Text(
                text = stringResource(R.string.stageplot_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = dimens.gutter, vertical = dimens.spaceXs),
            )

            OutlinedButton(
                onClick = { showAddSymbol = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.gutter, vertical = dimens.gutterSmall)
                    .heightIn(min = dimens.minTouch),
            ) {
                Text(stringResource(R.string.stageplot_palette))
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

    if (showAddSymbol) {
        AddSymbolSheet(
            onDismiss = { showAddSymbol = false },
            onAdd = viewModel::addItem,
        )
    }

    uiState.selectedItem?.let { item ->
        ItemEditSheet(
            item = item,
            onDismiss = { viewModel.select(null) },
            onRename = viewModel::renameSelected,
            onRecolor = viewModel::recolorSelected,
            onDelete = {
                viewModel.deleteSelected()
            },
        )
    }
}

/**
 * 記号の追加パレット。シートにしたのは、常時表示だとキャンバスの取り分を圧迫するため。
 * 複数個続けて置きたい場面が多いので、追加してもシートは自動で閉じない。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSymbolSheet(
    onDismiss: () -> Unit,
    onAdd: (StageSymbol) -> Unit,
) {
    val dimens = LocalPaDimens.current
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.gutter)
                .padding(bottom = dimens.spaceXl),
            verticalArrangement = Arrangement.spacedBy(dimens.space),
        ) {
            Text(
                text = stringResource(R.string.stageplot_palette),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceSm),
            ) {
                for (symbol in StageSymbol.PALETTE) {
                    SymbolOption(symbol = symbol, onClick = { onAdd(symbol) })
                }
            }
        }
    }
}

/** 記号1つぶんの選択肢。実際の図と同じ形のミニプレビュー付きにして、文字だけより見分けやすくする。 */
@Composable
private fun SymbolOption(symbol: StageSymbol, onClick: () -> Unit) {
    val dimens = LocalPaDimens.current
    PaCard(
        onClick = onClick,
        contentPadding = dimens.spaceSm,
        verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SymbolGlyph(symbol = symbol)
            Text(
                text = symbol.defaultLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** 記号の形（矩形・円・くさび）をそのまま縮小したミニプレビュー。 */
@Composable
private fun SymbolGlyph(symbol: StageSymbol) {
    val shape = when (symbol.shape) {
        StageShape.CIRCLE -> CircleShape
        StageShape.RECT, StageShape.WEDGE -> RoundedCornerShape(6.dp)
    }
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol.badge,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
        )
    }
}

/**
 * 選択中の記号の編集。図をタップすると自動で開く。閉じると選択も解除する。
 * 名前・色・削除をここに集約し、キャンバス側は常に何も乗らない状態にしておく。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemEditSheet(
    item: StageItem,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onRecolor: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    val dimens = LocalPaDimens.current
    val sheetState = rememberModalBottomSheetState()
    var label by remember(item.id) { mutableStateOf(item.label) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.gutter)
                .padding(bottom = dimens.spaceXl),
            verticalArrangement = Arrangement.spacedBy(dimens.space),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SymbolGlyph(symbol = item.symbol)
                Text(
                    text = item.symbol.defaultLabel,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            OutlinedTextField(
                value = label,
                onValueChange = {
                    label = it
                    onRename(it)
                },
                label = { Text(stringResource(R.string.stageplot_label)) },
                placeholder = { Text(item.symbol.defaultLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            ItemColorRow(
                selectedColorIndex = item.colorIndex,
                onSelect = onRecolor,
            )
            OutlinedButton(
                onClick = {
                    onDelete()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = dimens.minTouch),
            ) {
                Text(
                    text = stringResource(R.string.stageplot_delete_item),
                    color = MaterialTheme.colorScheme.error,
                )
            }
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

/**
 * 選んだ記号の色を選ぶ列。
 *
 * 「あの赤いマイク」のように、現場では位置よりも色で個体を呼ぶことが多い。
 */
@Composable
private fun ItemColorRow(
    selectedColorIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current
    val palette = stageItemPalette()
    val coerced = StageItemColor.coerce(selectedColorIndex)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
    ) {
        palette.forEachIndexed { index, color ->
            val selected = index == coerced
            Box(
                modifier = Modifier
                    .size(dimens.minTouch)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (selected) 3.dp else dimens.hairline,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = CircleShape,
                    )
                    .clickable { onSelect(index) }
                    .semantics {
                        contentDescription = "色 ${index + 1}"
                    },
            )
        }
    }
}

private const val PDF_MIME_TYPE = "application/pdf"
