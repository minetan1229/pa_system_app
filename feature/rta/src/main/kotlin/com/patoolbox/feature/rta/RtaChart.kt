package com.patoolbox.feature.rta

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil

/**
 * RTA のバーグラフ。
 *
 * 汎用チャートライブラリではなく Canvas に直接描いているのは、毎秒10回以上の
 * 更新に耐える必要があるため。バーの数は最大で 1/12 オクターブの約120本。
 */
@Composable
fun RtaChart(
    bands: List<RtaBand>,
    showPeaks: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 260.dp,
) {
    val textMeasurer = rememberTextMeasurer()
    val barColor = MaterialTheme.colorScheme.primary
    val peakColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = remember(labelColor) {
        TextStyle(fontSize = 9.sp, color = labelColor)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        if (bands.isEmpty()) return@Canvas

        val range = autoRange(bands)
        val axisWidth = 34f
        val labelHeight = 14f
        val plotWidth = size.width - axisWidth
        val plotHeight = size.height - labelHeight

        drawGrid(
            range = range,
            axisWidth = axisWidth,
            plotWidth = plotWidth,
            plotHeight = plotHeight,
            gridColor = gridColor,
            textMeasurer = textMeasurer,
            labelStyle = labelStyle,
        )

        val slot = plotWidth / bands.size
        val barWidth = (slot * 0.72f).coerceAtLeast(1f)

        bands.forEachIndexed { index, band ->
            val left = axisWidth + index * slot + (slot - barWidth) / 2f
            val barTop = range.toY(band.levelDb, plotHeight)

            if (barTop < plotHeight) {
                drawRect(
                    color = barColor,
                    topLeft = Offset(left, barTop),
                    size = Size(barWidth, plotHeight - barTop),
                )
            }

            if (showPeaks) {
                val peakY = range.toY(band.peakDb, plotHeight)
                if (peakY < plotHeight) {
                    drawRect(
                        color = peakColor,
                        topLeft = Offset(left, peakY),
                        size = Size(barWidth, 2f),
                    )
                }
            }
        }

        // バーが多いときは間引いてラベルを出す（重なると読めない）
        val labelEvery = ceil(bands.size / MAX_X_LABELS.toDouble()).toInt().coerceAtLeast(1)
        bands.forEachIndexed { index, band ->
            if (index % labelEvery != 0) return@forEachIndexed
            val layout = textMeasurer.measure(band.label, labelStyle)
            val centerX = axisWidth + index * slot + slot / 2f
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    x = (centerX - layout.size.width / 2f).coerceIn(
                        0f,
                        size.width - layout.size.width,
                    ),
                    y = plotHeight + 1f,
                ),
            )
        }
    }
}

private fun DrawScope.drawGrid(
    range: LevelRange,
    axisWidth: Float,
    plotWidth: Float,
    plotHeight: Float,
    gridColor: Color,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
) {
    var db = range.bottomDb
    while (db <= range.topDb) {
        val y = range.toY(db, plotHeight)
        drawRect(
            color = gridColor.copy(alpha = 0.5f),
            topLeft = Offset(axisWidth, y),
            size = Size(plotWidth, 1f),
        )
        val layout = textMeasurer.measure("%.0f".format(db), labelStyle)
        drawText(
            textLayoutResult = layout,
            topLeft = Offset(0f, (y - layout.size.height / 2f).coerceAtLeast(0f)),
        )
        db += GRID_STEP_DB
    }
}

internal data class LevelRange(val bottomDb: Double, val topDb: Double) {
    fun toY(db: Double, plotHeight: Float): Float {
        if (!db.isFinite()) return plotHeight
        val normalized = ((db - bottomDb) / (topDb - bottomDb)).coerceIn(0.0, 1.0)
        return (plotHeight * (1.0 - normalized)).toFloat()
    }
}

/** 表示範囲を自動で決める。手で合わせ直す手間を省くため。 */
internal fun autoRange(bands: List<RtaBand>): LevelRange {
    val max = bands.maxOf { if (it.levelDb.isFinite()) it.levelDb else Double.NEGATIVE_INFINITY }
    val top = if (max.isFinite()) {
        ceil((max + HEADROOM_DB) / GRID_STEP_DB) * GRID_STEP_DB
    } else {
        DEFAULT_TOP_DB
    }
    return LevelRange(bottomDb = top - SPAN_DB, topDb = top)
}

private const val GRID_STEP_DB = 10.0
private const val SPAN_DB = 70.0
private const val HEADROOM_DB = 5.0
private const val DEFAULT_TOP_DB = 110.0
private const val MAX_X_LABELS = 12
