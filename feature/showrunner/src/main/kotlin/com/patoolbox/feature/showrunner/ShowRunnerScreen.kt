package com.patoolbox.feature.showrunner

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.patoolbox.core.designsystem.component.PaSectionHeader
import com.patoolbox.core.designsystem.component.PaTone
import com.patoolbox.core.designsystem.component.PaUnderlineTabs
import com.patoolbox.core.designsystem.component.contrastingInk
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.SoundCue
import com.patoolbox.core.model.TimelineEntry
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.ui.DateTimeText
import com.patoolbox.core.ui.component.CalibrationBadge
import com.patoolbox.core.ui.component.FeedbackAlertPanel
import com.patoolbox.core.ui.component.KeepScreenOn
import com.patoolbox.core.ui.component.PaToolScaffold
import com.patoolbox.core.ui.component.SpectrumChart
import com.patoolbox.core.ui.component.SpectrumRange
import com.patoolbox.core.ui.identityColor
import com.patoolbox.core.ui.R as CoreUiR

/**
 * 本番万能コントローラー。
 *
 * 進行表のカウントダウン、SE の自動連動、ハウリング測定・スペクトラムアナライザを
 * 1画面（3タブ）にまとめてある。本番中に画面を持ち替えさせない、という
 * 本番タイマーと同じ考え方の道具。
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
    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.startMonitor()
    }
    var settingsCueId by remember { mutableStateOf<Long?>(null) }
    var settingsItemId by remember { mutableStateOf<Long?>(null) }
    var selectedTab by rememberSaveable { mutableStateOf(ShowRunnerTab.PROGRESS) }
    val accent = ToolId.SHOW_RUNNER.identityColor()

    PaToolScaffold(
        tool = ToolId.SHOW_RUNNER,
        onBack = onBack,
        modifier = modifier,
    ) { innerPadding ->
        KeepScreenOn(enabled = uiState.running || uiState.monitoring)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            PaUnderlineTabs(
                titles = ShowRunnerTab.entries.map { stringResource(it.titleRes) },
                selectedIndex = selectedTab.ordinal,
                onSelect = { index -> selectedTab = ShowRunnerTab.entries[index] },
                accent = accent,
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = dimens.gutter, vertical = dimens.gutterSmall),
                verticalArrangement = Arrangement.spacedBy(dimens.space),
            ) {
                when (selectedTab) {
                    ShowRunnerTab.PROGRESS -> ProgressTab(
                        uiState = uiState,
                        viewModel = viewModel,
                        onOpenItemSettings = { settingsItemId = it },
                    )

                    ShowRunnerTab.MONITOR -> MonitorTab(
                        uiState = uiState,
                        onToggleMonitor = {
                            when {
                                uiState.monitoring -> viewModel.stopMonitor()
                                viewModel.hasMicPermission() -> viewModel.startMonitor()
                                else -> micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onResetMax = viewModel::resetMaxLevel,
                        onClearLastFeedback = viewModel::clearLastFeedback,
                    )

                    ShowRunnerTab.PADS -> PadSection(
                        uiState = uiState,
                        onToggle = viewModel::togglePad,
                        onOpenSettings = { settingsCueId = it.id },
                        onImport = { pickLauncher.launch(Unit) },
                        onStopAll = viewModel::stopAllPads,
                    )
                }

                uiState.error?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
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

    uiState.schedule.firstOrNull { it.id == settingsItemId }?.let { item ->
        ScheduleItemSettingsSheet(
            item = item,
            pads = uiState.pads,
            onDismiss = { settingsItemId = null },
            onSetFixedTime = { epochMs -> viewModel.setItemFixedTime(item.id, epochMs) },
            onSetCue = { cueId -> viewModel.setCueLink(item.id, cueId) },
            onSetDelayMs = { delayMs -> viewModel.setCueDelayMs(item.id, delayMs) },
        )
    }
}

private enum class ShowRunnerTab(val titleRes: Int) {
    PROGRESS(R.string.showrunner_tab_progress),
    MONITOR(R.string.showrunner_tab_monitor),
    PADS(R.string.showrunner_tab_pads),
}

@Composable
private fun ProgressTab(
    uiState: ShowRunnerUiState,
    viewModel: ShowRunnerViewModel,
    onOpenItemSettings: (Long) -> Unit,
) {
    val dimens = LocalPaDimens.current
    Column(verticalArrangement = Arrangement.spacedBy(dimens.space)) {
        CountdownSection(uiState = uiState, viewModel = viewModel)
        ScheduleSection(uiState = uiState, viewModel = viewModel, onOpenItemSettings = onOpenItemSettings)
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

            ExtendControl(
                draftMinutes = uiState.draftExtendMinutes,
                onDraftChange = viewModel::setDraftExtendMinutes,
                onApply = viewModel::applyExtend,
            )
        }
    }
}

/**
 * 延長操作。プリセットを押すとその値が仮選択され、+/− で微調整してから適用する。
 * 「そのプリセットじゃ足りない」を、チップを増やすのではなくその場の調整で吸収する。
 */
