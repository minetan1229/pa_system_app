package com.patoolbox.feature.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.patoolbox.core.designsystem.component.BigReadout
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.dsp.FeedbackDetector
import com.patoolbox.core.ui.component.KeepScreenOn
import com.patoolbox.core.ui.component.MicPermissionGate
import com.patoolbox.core.ui.R as CoreUiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeedbackViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dimens = LocalPaDimens.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feedback_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(CoreUiR.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        if (!uiState.proStatus.isPro) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(dimens.gutter),
            ) {
                Text(
                    text = stringResource(R.string.feedback_pro),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.feedback_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            return@Scaffold
        }

        MicPermissionGate(modifier = Modifier.padding(innerPadding)) {
            KeepScreenOn(enabled = uiState.isMeasuring)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = dimens.gutter),
                verticalArrangement = Arrangement.spacedBy(dimens.gutterSmall),
            ) {
                val top = uiState.candidates.firstOrNull()

                BigReadout(
                    value = top?.let { "%.0f".format(it.frequencyHz) } ?: "----",
                    unit = if (top != null) "Hz" else null,
                    label = top?.noteName,
                    caption = when {
                        !uiState.isMeasuring -> stringResource(R.string.feedback_waiting)
                        uiState.candidates.isEmpty() -> stringResource(R.string.feedback_none)
                        else -> stringResource(
                            R.string.feedback_detected,
                            uiState.candidates.size,
                        )
                    },
                    valueColor = if (top != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(vertical = dimens.gutterSmall),
                )

                uiState.error?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(uiState.candidates, key = { it.frequencyHz.toInt() }) { candidate ->
                        CandidateCard(candidate)
                    }
                }

                Text(
                    text = stringResource(R.string.feedback_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Button(
                    onClick = viewModel::toggle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = dimens.minTouch)
                        .padding(bottom = dimens.gutter),
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
            }
        }
    }
}

@Composable
private fun CandidateCard(candidate: FeedbackDetector.Candidate) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.feedback_band, candidate.bandLabel),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = stringResource(
                    R.string.feedback_detail,
                    "%.0f".format(candidate.frequencyHz),
                    candidate.noteName,
                    "%.0f".format(candidate.prominenceDb),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = stringResource(R.string.feedback_action),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}
