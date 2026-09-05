package com.patoolbox.feature.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.component.BigReadout
import com.patoolbox.core.designsystem.component.PaFilterChip
import com.patoolbox.core.designsystem.component.PaNotice
import com.patoolbox.core.designsystem.component.PaPanel
import com.patoolbox.core.designsystem.component.PaPill
import com.patoolbox.core.designsystem.component.PaTone
import com.patoolbox.core.designsystem.component.content
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.dsp.FeedbackSensitivity
import com.patoolbox.core.dsp.FeedbackTracker
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.reference.BandDictionary
import com.patoolbox.core.ui.component.ChartLegend
import com.patoolbox.core.ui.component.KeepScreenOn
import com.patoolbox.core.ui.component.MicPermissionGate
import com.patoolbox.core.ui.component.PaToolScaffold
import com.patoolbox.core.ui.component.SpectrumChart
import com.patoolbox.core.ui.component.SpectrumMarker
import com.patoolbox.core.ui.component.SpectrumRange
import com.patoolbox.core.ui.component.formatHz
import com.patoolbox.core.ui.identityColor
import com.patoolbox.core.ui.R as CoreUiR

/**
 * ハウリング検出。
 *
 * 画面を3段に分けている。
 * 　上＝いちばん長く鳴っている1本（本番中はここしか見ない）
 * 　中＝スペクトラム図。いま鳴っている場所に印を立て、止まったものは点線で残す
 * 　下＝履歴の一覧。累計時間・最長連続・回数で「居座っているもの」を選ぶ
 *
 * 一覧を「いま鳴っている順」ではなく **累計時間順** を既定にしているのがこの画面の要点。
 * 現場で潰したいのは一瞬の派手な発振ではなく、鳴り続けて本番を壊す方だから。
 */
@Composable
fun FeedbackScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeedbackViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dimens = LocalPaDimens.current

    PaToolScaffold(
        tool = ToolId.FEEDBACK_FINDER,
        onBack = onBack,
        modifier = modifier,
        title = stringResource(R.string.feedback_title),
    ) { innerPadding ->
        if (!uiState.proStatus.isPro) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(dimens.gutter),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceSm),
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
                )
            }
            return@PaToolScaffold
        }

        MicPermissionGate(modifier = Modifier.padding(innerPadding)) {
            KeepScreenOn(enabled = uiState.isMeasuring)

            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = dimens.gutter),
                    verticalArrangement = Arrangement.spacedBy(dimens.spaceSm),
                ) {
                    item(key = "readout") { WorstReadout(uiState) }
                    item(key = "chart") { FeedbackChart(uiState) }
                    item(key = "controls") { ControlRow(uiState, viewModel) }
                    item(key = "sort") { SortRow(uiState, viewModel) }

                    if (uiState.tracks.isEmpty()) {
                        item(key = "empty") { EmptyNotice(uiState) }
                    }

                    items(uiState.tracks, key = { it.frequencyHz.toInt() }) { track ->
                        TrackPanel(track = track, elapsedMs = uiState.elapsedMs)
                    }

                    item(key = "note") {
                        PaNotice(
                            title = stringResource(R.string.feedback_note_title),
                            body = stringResource(R.string.feedback_note),
                            tone = PaTone.WARNING,
                            modifier = Modifier.padding(top = dimens.spaceSm),
                        )
                    }
                }

                BottomButtons(uiState, viewModel)
            }
        }
    }
}

/**
 * いちばん潰すべき1本。
 *
 * 出すのは周波数だけでなく **累計で何秒鳴ったか** まで。
 * 「630Hz」だけでは、それが本番を壊しているのか一瞬の芽なのか判断できない。
 */
