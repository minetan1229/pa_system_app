package com.patoolbox.feature.showtimer

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.component.BigReadout
import com.patoolbox.core.designsystem.component.PaCard
import com.patoolbox.core.designsystem.component.PaSectionHeader
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.ShowModeSettings
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.ui.component.CalibrationBadge
import com.patoolbox.core.ui.component.FeedbackAlertPanel
import com.patoolbox.core.ui.component.KeepScreenOn
import com.patoolbox.core.ui.component.PaToolScaffold
import com.patoolbox.core.ui.component.SpectrumChart
import com.patoolbox.core.ui.component.SpectrumRange

@Composable
fun ShowTimerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShowTimerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dimens = LocalPaDimens.current
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // 設定アプリでおやすみモードを許可してから戻ってくる導線があるので、
    // 画面に戻るたびに許可の状態を読み直す
    LifecycleResumeEffect(Unit) {
        viewModel.refreshNotificationPolicy()
        onPauseOrDispose { }
    }

    // モニタは任意なので、画面全体をマイク権限で塞がない。
    // 「時間だけ見たい」使い方を潰すと、本番前に開けなくなる
    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.startMonitor()
    }

    PaToolScaffold(
        tool = ToolId.SHOW_TIMER,
        onBack = onBack,
        modifier = modifier,
        title = stringResource(R.string.timer_title),
    ) { innerPadding ->
        // ステージから見えるように置いて使うので、既定では走っていなくても画面を消さない。
        // 本番モードに入っている間は、その設定に従う
        KeepScreenOn(enabled = uiState.shouldKeepScreenOn)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = dimens.gutter),
            verticalArrangement = Arrangement.spacedBy(dimens.space),
        ) {
            TimerReadout(uiState = uiState)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
            ) {
                Button(
                    onClick = viewModel::toggle,
                    modifier = Modifier
                        .weight(2f)
                        .heightIn(min = dimens.minTouch * 1.4f),
                ) {
                    Text(
                        stringResource(
                            if (uiState.running) R.string.timer_pause else R.string.timer_start,
                        ),
                    )
                }
                OutlinedButton(
                    onClick = viewModel::reset,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = dimens.minTouch * 1.4f),
                ) {
                    Text(stringResource(R.string.timer_reset))
                }
            }

            ModeSection(uiState = uiState, viewModel = viewModel)

            // カウントダウン中は延長操作を出す
            if (uiState.mode == TimerMode.COUNTDOWN && uiState.running) {
                ExtendSection(
                    targetMinutes = uiState.targetMinutes,
                    onAdd = { viewModel.setTargetMinutes(uiState.targetMinutes + it) },
                )
            }

            ShowModeSection(
                uiState = uiState,
                onToggle = viewModel::toggleShowMode,
                onChange = viewModel::setShowMode,
                onGrantPolicy = { context.startActivity(viewModel.notificationPolicySettingsIntent()) },
            )

            MonitorSection(
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

            Text(
                text = stringResource(R.string.timer_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = dimens.spaceXl),
            )
        }
    }
}

@Composable
private fun TimerReadout(uiState: ShowTimerUiState) {
    val dimens = LocalPaDimens.current
    // 押し（超過）に入った瞬間に赤へ変わる。色が切り替わること自体が合図になる
    val valueColor by animateColorAsState(
        targetValue = if (uiState.isOverrun) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        },
        label = "timerColor",
    )

    PaCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentPadding = dimens.spaceSm,
    ) {
        BigReadout(
            value = formatDuration(uiState.displayMillis),
            label = if (uiState.isOverrun) {
                stringResource(R.string.timer_over, "")
            } else {
                stringResource(
                    when (uiState.mode) {
                        TimerMode.COUNTDOWN -> R.string.timer_remaining_label
                        TimerMode.ELAPSED -> R.string.timer_elapsed_label
                    },
                )
            },
            valueColor = valueColor,
            modifier = Modifier.padding(vertical = dimens.space),
        )
        uiState.estimatedEndEpochMs?.let { endMs ->
            Text(
                text = stringResource(R.string.timer_estimated_end, formatTime(endMs)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = dimens.spaceSm),
            )
        }
    }
}

@Composable
private fun ModeSection(uiState: ShowTimerUiState, viewModel: ShowTimerViewModel) {
    val dimens = LocalPaDimens.current

    Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
        ModeButton(
            text = stringResource(R.string.timer_mode_countdown),
            selected = uiState.mode == TimerMode.COUNTDOWN,
            onClick = { viewModel.setMode(TimerMode.COUNTDOWN) },
            minTouch = dimens.minTouch,
        )
        ModeButton(
            text = stringResource(R.string.timer_mode_elapsed),
            selected = uiState.mode == TimerMode.ELAPSED,
            onClick = { viewModel.setMode(TimerMode.ELAPSED) },
            minTouch = dimens.minTouch,
        )
    }

    if (uiState.mode == TimerMode.COUNTDOWN) {
        Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
            Text(
                text = stringResource(R.string.timer_target),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceSm),
            ) {
                ShowTimerViewModel.PRESET_MINUTES.forEach { minutes ->
                    ModeButton(
                        text = stringResource(R.string.timer_preset, minutes),
                        selected = minutes == uiState.targetMinutes,
                        onClick = { viewModel.setTargetMinutes(minutes) },
                        minTouch = dimens.minTouch,
                    )
                }
            }
        }
    }
}

