package com.patoolbox.feature.measure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import com.patoolbox.core.dsp.ImpulseResponse
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.ui.component.KeepScreenOn
import com.patoolbox.core.ui.component.MicPermissionGate
import com.patoolbox.core.ui.component.PaToolScaffold

/**
 * 極性チェック。
 *
 * 「分からない」を返せることを重視している。スピーカーは最小位相系ではないので、
 * 逆相でも波形が単純に反転するとは限らない。差が小さいときに二択で断定すると、
 * 正しく繋がっているケーブルを疑って現場を止めることになる。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PolarityScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MeasureViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dimens = LocalPaDimens.current

    PaToolScaffold(
        tool = ToolId.POLARITY_CHECK,
        onBack = onBack,
        modifier = modifier,
        title = stringResource(R.string.polarity_title),
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
                val polarity = uiState.result?.polarity

                BigReadout(
                    value = when (polarity?.polarity) {
                        ImpulseResponse.Polarity.NORMAL -> stringResource(R.string.polarity_normal)
                        ImpulseResponse.Polarity.INVERTED ->
                            stringResource(R.string.polarity_inverted)
                        ImpulseResponse.Polarity.UNCERTAIN ->
                            stringResource(R.string.polarity_uncertain)
                        null -> "----"
                    },
                    label = polarity?.let {
                        stringResource(R.string.polarity_margin, "%.1f".format(it.marginDb))
                    },
                    caption = stringResource(R.string.polarity_caption),
                    valueColor = when (polarity?.polarity) {
                        ImpulseResponse.Polarity.INVERTED -> MaterialTheme.colorScheme.error
                        ImpulseResponse.Polarity.NORMAL -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(vertical = dimens.gutterSmall),
                )

                MeasureWarnings(uiState)

                if (polarity?.polarity == ImpulseResponse.Polarity.UNCERTAIN) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.polarity_uncertain_advice),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp),
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
                        windowMs = 30.0,
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
