package com.patoolbox.feature.measure

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.dsp.RoomAnalysis
import kotlin.math.roundToInt

/** Pro 専用ツールを無料で開いたときの本文。3画面で同じなのでまとめてある。 */
@Composable
internal fun MeasureProNotice(modifier: Modifier = Modifier) {
    val dimens = LocalPaDimens.current
    Column(modifier = modifier.fillMaxSize().padding(dimens.gutter)) {
        Text(
            text = stringResource(R.string.measure_pro),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.measure_pro_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** スイープの長さとレベル、測定ボタン。 */
@Composable
internal fun MeasureControls(
    uiState: MeasureUiState,
    onLengthChange: (SweepLength) -> Unit,
    onLevelChange: (SweepLevel) -> Unit,
    onMeasure: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(dimens.gutterSmall)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (length in SweepLength.entries) {
                FilterChip(
                    selected = uiState.sweepLength == length,
                    onClick = { onLengthChange(length) },
                    enabled = !uiState.isMeasuring,
                    label = { Text(length.label) },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (level in SweepLevel.entries) {
                FilterChip(
                    selected = uiState.sweepLevel == level,
                    onClick = { onLevelChange(level) },
                    enabled = !uiState.isMeasuring,
                    label = { Text(level.label) },
                )
            }
        }
        Text(
            text = stringResource(R.string.measure_level_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onMeasure,
            enabled = !uiState.isMeasuring,
            modifier = Modifier.fillMaxWidth().heightIn(min = dimens.minTouch),
        ) {
            Text(
                stringResource(
                    if (uiState.isMeasuring) R.string.measure_running else R.string.measure_start,
                ),
            )
        }
    }
}

/** 測定の状態に応じた注意書き。過大入力や信頼度の低さはここで必ず出す。 */
@Composable
internal fun MeasureWarnings(uiState: MeasureUiState, modifier: Modifier = Modifier) {
    val result = uiState.result
    val reading = uiState.reading
    val messages = buildList {
        (uiState.state as? MeasureState.Failed)?.let { add(it.message) }
        if (result?.clipped == true) add(stringResource(R.string.measure_clipped))
        if (reading != null && !reading.isReliable) {
            add(stringResource(R.string.measure_low_confidence))
        }
        if (reading?.method == RoomAnalysis.DelayMethod.CORRELATION) {
            add(stringResource(R.string.measure_fallback_correlation))
        }
    }
    if (messages.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            for (message in messages) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

/**
 * インパルス応答の包絡（dB）。
 *
 * 直接音の後に何がどれだけ遅れて来ているかが、この形で一目で分かる。
 * 単発の反射を機械が自動で拾って「壁まで何m」と断定するのは外したときの害が大きいので、
 * 数値ではなく形を出して読んでもらう方針にしている。
 */
@Composable
internal fun ImpulseChart(
    impulse: DoubleArray,
    sampleRate: Int,
    modifier: Modifier = Modifier,
    windowMs: Double = 200.0,
    height: Dp = 140.dp,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val samples = (windowMs / 1000.0 * sampleRate).toInt().coerceIn(1, impulse.size)
    val envelope = RoomAnalysis.envelopeDb(impulse.copyOf(samples), floorDb = FLOOR_DB)

    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        drawGrid(gridColor)
        drawEnvelope(envelope, lineColor)
    }
}

/** 残響の減衰カーブ。直線からどれだけ外れているかを目で確認できる。 */
@Composable
internal fun DecayChart(
    curveDb: DoubleArray,
    modifier: Modifier = Modifier,
    height: Dp = 140.dp,
) {
    val lineColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        drawGrid(gridColor)
        drawEnvelope(curveDb, lineColor)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGrid(color: Color) {
    // 10dB ごとの横線。目盛りの数字を出すより、間隔が分かれば十分
    for (step in 1 until GRID_LINES) {
        val y = size.height * step / GRID_LINES
        drawLine(color = color, start = Offset(0f, y), end = Offset(size.width, y))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEnvelope(
    valuesDb: DoubleArray,
    color: Color,
) {
    if (valuesDb.isEmpty()) return
    val path = Path()
    // 画面の横幅より点が多いので、1px あたり最大値を取って間引く。
    // 平均で潰すと、短い反射が消えてしまう
    val columns = size.width.roundToInt().coerceAtLeast(1)
    val perColumn = (valuesDb.size / columns).coerceAtLeast(1)

    var x = 0
    while (x < columns) {
        val from = x * perColumn
        if (from >= valuesDb.size) break
        val to = ((x + 1) * perColumn).coerceAtMost(valuesDb.size)
        var peak = FLOOR_DB
        for (i in from until to) {
            val value = valuesDb[i]
            if (value.isFinite() && value > peak) peak = value
        }
        val y = (peak / FLOOR_DB).coerceIn(0.0, 1.0) * size.height
        if (x == 0) path.moveTo(0f, y.toFloat()) else path.lineTo(x.toFloat(), y.toFloat())
        x++
    }
    drawPath(path = path, color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
}

private const val FLOOR_DB = -60.0
private const val GRID_LINES = 6