/** 実行中に時間を積み増しする操作。ShowRunner の ExtendControl と同じ考え方。 */
@Composable
private fun ExtendSection(
    targetMinutes: Int,
    onAdd: (Int) -> Unit,
) {
    val dimens = LocalPaDimens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.timer_extend_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        listOf(1, 3, 5, 10).forEach { mins ->
            OutlinedButton(
                onClick = { onAdd(mins) },
                modifier = Modifier.heightIn(min = dimens.minTouch),
            ) {
                Text("+${mins}分")
            }
        }
    }
}

/**
 * 本番モード。
 *
 * 「何を止めるか」を個別に選べるようにしてある。現場ごとに事情が違う
 * （アラームで転換を管理している、連絡を通知で受けている、BGM を別アプリで出している）
 * ので、一括で決め打ちにすると必ずどこかで使えなくなる。
 */
@Composable
private fun ShowModeSection(
    uiState: ShowTimerUiState,
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
            title = stringResource(R.string.show_mode_title),
            subtitle = stringResource(
                if (uiState.showModeActive) {
                    R.string.show_mode_on
                } else {
                    R.string.show_mode_off
                },
            ),
            trailing = {
                Switch(checked = uiState.showModeActive, onCheckedChange = { onToggle() })
            },
        )

        ShowModeToggle(
            label = stringResource(R.string.show_mode_silence),
            description = stringResource(R.string.show_mode_silence_desc),
            checked = settings.silenceNotifications,
            onCheckedChange = { onChange(settings.copy(silenceNotifications = it)) },
        )

        if (settings.silenceNotifications) {
            ShowModeToggle(
                label = stringResource(R.string.show_mode_alarms),
                description = stringResource(R.string.show_mode_alarms_desc),
                checked = settings.allowAlarms,
                onCheckedChange = { onChange(settings.copy(allowAlarms = it)) },
            )
        }

        ShowModeToggle(
            label = stringResource(R.string.show_mode_screen),
            description = stringResource(R.string.show_mode_screen_desc),
            checked = settings.keepScreenOn,
            onCheckedChange = { onChange(settings.copy(keepScreenOn = it)) },
        )

        ShowModeToggle(
            label = stringResource(R.string.show_mode_other_audio),
            description = stringResource(R.string.show_mode_other_audio_desc),
            checked = settings.allowOtherAppAudio,
            onCheckedChange = { onChange(settings.copy(allowOtherAppAudio = it)) },
        )

        // 許可はアプリから要求できない。設定画面へ送るところまでしかできないので、
        // 何をどこで許可するのかを明示する
        if (uiState.needsNotificationPolicyGrant) {
            Text(
                text = stringResource(R.string.show_mode_permission_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            OutlinedButton(
                onClick = onGrantPolicy,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = dimens.minTouch),
            ) {
                Text(stringResource(R.string.show_mode_open_settings))
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
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * 本番中のモニタ。
 *
 * 出すのはレベルの数字とスペクトラムだけにしてある。本番中に触る操作を増やすと
 * 肝心の時間表示が押し出されるので、細かい設定はアナライザ側に置いた。
 */
@Composable
private fun MonitorSection(
    uiState: ShowTimerUiState,
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
            title = stringResource(R.string.timer_monitor_title),
            subtitle = stringResource(R.string.timer_monitor_hint),
            trailing = {
                FilterChip(
                    selected = uiState.monitoring,
                    onClick = onToggleMonitor,
                    label = {
                        Text(
                            stringResource(
                                if (uiState.monitoring) {
                                    R.string.timer_monitor_stop
                                } else {
                                    R.string.timer_monitor_start
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
                        R.string.timer_monitor_level,
                        "%.0f".format(uiState.levelDb),
                        "%.0f".format(uiState.maxLevelDb),
                        uiState.unitLabel,
                    )
                } else {
                    stringResource(R.string.timer_monitor_waiting)
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TextButton(onClick = onResetMax) {
                Text(stringResource(R.string.timer_monitor_reset_max))
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
            frequencies = uiState.frequencies,
            range = SpectrumRange.auto(uiState.columnsDb, MONITOR_SPAN_DB),
            height = 160.dp,
        )
    }
}

@Composable
private fun ModeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    minTouch: Dp,
) {
    if (selected) {
        Button(onClick = onClick, modifier = Modifier.heightIn(min = minTouch)) { Text(text) }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.heightIn(min = minTouch),
        ) { Text(text) }
    }
}

/** epoch ms → "HH:mm" 形式の時刻文字列 */
private fun formatTime(epochMs: Long): String {
    val cal = java.util.Calendar.getInstance().also { it.timeInMillis = epochMs }
    return "%02d:%02d".format(cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
}

/** 1時間を超えたら h:mm:ss、それ未満は mm:ss。 */
internal fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

/** 本番モニタの縦軸の幅。細かく見る画面ではないので広めに固定する */
private const val MONITOR_SPAN_DB = 70.0
