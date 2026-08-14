package com.patoolbox.feature.analyzer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.component.BigReadout
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.dsp.OctaveSmoothing
import com.patoolbox.core.ui.component.KeepScreenOn
import com.patoolbox.core.ui.component.MicPermissionGate
import com.patoolbox.core.ui.R as CoreUiR

/**
 * FFT アナライザ。
 *
 * RTA が「帯域ごとにどれだけ出ているか」を見る道具なのに対して、
 * こちらは「その山が何 Hz なのか」を1Hz 単位で当てる道具。
 * ハウリングの芽、電源ハムの次数、共振の中心を特定するときに使う。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FftScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnalyzerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dimens = LocalPaDimens.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.fft_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(CoreUiR.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        if (!uiState.proStatus.isPro) {
            AnalyzerProNotice(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }

        MicPermissionGate(modifier = Modifier.padding(innerPadding)) {
            KeepScreenOn(enabled = uiState.isMeasuring)

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = dimens.gutter),
                verticalArrangement = Arrangement.spacedBy(dimens.gutterSmall),
            ) {
                BigReadout(
                    value = if (uiState.hasReading) {
                        "%.0f".format(uiState.peakFrequencyHz)
                    } else {
                        "----"
                    },
                    unit = if (uiState.hasReading) "Hz" else null,
                    label = if (uiState.hasReading) {
                        "%.1f %s".format(uiState.peakLevelDb, uiState.unitLabel)
                    } else {
                        null
                    },
                    caption = stringResource(R.string.fft_peak_caption),
                    modifier = Modifier.padding(vertical = dimens.gutterSmall),
                )

                SpectrumChart(
                    columnsDb = uiState.columnsDb,
                    peakHoldDb = uiState.peakHoldDb,
                    frequencies = uiState.frequencies,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    text = stringResource(
                        R.string.fft_axis,
                        uiState.fftSize.detail,
                        uiState.unitLabel,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                uiState.error?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (smoothing in OctaveSmoothing.entries) {
                        FilterChip(
                            selected = uiState.smoothing == smoothing,
                            onClick = { viewModel.setSmoothing(smoothing) },
                            label = { Text(smoothing.label) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (size in AnalyzerFftSize.entries) {
                        FilterChip(
                            selected = uiState.fftSize == size,
                            onClick = { viewModel.setFftSize(size) },
                            label = { Text(size.label) },
                        )
                    }
                    FilterChip(
                        selected = uiState.peakHold,
                        onClick = viewModel::togglePeakHold,
                        label = { Text(stringResource(R.string.analyzer_peak_hold)) },
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = viewModel::toggle,
                        modifier = Modifier.weight(1f).heightIn(min = dimens.minTouch),
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
                    if (uiState.peakHold) {
                        OutlinedButton(
                            onClick = viewModel::clearPeaks,
                            modifier = Modifier.heightIn(min = dimens.minTouch),
                        ) {
                            Text(stringResource(R.string.analyzer_clear_peaks))
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.fft_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = dimens.gutter),
                )
            }
        }
    }
}

@Composable
internal fun AnalyzerProNotice(modifier: Modifier = Modifier) {
    val dimens = LocalPaDimens.current
    Column(modifier = modifier.fillMaxSize().padding(dimens.gutter)) {
        Text(
            text = stringResource(R.string.analyzer_pro),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.analyzer_pro_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
}