@Composable
private fun ExtendControl(
    draftMinutes: Int,
    onDraftChange: (Int) -> Unit,
    onApply: () -> Unit,
) {
    val dimens = LocalPaDimens.current
    Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            text = stringResource(R.string.showrunner_extend_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceSm),
        ) {
            EXTEND_PRESET_MINUTES.forEach { minutes ->
                MinuteChip(
                    minutes = minutes,
                    selected = minutes == draftMinutes,
                    onClick = { onDraftChange(minutes) },
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = { onDraftChange(draftMinutes - 1) },
                modifier = Modifier.heightIn(min = dimens.minTouch),
            ) { Text("−1分") }
            Text(
                text = stringResource(R.string.showrunner_minutes, draftMinutes),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            OutlinedButton(
                onClick = { onDraftChange(draftMinutes + 1) },
                modifier = Modifier.heightIn(min = dimens.minTouch),
            ) { Text("+1分") }
        }
        Button(
            onClick = onApply,
            modifier = Modifier.fillMaxWidth().heightIn(min = dimens.minTouch),
        ) {
            Text(stringResource(R.string.showrunner_extend_apply, draftMinutes))
        }
    }
}

@Composable
private fun ScheduleSection(
    uiState: ShowRunnerUiState,
    viewModel: ShowRunnerViewModel,
    onOpenItemSettings: (Long) -> Unit,
) {
    val dimens = LocalPaDimens.current
    val timeline = remember(uiState.schedule, uiState.activeItemId, uiState.elapsedMillis, uiState.anchorEpochMs) {
        uiState.projectedTimeline(System.currentTimeMillis())
    }
    val timelineByItemId = remember(timeline) { timeline.associateBy { it.item.id } }

    PaPanel(
        title = stringResource(R.string.showrunner_schedule_title),
        subtitle = stringResource(R.string.showrunner_schedule_subtitle),
    ) {
        AnchorTimeField(
            anchorEpochMs = uiState.anchorEpochMs,
            onSetAnchor = viewModel::setAnchorTime,
        )

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
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = { viewModel.setDraftMinutes(uiState.draftMinutes - 1) },
                modifier = Modifier.heightIn(min = dimens.minTouch),
            ) { Text("−1分") }
            Text(
                text = stringResource(R.string.showrunner_minutes, uiState.draftMinutes),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            OutlinedButton(
                onClick = { viewModel.setDraftMinutes(uiState.draftMinutes + 1) },
                modifier = Modifier.heightIn(min = dimens.minTouch),
            ) { Text("+1分") }
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
                    timeRange = timelineByItemId[item.id],
                    isActive = item.id == uiState.activeItemId,
                    isFirst = index == 0,
                    isLast = index == uiState.schedule.lastIndex,
                    onStart = { viewModel.startItem(item.id) },
                    onRemove = { viewModel.removeScheduleItem(item.id) },
                    onMoveUp = { viewModel.moveUp(item.id) },
                    onMoveDown = { viewModel.moveDown(item.id) },
                    onOpenSettings = { onOpenItemSettings(item.id) },
                )
            }
        }
    }
}