@Composable
private fun WorstReadout(state: FeedbackUiState) {
    val dimens = LocalPaDimens.current
    val worst = state.worst

    BigReadout(
        value = worst?.let { "%.0f".format(it.frequencyHz) } ?: "----",
        unit = if (worst != null) "Hz" else null,
        label = worst?.let { "${it.noteName} / ${it.bandLabel} Hz 帯" },
        caption = when {
            worst == null && !state.isMeasuring -> stringResource(R.string.feedback_waiting)
            worst == null -> stringResource(R.string.feedback_none)
            else -> stringResource(
                R.string.feedback_worst_caption,
                formatDuration(worst.totalRingingMs),
                worst.episodes,
                formatDuration(worst.longestRunMs),
            )
        },
        valueColor = when {
            worst == null -> MaterialTheme.colorScheme.onSurfaceVariant
            worst.isActive -> MaterialTheme.colorScheme.error
            // 止まっているものは色を落とす。いま鳴っているかは一目で分かる必要がある
            else -> MaterialTheme.colorScheme.onSurface
        },
        modifier = Modifier.padding(vertical = dimens.spaceSm),
    )

    state.error?.let { error ->
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

/**
 * スペクトラム図。
 *
 * ハウリングは「周りより突出した1本の細い山」なので、
 * ならしを掛けずに出し、検出した位置に印を立てる。
 * ピーク保持の線を常に出しているのは、
 * その線の凸凹がそのまま「この会場で溜まりやすい帯域」になるため。
 */
@Composable
private fun FeedbackChart(state: FeedbackUiState) {
    val dimens = LocalPaDimens.current

    val markers = state.tracks
        // 印は多くても6本。それ以上は図が線だらけになって波形が読めない
        .sortedByDescending { it.totalRingingMs }
        .take(MAX_MARKERS)
        .map { track ->
            SpectrumMarker(
                frequencyHz = track.frequencyHz,
                label = formatHz(track.frequencyHz),
                active = track.isActive,
                // 長く鳴っているものほど太い線にする
                weight = (track.totalRingingMs / MARKER_FULL_WEIGHT_MS.toFloat()).coerceIn(0f, 1f),
            )
        }

    Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceXs)) {
        SpectrumChart(
            columnsDb = state.columnsDb,
            frequencies = state.frequencies,
            range = SpectrumRange.auto(state.columnsDb, SPAN_DB),
            peakHoldDb = state.peakHoldDb,
            markers = markers,
            height = CHART_HEIGHT,
        )
        ChartLegend(
            entries = listOf(
                stringResource(R.string.feedback_legend_now) to MaterialTheme.colorScheme.error,
                stringResource(R.string.feedback_legend_past) to
                    MaterialTheme.colorScheme.onSurfaceVariant,
                stringResource(R.string.feedback_legend_peak) to
                    MaterialTheme.colorScheme.tertiary,
            ),
        )
    }
}

/** 経過時間・本数・感度。測定中に触るのは感度だけなので、状態表示と同じ行に置く */
@Composable
private fun ControlRow(state: FeedbackUiState, viewModel: FeedbackViewModel) {
    val dimens = LocalPaDimens.current

    Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceXs)) {
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
            PaPill(
                text = stringResource(
                    R.string.feedback_elapsed,
                    formatDuration(state.elapsedMs),
                ),
                tone = PaTone.NEUTRAL,
            )
            PaPill(
                text = stringResource(R.string.feedback_track_count, state.establishedCount),
                tone = if (state.establishedCount > 0) PaTone.DANGER else PaTone.NEUTRAL,
            )
            if (state.activeTracks.isNotEmpty()) {
                PaPill(
                    text = stringResource(
                        R.string.feedback_active_count,
                        state.activeTracks.size,
                    ),
                    tone = PaTone.DANGER,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
        ) {
            Text(
                text = stringResource(R.string.feedback_sensitivity),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = dimens.spaceSm),
            )
            FeedbackSensitivity.entries.forEach { entry ->
                PaFilterChip(
                    text = entry.label,
                    selected = state.sensitivity == entry,
                    onClick = { viewModel.setSensitivity(entry) },
                    accent = ToolId.FEEDBACK_FINDER.identityColor(),
                )
            }
        }
        Text(
            text = state.sensitivity.detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 並べ替え。既定は「長い順」 */
@Composable
private fun SortRow(state: FeedbackUiState, viewModel: FeedbackViewModel) {
    val dimens = LocalPaDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
    ) {
        Text(
            text = stringResource(R.string.feedback_sort),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = dimens.spaceSm),
        )
        FeedbackTracker.Sort.entries.forEach { sort ->
            PaFilterChip(
                text = sort.label,
                selected = state.sort == sort,
                onClick = { viewModel.setSort(sort) },
                accent = ToolId.FEEDBACK_FINDER.identityColor(),
            )
        }
    }
}

