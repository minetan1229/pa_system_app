package com.patoolbox.feature.calibration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.component.BigReadout
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.CalibrationMethod
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.ui.component.CalibrationBadge
import com.patoolbox.core.ui.component.KeepScreenOn
import com.patoolbox.core.ui.component.MicPermissionGate
import com.patoolbox.core.ui.component.PaToolScaffold
import com.patoolbox.core.ui.R as CoreUiR

@Composable
fun CalibrationScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalibrationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CalibrationScreen(
        uiState = uiState,
        onStart = viewModel::start,
        onStop = viewModel::stop,
        onReferenceChange = viewModel::onReferenceChange,
        onApplyManual = viewModel::applyManual,
        onApplyCalibrator = viewModel::applyCalibrator,
        onClear = viewModel::clear,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalibrationScreen(
    uiState: CalibrationUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onReferenceChange: (String) -> Unit,
    onApplyManual: () -> Unit,
    onApplyCalibrator: (Double) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current

    PaToolScaffold(
        tool = ToolId.SPL_METER,
        onBack = onBack,
        modifier = modifier,
        title = stringResource(R.string.calibration_title),
        subtitle = stringResource(R.string.calibration_role),
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
                Text(
                    text = stringResource(R.string.calibration_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = dimens.gutterSmall),
                )

                CalibrationBadge(profile = uiState.profile)

                Text(
                    text = stringResource(
                        R.string.calibration_current_offset,
                        "%.1f".format(uiState.profile.offsetDb),
                        methodLabel(uiState.profile.method),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (uiState.deviceLabel.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.calibration_device, uiState.deviceLabel),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                BigReadout(
                    value = if (uiState.hasReading) {
                        "%.1f".format(uiState.measuredDbFs)
                    } else {
                        "--.-"
                    },
                    unit = "dBFS",
                    label = stringResource(R.string.calibration_measured),
                    modifier = Modifier.padding(vertical = dimens.gutterSmall),
                )

                Button(
                    onClick = if (uiState.isMeasuring) onStop else onStart,
                    modifier = Modifier.fillMaxWidth(),
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

                uiState.error?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                uiState.message?.let { key ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    ) {
                        Text(
                            text = stringResource(messageRes(key)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = stringResource(R.string.calibration_manual_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.calibration_manual_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = uiState.referenceInput,
                    onValueChange = onReferenceChange,
                    singleLine = true,
                    label = { Text(stringResource(R.string.calibration_reference_label)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = onApplyManual,
                    enabled = uiState.hasReading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.calibration_apply))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = stringResource(R.string.calibration_calibrator_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.calibration_calibrator_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onApplyCalibrator(CalibrationViewModel.CALIBRATOR_94_DB) },
                        enabled = uiState.hasReading,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.calibration_apply_94))
                    }
                    OutlinedButton(
                        onClick = { onApplyCalibrator(CalibrationViewModel.CALIBRATOR_114_DB) },
                        enabled = uiState.hasReading,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.calibration_apply_114))
                    }
                }

                TextButton(
                    onClick = onClear,
                    modifier = Modifier.padding(bottom = dimens.gutter),
                ) {
                    Text(stringResource(R.string.calibration_clear))
                }
            }
        }
    }
}

private fun methodLabel(method: CalibrationMethod): String = when (method) {
    CalibrationMethod.NONE -> "未校正"
    CalibrationMethod.MANUAL -> "手動"
    CalibrationMethod.CALIBRATOR -> "校正器"
}

private fun messageRes(key: String): Int = when (key) {
    CalibrationViewModel.MESSAGE_SAVED -> R.string.calibration_saved
    CalibrationViewModel.MESSAGE_NEED_MEASURE -> R.string.calibration_need_measure
    else -> R.string.calibration_invalid_input
}
