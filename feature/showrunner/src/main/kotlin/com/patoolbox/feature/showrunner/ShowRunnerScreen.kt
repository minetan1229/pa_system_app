package com.patoolbox.feature.showrunner

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.component.BigReadout
import com.patoolbox.core.designsystem.component.PaCard
import com.patoolbox.core.designsystem.component.PaNotice
import com.patoolbox.core.designsystem.component.PaPanel
import com.patoolbox.core.designsystem.component.PaPill
import com.patoolbox.core.designsystem.component.PaTone
import com.patoolbox.core.designsystem.component.contrastingInk
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.SoundCue
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.ui.component.PaToolScaffold
import com.patoolbox.core.ui.R as CoreUiR

/**
 * 本番進行コントローラー。
 *
 * 予定（タイトル＋持ち時間）を並べて上から順にカウントダウンする画面と、
 * SE パッド・同期音源（ループ再生のパッド）を1画面にまとめてある。
 * 本番中に画面を往復させない、という本番タイマーと同じ考え方の道具。
 */
@Composable
fun ShowRunnerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShowRunnerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dimens = LocalPaDimens.current
    val pickLauncher = rememberLauncherForActivityResult(PickShowAudioDocument()) { uri ->
        uri?.let(viewModel::import)
    }
    var settingsCueId by remember { mutableStateOf<Long?>(null) }

    PaToolScaffold(
        tool = ToolId.SHOW_RUNNER,
        onBack = onBack,
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = dimens.gutter, vertical = dimens.gutterSmall),
            verticalArrangement = Arrangement.spacedBy(dimens.space),
        ) {
            CountdownSection(uiState = uiState, viewModel = viewModel)

            ScheduleSection(uiState = uiState, viewModel = viewModel)

            PadSection(
                uiState = uiState,
                onToggle = viewModel::togglePad,
                onOpenSettings = { settingsCueId = it.id },
                onImport = { pickLauncher.launch(Unit) },
                onStopAll = viewModel::stopAllPads,
            )

            uiState.error?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    uiState.pads.firstOrNull { it.id == settingsCueId }?.let { cue ->
        PadSettingsSheet(
            cue = cue,
            onDismiss = { settingsCueId = null },
            onMarkSync = viewModel::markAsSync,
            onMarkOneShot = viewModel::markAsOneShot,
        )
    }
}

@Composable
private fun CountdownSection(
    uiState: ShowRunnerUiState,
    viewModel: ShowRunnerViewModel,
) {
    val dimens = LocalPaDimens.current
    val active = uiState.activeItem

    PaCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = dimens.space,
        verticalArrangement = Arrangement.spacedBy(dimens.spaceMd),
    ) {
        if (active == null) {
            Text(
                text = stringResource(R.string.showrunner_no_active),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            BigReadout(
                value = formatCountdown(uiState.displayMillis),
                label = active.title,
                caption = if (uiState.isOverrun) {
                    stringResource(R.string.showrunner_overrun)
                } else {
                    null
                },
                valueColor = if (uiState.isOverrun) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
                Button(
                    onClick = viewModel::togglePause,
                    modifier = Modifier.weight(1f).heightIn(min = dimens.minTouch),
                ) {
                    Text(
                        stringResource(
                            if (uiState.running) CoreUiR.string.measure_stop else R.string.showrunner_resume,
                        ),
                    )
                }
                OutlinedButton(
                    onClick = viewModel::resetActive,
                    modifier = Modifier.weight(1f).heightIn(min = dimens.minTouch),
                ) {
                    Text(stringResource(CoreUiR.string.measure_reset))
                }
            }

            uiState.nextItem?.let { next ->
                OutlinedButton(
                    onClick = viewModel::startNext,
                    modifier = Modifier.fillMaxWidth().heightIn(min = dimens.minTouch),
                ) {
                    Text(stringResource(R.string.showrunner_next, next.title))
                }
            }
        }
    }
}

@Composable
private fun ScheduleSection(
    uiState: ShowRunnerUiState,
    viewModel: ShowRunnerViewModel,
) {
    val dimens = LocalPaDimens.current

    PaPanel(
        title = stringResource(R.string.showrunner_schedule_title),
        subtitle = stringResource(R.string.showrunner_schedule_subtitle),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = uiState.draftTitle,
                onValueChange = viewModel::setDraftTitle,
                placeholder = { Text(stringResource(R.string.showrunner_draft_title_hint)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceSm),
        ) {
            PRESET_MINUTES.forEach { minutes ->
                MinuteChip(
                    minutes = minutes,
                    selected = minutes == uiState.draftMinutes,
                    onClick = { viewModel.setDraftMinutes(minutes) },
                )
            }
        }
        Button(
            onClick = viewModel::addScheduleItem,
            enabled = uiState.draftTitle.isNotBlank(),
            modifier = Modifier.fillMaxWidth().heightIn(min = dimens.minTouch),
        ) {
            Text(stringResource(R.string.showrunner_add))
        }

        if (uiState.schedule.isEmpty()) {
            Text(
                text = stringResource(R.string.showrunner_schedule_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            uiState.schedule.forEachIndexed { index, item ->
                ScheduleRow(
                    item = item,
                    isActive = item.id == uiState.activeItemId,
                    isFirst = index == 0,
                    isLast = index == uiState.schedule.lastIndex,
                    onStart = { viewModel.startItem(item.id) },
                    onRemove = { viewModel.removeScheduleItem(item.id) },
                    onMoveUp = { viewModel.moveUp(item.id) },
                    onMoveDown = { viewModel.moveDown(item.id) },
                )
            }
        }
    }
}

@Composable
private fun ScheduleRow(
    item: ScheduleItem,
    isActive: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onStart: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val dimens = LocalPaDimens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.showrunner_minutes, item.plannedMinutes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isActive) {
            PaPill(text = stringResource(R.string.showrunner_active), tone = PaTone.BRAND)
        }
        TextButton(onClick = onMoveUp, enabled = !isFirst) { Text("▲") }
        TextButton(onClick = onMoveDown, enabled = !isLast) { Text("▼") }
        TextButton(onClick = onStart) { Text(stringResource(R.string.showrunner_start)) }
        TextButton(onClick = onRemove) { Text(stringResource(R.string.showrunner_remove)) }
    }
}

