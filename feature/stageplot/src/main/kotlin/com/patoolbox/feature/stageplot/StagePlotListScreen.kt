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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.StagePlot
import com.patoolbox.core.ui.DateTimeText
import com.patoolbox.core.ui.R as CoreUiR

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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stageplot_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(CoreUiR.string.back))
                    }
                },
            )
        },
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
                Text(
                    text = stringResource(R.string.stageplot_empty),
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
                items(uiState.plots, key = { it.id }) { plot ->
                    PlotCard(
                        plot = plot,
                        onOpen = { onOpenPlot(plot.id) },
                        onDelete = { pendingDelete = plot.id },
                    )
                }
            }
        }

        if (showCreate) {
            CreateDialog(
                onDismiss = { showCreate = false },
                onCreate = { name ->
                    showCreate = false
                    viewModel.create(name, onOpenPlot)
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
                    text = plot.name,
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
private fun CreateDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.stageplot_add)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.stageplot_name)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name) }) {
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
