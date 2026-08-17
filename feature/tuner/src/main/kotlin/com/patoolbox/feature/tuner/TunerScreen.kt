package com.patoolbox.feature.tuner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.component.BigReadout
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.dsp.NoteNames
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.ui.component.KeepScreenOn
import com.patoolbox.core.ui.component.MicPermissionGate
import com.patoolbox.core.ui.component.PaToolScaffold
import kotlin.math.roundToInt
import com.patoolbox.core.ui.R as CoreUiR

@Composable
fun TunerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TunerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TunerScreen(
        uiState = uiState,
        onToggle = viewModel::toggle,
        onAdjustReference = viewModel::adjustReference,
        onResetReference = viewModel::resetReference,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TunerScreen(
    uiState: TunerUiState,
    onToggle: () -> Unit,
    onAdjustReference: (Double) -> Unit,
    onResetReference: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current

    PaToolScaffold(
        tool = ToolId.TUNER,
        onBack = onBack,
        modifier = modifier,
        title = stringResource(R.string.tuner_title),
    ) { innerPadding ->
        MicPermissionGate(modifier = Modifier.padding(innerPadding)) {
            KeepScreenOn(enabled = uiState.isListening)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = dimens.gutter),
                verticalArrangement = Arrangement.spacedBy(dimens.gutterSmall),
            ) {
                val note = uiState.note

                BigReadout(
                    value = note?.displayName ?: stringResource(R.string.tuner_no_pitch),
                    label = if (uiState.isInTune) {
                        stringResource(R.string.tuner_in_tune)
                    } else {
                        null
                    },
                    caption = if (note != null) {
                        stringResource(
                            R.string.tuner_frequency,
                            "%.1f".format(note.detectedHz),
                            "%.1f".format(note.targetHz),
                        )
                    } else {
                        stringResource(R.string.tuner_listening)
                    },
                    valueColor = if (uiState.isInTune) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.padding(top = dimens.gutter),
                )

                CentsMeter(
                    cents = note?.cents,
                    inTune = uiState.isInTune,
                )

                Text(
                    text = note?.let {
                        stringResource(R.string.tuner_cents, "%+.0f".format(it.cents))
                    } ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )

                if (uiState.isListening) {
                    Text(
                        text = stringResource(
                            R.string.tuner_clarity,
                            (uiState.clarity * 100).roundToInt(),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }

                uiState.error?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Text(
                    text = stringResource(
                        R.string.tuner_reference,
                        "%.0f".format(uiState.referenceAHz),
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = dimens.gutterSmall),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onAdjustReference(-1.0) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = dimens.minTouch),
                    ) { Text(stringResource(R.string.tuner_reference_down)) }
                    OutlinedButton(
                        onClick = { onAdjustReference(1.0) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = dimens.minTouch),
                    ) { Text(stringResource(R.string.tuner_reference_up)) }
                    OutlinedButton(
                        onClick = onResetReference,
                        enabled = uiState.referenceAHz != NoteNames.DEFAULT_REFERENCE_A_HZ,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = dimens.minTouch),
                    ) { Text(stringResource(R.string.tuner_reference_reset)) }
                }

                Button(
                    onClick = onToggle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = dimens.minTouch)
                        .padding(top = dimens.gutterSmall),
                ) {
                    Text(
                        stringResource(
                            if (uiState.isListening) {
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
}

/**
 * セント表示のメーター。±50セントを横一杯に取り、中央に合っているかを見る。
 * 針が細いと暗所で見えないので、太いバーで表現している。
 */
@Composable
private fun CentsMeter(
    cents: Double?,
    inTune: Boolean,
    modifier: Modifier = Modifier,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val centerColor = MaterialTheme.colorScheme.outline
    val needleColor = if (inTune) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        drawRect(color = trackColor, size = size)

        // 中央（0セント）の目印
        val centerX = size.width / 2f
        drawRect(
            color = centerColor,
            topLeft = Offset(centerX - 1f, 0f),
            size = Size(2f, size.height),
        )

        if (cents == null) return@Canvas

        val normalized = (cents / MAX_CENTS).coerceIn(-1.0, 1.0)
        val needleX = (centerX + normalized * centerX).toFloat()
        val needleWidth = 10f
        drawRect(
            color = needleColor,
            topLeft = Offset(needleX - needleWidth / 2f, 0f),
            size = Size(needleWidth, size.height),
        )
    }
}

private const val MAX_CENTS = 50.0
