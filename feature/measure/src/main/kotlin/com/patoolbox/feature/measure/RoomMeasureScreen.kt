package com.patoolbox.feature.measure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.component.BigReadout
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.dsp.ReverbTime
import com.patoolbox.core.ui.component.KeepScreenOn
import com.patoolbox.core.ui.component.MicPermissionGate
import com.patoolbox.core.ui.R as CoreUiR

/**
 * 残響測定（IR / RT60）。
 *
 * 広帯域の代表値だけでなく、必ずオクターブバンドごとに出す。
 * 低域だけ 2 倍長い部屋はいくらでもあって、そういう部屋では
 * 全体の音量ではなくローカットの入れ方で結果が変わるため。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomMeasureScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MeasureViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dimens = LocalPaDimens.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.room_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(CoreUiR.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        if (!uiState.proStatus.isPro) {
            MeasureProNotice(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }

        MicPermissionGate(modifier = Modifier.padding(innerPadding)) {
            KeepScreenOn(enabled = true)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = dimens.gutter),
                verticalArrangement = Arrangement.spacedBy(dimens.gutterSmall),
            ) {
                val reverb = uiState.result?.reverb
                val best = reverb?.bestFit

                BigReadout(
                    value = best?.let { "%.2f".format(it.rtSeconds) } ?: "----",
                    unit = if (best != null) "s" else null,
                    label = reverb?.bestLabel,
                    caption = when {
                        best == null -> stringResource(R.string.room_waiting)
                        !best.isReliable -> stringResource(R.string.room_nonlinear)
                        else -> stringResource(
                            R.string.room_range,
                            "%.0f".format(reverb.decayRangeDb),
                        )
                    },
                    modifier = Modifier.padding(vertical = dimens.gutterSmall),
                )

                MeasureWarnings(uiState)

                uiState.result?.let { result ->
                    ClarityCard(result.clarityC50Db, result.clarityC80Db, result.definitionPercent)

                    Text(
                        text = stringResource(R.string.room_bands_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    BandTable(result.bands)

                    Text(
                        text = stringResource(R.string.room_decay_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    DecayChart(curveDb = result.reverb.curveDb)
                    Text(
                        text = stringResource(R.string.room_decay_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Text(
                        text = stringResource(R.string.delay_impulse_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    ImpulseChart(impulse = result.impulse, sampleRate = result.sampleRate)
                }

                MeasureControls(
                    uiState = uiState,
                    onLengthChange = viewModel::setSweepLength,
                    onLevelChange = viewModel::setSweepLevel,
                    onMeasure = viewModel::measure,
                    modifier = Modifier.padding(bottom = dimens.gutter),
                )
            }
        }
    }
}

@Composable
private fun ClarityCard(c50: Double?, c80: Double?, d50: Double?) {
    if (c50 == null && c80 == null) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(
                    R.string.room_clarity,
                    c50?.let { "%+.1f".format(it) } ?: "--",
                    c80?.let { "%+.1f".format(it) } ?: "--",
                    d50?.let { "%.0f".format(it) } ?: "--",
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.room_clarity_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BandTable(bands: List<ReverbTime.BandResult>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            BandRow(
                band = stringResource(R.string.room_column_band),
                value = stringResource(R.string.room_column_rt),
                quality = stringResource(R.string.room_column_quality),
                header = true,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            for (entry in bands) {
                val fit = entry.result.bestFit
                BandRow(
                    band = entry.band.label,
                    value = fit?.let { "%.2f s".format(it.rtSeconds) }
                        ?: stringResource(R.string.room_unmeasurable),
                    quality = when {
                        fit == null -> "--"
                        fit.isReliable -> entry.result.bestLabel.orEmpty()
                        else -> stringResource(R.string.room_quality_poor)
                    },
                )
            }
        }
    }
}

@Composable
private fun BandRow(band: String, value: String, quality: String, header: Boolean = false) {
    val style = if (header) {
        MaterialTheme.typography.labelLarge
    } else {
        MaterialTheme.typography.bodyLarge
    }
    val color = if (header) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(text = band, style = style, color = color, modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = style,
            color = color,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = quality,
            style = style,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.8f),
        )
    }
}
