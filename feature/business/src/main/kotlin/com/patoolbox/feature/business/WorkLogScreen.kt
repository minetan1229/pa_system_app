package com.patoolbox.feature.business

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.patoolbox.core.calc.RateType
import com.patoolbox.core.calc.WorkLogCalculator
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.WorkLogEntry
import com.patoolbox.core.ui.DateTimeText

/**
 * 稼働記録。
 *
 * 撤収が翌日になる現場が普通にあるので、終了時刻が開始より小さければ
 * 日をまたいだものとして扱う（[WorkLogCalculator]）。
 */
@Composable
fun WorkLogScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkLogViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dimens = LocalPaDimens.current
    var editing by remember { mutableStateOf<WorkLogEntry?>(null) }

    BusinessScaffold(
        title = stringResource(R.string.worklog_title),
        onBack = onBack,
        proStatus = uiState.proStatus,
        modifier = modifier,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editing = WorkLogEntry(dateEpochMs = System.currentTimeMillis())
                },
                text = { Text(stringResource(R.string.worklog_add)) },
                icon = {},
            )
        },
    ) { contentModifier ->
        Column(modifier = contentModifier) {
            val total = uiState.total
            Text(
                text = stringResource(
                    R.string.worklog_total,
                    uiState.entries.size,
                    total.hoursLabel,
                    formatYen(total.amount),
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = dimens.gutterSmall),
            )

            if (uiState.entries.isEmpty()) {
                Text(
                    text = stringResource(R.string.worklog_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(dimens.gutterSmall),
                modifier = Modifier.padding(bottom = dimens.gutter),
            ) {
                items(uiState.entries, key = { it.id }) { entry ->
                    val summary = uiState.summaryOf(entry)
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { editing = entry },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = entry.title.ifBlank {
                                    DateTimeText.formatDate(entry.dateEpochMs)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(
                                    R.string.worklog_row,
                                    WorkLogCalculator.formatTimeOfDay(entry.startMinutesOfDay),
                                    WorkLogCalculator.formatTimeOfDay(entry.endMinutesOfDay),
                                    summary.hoursLabel,
                                    formatYen(summary.amount),
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    editing?.let { entry ->
        WorkLogDialog(
            entry = entry,
            onDismiss = { editing = null },
            onSave = {
                viewModel.save(it)
                editing = null
            },
            onDelete = {
                viewModel.delete(entry)
                editing = null
            },
        )
    }
}

@Composable
private fun WorkLogDialog(
    entry: WorkLogEntry,
    onDismiss: () -> Unit,
    onSave: (WorkLogEntry) -> Unit,
    onDelete: () -> Unit,
) {
    var draft by remember(entry.id) { mutableStateOf(entry) }
    var start by remember(entry.id) {
        mutableStateOf(WorkLogCalculator.formatTimeOfDay(entry.startMinutesOfDay))
    }
    var end by remember(entry.id) {
        mutableStateOf(WorkLogCalculator.formatTimeOfDay(entry.endMinutesOfDay))
    }
    var breakText by remember(entry.id) { mutableStateOf(entry.breakMinutes.toString()) }
    var rateText by remember(entry.id) { mutableStateOf(entry.rate.toString()) }
    var transportText by remember(entry.id) { mutableStateOf(entry.transportFee.toString()) }
    var multiplierText by remember(entry.id) { mutableStateOf(entry.multiplier.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.worklog_add)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                TextField(
                    value = draft.title,
                    onValueChange = { draft = draft.copy(title = it) },
                    label = stringResource(R.string.worklog_name),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = start,
                        onValueChange = { start = it },
                        label = stringResource(R.string.worklog_start),
                        modifier = Modifier.weight(1f),
                    )
                    TextField(
                        value = end,
                        onValueChange = { end = it },
                        label = stringResource(R.string.worklog_end),
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    text = stringResource(R.string.worklog_overnight),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = breakText,
                        onValueChange = { breakText = it },
                        label = stringResource(R.string.worklog_break),
                        numeric = true,
                        modifier = Modifier.weight(1f),
                    )
                    TextField(
                        value = multiplierText,
                        onValueChange = { multiplierText = it },
                        label = stringResource(R.string.worklog_multiplier),
                        numeric = true,
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (type in RateType.entries) {
                        FilterChip(
                            selected = draft.rateTypeName == type.name,
                            onClick = { draft = draft.copy(rateTypeName = type.name) },
                            label = { Text(type.label) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = rateText,
                        onValueChange = { rateText = it },
                        label = stringResource(R.string.worklog_rate),
                        numeric = true,
                        modifier = Modifier.weight(1f),
                    )
                    TextField(
                        value = transportText,
                        onValueChange = { transportText = it },
                        label = stringResource(R.string.worklog_transport),
                        numeric = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    text = stringResource(R.string.worklog_transport_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        draft.copy(
                            startMinutesOfDay = WorkLogCalculator.parseTimeOfDay(start)
                                ?: draft.startMinutesOfDay,
                            endMinutesOfDay = WorkLogCalculator.parseTimeOfDay(end)
                                ?: draft.endMinutesOfDay,
                            breakMinutes = breakText.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                            rate = rateText.toLongOrNull()?.coerceAtLeast(0) ?: 0,
                            transportFee = transportText.toLongOrNull()?.coerceAtLeast(0) ?: 0,
                            multiplier = multiplierText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 1.0,
                            dateEpochMs = draft.dateEpochMs.takeIf { it > 0 }
                                ?: System.currentTimeMillis(),
                        ),
                    )
                },
            ) {
                Text(stringResource(R.string.business_save))
            }
        },
        dismissButton = {
            Row {
                if (entry.id != 0L) {
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
