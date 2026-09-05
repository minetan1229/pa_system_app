package com.patoolbox.feature.showrunner

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.component.BigReadout
import com.patoolbox.core.designsystem.component.PaCard
import com.patoolbox.core.designsystem.component.PaFilterChip
import com.patoolbox.core.designsystem.component.PaNotice
import com.patoolbox.core.designsystem.component.PaPanel
import com.patoolbox.core.designsystem.component.PaPill
import com.patoolbox.core.designsystem.component.PaSectionHeader
import com.patoolbox.core.designsystem.component.PaTone
import com.patoolbox.core.designsystem.component.content
import com.patoolbox.core.designsystem.component.contrastingInk
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.dsp.FeedbackSensitivity
import com.patoolbox.core.dsp.FeedbackTracker
import com.patoolbox.core.model.Job
import com.patoolbox.core.model.ShowModeSettings
import com.patoolbox.core.model.SoundCue
import com.patoolbox.core.model.TimelineEntry
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.reference.BandDictionary
import com.patoolbox.core.ui.DateTimeText
import com.patoolbox.core.ui.component.CalibrationBadge
import com.patoolbox.core.ui.component.ChartLegend
import com.patoolbox.core.ui.component.KeepScreenOn
import com.patoolbox.core.ui.component.PaToolScaffold
import com.patoolbox.core.ui.component.SpectrumChart
import com.patoolbox.core.ui.component.SpectrumMarker
import com.patoolbox.core.ui.component.SpectrumRange
import com.patoolbox.core.ui.component.formatHz
import com.patoolbox.core.ui.identityColor
import com.patoolbox.core.ui.R as CoreUiR

/**
 * 本番万能コントローラー。
 *
 * **見るための画面**にしてある。1画面を上から
 * 　いま何分（大きい数字）→ 進行表 → ハウリング → 本番モード → SE
 * と流し、タブも入力フォームも置かない。進行表そのものは「案件管理」で
 * 日付ごと作っておき、この画面は当日それを読み込んで**進めるだけ**にする。
 * 本番タイマーと同じ「本番中に読める密度」を、機能を足しても崩さないための作り。
 *
 * ハウリングだけは例外的に厚い。周波数を出すだけでは打つ手が決まらないので、
 * 帯域ごとの直し方（[BandDictionary]）まで**この画面の中で**開けるようにしてある
 * （別画面へ飛ばすと、本番中は戻ってこられない）。
 *
 * @param autoStart ホームの「もう始まっています」から入ってきたとき true。
 *   読み込み済みの進行表の、いま当たっている項目から数え始める
 */
