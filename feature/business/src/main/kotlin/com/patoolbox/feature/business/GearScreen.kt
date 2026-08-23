package com.patoolbox.feature.business

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.GearCategory
import com.patoolbox.core.model.GearItem
import com.patoolbox.core.model.GearStatus
import com.patoolbox.core.model.ToolId

/**
 * 機材台帳。
 *
 * 数量を持たせているのは、ケーブルやスタンドを1本ずつ登録するのが
 * 現実的でないため。同じ型番はまとめて数える。
 */
@Composable
fun GearScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GearViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dimens = LocalPaDimens.current
    var editing by remember { mutableStateOf<GearItem?>(null) }
    var showPresets by remember { mutableStateOf(false) }

    BusinessScaffold(
        tool = ToolId.GEAR_INVENTORY,
        title = stringResource(R.string.gear_title),
        onBack = onBack,
        proStatus = uiState.proStatus,
        modifier = modifier,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = GearItem(name = "") },
                text = { Text(stringResource(R.string.gear_add)) },
                icon = {},
            )
        },
    ) { contentModifier ->
        Column(modifier = contentModifier) {
            Text(
                text = stringResource(R.string.gear_summary, uiState.usableCount, uiState.items.size),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = dimens.gutterSmall),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                FilterChip(
                    selected = uiState.filter == null,
                    onClick = { viewModel.setFilter(null) },
                    label = { Text(stringResource(R.string.gear_all)) },
                )
                for (category in GearCategory.entries) {
                    FilterChip(
                        selected = uiState.filter == category,
                        onClick = { viewModel.setFilter(category) },
                        label = { Text(category.label) },
                    )
                }
            }

            // 一件ずつ手で打つのがつらい定番の型番は、選ぶだけで編集画面まで進めるようにしてある。
            // ここで台帳に足してしまわないのは、持ってもいない機材が並ぶと台帳が信用できなくなるため
            TextButton(onClick = { showPresets = true }) {
                Text(stringResource(R.string.gear_add_from_preset))
            }

            if (uiState.visible.isEmpty()) {
                Text(
                    text = stringResource(R.string.gear_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = dimens.gutter),
                )
                return@Column
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(dimens.gutterSmall),
                modifier = Modifier.padding(vertical = dimens.gutterSmall),
            ) {
                items(uiState.visible, key = { it.id }) { item ->
                    GearCard(item = item, onClick = { editing = item })
                }
            }
        }
    }

    editing?.let { item ->
        GearDialog(
            item = item,
            onDismiss = { editing = null },
            onSave = {
                viewModel.save(it)
                editing = null
            },
            onDelete = {
                viewModel.delete(item)
                editing = null
            },
        )
    }

    if (showPresets) {
        GearPresetDialog(
            onDismiss = { showPresets = false },
            onSelect = { preset ->
                showPresets = false
                editing = preset
            },
        )
    }
}

/**
 * よくある機材の選択肢。
 *
 * 選んだ時点ではまだ何も保存しない。次に開く [GearDialog] で
 * 台数・シリアル・状態を確認してから保存する、既存の追加フローに合流させている。
 */
@Composable
private fun GearPresetDialog(
    onDismiss: () -> Unit,
    onSelect: (GearItem) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.gear_preset_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.gear_preset_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(GearPresets.ALL) { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(preset) }
                                .padding(vertical = 10.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "${preset.category.label} ／ ${preset.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.business_cancel))
            }
        },
    )
}

@Composable
private fun GearCard(item: GearItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${item.name}（${item.quantity}）",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = listOfNotNull(
                        item.category.label,
                        item.displayName.takeIf { it != item.name },
                        item.serial.takeIf { it.isNotBlank() }?.let { "S/N $it" },
                    ).joinToString(" ／ "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = item.status.label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (item.status.isUsable) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        }
    }
}

@Composable
private fun GearDialog(
    item: GearItem,
    onDismiss: () -> Unit,
    onSave: (GearItem) -> Unit,
    onDelete: () -> Unit,
) {
    var draft by remember(item.id) { mutableStateOf(item) }
    var quantityText by remember(item.id) { mutableStateOf(item.quantity.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.gear_add)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                // ダイアログの中身が縦に伸びるので、はみ出したらスクロールできるようにする
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                TextField(
                    value = draft.name,
                    onValueChange = { draft = draft.copy(name = it) },
                    label = stringResource(R.string.gear_name),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = draft.maker,
                        onValueChange = { draft = draft.copy(maker = it) },
                        label = stringResource(R.string.gear_maker),
                        modifier = Modifier.weight(1f),
                    )
                    TextField(
                        value = draft.modelName,
                        onValueChange = { draft = draft.copy(modelName = it) },
                        label = stringResource(R.string.gear_model),
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = draft.serial,
                        onValueChange = { draft = draft.copy(serial = it) },
                        label = stringResource(R.string.gear_serial),
                        modifier = Modifier.weight(1f),
                    )
                    TextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = stringResource(R.string.gear_quantity),
                        numeric = true,
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    for (category in GearCategory.entries) {
                        FilterChip(
                            selected = draft.category == category,
                            onClick = { draft = draft.copy(category = category) },
                            label = { Text(category.label) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (status in GearStatus.entries) {
                        FilterChip(
                            selected = draft.status == status,
                            onClick = { draft = draft.copy(status = status) },
                            label = { Text(status.label) },
                        )
                    }
                }
                TextField(
                    value = draft.note,
                    onValueChange = { draft = draft.copy(note = it) },
                    label = stringResource(R.string.gear_note),
                    singleLine = false,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        draft.copy(
                            name = draft.name.ifBlank { draft.displayName.ifBlank { "無題" } },
                            quantity = quantityText.toIntOrNull()?.coerceAtLeast(0) ?: 1,
                        ),
                    )
                },
            ) {
                Text(stringResource(R.string.business_save))
            }
        },
        dismissButton = {
            Row {
                if (item.id != 0L) {
                    TextButton(onClick = onDelete) {
                        Text(
                            text = stringResource(R.string.business_delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.business_cancel))
                }
            }
        },
    )
}