/** 全体の開始予定時刻。空にすると時刻表示自体をやめ、これまでどおり分表示だけになる。 */
@Composable
private fun AnchorTimeField(
    anchorEpochMs: Long?,
    onSetAnchor: (Long?) -> Unit,
) {
    val dimens = LocalPaDimens.current
    var text by remember(anchorEpochMs) { mutableStateOf(DateTimeText.formatTime(anchorEpochMs)) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { value ->
                text = value
                val parsed = DateTimeText.parseTime(value)
                onSetAnchor(if (parsed != null) DateTimeText.toEpochMs(null, parsed) else null)
            },
            label = { Text(stringResource(R.string.showrunner_anchor_label)) },
            placeholder = { Text(stringResource(R.string.showrunner_anchor_hint)) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        if (anchorEpochMs != null) {
            TextButton(onClick = { text = ""; onSetAnchor(null) }) {
                Text(stringResource(R.string.showrunner_anchor_clear))
            }
        }
    }
}

@Composable
private fun ScheduleRow(
    item: ScheduleItem,
    timeRange: TimelineEntry?,
    isActive: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onStart: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onOpenSettings: () -> Unit,
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
                text = if (timeRange != null) {
                    stringResource(
                        R.string.showrunner_time_range,
                        DateTimeText.formatTime(timeRange.startAtEpochMs),
                        DateTimeText.formatTime(timeRange.endAtEpochMs),
                    )
                } else {
                    stringResource(R.string.showrunner_minutes, item.totalMinutes)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (item.linkedSoundCueId != null) {
                Text(
                    text = stringResource(R.string.showrunner_item_cue_badge),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (isActive) {
            PaPill(text = stringResource(R.string.showrunner_active), tone = PaTone.BRAND)
        }
        TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.showrunner_item_settings_short)) }
        TextButton(onClick = onMoveUp, enabled = !isFirst) { Text("▲") }
        TextButton(onClick = onMoveDown, enabled = !isLast) { Text("▼") }
        TextButton(onClick = onStart) { Text(stringResource(R.string.showrunner_start)) }
        TextButton(onClick = onRemove) { Text(stringResource(R.string.showrunner_remove)) }
    }
}

