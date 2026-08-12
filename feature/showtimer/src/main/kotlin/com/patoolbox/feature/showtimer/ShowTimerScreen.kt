package com.patoolbox.feature.showtimer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.component.BigReadout
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.ui.component.KeepScreenOn
import com.patoolbox.core.ui.R as CoreUiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowTimerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShowTimerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dimens = LocalPaDimens.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.timer_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(CoreUiR.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        // ステージから見えるように置いて使うので、走っていなくても画面は消さない
        KeepScreenOn(enabled = true)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = dimens.gutter),
            verticalArrangement = Arrangement.spacedBy(dimens.gutterSmall),
        ) {
            BigReadout(
                value = formatDuration(uiState.displayMillis),
                label = if (uiState.isOverrun) {
                    stringResource(R.string.timer_over, "")
                } else {
                    stringResource(
                        when (uiState.mode) {
                            TimerMode.COUNTDOWN -> R.string.timer_remaining_label
                            TimerMode.ELAPSED -> R.string.timer_elapsed_label
                        },
                    )
                },
                valueColor = if (uiState.isOverrun) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.padding(vertical = dimens.gutter),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeButton(
                    text = stringResource(R.string.timer_mode_countdown),
                    selected = uiState.mode == TimerMode.COUNTDOWN,
                    onClick = { viewModel.setMode(TimerMode.COUNTDOWN) },
                    minTouch = dimens.minTouch,
                )
                ModeButton(
                    text = stringResource(R.string.timer_mode_elapsed),
                    selected = uiState.mode == TimerMode.ELAPSED,
                    onClick = { viewModel.setMode(TimerMode.ELAPSED) },
                    minTouch = dimens.minTouch,
                )
            }

            if (uiState.mode == TimerMode.COUNTDOWN) {
                Text(
                    text = stringResource(R.string.timer_target),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ShowTimerViewModel.PRESET_MINUTES.forEach { minutes ->
                        ModeButton(
                            text = stringResource(R.string.timer_preset, minutes),
                            selected = minutes == uiState.targetMinutes,
                            onClick = { viewModel.setTargetMinutes(minutes) },
                            minTouch = dimens.minTouch,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimens.gutterSmall),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = viewModel::toggle,
                    modifier = Modifier
                        .weight(2f)
                        .heightIn(min = dimens.minTouch * 1.4f),
                ) {
                    Text(
                        stringResource(
                            if (uiState.running) R.string.timer_pause else R.string.timer_start,
                        ),
                    )
                }
                OutlinedButton(
                    onClick = viewModel::reset,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = dimens.minTouch * 1.4f),
                ) {
                    Text(stringResource(R.string.timer_reset))
                }
            }

            Text(
                text = stringResource(R.string.timer_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ModeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    minTouch: Dp,
) {
    if (selected) {
        Button(onClick = onClick, modifier = Modifier.heightIn(min = minTouch)) { Text(text) }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.heightIn(min = minTouch),
        ) { Text(text) }
    }
}

/** 1時間を超えたら h:mm:ss、それ未満は mm:ss。 */
internal fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
