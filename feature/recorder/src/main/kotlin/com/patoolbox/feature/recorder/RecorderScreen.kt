package com.patoolbox.feature.recorder

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.component.BigReadout
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.Recording
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.ui.DateTimeText
import com.patoolbox.core.ui.component.KeepScreenOn
import com.patoolbox.core.ui.component.MicPermissionGate
import com.patoolbox.core.ui.component.PaToolScaffold

/**
 * 現場の録音。
 *
 * 録音中は経過時間とレベルを大きく出す。撮れているかどうかが
 * 一目で分からない録音機は、現場では使われない。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecorderScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecorderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dimens = LocalPaDimens.current
    val context = LocalContext.current
    var pendingExport by remember { mutableStateOf<Recording?>(null) }

    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(WAV_MIME_TYPE),
    ) { uri ->
        val recording = pendingExport
        pendingExport = null
        if (uri != null && recording != null) {
            context.contentResolver.openOutputStream(uri)?.let {
                viewModel.exportTo(recording, it)
            }
        }
    }

    PaToolScaffold(
        tool = ToolId.RECORDER,
        onBack = onBack,
        modifier = modifier,
        title = stringResource(R.string.recorder_title),
    ) { innerPadding ->
        if (!uiState.proStatus.isPro) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(dimens.gutter),
            ) {
                Text(
                    text = stringResource(R.string.recorder_pro),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            return@PaToolScaffold
        }

        MicPermissionGate(modifier = Modifier.padding(innerPadding)) {
            KeepScreenOn(enabled = uiState.isRecording)

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = dimens.gutter),
                verticalArrangement = Arrangement.spacedBy(dimens.gutterSmall),
            ) {
                val elapsed = uiState.elapsedSeconds.toInt()
                BigReadout(
                    value = "%d:%02d".format(elapsed / 60, elapsed % 60),
                    label = if (uiState.isRecording) {
                        "%.0f dBFS".format(uiState.levelDb)
                    } else {
                        null
                    },
                    caption = stringResource(
                        if (uiState.isRecording) {
                            R.string.recorder_recording
                        } else {
                            R.string.recorder_idle
                        },
                    ),
                    valueColor = if (uiState.isRecording) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.padding(vertical = dimens.gutterSmall),
                )

                LevelBar(levelDb = uiState.levelDb, clipped = uiState.clipped)

                if (uiState.clipped) {
                    Text(
                        text = stringResource(R.string.recorder_clipped),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                uiState.error?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Button(
                    onClick = viewModel::toggleRecording,
                    colors = if (uiState.isRecording) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = dimens.minTouch),
                ) {
                    Text(
                        stringResource(
                            if (uiState.isRecording) {
                                R.string.recorder_stop
                            } else {
                                R.string.recorder_start
                            },
                        ),
                    )
                }

                Text(
                    text = stringResource(R.string.recorder_list, uiState.recordings.size),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(dimens.gutterSmall),
                    modifier = Modifier.weight(1f).padding(bottom = dimens.gutter),
                ) {
                    items(uiState.recordings, key = { it.id }) { recording ->
                        RecordingCard(
                            recording = recording,
                            isPlaying = uiState.playingId == recording.id,
                            onPlay = {
                                if (uiState.playingId == recording.id) {
                                    viewModel.stopPlayback()
                                } else {
                                    viewModel.play(recording)
                                }
                            },
                            onExport = {
                                pendingExport = recording
                                createLauncher.launch(viewModel.suggestedFileName(recording))
                            },
                            onDelete = { viewModel.delete(recording) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * レベルメーター。-60dBFS を左端、0dBFS を右端にした対数目盛り。
 * リニアにすると実用域（-20dB 付近）が右端に寄って読めない。
 */
@Composable
private fun LevelBar(levelDb: Double, clipped: Boolean) {
    val normalized = ((levelDb - METER_FLOOR_DB) / -METER_FLOOR_DB)
        .coerceIn(0.0, 1.0)
        .toFloat()

    LinearProgressIndicator(
        progress = { normalized },
        color = if (clipped) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = 12.dp),
    )
}

@Composable
private fun RecordingCard(
    recording: Recording,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = recording.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(
                    R.string.recorder_summary,
                    recording.durationLabel,
                    "%.1f".format(recording.sizeBytes / 1_000_000.0),
                    DateTimeText.formatDateTime(recording.startedAtEpochMs),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (recording.clipped) {
                Text(
                    text = stringResource(R.string.recorder_clipped),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onPlay) {
                    Text(
                        stringResource(
                            if (isPlaying) R.string.recorder_pause else R.string.recorder_play,
                        ),
                    )
                }
                TextButton(onClick = onExport) {
                    Text(stringResource(R.string.recorder_export))
                }
                TextButton(onClick = onDelete) {
                    Text(
                        text = stringResource(R.string.recorder_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

private const val WAV_MIME_TYPE = "audio/wav"
private const val METER_FLOOR_DB = -60.0