/**
 * 進行表の項目ごとの設定。固定開始時刻・連動するSE・遅延をまとめる。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleItemSettingsSheet(
    item: ScheduleItem,
    pads: List<SoundCue>,
    onDismiss: () -> Unit,
    onSetFixedTime: (Long?) -> Unit,
    onSetCue: (Long?) -> Unit,
    onSetDelayMs: (Long) -> Unit,
) {
    val dimens = LocalPaDimens.current
    val sheetState = rememberModalBottomSheetState()
    var fixedTimeText by remember(item.id) { mutableStateOf(DateTimeText.formatTime(item.fixedStartEpochMs)) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.gutter)
                .padding(bottom = dimens.spaceXl),
            verticalArrangement = Arrangement.spacedBy(dimens.space),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            OutlinedTextField(
                value = fixedTimeText,
                onValueChange = { value ->
                    fixedTimeText = value
                    val parsed = DateTimeText.parseTime(value)
                    onSetFixedTime(if (parsed != null) DateTimeText.toEpochMs(null, parsed) else null)
                },
                label = { Text(stringResource(R.string.showrunner_item_fixed_time)) },
                placeholder = { Text(stringResource(R.string.showrunner_anchor_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = stringResource(R.string.showrunner_item_cue_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceSm),
            ) {
                FilterChip(
                    selected = item.linkedSoundCueId == null,
                    onClick = { onSetCue(null) },
                    label = { Text(stringResource(R.string.showrunner_item_cue_none)) },
                )
                pads.forEach { cue ->
                    FilterChip(
                        selected = item.linkedSoundCueId == cue.id,
                        onClick = { onSetCue(cue.id) },
                        label = { Text(cue.title) },
                    )
                }
            }

            if (item.linkedSoundCueId != null) {
                Text(
                    text = stringResource(R.string.showrunner_item_cue_delay),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
                    verticalArrangement = Arrangement.spacedBy(dimens.spaceSm),
                ) {
                    CUE_DELAY_PRESETS_MS.forEach { presetMs ->
                        val selected = item.cueDelayMs == presetMs
                        if (selected) {
                            Button(onClick = { onSetDelayMs(presetMs) }) {
                                Text(stringResource(R.string.showrunner_item_cue_delay_seconds, presetMs / 1000f))
                            }
                        } else {
                            OutlinedButton(onClick = { onSetDelayMs(presetMs) }) {
                                Text(stringResource(R.string.showrunner_item_cue_delay_seconds, presetMs / 1000f))
                            }
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { onSetDelayMs((item.cueDelayMs - 500).coerceAtLeast(0)) },
                    ) { Text("−0.5秒") }
                    Text(
                        text = stringResource(R.string.showrunner_item_cue_delay_seconds, item.cueDelayMs / 1000f),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    OutlinedButton(
                        onClick = { onSetDelayMs(item.cueDelayMs + 500) },
                    ) { Text("+0.5秒") }
                }
            }
        }
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

/**
 * ハウリング測定・スペクトラムアナライザ。
 *
 * 本番タイマーのモニタと同じ考え方——本番中に画面を離れずレベル/ハウリング/スペクトラムを見る。
 */
@Composable
private fun MonitorTab(
    uiState: ShowRunnerUiState,
    onToggleMonitor: () -> Unit,
    onResetMax: () -> Unit,
    onClearLastFeedback: () -> Unit,
) {
    val dimens = LocalPaDimens.current

    PaCard(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceMd),
    ) {
        PaSectionHeader(
            title = stringResource(R.string.showrunner_monitor_title),
            subtitle = stringResource(R.string.showrunner_monitor_hint),
            trailing = {
                FilterChip(
                    selected = uiState.monitoring,
                    onClick = onToggleMonitor,
                    label = {
                        Text(
                            stringResource(
                                if (uiState.monitoring) {
                                    R.string.showrunner_monitor_stop
                                } else {
                                    R.string.showrunner_monitor_start
                                },
                            ),
                        )
                    },
                )
            },
        )

        uiState.monitorError?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (!uiState.monitoring) return@PaCard

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (uiState.hasLevel) {
                    stringResource(
                        R.string.showrunner_monitor_level,
                        "%.0f".format(uiState.levelDb),
                        "%.0f".format(uiState.maxLevelDb),
                        uiState.unitLabel,
                    )
                } else {
                    stringResource(R.string.showrunner_monitor_waiting)
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TextButton(onClick = onResetMax) {
                Text(stringResource(R.string.showrunner_monitor_reset_max))
            }
        }

        CalibrationBadge(profile = uiState.calibration)

        FeedbackAlertPanel(
            current = uiState.feedback,
            last = uiState.lastFeedback,
            onClearLast = onClearLastFeedback,
        )

        SpectrumChart(
            columnsDb = uiState.columnsDb,
            peakHoldDb = uiState.peakHoldDb,
            frequencies = uiState.frequencies,
            range = SpectrumRange.auto(uiState.columnsDb, MONITOR_SPAN_DB),
            height = 200.dp,
        )
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
private val EXTEND_PRESET_MINUTES = listOf(1, 3, 5, 10)
private val CUE_DELAY_PRESETS_MS = listOf(0L, 1_000L, 3_000L, 5_000L)

/** 本番モニタの縦軸の幅。細かく見る画面ではないので広めに固定する */
private const val MONITOR_SPAN_DB = 70.0

private fun formatCountdown(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
