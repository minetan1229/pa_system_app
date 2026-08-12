package com.patoolbox.feature.spl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.component.BigReadout
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.dsp.FrequencyWeighting
import com.patoolbox.core.dsp.TimeWeighting
import com.patoolbox.core.ui.component.CalibrationBadge
import com.patoolbox.core.ui.component.InputSourceBadge
import com.patoolbox.core.ui.component.KeepScreenOn
import com.patoolbox.core.ui.component.MicPermissionGate
import com.patoolbox.core.model.CalibrationConfidence
import com.patoolbox.core.ui.R as CoreUiR

@Composable
fun SplScreen(
    onBack: () -> Unit,
    onOpenCalibration: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SplViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SplScreen(
        uiState = uiState,
        onStart = viewModel::start,
        onStop = viewModel::stop,
        onReset = viewModel::reset,
        onFrequencyWeighting = viewModel::setFrequencyWeighting,
        onTimeWeighting = viewModel::setTimeWeighting,
        onBack = onBack,
        onOpenCalibration = onOpenCalibration,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SplScreen(
    uiState: SplUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    onFrequencyWeighting: (FrequencyWeighting) -> Unit,
    onTimeWeighting: (TimeWeighting) -> Unit,
    onBack: () -> Unit,
    onOpenCalibration: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.spl_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(CoreUiR.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        MicPermissionGate(modifier = Modifier.padding(innerPadding)) {
            KeepScreenOn(enabled = uiState.isMeasuring)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = dimens.gutter),
                verticalArrangement = Arrangement.spacedBy(dimens.gutterSmall),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CalibrationBadge(profile = uiState.calibration)
                    if (uiState.inputSourceLabel.isNotEmpty()) {
                        InputSourceBadge(
                            sourceLabel = uiState.inputSourceLabel,
                            isMeasurementGrade = uiState.inputSourceIsMeasurementGrade,
                        )
                    }
                }

                BigReadout(
                    value = if (uiState.hasReading) {
                        formatDb(uiState.instantDb)
                    } else {
                        stringResource(R.string.spl_no_value)
                    },
                    unit = uiState.frequencyWeighting.displayName,
                    label = "${uiState.timeWeighting.label} / ${uiState.frequencyWeighting.displayName}",
                    caption = if (uiState.hasReading) {
                        stringResource(
                            CoreUiR.string.measure_elapsed,
                            formatElapsed(uiState.elapsedSeconds),
                        )
                    } else {
                        stringResource(R.string.spl_waiting)
                    },
                    modifier = Modifier.padding(vertical = dimens.gutterSmall),
                )

                if (uiState.clipped) {
                    WarningCard(text = stringResource(CoreUiR.string.measure_clipping))
                }
                uiState.error?.let { WarningCard(text = it) }

                StatRow(
                    values = listOf(
                        stringResource(R.string.spl_leq) to uiState.leqDb,
                        stringResource(R.string.spl_max) to uiState.maxDb,
                        stringResource(R.string.spl_min) to uiState.minDb,
                        stringResource(R.string.spl_peak) to uiState.peakDb,
                    ),
                    hasReading = uiState.hasReading,
                )
                StatRow(
                    values = listOf(
                        stringResource(R.string.spl_l10) to uiState.l10Db,
                        stringResource(R.string.spl_l50) to uiState.l50Db,
                        stringResource(R.string.spl_l90) to uiState.l90Db,
                    ),
                    hasReading = uiState.hasReading,
                )

                SelectorRow(
                    label = stringResource(R.string.spl_weighting),
                    options = FrequencyWeighting.entries.map { it to it.displayName },
                    selected = uiState.frequencyWeighting,
                    onSelect = onFrequencyWeighting,
                    minTouch = dimens.minTouch,
                )

                SelectorRow(
                    label = stringResource(R.string.spl_time_weighting),
                    options = TimeWeighting.entries.map { it to it.label },
                    selected = uiState.timeWeighting,
                    onSelect = onTimeWeighting,
                    minTouch = dimens.minTouch,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = dimens.gutterSmall),
                    horizontalArrangement = Arrangement.spacedBy(dimens.gutterSmall),
                ) {
                    Button(
                        onClick = if (uiState.isMeasuring) onStop else onStart,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            stringResource(
                                if (uiState.isMeasuring) {
                                    CoreUiR.string.measure_stop
                                } else {
                                    CoreUiR.string.measure_start
                                },
                            ),
                        )
                    }
                    OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) {
                        Text(stringResource(CoreUiR.string.measure_reset))
                    }
                }

                if (uiState.calibration.confidence == CalibrationConfidence.UNCALIBRATED) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.spl_uncalibrated_note),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(onClick = onOpenCalibration) {
                                Text(stringResource(R.string.spl_calibrate))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(
    values: List<Pair<String, Double>>,
    hasReading: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        values.forEach { (label, db) ->
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = if (hasReading) {
                            formatDb(db)
                        } else {
                            stringResource(R.string.spl_no_value)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> SelectorRow(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    minTouch: Dp,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, text) ->
                if (value == selected) {
                    Button(
                        onClick = { onSelect(value) },
                        modifier = Modifier.heightIn(min = minTouch),
                    ) { Text(text) }
                } else {
                    OutlinedButton(
                        onClick = { onSelect(value) },
                        modifier = Modifier.heightIn(min = minTouch),
                    ) { Text(text) }
                }
            }
        }
    }
}

@Composable
private fun WarningCard(text: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp),
        )
    }
}

/** 有限でない値は「--.-」相当にする（測定前や無音時に -Infinity を出さない）。 */
internal fun formatDb(db: Double): String =
    if (db.isFinite()) "%.1f".format(db) else "--.-"

internal fun formatElapsed(seconds: Double): String {
    val total = seconds.toInt()
    return "%02d:%02d".format(total / 60, total % 60)
}
