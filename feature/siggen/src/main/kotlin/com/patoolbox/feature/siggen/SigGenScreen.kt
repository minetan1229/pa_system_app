package com.patoolbox.feature.siggen

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.component.BigReadout
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.ui.R as CoreUiR
import kotlin.math.exp
import kotlin.math.ln

@Composable
fun SigGenScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SigGenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SigGenScreen(
        uiState = uiState,
        onTogglePlay = viewModel::togglePlay,
        onWaveform = viewModel::setWaveform,
        onFrequency = viewModel::setFrequency,
        onLevel = viewModel::setLevel,
        onBurst = viewModel::setBurst,
        onPreset1k = viewModel::applyPreset1kHz,
        onPresetPink = viewModel::applyPresetPink,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SigGenScreen(
    uiState: SigGenUiState,
    onTogglePlay: () -> Unit,
    onWaveform: (Waveform) -> Unit,
    onFrequency: (Double) -> Unit,
    onLevel: (Double) -> Unit,
    onBurst: (Boolean) -> Unit,
    onPreset1k: () -> Unit,
    onPresetPink: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.siggen_title)) },
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.gutter),
            verticalArrangement = Arrangement.spacedBy(dimens.gutterSmall),
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Text(
                    text = stringResource(R.string.siggen_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp),
                )
            }

            BigReadout(
                value = if (uiState.waveform.hasFrequency) {
                    formatFrequency(uiState.frequencyHz)
                } else {
                    "%.0f".format(uiState.levelDbFs)
                },
                unit = if (uiState.waveform.hasFrequency) "Hz" else "dBFS",
                label = stringResource(uiState.waveform.labelRes()),
                caption = "%.0f dBFS".format(uiState.levelDbFs),
                modifier = Modifier.padding(vertical = dimens.gutterSmall),
            )

            uiState.error?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Text(
                text = stringResource(R.string.siggen_waveform),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Waveform.entries.forEach { waveform ->
                    val label = stringResource(waveform.labelRes())
                    if (waveform == uiState.waveform) {
                        Button(
                            onClick = { onWaveform(waveform) },
                            modifier = Modifier.heightIn(min = dimens.minTouch),
                        ) { Text(label) }
                    } else {
                        OutlinedButton(
                            onClick = { onWaveform(waveform) },
                            modifier = Modifier.heightIn(min = dimens.minTouch),
                        ) { Text(label) }
                    }
                }
            }

            if (uiState.waveform.hasFrequency) {
                Text(
                    text = stringResource(R.string.siggen_frequency),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // 周波数は対数で動かす（低域が触れないと使い物にならない）
                Slider(
                    value = frequencyToSlider(uiState.frequencyHz).toFloat(),
                    onValueChange = { onFrequency(sliderToFrequency(it.toDouble())) },
                    modifier = Modifier.heightIn(min = dimens.minTouch),
                )
            }

            if (uiState.waveform == Waveform.LOG_SWEEP ||
                uiState.waveform == Waveform.LINEAR_SWEEP
            ) {
                Text(
                    text = stringResource(
                        R.string.siggen_sweep_range,
                        "%.0f".format(uiState.sweepSeconds),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = stringResource(R.string.siggen_level),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = uiState.levelDbFs.toFloat(),
                onValueChange = { onLevel(it.toDouble()) },
                valueRange = SigGenUiState.MIN_LEVEL.toFloat()..SigGenUiState.MAX_LEVEL.toFloat(),
                modifier = Modifier.heightIn(min = dimens.minTouch),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.siggen_burst),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = uiState.burst, onCheckedChange = onBurst)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPreset1k, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.siggen_preset_1k))
                }
                OutlinedButton(onClick = onPresetPink, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.siggen_preset_pink))
                }
            }

            Button(
                onClick = onTogglePlay,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = dimens.minTouch)
                    .padding(bottom = dimens.gutter),
            ) {
                Text(
                    stringResource(
                        if (uiState.isPlaying) R.string.siggen_stop else R.string.siggen_play,
                    ),
                )
            }
        }
    }
}

private fun Waveform.labelRes(): Int = when (this) {
    Waveform.SINE -> R.string.siggen_sine
    Waveform.PINK_NOISE -> R.string.siggen_pink
    Waveform.WHITE_NOISE -> R.string.siggen_white
    Waveform.SQUARE -> R.string.siggen_square
    Waveform.LOG_SWEEP -> R.string.siggen_log_sweep
    Waveform.LINEAR_SWEEP -> R.string.siggen_lin_sweep
    Waveform.DUAL_TONE -> R.string.siggen_dual_tone
}

private fun frequencyToSlider(hz: Double): Double {
    val min = ln(SigGenUiState.MIN_FREQUENCY)
    val max = ln(SigGenUiState.MAX_FREQUENCY)
    return ((ln(hz) - min) / (max - min)).coerceIn(0.0, 1.0)
}

private fun sliderToFrequency(position: Double): Double {
    val min = ln(SigGenUiState.MIN_FREQUENCY)
    val max = ln(SigGenUiState.MAX_FREQUENCY)
    return exp(min + position.coerceIn(0.0, 1.0) * (max - min))
}

internal fun formatFrequency(hz: Double): String = when {
    hz >= 10000 -> "%.1fk".format(hz / 1000)
    hz >= 1000 -> "%.2fk".format(hz / 1000)
    hz >= 100 -> "%.0f".format(hz)
    else -> "%.1f".format(hz)
}
