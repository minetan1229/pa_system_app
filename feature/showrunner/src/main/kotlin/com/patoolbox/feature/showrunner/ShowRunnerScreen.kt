package com.patoolbox.feature.showrunner

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
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
import com.patoolbox.core.model.Job
import com.patoolbox.core.model.ShowModeSettings
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
 * 進行表のカウントダウン、SE の自動連動、ハウリング測定・スペクトラムアナライザ、
 * 本番モード（通知ミュート）を 1画面（3タブ）にまとめてある。
 * 本番中に画面を持ち替えさせない、という考え方の道具。
 */
@Composable
fun ShowRunnerScreen(
    onBack: () -> Unit,
    onOpenFeedback: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ShowRunnerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dimens = LocalPaDimens.current
    val context = LocalContext.current
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

    // 設定アプリでおやすみモードを許可してから戻ってくる導線があるので、
    // 画面に戻るたびに許可の状態を読み直す
    LifecycleResumeEffect(Unit) {
        viewModel.refreshNotificationPolicy()
        onPauseOrDispose { }
    }

    PaToolScaffold(
        tool = ToolId.SHOW_RUNNER,
        onBack = onBack,
        modifier = modifier,
    ) { innerPadding ->
        KeepScreenOn(enabled = uiState.running || uiState.monitoring || uiState.showModeActive)

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
                        onGrantPolicy = { context.startActivity(viewModel.notificationPolicySettingsIntent()) },
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
                        onOpenFeedback = onOpenFeedback,
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
        val index = uiState.schedule.indexOfFirst { it.id == item.id }
        ScheduleItemSettingsSheet(
            item = item,
            pads = uiState.pads,
            isFirst = index == 0,
            isLast = index == uiState.schedule.lastIndex,
            onDismiss = { settingsItemId = null },
            onRename = { title -> viewModel.renameItem(item.id, title) },
            onSetDuration = { mins -> viewModel.setItemDuration(item.id, mins) },
            onSetFixedTime = { epochMs -> viewModel.setItemFixedTime(item.id, epochMs) },
            onSetCue = { cueId -> viewModel.setCueLink(item.id, cueId) },
            onSetDelayMs = { delayMs -> viewModel.setCueDelayMs(item.id, delayMs) },
            onMoveUp = { viewModel.moveUp(item.id); settingsItemId = null },
            onMoveDown = { viewModel.moveDown(item.id); settingsItemId = null },
            onRemove = { viewModel.removeScheduleItem(item.id); settingsItemId = null },
            onStart = { viewModel.startItem(item.id); settingsItemId = null },
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
    onGrantPolicy: () -> Unit,
) {
    val dimens = LocalPaDimens.current
    var showImportDialog by rememberSaveable { mutableStateOf(false) }
    var importDone by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(dimens.space)) {
        // 初回ガイド（スケジュールが空のときだけ出す）
        if (uiState.schedule.isEmpty()) {
            UsageGuide()
        }

        ShowModeSection(
            uiState = uiState,
            onToggle = viewModel::toggleShowMode,
            onChange = viewModel::setShowMode,
            onGrantPolicy = onGrantPolicy,
        )
        CountdownSection(uiState = uiState, viewModel = viewModel)
        ScheduleSection(
            uiState = uiState,
            viewModel = viewModel,
            onOpenItemSettings = onOpenItemSettings,
            onImportFromJob = { showImportDialog = true },
        )
    }

    if (showImportDialog) {
        ImportJobDialog(
            jobs = uiState.availableJobs,
            onDismiss = { showImportDialog = false },
            onImport = { jobId ->
                viewModel.importFromJob(jobId)
                showImportDialog = false
                importDone = true
            },
        )
    }
}

/** 使い方の3ステップ。進行表が空のときだけ表示する */
@Composable
private fun UsageGuide() {
    val dimens = LocalPaDimens.current
    PaCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentPadding = dimens.spaceMd,
        verticalArrangement = Arrangement.spacedBy(dimens.spaceSm),
    ) {
        Text(
            text = stringResource(R.string.showrunner_guide_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        listOf(
            R.string.showrunner_guide_step1_label to R.string.showrunner_guide_step1_body,
            R.string.showrunner_guide_step2_label to R.string.showrunner_guide_step2_body,
            R.string.showrunner_guide_step3_label to R.string.showrunner_guide_step3_body,
        ).forEach { (labelRes, bodyRes) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
                modifier = Modifier.padding(vertical = dimens.spaceXs),
            ) {
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.width(72.dp),
                )
                Text(
                    text = stringResource(bodyRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// ─── 本番モード ──────────────────────────────────────────────────────────────

/**
 * 本番モードの折りたたみカード。
 * ShowTimerScreen の同じ機能を ShowRunner にも載せる——本番中に画面を切り替えさせない。
 */
@Composable
private fun ShowModeSection(
    uiState: ShowRunnerUiState,
    onToggle: () -> Unit,
    onChange: (ShowModeSettings) -> Unit,
    onGrantPolicy: () -> Unit,
) {
    val dimens = LocalPaDimens.current
    val settings = uiState.showMode

    PaCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = if (uiState.showModeActive) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        borderColor = if (uiState.showModeActive) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        verticalArrangement = Arrangement.spacedBy(dimens.spaceMd),
    ) {
        PaSectionHeader(
            title = stringResource(R.string.showrunner_show_mode_title),
            subtitle = stringResource(
                if (uiState.showModeActive) R.string.showrunner_show_mode_on
                else R.string.showrunner_show_mode_off,
            ),
            trailing = {
                Switch(checked = uiState.showModeActive, onCheckedChange = { onToggle() })
            },
        )

        ShowModeToggle(
            label = stringResource(R.string.showrunner_show_mode_silence),
            description = stringResource(R.string.showrunner_show_mode_silence_desc),
            checked = settings.silenceNotifications,
            onCheckedChange = { onChange(settings.copy(silenceNotifications = it)) },
        )

        if (settings.silenceNotifications) {
            ShowModeToggle(
                label = stringResource(R.string.showrunner_show_mode_alarms),
                description = stringResource(R.string.showrunner_show_mode_alarms_desc),
                checked = settings.allowAlarms,
                onCheckedChange = { onChange(settings.copy(allowAlarms = it)) },
            )
        }

        ShowModeToggle(
            label = stringResource(R.string.showrunner_show_mode_screen),
            description = stringResource(R.string.showrunner_show_mode_screen_desc),
            checked = settings.keepScreenOn,
            onCheckedChange = { onChange(settings.copy(keepScreenOn = it)) },
        )

        if (uiState.needsNotificationPolicyGrant) {
            Text(
                text = stringResource(R.string.showrunner_show_mode_permission_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            OutlinedButton(
                onClick = onGrantPolicy,
                modifier = Modifier.fillMaxWidth().heightIn(min = dimens.minTouch),
            ) {
                Text(stringResource(R.string.showrunner_show_mode_open_settings))
            }
        }
    }
}

@Composable
private fun ShowModeToggle(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val dimens = LocalPaDimens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimens.space),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// ─── カウントダウン ──────────────────────────────────────────────────────────

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
                caption = if (uiState.isOverrun) stringResource(R.string.showrunner_overrun) else null,
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EXTEND_PRESET_MINUTES.forEach { minutes ->
                MinuteChip(
                    minutes = minutes,
                    selected = minutes == draftMinutes,
                    onClick = { onDraftChange(minutes) },
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            OutlinedButton(
                onClick = { onDraftChange(draftMinutes - 1) },
                modifier = Modifier.heightIn(min = dimens.minTouch),
            ) { Text("−1") }
            Text(
                text = stringResource(R.string.showrunner_minutes, draftMinutes),
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedButton(
                onClick = { onDraftChange(draftMinutes + 1) },
                modifier = Modifier.heightIn(min = dimens.minTouch),
            ) { Text("+1") }
        }
        Button(
            onClick = onApply,
            modifier = Modifier.fillMaxWidth().heightIn(min = dimens.minTouch),
        ) {
            Text(stringResource(R.string.showrunner_extend_apply, draftMinutes))
        }
    }
}

// ─── 進行表 ──────────────────────────────────────────────────────────────────

@Composable
private fun ScheduleSection(
    uiState: ShowRunnerUiState,
    viewModel: ShowRunnerViewModel,
    onOpenItemSettings: (Long) -> Unit,
    onImportFromJob: () -> Unit = {},
) {
    val dimens = LocalPaDimens.current
    val timeline = remember(uiState.schedule, uiState.activeItemId, uiState.elapsedMillis, uiState.anchorEpochMs) {
        uiState.projectedTimeline(System.currentTimeMillis())
    }
    val timelineByItemId = remember(timeline) { timeline.associateBy { it.item.id } }

    PaPanel(
        title = stringResource(R.string.showrunner_schedule_title),
        subtitle = stringResource(R.string.showrunner_schedule_subtitle),
        trailing = {
            if (uiState.totalScheduleMinutes > 0) {
                Text(
                    text = stringResource(R.string.showrunner_total_duration, uiState.totalScheduleMinutes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    ) {
        AnchorTimeField(
            anchorEpochMs = uiState.anchorEpochMs,
            onSetAnchor = viewModel::setAnchorTime,
        )

        // ─ 追加フォーム ─
        AddItemForm(uiState = uiState, viewModel = viewModel)

        // 案件から取り込む
        if (uiState.availableJobs.isNotEmpty()) {
            OutlinedButton(
                onClick = onImportFromJob,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.showrunner_import_from_job))
            }
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
                    onStart = { viewModel.startItem(item.id) },
                    onOpenSettings = { onOpenItemSettings(item.id) },
                )
                if (index < uiState.schedule.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        modifier = Modifier.padding(vertical = dimens.spaceXs / 2),
                    )
                }
            }
        }
    }
}

@Composable
private fun AddItemForm(
    uiState: ShowRunnerUiState,
    viewModel: ShowRunnerViewModel,
) {
    val dimens = LocalPaDimens.current
    Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
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
        // 時間プリセット + 微調整
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.showrunner_draft_duration_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                text = if (uiState.draftMinutes == 0) {
                    stringResource(R.string.showrunner_duration_unset)
                } else {
                    stringResource(R.string.showrunner_minutes, uiState.draftMinutes)
                },
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
    }
}

/** 全体の開始予定時刻。空にすると時刻表示自体をやめる。 */
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
            supportingText = { Text(stringResource(R.string.showrunner_anchor_note)) },
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

/**
 * 進行表の1行。タップで設定シートを開く。「開始」だけをインラインに残す。
 */
@Composable
private fun ScheduleRow(
    item: ScheduleItem,
    timeRange: TimelineEntry?,
    isActive: Boolean,
    onStart: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val dimens = LocalPaDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenSettings)
            .padding(vertical = dimens.spaceXs),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // アクティブ状態のインジケーター
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                ),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (timeRange != null) {
                        stringResource(
                            R.string.showrunner_time_range,
                            DateTimeText.formatTime(timeRange.startAtEpochMs),
                            DateTimeText.formatTime(timeRange.endAtEpochMs),
                        )
                    } else if (item.totalMinutes == 0) {
                        stringResource(R.string.showrunner_duration_unset)
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
        }

        if (isActive) {
            PaPill(text = stringResource(R.string.showrunner_active), tone = PaTone.BRAND)
        }

        // 「開始」だけをインラインに残す。設定・移動・削除はタップで開くシートへ
        TextButton(
            onClick = onStart,
            colors = ButtonDefaults.textButtonColors(
                contentColor = if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Text(stringResource(R.string.showrunner_start))
        }
    }
}

/**
 * 進行表の項目設定。タップで開く。
 * 名前変更・時間・ピン留め時刻・SE連動・順序変更・削除をここにまとめる。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleItemSettingsSheet(
    item: ScheduleItem,
    pads: List<SoundCue>,
    isFirst: Boolean,
    isLast: Boolean,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onSetDuration: (Int) -> Unit,
    onSetFixedTime: (Long?) -> Unit,
    onSetCue: (Long?) -> Unit,
    onSetDelayMs: (Long) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onStart: () -> Unit,
) {
    val dimens = LocalPaDimens.current
    val sheetState = rememberModalBottomSheetState()
    var titleDraft by remember(item.id) { mutableStateOf(item.title) }
    var durationDraft by remember(item.id) { mutableStateOf(item.plannedMinutes) }
    var fixedTimeText by remember(item.id) { mutableStateOf(DateTimeText.formatTime(item.fixedStartEpochMs)) }
    var confirmingDelete by remember(item.id) { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.gutter)
                .padding(bottom = dimens.spaceXl),
            verticalArrangement = Arrangement.spacedBy(dimens.space),
        ) {
            // ─ 名前 ─
            OutlinedTextField(
                value = titleDraft,
                onValueChange = { titleDraft = it },
                label = { Text(stringResource(R.string.showrunner_item_edit_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (titleDraft != item.title && titleDraft.isNotBlank()) {
                        TextButton(onClick = { onRename(titleDraft) }) {
                            Text(stringResource(R.string.showrunner_item_edit_save))
                        }
                    }
                },
            )

            // ─ 持ち時間 ─
            Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
                Text(
                    text = stringResource(R.string.showrunner_item_edit_duration),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = {
                            durationDraft = (durationDraft - 1).coerceAtLeast(0)
                            onSetDuration(durationDraft)
                        },
                        modifier = Modifier.heightIn(min = dimens.minTouch),
                    ) { Text("−1分") }
                    Text(
                        text = if (durationDraft == 0) {
                            stringResource(R.string.showrunner_duration_unset)
                        } else {
                            stringResource(R.string.showrunner_minutes, durationDraft)
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    OutlinedButton(
                        onClick = {
                            durationDraft = (durationDraft + 1).coerceAtMost(600)
                            onSetDuration(durationDraft)
                        },
                        modifier = Modifier.heightIn(min = dimens.minTouch),
                    ) { Text("+1分") }
                }
            }

            // ─ ピン留め時刻 ─
            OutlinedTextField(
                value = fixedTimeText,
                onValueChange = { value ->
                    fixedTimeText = value
                    val parsed = DateTimeText.parseTime(value)
                    onSetFixedTime(if (parsed != null) DateTimeText.toEpochMs(null, parsed) else null)
                },
                label = { Text(stringResource(R.string.showrunner_item_fixed_time)) },
                placeholder = { Text(stringResource(R.string.showrunner_anchor_hint)) },
                supportingText = { Text(stringResource(R.string.showrunner_item_fixed_time_note)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // ─ SE連動 ─
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
                    )
                    OutlinedButton(
                        onClick = { onSetDelayMs(item.cueDelayMs + 500) },
                    ) { Text("+0.5秒") }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ─ 操作ボタン群 ─
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
            ) {
                OutlinedButton(
                    onClick = onMoveUp,
                    enabled = !isFirst,
                    modifier = Modifier.weight(1f),
                ) { Text("▲ 上へ") }
                OutlinedButton(
                    onClick = onMoveDown,
                    enabled = !isLast,
                    modifier = Modifier.weight(1f),
                ) { Text("▼ 下へ") }
                Button(
                    onClick = onStart,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.showrunner_start)) }
            }

            if (confirmingDelete) {
                Text(
                    text = stringResource(R.string.showrunner_remove_confirm, item.title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
                    OutlinedButton(
                        onClick = { confirmingDelete = false },
                        modifier = Modifier.weight(1f).heightIn(min = dimens.minTouch),
                    ) { Text(stringResource(R.string.showrunner_remove_cancel)) }
                    Button(
                        onClick = onRemove,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f).heightIn(min = dimens.minTouch),
                    ) { Text(stringResource(R.string.showrunner_remove)) }
                }
            } else {
                TextButton(
                    onClick = { confirmingDelete = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.showrunner_remove),
                        color = MaterialTheme.colorScheme.error,
                    )
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
            Text(
                if (minutes == 0) stringResource(R.string.showrunner_duration_unset)
                else stringResource(R.string.showrunner_minutes, minutes),
            )
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = Modifier.heightIn(min = dimens.minTouch)) {
            Text(
                if (minutes == 0) stringResource(R.string.showrunner_duration_unset)
                else stringResource(R.string.showrunner_minutes, minutes),
            )
        }
    }
}

// ─── モニタータブ ────────────────────────────────────────────────────────────

/**
 * ハウリング測定・スペクトラムアナライザ。
 * EQ が原因と疑われるときは「ハウリング検知」画面へのジャンプボタンを出す。
 */
@Composable
private fun MonitorTab(
    uiState: ShowRunnerUiState,
    onToggleMonitor: () -> Unit,
    onResetMax: () -> Unit,
    onClearLastFeedback: () -> Unit,
    onOpenFeedback: () -> Unit,
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
                                if (uiState.monitoring) R.string.showrunner_monitor_stop
                                else R.string.showrunner_monitor_start,
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

        if (!uiState.monitoring) {
            // モニタを止めているときも「ハウリング検知へ」は出す
            OutlinedButton(
                onClick = onOpenFeedback,
                modifier = Modifier.fillMaxWidth().heightIn(min = dimens.minTouch),
            ) {
                Text(stringResource(R.string.showrunner_to_feedback))
            }
            return@PaCard
        }

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

        // ハウリングが検知された、またはされていたとき → 詳細画面へのボタンを強調
        val showFeedbackButton = uiState.feedback != null || uiState.lastFeedback != null
        if (showFeedbackButton) {
            Button(
                onClick = onOpenFeedback,
                modifier = Modifier.fillMaxWidth().heightIn(min = dimens.minTouch),
            ) {
                Text(stringResource(R.string.showrunner_to_feedback_urgent))
            }
        } else {
            OutlinedButton(
                onClick = onOpenFeedback,
                modifier = Modifier.fillMaxWidth().heightIn(min = dimens.minTouch),
            ) {
                Text(stringResource(R.string.showrunner_to_feedback))
            }
        }

        SpectrumChart(
            columnsDb = uiState.columnsDb,
            peakHoldDb = uiState.peakHoldDb,
            frequencies = uiState.frequencies,
            range = SpectrumRange.auto(uiState.columnsDb, MONITOR_SPAN_DB),
            height = 200.dp,
        )
    }
}

// ─── SEパッドタブ ────────────────────────────────────────────────────────────

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
 * パッド1枚。押すと再生・停止、長押しで設定シートを開く。
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
            color = if (playing) contrastingInk(accent) else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * パッドの設定。長押しで開く。
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
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.showrunner_pad_settings_sync),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.showrunner_pad_settings_sync_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(dimens.space))
                Switch(
                    checked = cue.loop,
                    onCheckedChange = { checked ->
                        if (checked) onMarkSync(cue) else onMarkOneShot(cue)
                    },
                )
            }
        }
    }
}

private val PRESET_MINUTES = listOf(0, 1, 3, 5, 10, 15, 20, 30)
private val EXTEND_PRESET_MINUTES = listOf(1, 3, 5, 10)
private val CUE_DELAY_PRESETS_MS = listOf(0L, 1_000L, 3_000L, 5_000L)

private const val MONITOR_SPAN_DB = 70.0

private fun formatCountdown(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

/** 案件管理で作った進行表を選んで取り込むダイアログ */
@Composable
private fun ImportJobDialog(
    jobs: List<Job>,
    onDismiss: () -> Unit,
    onImport: (Long) -> Unit,
) {
    val dimens = LocalPaDimens.current
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.showrunner_import_from_job)) },
        text = {
            if (jobs.isEmpty()) {
                Text(
                    text = stringResource(R.string.showrunner_import_from_job_none),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceXs)) {
                    Text(
                        text = stringResource(R.string.showrunner_import_from_job_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = dimens.spaceXs))
                    jobs.forEach { job ->
                        TextButton(
                            onClick = { onImport(job.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = job.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.showrunner_import_from_job_cancel))
            }
        },
    )
}
