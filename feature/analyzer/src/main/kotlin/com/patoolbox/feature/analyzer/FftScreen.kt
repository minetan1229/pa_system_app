package com.patoolbox.feature.analyzer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.component.BigReadout
import com.patoolbox.core.designsystem.component.PaCard
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.dsp.OctaveSmoothing
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.ui.component.CalibrationBadge
import com.patoolbox.core.ui.component.ChartLegend
import com.patoolbox.core.ui.component.KeepScreenOn
import com.patoolbox.core.ui.component.MicPermissionGate
import com.patoolbox.core.ui.component.PaToolScaffold
import com.patoolbox.core.ui.component.SpectrumChart
import com.patoolbox.core.ui.component.SpectrumRange
import com.patoolbox.core.ui.component.formatHz
import com.patoolbox.core.ui.R as CoreUiR

/**
 * スペクトラムアナライザ。
 *
 * RTA が「帯域ごとにどれだけ出ているか」を見る道具なのに対して、
 * こちらは「その山が何 Hz なのか」を 1Hz 単位で当てる道具。
 * ハウリングの芽、電源ハムの次数、共振の中心を特定するときに使う。
 *
 * 大表示はレベル（dB）。現場で最初に判断するのは
 * 「対処が要るほど出ているのか」で、周波数はその次に要る情報なので、
 * 数字の大小関係を判断の順番に合わせてある。
 *
 * 画面は縦にスクロールする。図・操作・読み取りを全部詰めると小さい端末では
 * 縦に収まらず、以前は一番下の開始ボタンが画面外に出て操作できなくなっていた。
 */
