package com.patoolbox.feature.rta

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.dsp.BandResolution
import com.patoolbox.core.dsp.FrequencyWeighting
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.ui.component.CalibrationBadge
import com.patoolbox.core.ui.component.KeepScreenOn
import com.patoolbox.core.ui.component.MicPermissionGate
import com.patoolbox.core.ui.component.PaToolScaffold
import com.patoolbox.core.ui.R as CoreUiR

@Composable
fun RtaScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RtaViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RtaScreen(
        uiState = uiState,
        onStart = viewModel::start,
        onStop = viewModel::stop,
        onResolution = viewModel::setResolution,
        onWeighting = viewModel::setWeighting,
        onAveraging = viewModel::setAveraging,
        onTogglePeakHold = viewModel::togglePeakHold,
        onClearPeaks = viewModel::clearPeaks,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RtaScreen(
    uiState: RtaUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onResolution: (BandResolution) -> Unit,
    onWeighting: (FrequencyWeighting) -> Unit,
    onAveraging: (RtaAveraging) -> Unit,
    onTogglePeakHold: () -> Unit,
    onClearPeaks: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current

    PaToolScaffold(
        tool = ToolId.RTA,
        onBack = onBack,
        modifier = modifier,
        title = stringResource(R.string.rta_title),
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
                    Text(
                        text = if (uiState.hasReading) {
                            stringResource(R.string.rta_total, "%.1f".format(uiState.totalDb))
                        } else {
                            stringResource(R.string.rta_waiting)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                RtaChart(
                    bands = uiState.bands,
                    showPeaks = uiState.peakHold,
                )

                uiState.error?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Text(
                    text = stringResource(R.string.rta_resolution),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BandResolution.entries.forEach { resolution ->
                        val locked = resolution.requiresPro && !uiState.proStatus.isPro
                        ToggleButton(
                            text = resolution.displayName,
                            selected = resolution == uiState.resolution,
                            enabled = !locked,
                            onClick = { onResolution(resolution) },
                            minTouch = dimens.minTouch,
                        )
                    }
                }
                if (!uiState.proStatus.isPro) {
                    Text(
                        text = stringResource(R.string.rta_pro_required),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Text(
                    text = stringResource(R.string.rta_weighting),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FrequencyWeighting.entries.forEach { weighting ->
                        ToggleButton(
                            text = weighting.displayName,
                            selected = weighting == uiState.weighting,
                            onClick = { onWeighting(weighting) },
                            minTouch = dimens.minTouch,
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.rta_averaging),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RtaAveraging.entries.forEach { averaging ->
                        ToggleButton(
                            text = stringResource(averaging.labelRes()),
                            selected = averaging == uiState.averaging,
                            onClick = { onAveraging(averaging) },
                            minTouch = dimens.minTouch,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.rta_peak_hold),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = uiState.peakHold,
                        onCheckedChange = { onTogglePeakHold() },
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = dimens.gutter),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = if (uiState.isMeasuring) onStop else onStart,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = dimens.minTouch),
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
                    OutlinedButton(
                        onClick = onClearPeaks,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = dimens.minTouch),
                    ) {
                        Text(stringResource(CoreUiR.string.measure_reset))
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    minTouch: Dp,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (selected) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.heightIn(min = minTouch),
        ) { Text(text) }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.heightIn(min = minTouch),
        ) { Text(text) }
    }
}

private fun RtaAveraging.labelRes(): Int = when (this) {
    RtaAveraging.FAST -> R.string.rta_avg_fast
    RtaAveraging.NORMAL -> R.string.rta_avg_normal
    RtaAveraging.SLOW -> R.string.rta_avg_slow
}
