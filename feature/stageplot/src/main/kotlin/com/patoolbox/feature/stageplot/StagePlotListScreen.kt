package com.patoolbox.feature.stageplot

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.StagePlot
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.ui.DateTimeText
import com.patoolbox.core.ui.component.PaToolScaffold

/**
 * イベント名プレフィックスの区切り文字。"イベント / 配置図名" の形式で保存する。
 * 区切り文字が無ければグループなし扱い。
 */
private const val EVENT_SEPARATOR = " / "

private data class PlotGroup(val eventName: String?, val plots: List<StagePlot>)

/** name から "(イベント名) / (配置図名)" を分解する */
private fun StagePlot.eventName(): String? {
    val idx = name.indexOf(EVENT_SEPARATOR)
    return if (idx > 0) name.substring(0, idx) else null
}

private fun StagePlot.displayName(): String {
    val idx = name.indexOf(EVENT_SEPARATOR)
    return if (idx > 0) name.substring(idx + EVENT_SEPARATOR.length) else name
}

/** イベント名でグループ化する。イベント名なしはまとめて末尾に置く */
private fun List<StagePlot>.groupedByEvent(): List<PlotGroup> {
    val withEvent = groupBy { it.eventName() }
    val sortedKeys = withEvent.keys
        .filterNotNull()
        .sorted()
    val noEvent = withEvent[null].orEmpty()
    return sortedKeys.map { key -> PlotGroup(key, withEvent[key]!!) } +
        if (noEvent.isNotEmpty()) listOf(PlotGroup(null, noEvent)) else emptyList()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StagePlotListScreen(
    onOpenPlot: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StagePlotListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dimens = LocalPaDimens.current
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by rememberSaveable { mutableStateOf<Long?>(null) }

    PaToolScaffold(
        tool = ToolId.STAGE_PLOT,
        onBack = onBack,
        modifier = modifier,
        title = stringResource(R.string.stageplot_title),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                text = { Text(stringResource(R.string.stageplot_add)) },
                icon = {},
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
                Text(
                    text = stringResource(R.string.stageplot_limit_reached, limit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = dimens.gutterSmall),
                )
            }

            if (uiState.plots.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = dimens.gutter),
                    verticalArrangement = Arrangement.spacedBy(dimens.spaceSm),
                ) {
                    Text(
                        text = stringResource(R.string.stageplot_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.stageplot_empty_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                return@Column
            }

            val groups = uiState.plots.groupedByEvent()

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(dimens.gutterSmall),
                modifier = Modifier.padding(vertical = dimens.gutterSmall),
            ) {
                groups.forEach { group ->
                    if (group.eventName != null) {
                        item(key = "header_${group.eventName}") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = dimens.spaceMd, bottom = dimens.spaceXs),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
                            ) {
                                Text(
                                    text = group.eventName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                            }
                        }
                    }
                    items(group.plots, key = { it.id }) { plot ->
                        PlotCard(
                            plot = plot,
                            onOpen = { onOpenPlot(plot.id) },
                            onDelete = { pendingDelete = plot.id },
                        )
                    }
                }
            }
        }

        if (showCreate) {
            CreateDialog(
                onDismiss = { showCreate = false },
                onCreate = { eventName, plotName ->
                    showCreate = false
                    val fullName = if (eventName.isNotBlank()) {
                        "$eventName$EVENT_SEPARATOR$plotName"
                    } else {
                        plotName
                    }
                    viewModel.create(fullName, onOpenPlot)
                },
            )
        }

        pendingDelete?.let { id ->
            val plot = uiState.plots.firstOrNull { it.id == id }
            if (plot == null) {
                pendingDelete = null
            } else {
                AlertDialog(
                    onDismissRequest = { pendingDelete = null },
                    title = { Text(stringResource(R.string.stageplot_delete)) },
                    text = {
                        Text(stringResource(R.string.stageplot_delete_confirm, plot.name))
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                pendingDelete = null
                                viewModel.delete(plot)
                            },
                        ) {
                            Text(
                                text = stringResource(R.string.stageplot_delete),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingDelete = null }) {
                            Text(stringResource(R.string.stageplot_cancel))
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun PlotCard(plot: StagePlot, onOpen: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plot.displayName(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(
                        R.string.stageplot_summary,
                        "%.1f".format(plot.stageWidthMeters),
                        "%.1f".format(plot.stageDepthMeters),
                        DateTimeText.formatDateTime(plot.updatedAtEpochMs),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onDelete) {
                Text(
                    text = stringResource(R.string.stageplot_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun CreateDialog(onDismiss: () -> Unit, onCreate: (eventName: String, plotName: String) -> Unit) {
    var eventName by rememberSaveable { mutableStateOf("") }
    var plotName by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.stageplot_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = eventName,
                    onValueChange = { eventName = it },
                    label = { Text(stringResource(R.string.stageplot_event_name)) },
                    placeholder = { Text(stringResource(R.string.stageplot_event_name_hint)) },
                    supportingText = { Text(stringResource(R.string.stageplot_event_name_note)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = plotName,
                    onValueChange = { plotName = it },
                    label = { Text(stringResource(R.string.stageplot_name)) },
                    placeholder = { Text(stringResource(R.string.stageplot_name_hint)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(eventName.trim(), plotName.trim()) },
                enabled = plotName.isNotBlank(),
            ) {
                Text(stringResource(R.string.stageplot_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.stageplot_cancel))
            }
        },
    )
}
