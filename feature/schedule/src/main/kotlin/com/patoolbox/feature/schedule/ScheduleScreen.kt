package com.patoolbox.feature.schedule

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.TimelineEntry
import com.patoolbox.core.ui.DateTimeText
import com.patoolbox.core.ui.R as CoreUiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dimens = LocalPaDimens.current
    var showAdd by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(uiState.job?.name ?: stringResource(R.string.schedule_title))
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(CoreUiR.string.back))
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
            Text(
                text = if (uiState.hasAnchor) {
                    stringResource(
                        R.string.schedule_anchor_start,
                        DateTimeText.formatTime(uiState.anchorEpochMs),
                    )
                } else {
                    stringResource(R.string.schedule_no_anchor)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    R.string.schedule_total,
                    uiState.totalMinutes / 60,
                    uiState.totalMinutes % 60,
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            uiState.overrunning.forEach { entry ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.schedule_overrun, entry.item.title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            Text(
                text = stringResource(R.string.schedule_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp),
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(uiState.entries, key = { it.item.id }) { entry ->
                    ScheduleRow(
                        entry = entry,
                        onUp = { viewModel.move(entry.item, -1) },
                        onDown = { viewModel.move(entry.item, 1) },
                        onDelete = { viewModel.delete(entry.item) },
                    )
                }
                item {
                    OutlinedButton(
                        onClick = { showAdd = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = dimens.gutterSmall),
                    ) { Text(stringResource(R.string.schedule_add)) }
                }
            }
        }
    }

    if (showAdd) {
        AddItemDialog(
            eventDateEpochMs = uiState.job?.eventDateEpochMs,
            onDismiss = { showAdd = false },
            onAdd = { title, minutes, owner, fixedStart ->
                showAdd = false
                viewModel.add(title, minutes, owner, fixedStart)
            },
        )
    }
}

@Composable
private fun ScheduleRow(
    entry: TimelineEntry,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (entry.isAnchor) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        R.string.schedule_range,
                        DateTimeText.formatTime(entry.startAtEpochMs),
                        DateTimeText.formatTime(entry.endAtEpochMs),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (entry.isAnchor) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
                Text(
                    text = entry.item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (entry.item.owner.isNotBlank() || entry.isAnchor) {
                    Text(
                        text = buildString {
                            append(entry.item.owner)
                            if (entry.isAnchor) {
                                if (isNotEmpty()) append(" / ")
                                append(stringResource(R.string.schedule_anchor_label))
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TextButton(onClick = onUp) { Text(stringResource(R.string.schedule_up)) }
            TextButton(onClick = onDown) { Text(stringResource(R.string.schedule_down)) }
            TextButton(onClick = onDelete) {
                Text(
                    text = stringResource(R.string.schedule_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun AddItemDialog(
    eventDateEpochMs: Long?,
    onDismiss: () -> Unit,
    onAdd: (String, Int, String, Long?) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableStateOf("30") }
    var owner by rememberSaveable { mutableStateOf("") }
    var fixedTime by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.schedule_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.schedule_item_title)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { minutes = it },
                    label = { Text(stringResource(R.string.schedule_duration)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = owner,
                    onValueChange = { owner = it },
                    label = { Text(stringResource(R.string.schedule_owner)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = fixedTime,
                    onValueChange = { fixedTime = it },
                    label = { Text(stringResource(R.string.schedule_fixed_time)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val date = eventDateEpochMs?.let {
                        DateTimeText.parseDate(DateTimeText.formatDate(it))
                    }
                    val fixedStart = DateTimeText.parseTime(fixedTime)
                        ?.let { DateTimeText.toEpochMs(date, it) }
                    onAdd(title, minutes.toIntOrNull() ?: 0, owner, fixedStart)
                },
            ) { Text(stringResource(R.string.schedule_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(CoreUiR.string.back))
            }
        },
    )
}