@Composable
private fun EmptyNotice(state: FeedbackUiState) {
    PaNotice(
        title = stringResource(
            if (state.isMeasuring) R.string.feedback_none else R.string.feedback_waiting,
        ),
        body = stringResource(R.string.feedback_empty_body),
        tone = PaTone.INFO,
    )
}

/**
 * 履歴1本。
 *
 * 数字を4つ（累計・最長連続・回数・突出量）並べるのは多いが、
 * この4つが揃わないと「定在波なのか、演者が動いただけなのか」を切り分けられない。
 * 累計時間だけは棒でも出す。並べたときに桁を読み比べなくて済むようにするため。
 */
@Composable
private fun TrackPanel(track: FeedbackTracker.Track, elapsedMs: Long) {
    val dimens = LocalPaDimens.current
    val tone = if (track.isActive) PaTone.DANGER else PaTone.NEUTRAL
    val band = BandDictionary.at(track.frequencyHz)

    PaPanel(
        title = stringResource(
            R.string.feedback_track_title,
            "%.0f".format(track.frequencyHz),
            track.noteName,
        ),
        subtitle = band?.let { "${it.label}：${it.oneLiner}" }
            ?: stringResource(R.string.feedback_band, track.bandLabel),
        rail = tone.content(),
        trailing = {
            PaPill(
                text = stringResource(
                    if (track.isActive) R.string.feedback_state_now else R.string.feedback_state_past,
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
                label = stringResource(R.string.feedback_stat_total),
                value = formatDuration(track.totalRingingMs),
            )
            StatColumn(
                label = stringResource(R.string.feedback_stat_longest),
                value = formatDuration(track.longestRunMs),
            )
            StatColumn(
                label = stringResource(R.string.feedback_stat_episodes),
                value = stringResource(R.string.feedback_stat_times, track.episodes),
            )
            StatColumn(
                label = stringResource(R.string.feedback_stat_prominence),
                value = "%.0f dB".format(track.peakProminenceDb),
            )
        }

        Text(
            text = when {
                track.isSustained -> stringResource(R.string.feedback_verdict_sustained)
                track.isDrifting -> stringResource(
                    R.string.feedback_verdict_drifting,
                    "%.0f".format(track.lowestHz),
                    "%.0f".format(track.highestHz),
                )

                track.episodes >= REPEATING_EPISODES -> stringResource(
                    R.string.feedback_verdict_repeating,
                )

                else -> stringResource(R.string.feedback_verdict_short)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Text(
            text = band?.feedbackNote ?: stringResource(R.string.feedback_action),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 鳴っていた時間の棒。
 *
 * 分母は記録開始からの経過時間。「20分のうち4分」が一目で分かる。
 * 数字も必ず添える（棒だけでは卓に入れる判断ができない）。
 */
@Composable
private fun RingingBar(totalMs: Long, elapsedMs: Long, accent: androidx.compose.ui.graphics.Color) {
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

@Composable
private fun BottomButtons(state: FeedbackUiState, viewModel: FeedbackViewModel) {
    val dimens = LocalPaDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimens.gutter),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
    ) {
        Button(
            onClick = viewModel::toggle,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = dimens.minTouch),
        ) {
            Text(
                stringResource(
                    if (state.isMeasuring) {
                        CoreUiR.string.measure_stop
                    } else {
                        CoreUiR.string.measure_start
                    },
                ),
            )
        }
        OutlinedButton(
            onClick = viewModel::clearHistory,
            enabled = state.tracks.isNotEmpty(),
            modifier = Modifier.heightIn(min = dimens.minTouch),
        ) {
            Text(stringResource(R.string.feedback_clear))
        }
    }
}

/**
 * 時間の表示。
 *
 * 秒だけで出すと「184秒」のような読みにくい数字になり、
 * ミリ秒まで出すと本番中に読めない。1分を境に単位を変える。
 */
internal fun formatDuration(ms: Long): String = when {
    ms < 1_000 -> "%d ms".format(ms)
    ms < 60_000 -> "%.1f 秒".format(ms / 1000.0)
    else -> "%d分%02d秒".format(ms / 60_000, (ms % 60_000) / 1000)
}

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