@Composable
private fun MinuteChip(minutes: Int, selected: Boolean, onClick: () -> Unit) {
    val dimens = LocalPaDimens.current
    if (selected) {
        Button(onClick = onClick, modifier = Modifier.heightIn(min = dimens.minTouch)) {
            Text(stringResource(R.string.showrunner_minutes, minutes))
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = Modifier.heightIn(min = dimens.minTouch)) {
            Text(stringResource(R.string.showrunner_minutes, minutes))
        }
    }
}

@Composable
private fun PadSection(
    uiState: ShowRunnerUiState,
    onToggle: (SoundCue) -> Unit,
    onOpenSettings: (SoundCue) -> Unit,
    onImport: () -> Unit,
    onStopAll: () -> Unit,
) {
    val dimens = LocalPaDimens.current

    PaPanel(
        title = stringResource(R.string.showrunner_pads_title),
        subtitle = stringResource(R.string.showrunner_pads_subtitle),
        trailing = {
            if (uiState.playingIds.isNotEmpty()) {
                TextButton(onClick = onStopAll) { Text(stringResource(R.string.showrunner_pads_stop_all)) }
            }
        },
    ) {
        if (!uiState.canAddMorePads) {
            PaNotice(
                title = stringResource(R.string.showrunner_pads_limit),
                tone = PaTone.WARNING,
            )
        }
        OutlinedButton(
            onClick = onImport,
            enabled = uiState.canAddMorePads && !uiState.importing,
            modifier = Modifier.fillMaxWidth().heightIn(min = dimens.minTouch),
        ) {
            Text(
                stringResource(
                    if (uiState.importing) R.string.showrunner_importing else R.string.showrunner_import,
                ),
            )
        }

        if (uiState.pads.isEmpty()) {
            Text(
                text = stringResource(R.string.showrunner_pads_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceSm),
            ) {
                uiState.pads.forEach { cue ->
                    PadTile(
                        cue = cue,
                        playing = cue.id in uiState.playingIds,
                        onClick = { onToggle(cue) },
                        onLongClick = { onOpenSettings(cue) },
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.showrunner_pads_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * パッド1枚。押すと再生・停止、長押しで設定シートを開く（SEパッド画面と同じ操作）。
 */
@Composable
private fun PadTile(
    cue: SoundCue,
    playing: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val dimens = LocalPaDimens.current
    val accent = padColor(cue.colorIndex)
    val interactionSource = remember { MutableInteractionSource() }
    PaCard(
        modifier = Modifier
            .size(width = 104.dp, height = 72.dp)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        containerColor = if (playing) accent else MaterialTheme.colorScheme.surfaceContainer,
        contentPadding = dimens.spaceSm,
        verticalArrangement = Arrangement.spacedBy(dimens.spaceXs / 2),
    ) {
        Text(
            text = cue.title,
            style = MaterialTheme.typography.labelLarge,
            color = if (playing) contrastingInk(accent) else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
        )
        Text(
            text = buildString {
                append(cue.durationLabel)
                if (cue.loop) append("  ↻")
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (playing) {
                contrastingInk(accent)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

/**
 * パッドの設定。長押しで開く。同期音源（ループ再生）↔単発の切り替えをここに集約してある。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PadSettingsSheet(
    cue: SoundCue,
    onDismiss: () -> Unit,
    onMarkSync: (SoundCue) -> Unit,
    onMarkOneShot: (SoundCue) -> Unit,
) {
    val dimens = LocalPaDimens.current
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.gutter)
                .padding(bottom = dimens.spaceXl),
            verticalArrangement = Arrangement.spacedBy(dimens.space),
        ) {
            Text(
                text = cue.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.showrunner_pad_settings_sync),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Switch(
                    checked = cue.loop,
                    onCheckedChange = { checked ->
                        if (checked) onMarkSync(cue) else onMarkOneShot(cue)
                    },
                )
            }
            Text(
                text = stringResource(R.string.showrunner_pad_settings_sync_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val PRESET_MINUTES = listOf(1, 3, 5, 10, 15, 20, 30)

private fun formatCountdown(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
