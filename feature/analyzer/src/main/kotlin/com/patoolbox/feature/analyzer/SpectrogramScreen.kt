package com.patoolbox.feature.analyzer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.dsp.OctaveSmoothing
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.ui.component.KeepScreenOn
import com.patoolbox.core.ui.component.MicPermissionGate
import com.patoolbox.core.ui.component.PaToolScaffold
import com.patoolbox.core.ui.component.SpectrumRange
import com.patoolbox.core.ui.R as CoreUiR

/**
 * スペクトログラム。
 *
 * 縦が時間（上が最新）、横が対数周波数、色が強さ。
 * 一瞬しか出ない音を探すための道具で、スペクトラムの瞬間表示では見逃すものが残る。
 * ハウリングが立ち上がる直前の帯域、断続的なノイズ、ワイヤレスの飛び込みなど。
 */
@Composable
fun SpectrogramScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnalyzerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dimens = LocalPaDimens.current
    val scrollState = rememberScrollState()

    PaToolScaffold(
        tool = ToolId.SPECTROGRAM,
        onBack = onBack,
        modifier = modifier,
        title = stringResource(R.string.spectrogram_title),
    ) { innerPadding ->
        if (!uiState.proStatus.isPro) {
            AnalyzerProNotice(modifier = Modifier.padding(innerPadding))
            return@PaToolScaffold
        }

        MicPermissionGate(modifier = Modifier.padding(innerPadding)) {
            KeepScreenOn(enabled = uiState.isMeasuring)

            val range = remember(uiState.frame, uiState.span) {
                SpectrumRange.auto(uiState.columnsDb, uiState.span.db)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = dimens.gutter),
                verticalArrangement = Arrangement.spacedBy(dimens.gutterSmall),
            ) {
                SpectrogramView(
                    buffer = viewModel.spectrogram,
                    frame = uiState.frame,
                    range = range,
                    hopSeconds = viewModel.hopSeconds,
                    minHz = uiState.frequencies.firstOrNull() ?: DEFAULT_MIN_HZ,
                    maxHz = uiState.frequencies.lastOrNull() ?: DEFAULT_MAX_HZ,
                    height = 320.dp,
                )

                Button(
                    onClick = viewModel::toggle,
                    modifier = Modifier.fillMaxWidth().heightIn(min = dimens.minTouch),
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

                ChipGroup(label = stringResource(R.string.analyzer_smoothing)) {
                    for (smoothing in OctaveSmoothing.entries) {
                        FilterChip(
                            selected = uiState.smoothing == smoothing,
                            onClick = { viewModel.setSmoothing(smoothing) },
                            label = { Text(smoothing.label) },
                        )
                    }
                }
                ChipGroup(label = stringResource(R.string.analyzer_resolution)) {
                    for (size in AnalyzerFftSize.entries) {
                        FilterChip(
                            selected = uiState.fftSize == size,
                            onClick = { viewModel.setFftSize(size) },
                            label = { Text(size.label) },
                        )
                    }
                }
                ChipGroup(label = stringResource(R.string.analyzer_span)) {
                    for (span in AnalyzerSpan.entries) {
                        FilterChip(
                            selected = uiState.span == span,
                            onClick = { viewModel.setSpan(span) },
                            label = { Text(span.label) },
                        )
                    }
                }

                Text(
                    text = stringResource(
                        R.string.spectrogram_axis,
                        "%.0f".format(viewModel.historySeconds),
                        uiState.unitLabel,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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

@Composable
private fun ChipGroup(label: String, content: @Composable FlowRowScope.() -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

private const val DEFAULT_MIN_HZ = 20.0
private const val DEFAULT_MAX_HZ = 20000.0
