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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.component.BigReadout
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.ui.component.KeepScreenOn
import com.patoolbox.core.ui.component.MicPermissionGate
import com.patoolbox.core.ui.component.PaToolScaffold

/**
 * ディレイ実測。
 *
 * **基準を取ってからの差** を主役にしている。1回の測定で出る絶対値には
 * 端末の入出力レイテンシが乗っていて、その量は端末ごとに違ううえに
 * 数十msに達する。一方、同じ端末で測った2点の差ならレイテンシは打ち消されるので、
 * 校正なしでそのまま使える。ディレイタワーの追い込みはまさにこの引き算なので、
 * 現場の作業とも一致する。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DelayFinderScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MeasureViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dimens = LocalPaDimens.current

    PaToolScaffold(
        tool = ToolId.DELAY_FINDER,
        onBack = onBack,
        modifier = modifier,
        title = stringResource(R.string.delay_finder_title),
    ) { innerPadding ->
        if (!uiState.proStatus.isPro) {
            MeasureProNotice(modifier = Modifier.padding(innerPadding))
            return@PaToolScaffold
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
                val reading = uiState.reading
                val reference = uiState.reference

                if (reference != null && reading != null) {
                    val differenceMs = reading.differenceMsFrom(reference)
                    val meters = reading.differenceMetersFrom(reference, uiState.speedOfSound)
                    BigReadout(
                        value = "%+.1f".format(differenceMs),
                        unit = "ms",
                        label = stringResource(R.string.delay_difference),
                        caption = stringResource(R.string.delay_distance, "%+.2f".format(meters)),
                        modifier = Modifier.padding(vertical = dimens.gutterSmall),
                    )
                } else {
                    BigReadout(
                        value = reading?.let { "%.1f".format(it.rawMs) } ?: "----",
                        unit = if (reading != null) "ms" else null,
                        label = stringResource(R.string.delay_raw),
                        caption = stringResource(R.string.delay_raw_note),
                        valueColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = dimens.gutterSmall),
                    )
                }

                MeasureWarnings(uiState)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = viewModel::markAsReference,
                        enabled = reading != null,
                    ) {
                        Text(stringResource(R.string.delay_set_reference))
                    }
                    if (reference != null) {
                        OutlinedButton(onClick = viewModel::clearReference) {
                            Text(stringResource(R.string.delay_clear_reference))
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.delay_workflow_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.delay_workflow),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                uiState.result?.let { result ->
                    Text(
                        text = stringResource(R.string.delay_impulse_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    ImpulseChart(
                        impulse = result.impulse,
                        sampleRate = result.sampleRate,
                    )
                    Text(
                        text = stringResource(
                            R.string.delay_confidence,
                            "%.0f".format(result.delay.confidenceDb),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