@Composable
fun FftScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnalyzerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dimens = LocalPaDimens.current
    val scrollState = rememberScrollState()

    PaToolScaffold(
        tool = ToolId.FFT,
        onBack = onBack,
        modifier = modifier,
        title = stringResource(R.string.fft_title),
    ) { innerPadding ->
        if (!uiState.proStatus.isPro) {
            AnalyzerProNotice(modifier = Modifier.padding(innerPadding))
            return@PaToolScaffold
        }

        MicPermissionGate(modifier = Modifier.padding(innerPadding)) {
            KeepScreenOn(enabled = uiState.isMeasuring)

            val range = remember(uiState.frame, uiState.span) {
                SpectrumRange.auto(uiState.columnsDb, uiState.span.db)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = dimens.gutter),
                verticalArrangement = Arrangement.spacedBy(dimens.gutterSmall),
            ) {
                // 大きいのはレベル。周波数はその下に添える。
                // 「何dB出ているか」→「それは何Hzか」の順で読めるようにしてある
                BigReadout(
                    value = if (uiState.hasReading) {
                        "%.1f".format(uiState.peakLevelDb)
                    } else {
                        "--.-"
                    },
                    unit = uiState.unitLabel,
                    label = stringResource(R.string.fft_level_label),
                    caption = if (uiState.hasReading) {
                        stringResource(
                            R.string.fft_peak_caption,
                            formatHz(uiState.peakFrequencyHz),
                            uiState.peakNote?.let { "（$it）" }.orEmpty(),
                        )
                    } else {
                        null
                    },
                    maxFontSize = 72.sp,
                    modifier = Modifier.padding(vertical = dimens.gutterSmall),
                )

                // 表示している dB には常に校正オフセットが乗っている。
                // どの状態で読んでいるのかを隠さないため、図の手前に必ず出す
                CalibrationBadge(profile = uiState.calibration)

                ChartLegend(
                    entries = buildList {
                        add(
                            stringResource(R.string.analyzer_legend_live) to
                                MaterialTheme.colorScheme.primary,
                        )
                        if (uiState.peakHold) {
                            add(
                                stringResource(R.string.analyzer_legend_peak) to
                                    MaterialTheme.colorScheme.tertiary,
                            )
                        }
                        if (uiState.cursorHz != null) {
                            add(
                                stringResource(R.string.analyzer_legend_cursor) to
                                    MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                )

                SpectrumChart(
                    columnsDb = uiState.columnsDb,
                    frequencies = uiState.frequencies,
                    range = range,
                    peakHoldDb = uiState.peakHoldDb,
                    cursorHz = uiState.cursorHz,
                    onCursorChange = { hz -> hz?.let(viewModel::setCursor) },
                    harmonics = if (uiState.showHarmonics) HARMONIC_ORDERS else 0,
                    height = 240.dp,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = viewModel::toggle,
                        modifier = Modifier.weight(1f).heightIn(min = dimens.minTouch),
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
                    // 保存は測定中に一番よく押す操作なので、設定の奥ではなく図のすぐ下に置く
                    OutlinedButton(
                        onClick = viewModel::savePeaks,
                        enabled = uiState.topPeaks.isNotEmpty(),
                        modifier = Modifier.weight(1f).heightIn(min = dimens.minTouch),
                    ) {
                        Text(stringResource(R.string.analyzer_save_peaks))
                    }
                }

                // ピーク保持は「押しっぱなしで残す」表示なので、
                // 入り切りと消去を隣り合わせにして状態が分かるようにする
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = uiState.peakHold,
                        onClick = viewModel::togglePeakHold,
                        label = { Text(stringResource(R.string.analyzer_peak_hold)) },
                    )
                    if (uiState.peakHold) {
                        TextButton(onClick = viewModel::clearPeaks) {
                            Text(stringResource(R.string.analyzer_clear_peaks))
                        }
                    }
                    FilterChip(
                        selected = uiState.showHarmonics,
                        onClick = viewModel::toggleHarmonics,
                        label = { Text(stringResource(R.string.analyzer_harmonics)) },
                    )
                }
                if (uiState.peakHold) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AnalyzerPeakHoldDuration.entries.forEach { duration ->
                            FilterChip(
                                selected = duration == uiState.peakHoldDuration,
                                onClick = { viewModel.setPeakHoldDuration(duration) },
                                label = { Text(duration.label) },
                            )
                        }
                    }
                }

                CursorCard(
                    cursorHz = uiState.cursorHz,
                    cursorLevelDb = uiState.cursorLevelDb,
                    cursorNote = uiState.cursorNote,
                    unitLabel = uiState.unitLabel,
                    onClear = viewModel::clearCursor,
                )

                PeakList(peaks = uiState.topPeaks, unitLabel = uiState.unitLabel)

                SavedPeaks(
                    saved = uiState.savedPeaks,
                    unitLabel = uiState.unitLabel,
                    onClear = viewModel::clearSavedPeaks,
                )

                uiState.error?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                ChipGroup(label = stringResource(R.string.analyzer_smoothing)) {
                    for (smoothing in OctaveSmoothing.entries) {
                        FilterChip(
                            selected = uiState.smoothing == smoothing,
                            onClick = { viewModel.setSmoothing(smoothing) },
                            label = { Text(smoothing.label) },
                        )
                    }
                }
                ChipGroup(label = stringResource(R.string.analyzer_resolution)) {
                    for (size in AnalyzerFftSize.entries) {
                        FilterChip(
                            selected = uiState.fftSize == size,
                            onClick = { viewModel.setFftSize(size) },
                            label = { Text(size.label) },
                        )
                    }
                }
                ChipGroup(label = stringResource(R.string.analyzer_averaging)) {
                    for (averaging in AnalyzerAveraging.entries) {
                        FilterChip(
                            selected = uiState.averaging == averaging,
                            onClick = { viewModel.setAveraging(averaging) },
                            label = { Text(averaging.label) },
                        )
                    }
                }
                ChipGroup(label = stringResource(R.string.analyzer_window)) {
                    for (window in AnalyzerWindow.entries) {
                        FilterChip(
                            selected = uiState.window == window,
                            onClick = { viewModel.setWindow(window) },
                            label = { Text(window.label) },
                        )
                    }
                }
                Text(
                    text = uiState.window.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ChipGroup(label = stringResource(R.string.analyzer_span)) {
                    for (span in AnalyzerSpan.entries) {
                        FilterChip(
                            selected = uiState.span == span,
                            onClick = { viewModel.setSpan(span) },
                            label = { Text(span.label) },
                        )
                    }
                }

                Text(
                    text = stringResource(
                        R.string.fft_axis,
                        uiState.fftSize.detail,
                        uiState.unitLabel,
                        "%.0f".format(uiState.span.db),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.fft_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = dimens.gutter),
                )
            }
        }
    }
}