@Composable
fun ShowRunnerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    autoStart: Boolean = false,
    onOpenSchedules: () -> Unit = {},
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
    var showImportDialog by rememberSaveable { mutableStateOf(false) }
    var showFeedbackDetail by rememberSaveable { mutableStateOf(false) }
    var promptDismissedFor by rememberSaveable { mutableStateOf<Long?>(null) }

    // 設定アプリでおやすみモードを許可してから戻る導線があるので、画面に戻るたびに読み直す。
    // 同時に「予定ではもう始まっている項目」も取り直す——時計は進んでも Flow は流れない
    LifecycleResumeEffect(Unit) {
        viewModel.refreshNotificationPolicy()
        viewModel.refreshSchedulePosition()
        onPauseOrDispose { }
    }

    // ホームから「スタート」で入ってきたとき。進行表が読み込まれ次第、1度だけ走らせる
    LaunchedEffect(autoStart, uiState.schedule.isNotEmpty()) {
        if (autoStart && uiState.schedule.isNotEmpty() && uiState.activeItemId == null) {
            viewModel.startFromNow()
        }
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
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.gutter, vertical = dimens.gutterSmall),
            verticalArrangement = Arrangement.spacedBy(dimens.space),
        ) {
            LoadedShowRow(
                uiState = uiState,
                onPickShow = { showImportDialog = true },
                onClear = viewModel::clearSchedule,
            )

            // 「予定ではもう始まっています」。押すまで勝手には走らせない——
            // 現場では予定より早く始まることも遅れることもあり、決めるのは人
            val suggested = uiState.suggestedItem
            if (suggested != null && promptDismissedFor != suggested.id) {
                StartPrompt(
                    title = suggested.title,
                    plannedStartEpochMs = uiState.suggestedStartEpochMs,
                    onStart = viewModel::startSuggested,
                    onLater = { promptDismissedFor = suggested.id },
                )
            }

            CountdownSection(uiState = uiState, viewModel = viewModel)

            ScheduleSection(
                uiState = uiState,
                onStart = { id -> viewModel.startItem(id) },
                onOpenItemSettings = { settingsItemId = it },
                onPickShow = { showImportDialog = true },
                onOpenSchedules = onOpenSchedules,
            )

            FeedbackSection(
                uiState = uiState,
                onToggleMonitor = {
                    when {
                        uiState.monitoring -> viewModel.stopMonitor()
                        viewModel.hasMicPermission() -> viewModel.startMonitor()
                        else -> micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onResetMax = viewModel::resetMaxLevel,
                onOpenDetail = { showFeedbackDetail = true },
            )

            ShowModeSection(
                uiState = uiState,
                onToggle = viewModel::toggleShowMode,
                onChange = viewModel::setShowMode,
                onGrantPolicy = {
                    context.startActivity(viewModel.notificationPolicySettingsIntent())
                },
            )

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

    if (showFeedbackDetail) {
        FeedbackDetailSheet(
            uiState = uiState,
            onDismiss = { showFeedbackDetail = false },
            onSetSensitivity = viewModel::setSensitivity,
            onSetSort = viewModel::setFeedbackSort,
            onClearHistory = viewModel::clearFeedbackHistory,
        )
    }

    if (showImportDialog) {
        ImportJobDialog(
            jobs = uiState.availableJobs,
            onDismiss = { showImportDialog = false },
            onImport = { jobId ->
                viewModel.importFromJob(jobId)
                showImportDialog = false
            },
        )
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
            onSetCue = { cueId -> viewModel.setCueLink(item.id, cueId) },
            onSetDelayMs = { delayMs -> viewModel.setCueDelayMs(item.id, delayMs) },
            onMoveUp = { viewModel.moveUp(item.id); settingsItemId = null },
            onMoveDown = { viewModel.moveDown(item.id); settingsItemId = null },
            onRemove = { viewModel.removeScheduleItem(item.id); settingsItemId = null },
            onStart = { viewModel.startItem(item.id); settingsItemId = null },
        )
    }
}

// ─── 読み込んでいる進行表 ────────────────────────────────────────────────────

/**
 * いまどの進行表を握っているか。
 *
 * 自動で入ったのか自分で選んだのかを必ず書く。黙って中身が入れ替わったように
 * 見えるのが、本番前にいちばん怖い。
 */
@Composable
private fun LoadedShowRow(
    uiState: ShowRunnerUiState,
    onPickShow: () -> Unit,
    onClear: () -> Unit,
) {
    val dimens = LocalPaDimens.current
    val name = uiState.loadedShowName ?: return

    PaCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentPadding = dimens.spaceMd,
        verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
    ) {
        Text(
            text = stringResource(
                if (uiState.autoLoaded) {
                    R.string.showrunner_loaded_auto
                } else {
                    R.string.showrunner_loaded_manual
                },
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = stringResource(
                R.string.showrunner_loaded_name,
                name,
                DateTimeText.formatTime(uiState.anchorEpochMs),
                uiState.totalScheduleMinutes,
            ),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
            TextButton(onClick = onPickShow) {
                Text(stringResource(R.string.showrunner_change_show))
            }
            TextButton(onClick = onClear) {
                Text(stringResource(R.string.showrunner_clear_show))
            }
        }
    }
}

/** 予定の時刻を過ぎている項目があるときだけ出す確認。 */
@Composable
private fun StartPrompt(
    title: String,
    plannedStartEpochMs: Long?,
    onStart: () -> Unit,
    onLater: () -> Unit,
) {
    val dimens = LocalPaDimens.current

    PaCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        borderColor = MaterialTheme.colorScheme.primary,
        contentPadding = dimens.space,
        verticalArrangement = Arrangement.spacedBy(dimens.spaceMd),
    ) {
        Text(
            text = stringResource(R.string.showrunner_prompt_title, title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = stringResource(
                R.string.showrunner_prompt_body,
                DateTimeText.formatTime(plannedStartEpochMs),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
            Button(
                onClick = onStart,
                modifier = Modifier.weight(1f).heightIn(min = dimens.minTouch),
            ) {
                Text(stringResource(R.string.showrunner_prompt_start))
            }
            OutlinedButton(
                onClick = onLater,
                modifier = Modifier.heightIn(min = dimens.minTouch),
            ) {
                Text(stringResource(R.string.showrunner_prompt_later))
            }
        }
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
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentPadding = dimens.space,
        verticalArrangement = Arrangement.spacedBy(dimens.spaceMd),
    ) {
        if (active == null) {
            Text(
                text = stringResource(R.string.showrunner_no_active),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@PaCard
        }

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
                modifier = Modifier.weight(2f).heightIn(min = dimens.minTouch * 1.3f),
            ) {
                Text(
                    stringResource(
                        if (uiState.running) {
                            CoreUiR.string.measure_stop
                        } else {
                            R.string.showrunner_resume
                        },
                    ),
                )
            }
            OutlinedButton(
                onClick = viewModel::resetActive,
                modifier = Modifier.weight(1f).heightIn(min = dimens.minTouch * 1.3f),
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

/** 延長操作。プリセットを押すとその値が仮選択され、+/− で微調整してから適用する。 */
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
    onStart: (Long) -> Unit,
    onOpenItemSettings: (Long) -> Unit,
    onPickShow: () -> Unit,
    onOpenSchedules: () -> Unit,
) {
    val dimens = LocalPaDimens.current
    val timeline = remember(
        uiState.schedule,
        uiState.activeItemId,
        uiState.elapsedMillis,
        uiState.anchorEpochMs,
    ) {
        uiState.projectedTimeline(System.currentTimeMillis())
    }
    val timelineByItemId = remember(timeline) { timeline.associateBy { it.item.id } }

    PaPanel(
        title = stringResource(R.string.showrunner_schedule_title),
        subtitle = stringResource(R.string.showrunner_schedule_subtitle),
        trailing = {
            if (uiState.totalScheduleMinutes > 0) {
                Text(
                    text = stringResource(
                        R.string.showrunner_total_duration,
                        uiState.totalScheduleMinutes,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    ) {
        if (uiState.schedule.isEmpty()) {
            PaNotice(
                title = stringResource(R.string.showrunner_empty_title),
                body = stringResource(R.string.showrunner_empty_body),
                tone = PaTone.INFO,
            )
        } else {
            uiState.schedule.forEachIndexed { index, item ->
                ScheduleRow(
                    item = item,
                    timeRange = timelineByItemId[item.id],
                    isActive = item.id == uiState.activeItemId,
                    onStart = { onStart(item.id) },
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

        OutlinedButton(
            onClick = onPickShow,
            enabled = uiState.availableJobs.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().heightIn(min = dimens.minTouch),
        ) {
            Text(stringResource(R.string.showrunner_import_from_job))
        }
        TextButton(onClick = onOpenSchedules, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.showrunner_open_schedules))
        }
    }
}

/** 進行表の1行。タップで設定シートを開く。「開始」だけをインラインに残す。 */
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
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
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
                    text = when {
                        timeRange != null -> stringResource(
                            R.string.showrunner_time_range,
                            DateTimeText.formatTime(timeRange.startAtEpochMs),
                            DateTimeText.formatTime(timeRange.endAtEpochMs),
                        )

                        item.totalMinutes == 0 -> stringResource(R.string.showrunner_duration_unset)
                        else -> stringResource(R.string.showrunner_minutes, item.totalMinutes)
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

        TextButton(
            onClick = onStart,
            colors = ButtonDefaults.textButtonColors(
                contentColor = if (isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            ),
        ) {
            Text(stringResource(R.string.showrunner_start))
        }
    }
}

/**
 * 進行表の項目設定。行のタップで開く。
 *
 * 当日その場で直すぶんだけを置く（名前・持ち時間・SE連動・順序・削除）。
 * 日付や全体の開始時刻は「案件管理」の進行表側で決めるので、ここには置かない——
 * 同じものを2か所で直せると、どちらが本物か分からなくなる。
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
    var confirmingDelete by remember(item.id) { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.gutter)
                .padding(bottom = dimens.spaceXl),
            verticalArrangement = Arrangement.spacedBy(dimens.space),
        ) {
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
                        val label = stringResource(
                            R.string.showrunner_item_cue_delay_seconds,
                            presetMs / 1000f,
                        )
                        if (item.cueDelayMs == presetMs) {
                            Button(onClick = { onSetDelayMs(presetMs) }) { Text(label) }
                        } else {
                            OutlinedButton(onClick = { onSetDelayMs(presetMs) }) { Text(label) }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
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
    val label = if (minutes == 0) {
        stringResource(R.string.showrunner_duration_unset)
    } else {
        stringResource(R.string.showrunner_minutes, minutes)
    }
    if (selected) {
        Button(onClick = onClick, modifier = Modifier.heightIn(min = dimens.minTouch)) {
            Text(label)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = Modifier.heightIn(min = dimens.minTouch)) {
            Text(label)
        }
    }
}

// ─── ハウリング ──────────────────────────────────────────────────────────────

/**
 * ハウリング検出（本編は下のシート）。
 *
 * ここに出すのは「いま鳴っているか」と「いちばん長く鳴っているのはどれか」だけ。
 * 本番中に読めるのはこの2つが限界で、直し方はボタン1つ先に置く。
 */
@Composable
private fun FeedbackSection(
    uiState: ShowRunnerUiState,
    onToggleMonitor: () -> Unit,
    onResetMax: () -> Unit,
    onOpenDetail: () -> Unit,
) {
    val dimens = LocalPaDimens.current
    val worst = uiState.worstFeedback
    val ringing = uiState.feedback != null

    PaCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = if (ringing) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        borderColor = if (ringing) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        verticalArrangement = Arrangement.spacedBy(dimens.spaceMd),
    ) {
        PaSectionHeader(
            title = stringResource(R.string.showrunner_feedback_title),
            subtitle = stringResource(R.string.showrunner_feedback_hint),
            trailing = {
                PaFilterChip(
                    text = stringResource(
                        if (uiState.monitoring) {
                            R.string.showrunner_monitor_stop
                        } else {
                            R.string.showrunner_monitor_start
                        },
                    ),
                    selected = uiState.monitoring,
                    onClick = onToggleMonitor,
                    accent = ToolId.FEEDBACK_FINDER.identityColor(),
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

        BigReadout(
            value = worst?.let { "%.0f".format(it.frequencyHz) } ?: "----",
            unit = if (worst != null) "Hz" else null,
            label = worst?.let { "${it.noteName} / ${it.bandLabel} Hz 帯" },
            caption = when {
                worst == null && !uiState.monitoring ->
                    stringResource(R.string.showrunner_feedback_waiting)

                worst == null -> stringResource(R.string.showrunner_feedback_none)
                else -> stringResource(
                    R.string.showrunner_feedback_worst_caption,
                    formatDuration(worst.totalRingingMs),
                    worst.episodes,
                    formatDuration(worst.longestRunMs),
                )
            },
            valueColor = when {
                worst == null -> MaterialTheme.colorScheme.onSurfaceVariant
                worst.isActive -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            },
        )

        if (uiState.monitoring) {
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
        }

        // 直し方は同じ画面の中で開く。別の画面へ飛ばすと、本番中は戻ってこられない
        val count = uiState.feedbackTracks.size
        val open: @Composable () -> Unit = {
            Text(
                text = if (count > 0) {
                    stringResource(R.string.showrunner_feedback_open, count)
                } else {
                    stringResource(R.string.showrunner_feedback_open_empty)
                },
            )
        }
        if (ringing || count > 0) {
            Button(
                onClick = onOpenDetail,
                modifier = Modifier.fillMaxWidth().heightIn(min = dimens.minTouch),
            ) { open() }
        } else {
            OutlinedButton(
                onClick = onOpenDetail,
                modifier = Modifier.fillMaxWidth().heightIn(min = dimens.minTouch),
            ) { open() }
        }
    }
}

/**
 * ハウリングの直し方。
 *
 * ハウリング検出の画面と同じ中身をシートで開く。
 * 　上＝スペクトラム図（いま鳴っている場所に印）
 * 　中＝感度と並べ替え
 * 　下＝1本ずつの履歴と、その帯域で打つ手（[BandDictionary]）
 * 並べ替えの既定を **累計時間順** にしてあるのがこの一覧の要点で、
 * 潰したいのは一瞬の派手な発振ではなく、鳴り続けて本番を壊す方だから。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedbackDetailSheet(
    uiState: ShowRunnerUiState,
    onDismiss: () -> Unit,
    onSetSensitivity: (FeedbackSensitivity) -> Unit,
    onSetSort: (FeedbackTracker.Sort) -> Unit,
    onClearHistory: () -> Unit,
) {
    val dimens = LocalPaDimens.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accent = ToolId.FEEDBACK_FINDER.identityColor()

    val markers = uiState.feedbackTracks
        // 印は多くても6本。それ以上は図が線だらけになって波形が読めない
        .sortedByDescending { it.totalRingingMs }
        .take(MAX_MARKERS)
        .map { track ->
            SpectrumMarker(
                frequencyHz = track.frequencyHz,
                label = formatHz(track.frequencyHz),
                active = track.isActive,
                weight = (track.totalRingingMs / MARKER_FULL_WEIGHT_MS.toFloat()).coerceIn(0f, 1f),
            )
        }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.gutter)
                .padding(bottom = dimens.spaceXl),
            verticalArrangement = Arrangement.spacedBy(dimens.space),
        ) {
            PaSectionHeader(
                title = stringResource(R.string.showrunner_feedback_detail_title),
                subtitle = stringResource(
                    R.string.showrunner_feedback_elapsed,
                    formatDuration(uiState.feedbackElapsedMs),
                ),
                trailing = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.showrunner_feedback_close))
                    }
                },
            )

            Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceXs)) {
                SpectrumChart(
                    columnsDb = uiState.columnsDb,
                    frequencies = uiState.frequencies,
                    range = SpectrumRange.auto(uiState.columnsDb, SPAN_DB),
                    peakHoldDb = uiState.peakHoldDb,
                    markers = markers,
                    height = CHART_HEIGHT,
                )
                ChartLegend(
                    entries = listOf(
                        stringResource(R.string.showrunner_feedback_legend_now) to
                            MaterialTheme.colorScheme.error,
                        stringResource(R.string.showrunner_feedback_legend_past) to
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        stringResource(R.string.showrunner_feedback_legend_peak) to
                            MaterialTheme.colorScheme.tertiary,
                    ),
                )
            }

            ChipRow(label = stringResource(R.string.showrunner_feedback_sensitivity)) {
                FeedbackSensitivity.entries.forEach { entry ->
                    PaFilterChip(
                        text = entry.label,
                        selected = uiState.sensitivity == entry,
                        onClick = { onSetSensitivity(entry) },
                        accent = accent,
                    )
                }
            }
            Text(
                text = uiState.sensitivity.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ChipRow(label = stringResource(R.string.showrunner_feedback_sort)) {
                FeedbackTracker.Sort.entries.forEach { sort ->
                    PaFilterChip(
                        text = sort.label,
                        selected = uiState.feedbackSort == sort,
                        onClick = { onSetSort(sort) },
                        accent = accent,
                    )
                }
            }

            if (uiState.feedbackTracks.isEmpty()) {
                PaNotice(
                    title = stringResource(
                        if (uiState.monitoring) {
                            R.string.showrunner_feedback_none
                        } else {
                            R.string.showrunner_feedback_waiting
                        },
                    ),
                    body = stringResource(R.string.showrunner_feedback_empty_body),
                    tone = PaTone.INFO,
                )
            } else {
                uiState.feedbackTracks.forEach { track ->
                    TrackPanel(track = track, elapsedMs = uiState.feedbackElapsedMs)
                }
            }

            OutlinedButton(
                onClick = onClearHistory,
                enabled = uiState.feedbackTracks.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().heightIn(min = dimens.minTouch),
            ) {
                Text(stringResource(R.string.showrunner_feedback_clear))
            }
        }
    }
}

@Composable
private fun ChipRow(label: String, content: @Composable () -> Unit) {
    val dimens = LocalPaDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

/**
 * ハウリング1本ぶんの履歴。
 *
 * 数字を4つ（累計・最長連続・回数・突出量）並べるのは多いが、
 * この4つが揃わないと「定在波なのか、演者が動いただけなのか」を切り分けられない。
 */
@Composable
private fun TrackPanel(track: FeedbackTracker.Track, elapsedMs: Long) {
    val dimens = LocalPaDimens.current
    val tone = if (track.isActive) PaTone.DANGER else PaTone.NEUTRAL
    val band = BandDictionary.at(track.frequencyHz)

    PaPanel(
        title = stringResource(
            R.string.showrunner_feedback_track_title,
            "%.0f".format(track.frequencyHz),
            track.noteName,
        ),
        subtitle = band?.let { "${it.label}：${it.oneLiner}" }
            ?: stringResource(R.string.showrunner_feedback_band, track.bandLabel),
        rail = tone.content(),
        trailing = {
            PaPill(
                text = stringResource(
                    if (track.isActive) {
                        R.string.showrunner_feedback_state_now
                    } else {
                        R.string.showrunner_feedback_state_past
                    },
                ),
                tone = tone,
            )
        },
    ) {
        RingingBar(
            totalMs = track.totalRingingMs,
            elapsedMs = elapsedMs,
            accent = tone.content(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceMd)) {
            StatColumn(
                label = stringResource(R.string.showrunner_feedback_stat_total),
                value = formatDuration(track.totalRingingMs),
            )
            StatColumn(
                label = stringResource(R.string.showrunner_feedback_stat_longest),
                value = formatDuration(track.longestRunMs),
            )
            StatColumn(
                label = stringResource(R.string.showrunner_feedback_stat_episodes),
                value = stringResource(R.string.showrunner_feedback_stat_times, track.episodes),
            )
            StatColumn(
                label = stringResource(R.string.showrunner_feedback_stat_prominence),
                value = "%.0f dB".format(track.peakProminenceDb),
            )
        }

        Text(
            text = when {
                track.isSustained -> stringResource(R.string.showrunner_feedback_verdict_sustained)
                track.isDrifting -> stringResource(
                    R.string.showrunner_feedback_verdict_drifting,
                    "%.0f".format(track.lowestHz),
                    "%.0f".format(track.highestHz),
                )

                track.episodes >= REPEATING_EPISODES ->
                    stringResource(R.string.showrunner_feedback_verdict_repeating)

                else -> stringResource(R.string.showrunner_feedback_verdict_short)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        // 帯域ごとの「打つ手」。周波数だけ出しても卓の前では動けない
        band?.feedbackNote?.let { note ->
            Text(
                text = note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 鳴っていた時間の棒。分母は記録開始からの経過時間。数字も必ず添える */
@Composable
private fun RingingBar(totalMs: Long, elapsedMs: Long, accent: Color) {
    val dimens = LocalPaDimens.current
    val ratio = if (elapsedMs > 0) (totalMs.toFloat() / elapsedMs).coerceIn(0f, 1f) else 0f

    Row(
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(BAR_HEIGHT)
                .clip(RoundedCornerShape(dimens.cornerSmall))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio)
                    .height(BAR_HEIGHT)
                    .clip(RoundedCornerShape(dimens.cornerSmall))
                    .background(accent),
            )
        }
        Text(
            text = "%.0f%%".format(ratio * 100),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(PERCENT_COLUMN_WIDTH),
        )
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ─── 本番モード ──────────────────────────────────────────────────────────────

/**
 * 本番モード。既定では見出しとスイッチだけを出し、細かい設定は開いたときだけ。
 * 本番中に読む必要がない設定に場所を取らせない。
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
    var expanded by rememberSaveable { mutableStateOf(false) }

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
                if (uiState.showModeActive) {
                    R.string.showrunner_show_mode_on
                } else {
                    R.string.showrunner_show_mode_off
                },
            ),
            trailing = {
                Switch(checked = uiState.showModeActive, onCheckedChange = { onToggle() })
            },
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

        if (!expanded) {
            TextButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.showrunner_show_mode_details))
            }
            return@PaCard
        }

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

// ─── SEパッド ────────────────────────────────────────────────────────────────

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
                TextButton(onClick = onStopAll) {
                    Text(stringResource(R.string.showrunner_pads_stop_all))
                }
            }
        },
    ) {
        if (!uiState.canAddMorePads) {
            PaNotice(
                title = stringResource(R.string.showrunner_pads_limit),
                tone = PaTone.WARNING,
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

        OutlinedButton(
            onClick = onImport,
            enabled = uiState.canAddMorePads && !uiState.importing,
            modifier = Modifier.fillMaxWidth().heightIn(min = dimens.minTouch),
        ) {
            Text(
                stringResource(
                    if (uiState.importing) {
                        R.string.showrunner_importing
                    } else {
                        R.string.showrunner_import
                    },
                ),
            )
        }
        Text(
            text = stringResource(R.string.showrunner_pads_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** パッド1枚。押すと再生・停止、長押しで設定シートを開く。 */
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

/** パッドの設定。長押しで開く。 */
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
            Text(text = cue.title, style = MaterialTheme.typography.titleLarge)
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

/** 案件管理で作った進行表を選んで取り込むダイアログ。日付ごと読み込む */
@Composable
private fun ImportJobDialog(
    jobs: List<Job>,
    onDismiss: () -> Unit,
    onImport: (Long) -> Unit,
) {
    val dimens = LocalPaDimens.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.showrunner_import_from_job)) },
        text = {
            if (jobs.isEmpty()) {
                Text(
                    text = stringResource(R.string.showrunner_import_from_job_none),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
                ) {
                    Text(
                        text = stringResource(R.string.showrunner_import_from_job_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = dimens.spaceXs))
                    jobs.forEach { job ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onImport(job.id) }
                                .padding(vertical = dimens.spaceSm),
                        ) {
                            Text(text = job.name, style = MaterialTheme.typography.bodyLarge)
                            val date = DateTimeText.formatDate(
                                job.eventDateEpochMs ?: job.loadInAtEpochMs,
                            )
                            if (date.isNotEmpty()) {
                                Text(
                                    text = stringResource(
                                        R.string.showrunner_import_job_date,
                                        date,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
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

private val EXTEND_PRESET_MINUTES = listOf(1, 3, 5, 10)
private val CUE_DELAY_PRESETS_MS = listOf(0L, 1_000L, 3_000L, 5_000L)

/** 図の縦軸の幅。ハウリングの山は暗騒音から 30dB 以上立つので広めに取る */
private const val SPAN_DB = 70.0

/** 印を立てる本数の上限 */
private const val MAX_MARKERS = 6

/** この時間まで鳴っていたら印の線を最大の太さにする */
private const val MARKER_FULL_WEIGHT_MS = 10_000L

/** 何回に分けて鳴ったら「繰り返している」と言うか */
private const val REPEATING_EPISODES = 3

private val CHART_HEIGHT = 220.dp
private val BAR_HEIGHT = 8.dp
private val PERCENT_COLUMN_WIDTH = 44.dp

private fun formatCountdown(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

/**
 * 時間の表示。
 * 秒だけだと「184秒」になり、ミリ秒まで出すと本番中に読めない。1分を境に単位を変える。
 */
private fun formatDuration(ms: Long): String = when {
    ms < 1_000 -> "%d ms".format(ms)
    ms < 60_000 -> "%.1f 秒".format(ms / 1000.0)
    else -> "%d分%02d秒".format(ms / 60_000, (ms % 60_000) / 1000)
}
