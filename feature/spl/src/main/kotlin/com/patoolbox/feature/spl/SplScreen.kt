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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.component.BigReadout
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.dsp.FrequencyWeighting
import com.patoolbox.core.dsp.TimeWeighting
import com.patoolbox.core.model.CalibrationConfidence
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.ui.component.CalibrationBadge
import com.patoolbox.core.ui.component.InputSourceBadge
import com.patoolbox.core.ui.component.KeepScreenOn
import com.patoolbox.core.ui.component.MicPermissionGate
import com.patoolbox.core.ui.component.PaToolScaffold
import com.patoolbox.core.ui.R as CoreUiR

@Composable
fun SplScreen(
    onBack: () -> Unit,
    onOpenCalibration: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SplViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var recordTitle by rememberSaveable { mutableStateOf("") }

    SplScreen(
        uiState = uiState,
        onStart = viewModel::start,
        onStop = viewModel::stop,
        onReset = viewModel::reset,
        onFrequencyWeighting = viewModel::setFrequencyWeighting,
        onTimeWeighting = viewModel::setTimeWeighting,
        onReadoutAveraging = viewModel::setReadoutAveraging,
        onToggleLogging = {
            if (uiState.isLogging) showSaveDialog = true else viewModel.startLogging()
        },
        onBack = onBack,
        onOpenCalibration = onOpenCalibration,
        modifier = modifier,
    )

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text(stringResource(R.string.spl_record_stop)) },
            text = {
                OutlinedTextField(
                    value = recordTitle,
                    onValueChange = { recordTitle = it },
                    label = { Text(stringResource(R.string.spl_record_title)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSaveDialog = false
                        viewModel.stopLoggingAndSave(recordTitle)
                        recordTitle = ""
                    },
                ) { Text(stringResource(R.string.spl_record_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text(stringResource(R.string.spl_record_cancel))
                }
            },
        )
    }
}

@Composable
internal fun SplScreen(
    uiState: SplUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    onFrequencyWeighting: (FrequencyWeighting) -> Unit,
    onTimeWeighting: (TimeWeighting) -> Unit,
    onReadoutAveraging: (ReadoutAveraging) -> Unit,
    onToggleLogging: () -> Unit,
    onBack: () -> Unit,
    onOpenCalibration: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current

    PaToolScaffold(
        tool = ToolId.SPL_METER,
        onBack = onBack,
        modifier = modifier,
        title = stringResource(R.string.spl_title),
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

                // 大表示は既定で 0.5 秒平均。Fast のままだと数字が毎秒何度も跳ねて、
                // 卓から目を上げた一瞬では読めない。記録側（Leq/Lmax）は規格どおりのまま
                BigReadout(
                    value = if (uiState.hasReading) {
                        formatDb(uiState.readoutDb)
                    } else {
                        stringResource(R.string.spl_no_value)
                    },
                    unit = uiState.frequencyWeighting.displayName,
                    label = "${uiState.timeWeighting.label} / ${uiState.frequencyWeighting.displayName}",
                    caption = if (uiState.hasReading) {
                        val elapsed = formatElapsed(uiState.elapsedSeconds)
                        if (uiState.readoutAveraging == ReadoutAveraging.INSTANT) {
                            stringResource(R.string.spl_readout_caption_instant, elapsed)
                        } else {
                            stringResource(
                                R.string.spl_readout_caption,
                                uiState.readoutAveraging.label,
                                elapsed,
                            )
                        }
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

                SelectorRow(
                    label = stringResource(R.string.spl_readout_averaging),
                    options = ReadoutAveraging.entries.map { it to it.label },
                    selected = uiState.readoutAveraging,
                    onSelect = onReadoutAveraging,
                    minTouch = dimens.minTouch,
                )
                Text(
                    text = stringResource(R.string.spl_readout_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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

                // 記録は Pro。押せない理由が分かるよう、隠さず無効で出す
                Button(
                    onClick = onToggleLogging,
                    enabled = uiState.canLog || uiState.isLogging,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            when {
                                uiState.isLogging -> R.string.spl_record_stop
                                uiState.proStatus.isPro -> R.string.spl_record_start
                                else -> R.string.spl_record_pro
                            },
                        ),
                    )
                }
                if (uiState.isLogging) {
                    Text(
                        text = stringResource(R.string.spl_record_count, uiState.loggedSamples),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                uiState.savedMessage?.let {
                    Text(
                        text = stringResource(R.string.spl_record_saved),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
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
