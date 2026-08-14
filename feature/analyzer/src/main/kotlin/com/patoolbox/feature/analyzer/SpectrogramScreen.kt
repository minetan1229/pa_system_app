package com.patoolbox.feature.analyzer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.dsp.OctaveSmoothing
import com.patoolbox.core.ui.component.KeepScreenOn
import com.patoolbox.core.ui.component.MicPermissionGate
import com.patoolbox.core.ui.R as CoreUiR

/**
 * スペクトログラム。
 *
 * 縦が時間（上が最新）、横が対数周波数、色が強さ。
 * 一瞬しか出ない音を探すための道具で、FFT の瞬間表示では見逃すものが残る。
 * ハウリングが立ち上がる直前の帯域、断続的なノイズ、ワイヤレスの飛び込みなど。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpectrogramScreen(
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
                title = { Text(stringResource(R.string.spectrogram_title)) },
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
                SpectrogramView(
                    buffer = viewModel.spectrogram,
                    frame = uiState.frame,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    text = stringResource(R.string.spectrogram_axis),
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
                }

                Button(
                    onClick = viewModel::toggle,
                    modifier = Modifier.heightIn(min = dimens.minTouch),
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

                Text(
                    text = stringResource(R.string.spectrogram_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = dimens.gutter),
                )
            }
        }
    }
}
