package com.patoolbox.feature.spl

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.patoolbox.core.model.Measurement
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.ui.DateTimeText
import com.patoolbox.core.ui.component.PaToolScaffold
import com.patoolbox.core.ui.R as CoreUiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplLogScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SplLogViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dimens = LocalPaDimens.current
    val context = LocalContext.current
    var pendingExport by remember { mutableStateOf<Measurement?>(null) }

    val createCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(CSV_MIME_TYPE),
    ) { uri ->
        val measurement = pendingExport
        pendingExport = null
        if (uri != null && measurement != null) {
            context.contentResolver.openOutputStream(uri)?.let { stream ->
                viewModel.exportCsv(measurement, stream)
            }
        }
    }

    PaToolScaffold(
        tool = ToolId.SPL_LOGGER,
        onBack = onBack,
        modifier = modifier,
        title = stringResource(R.string.spl_log_title),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = dimens.gutter),
        ) {
            if (uiState.measurements.isEmpty()) {
                Text(
                    text = stringResource(R.string.spl_log_empty),
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
                items(uiState.measurements, key = { it.id }) { measurement ->
                    MeasurementCard(
                        measurement = measurement,
                        onExport = {
                            pendingExport = measurement
                            createCsvLauncher.launch(viewModel.suggestedFileName(measurement))
                        },
                        onDelete = { viewModel.delete(measurement) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MeasurementCard(
    measurement: Measurement,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = measurement.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${DateTimeText.formatDateTime(measurement.startedAtEpochMs)} / " +
                    "${measurement.durationSeconds / 60}分 / ${measurement.weightingLabel}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    R.string.spl_log_summary,
                    "%.1f".format(measurement.leqDb),
                    "%.1f".format(measurement.maxDb),
                    "%.1f".format(measurement.minDb),
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // 未校正やクリップは記録として重要なので、一覧の時点で見えるようにする
            if (measurement.isUncalibrated) {
                Text(
                    text = stringResource(R.string.spl_log_uncalibrated),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (measurement.clipped) {
                Text(
                    text = stringResource(CoreUiR.string.measure_clipping),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onExport) {
                    Text(stringResource(R.string.spl_log_export))
                }
                TextButton(onClick = onDelete) {
                    Text(
                        text = stringResource(R.string.spl_log_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

private const val CSV_MIME_TYPE = "text/csv"
