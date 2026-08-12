package com.patoolbox.feature.metronome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.component.BigReadout
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.ui.component.KeepScreenOn
import com.patoolbox.core.ui.R as CoreUiR

@Composable
fun MetronomeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MetronomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MetronomeScreen(
        uiState = uiState,
        onToggle = viewModel::toggle,
        onAdjustBpm = viewModel::adjustBpm,
        onSetBpm = viewModel::setBpm,
        onTap = viewModel::tap,
        onBeatsPerBar = viewModel::setBeatsPerBar,
        onAccent = viewModel::setAccent,
        onLevel = viewModel::setLevel,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MetronomeScreen(
    uiState: MetronomeUiState,
    onToggle: () -> Unit,
    onAdjustBpm: (Int) -> Unit,
    onSetBpm: (Int) -> Unit,
    onTap: () -> Unit,
    onBeatsPerBar: (Int) -> Unit,
    onAccent: (Boolean) -> Unit,
    onLevel: (Double) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.metronome_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(CoreUiR.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        KeepScreenOn(enabled = uiState.isPlaying)

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
                    text = stringResource(R.string.metronome_headphone_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp),
                )
            }

            BigReadout(
                value = uiState.bpm.toString(),
                unit = stringResource(R.string.metronome_bpm),
                modifier = Modifier.padding(vertical = dimens.gutterSmall),
            )

            BeatIndicator(
                beatsPerBar = uiState.beatsPerBar,
                currentBeat = uiState.currentBeat,
                active = uiState.isPlaying,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            uiState.error?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onAdjustBpm(-10) },
                    modifier = Modifier.weight(1f).heightIn(min = dimens.minTouch),
                ) { Text(stringResource(R.string.metronome_minus_ten)) }
                OutlinedButton(
                    onClick = { onAdjustBpm(-1) },
                    modifier = Modifier.weight(1f).heightIn(min = dimens.minTouch),
                ) { Text(stringResource(R.string.metronome_minus)) }
                OutlinedButton(
                    onClick = { onAdjustBpm(1) },
                    modifier = Modifier.weight(1f).heightIn(min = dimens.minTouch),
                ) { Text(stringResource(R.string.metronome_plus)) }
                OutlinedButton(
                    onClick = { onAdjustBpm(10) },
                    modifier = Modifier.weight(1f).heightIn(min = dimens.minTouch),
                ) { Text(stringResource(R.string.metronome_plus_ten)) }
            }

            Slider(
                value = uiState.bpm.toFloat(),
                onValueChange = { onSetBpm(it.toInt()) },
                valueRange = 30f..300f,
                modifier = Modifier.heightIn(min = dimens.minTouch),
            )

            OutlinedButton(
                onClick = onTap,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = dimens.minTouch * 1.5f),
            ) { Text(stringResource(R.string.metronome_tap)) }

            Text(
                text = stringResource(R.string.metronome_beats),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(2, 3, 4, 6).forEach { beats ->
                    val label = stringResource(R.string.metronome_beats_value, beats)
                    if (beats == uiState.beatsPerBar) {
                        Button(
                            onClick = { onBeatsPerBar(beats) },
                            modifier = Modifier.heightIn(min = dimens.minTouch),
                        ) { Text(label) }
                    } else {
                        OutlinedButton(
                            onClick = { onBeatsPerBar(beats) },
                            modifier = Modifier.heightIn(min = dimens.minTouch),
                        ) { Text(label) }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.metronome_accent),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = uiState.accentFirstBeat, onCheckedChange = onAccent)
            }

            Text(
                text = stringResource(R.string.metronome_level),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = uiState.levelDbFs.toFloat(),
                onValueChange = { onLevel(it.toDouble()) },
                valueRange = MetronomeUiState.MIN_LEVEL.toFloat()..
                    MetronomeUiState.MAX_LEVEL.toFloat(),
                modifier = Modifier.heightIn(min = dimens.minTouch),
            )

            Button(
                onClick = onToggle,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = dimens.minTouch)
                    .padding(bottom = dimens.gutter),
            ) {
                Text(
                    stringResource(
                        if (uiState.isPlaying) {
                            CoreUiR.string.measure_stop
                        } else {
                            CoreUiR.string.measure_start
                        },
                    ),
                )
            }
        }
    }
}

/** 拍のインジケータ。1拍目だけ大きく出して小節の頭が分かるようにする。 */
@Composable
private fun BeatIndicator(
    beatsPerBar: Int,
    currentBeat: Int,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(beatsPerBar) { index ->
            val isCurrent = active && index == currentBeat
            val color = when {
                isCurrent && index == 0 -> MaterialTheme.colorScheme.tertiary
                isCurrent -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceContainerHighest
            }
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(if (index == 0) 22.dp else 16.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}