/**
 * カーソルの読み取り。
 *
 * 「一番大きい山」は自動で追えるが、現場で知りたいのは
 * 「いま自分が気にしている帯域が何dB出ているか」であることが多い。
 * 図を触った位置の数字をここに出す。
 */
@Composable
private fun CursorCard(
    cursorHz: Double?,
    cursorLevelDb: Double,
    cursorNote: String?,
    unitLabel: String,
    onClear: () -> Unit,
) {
    val dimens = LocalPaDimens.current
    if (cursorHz == null) {
        Text(
            text = stringResource(R.string.analyzer_cursor_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    PaCard(modifier = Modifier.fillMaxWidth(), contentPadding = dimens.spaceMd) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.analyzer_cursor),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = buildString {
                        append("${formatHz(cursorHz)}Hz")
                        if (cursorNote != null) append(" ($cursorNote)")
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (cursorLevelDb.isFinite()) {
                        "%.1f %s".format(cursorLevelDb, unitLabel)
                    } else {
                        stringResource(R.string.analyzer_no_signal)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onClear) {
                Text(stringResource(R.string.analyzer_cursor_clear))
            }
        }
    }
}

/**
 * 目立っている山の一覧。
 *
 * 「1つの山の肩」で表が埋まらないよう、1/3オクターブ以上離れたものだけを出している
 * （[com.patoolbox.core.dsp.SpectrumPipeline] 側で選別）。
 */
@Composable
private fun PeakList(peaks: List<AnalyzerPeak>, unitLabel: String) {
    Text(
        text = stringResource(R.string.analyzer_peaks),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (peaks.isEmpty()) {
        Text(
            text = stringResource(R.string.analyzer_peaks_empty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    for (peak in peaks) {
        PeakRow(peak = peak, unitLabel = unitLabel)
    }
}

/**
 * 控えた読みの一覧。
 *
 * 新しいものを上に積む。リハで何度か測って比べる使い方が前提なので、
 * 直前に押したものが一番上に出た方が見比べやすい。
 */
@Composable
private fun SavedPeaks(
    saved: List<SavedPeakSet>,
    unitLabel: String,
    onClear: () -> Unit,
) {
    if (saved.isEmpty()) return
    val dimens = LocalPaDimens.current

    PaCard(modifier = Modifier.fillMaxWidth(), contentPadding = dimens.spaceMd) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.analyzer_saved_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onClear) {
                Text(stringResource(R.string.analyzer_saved_clear))
            }
        }

        for (entry in saved) {
            Text(
                text = stringResource(R.string.analyzer_saved_entry, entry.label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            for (peak in entry.peaks) {
                PeakRow(peak = peak, unitLabel = unitLabel)
            }
        }

        Text(
            text = stringResource(R.string.analyzer_saved_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PeakRow(peak: AnalyzerPeak, unitLabel: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = buildString {
                append("${formatHz(peak.frequencyHz)}Hz")
                peak.noteName?.let { append("  $it") }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "%.1f %s".format(peak.levelDb, unitLabel),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 見出し付きのチップ列。何を選んでいるのかが分からない裸のチップを避けるため */
@Composable
private fun ChipGroup(label: String, content: @Composable FlowRowScope.() -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

/** 何次まで倍音の線を引くか。5次まで見れば、ハムか共振かはほぼ判別できる */
private const val HARMONIC_ORDERS = 5

@Composable
internal fun AnalyzerProNotice(modifier: Modifier = Modifier) {
    val dimens = LocalPaDimens.current
    Column(modifier = modifier.fillMaxSize().padding(dimens.gutter)) {
        Text(
            text = stringResource(R.string.analyzer_pro),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.analyzer_pro_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
}
