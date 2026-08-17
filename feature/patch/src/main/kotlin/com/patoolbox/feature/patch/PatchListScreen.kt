package com.patoolbox.feature.patch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.PatchSheet
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.ui.component.PaToolScaffold
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatchListScreen(
    onOpenSheet: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PatchListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dimens = LocalPaDimens.current

    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by rememberSaveable { mutableStateOf<Long?>(null) }

    PaToolScaffold(
        tool = ToolId.PATCH_SHEET,
        onBack = onBack,
        modifier = modifier,
        title = stringResource(R.string.patch_title),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                text = { Text(stringResource(R.string.patch_add)) },
                icon = {},
                expanded = true,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = dimens.gutter),
        ) {
            val limit = uiState.saveLimit
            if (limit != null && !uiState.canCreate) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = dimens.gutterSmall),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.patch_limit_reached, limit),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            if (uiState.sheets.isEmpty()) {
                Text(
                    text = stringResource(R.string.patch_empty),
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
                items(uiState.sheets, key = { it.id }) { sheet ->
                    SheetCard(
                        sheet = sheet,
                        onClick = { onOpenSheet(sheet.id) },
                        onDelete = { pendingDelete = sheet.id },
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateSheetDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, channels ->
                showCreateDialog = false
                viewModel.create(name, channels) { id -> onOpenSheet(id) }
            },
        )
    }

    pendingDelete?.let { id ->
        val sheet = uiState.sheets.firstOrNull { it.id == id }
        if (sheet == null) {
            pendingDelete = null
        } else {
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text(stringResource(R.string.patch_delete)) },
                text = { Text(stringResource(R.string.patch_delete_confirm, sheet.name)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pendingDelete = null
                            viewModel.delete(sheet)
                        },
                    ) { Text(stringResource(R.string.patch_delete)) }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) {
                        Text(stringResource(R.string.patch_cancel))
                    }
                },
            )
        }
    }
}

@Composable
private fun SheetCard(
    sheet: PatchSheet,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sheet.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(
                        R.string.patch_updated,
                        formatDateTime(sheet.updatedAtEpochMs),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onDelete) {
                Text(stringResource(R.string.patch_delete))
            }
        }
    }
}

@Composable
private fun CreateSheetDialog(
    onDismiss: () -> Unit,
    onCreate: (String, Int) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var channels by rememberSaveable {
        mutableStateOf(PatchListViewModel.DEFAULT_CHANNELS.toString())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.patch_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.patch_new_name)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = channels,
                    onValueChange = { channels = it },
                    label = { Text(stringResource(R.string.patch_new_channels)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreate(
                        name,
                        channels.toIntOrNull() ?: PatchListViewModel.DEFAULT_CHANNELS,
                    )
                },
            ) { Text(stringResource(R.string.patch_create)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.patch_cancel))
            }
        },
    )
}

private val dateFormat: DateFormat =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)

internal fun formatDateTime(epochMs: Long): String =
    if (epochMs <= 0L) "--" else dateFormat.format(Date(epochMs))
