package com.patoolbox.feature.analyzer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import com.patoolbox.core.dsp.SpectrogramBuffer
import com.patoolbox.core.ui.component.SpectrumRange
import com.patoolbox.core.ui.component.formatHz
import kotlin.math.ln
import kotlin.math.roundToInt

/** 縦線と数字を出すオクターブの節目 */
private val GRID_FREQUENCIES = doubleArrayOf(
    31.5, 63.0, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0,
)

/**
 * スペクトログラム。
 *
 * 毎フレーム Bitmap を作り直さず、1枚を使い回して画素だけ書き換える。
 * 256×300 の画素書き込みは1ミリ秒に満たないので、リングを工夫するより
 * 単純に全面書き直す方が読みやすく、実測でも十分間に合う。
 *
 * 色の範囲は [range] に従う。固定にすると、校正済み（dB SPL）と未校正（dBFS）で
 * 100dB 近くずれて片方が真っ黒になる。
 */
@Composable
internal fun SpectrogramView(
    buffer: SpectrogramBuffer,
    /** これが変わったときだけ描き直す */
    frame: Long,
    range: SpectrumRange,
    /** 1行あたりの時間。左の時間目盛りに使う */
    hopSeconds: Double,
    minHz: Double,
    maxHz: Double,
    modifier: Modifier = Modifier,
    height: Dp = 300.dp,
) {
    val coldColor = MaterialTheme.colorScheme.surfaceContainerLowest
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val hotColor = MaterialTheme.colorScheme.error
    val midColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = remember(labelColor) { TextStyle(fontSize = 9.sp, color = labelColor) }
    val textMeasurer = rememberTextMeasurer()

    val density = LocalDensity.current
    val axisWidth = with(density) { AXIS_WIDTH.toPx() }
    val labelHeight = with(density) { LABEL_HEIGHT.toPx() }

    val bitmap = remember(buffer.columns, buffer.historySize) {
        createBitmap(buffer.columns, buffer.historySize)
    }
    val pixels = remember(buffer.columns, buffer.historySize) {
        IntArray(buffer.columns * buffer.historySize)
    }
    val palette = remember(coldColor, midColor, hotColor) {
        buildPalette(coldColor, midColor, hotColor)
    }

    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        // frame を読むことで、履歴が進んだときに描き直しが走る
        @Suppress("UNUSED_EXPRESSION") frame

        val plotWidth = size.width - axisWidth
        val plotHeight = size.height - labelHeight
        if (plotWidth <= 0f || plotHeight <= 0f) return@Canvas

        pixels.fill(palette[0])
        // 上を最新にする。時間が下に流れていく見え方の方が、
        // 「さっき何が鳴ったか」を遡るときに直感に合う
        buffer.forEachNewestFirst { index, row ->
            val base = index * buffer.columns
            for (column in 0 until buffer.columns) {
                pixels[base + column] = palette[paletteIndex(row[column], range)]
            }
        }
        bitmap.setPixels(pixels, 0, buffer.columns, 0, 0, buffer.columns, buffer.historySize)

        drawImage(
            image = bitmap.asImageBitmap(),
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(buffer.columns, buffer.historySize),
            dstOffset = IntOffset(axisWidth.roundToInt(), 0),
            dstSize = IntSize(plotWidth.roundToInt(), plotHeight.roundToInt()),
        )

        drawFrequencyAxis(
            minHz = minHz,
            maxHz = maxHz,
            axisWidth = axisWidth,
            plotWidth = plotWidth,
            plotHeight = plotHeight,
            gridColor = gridColor,
            draw = { text, x ->
                val layout = textMeasurer.measure(text, labelStyle)
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        x = (x - layout.size.width / 2f)
                            .coerceIn(0f, size.width - layout.size.width),
                        y = (size.height - layout.size.height).coerceAtLeast(plotHeight),
                    ),
                )
            },
        )

        // 時間の目盛り。何秒前の出来事なのかが分からないと、
        // 「さっきのハウリング」を指し示せない
        val totalSeconds = buffer.historySize * hopSeconds
        if (totalSeconds > 0.0) {
            val step = timeStepSeconds(totalSeconds)
            var seconds = 0.0
            while (seconds <= totalSeconds) {
                val y = (plotHeight * seconds / totalSeconds).toFloat()
                drawLine(
                    color = gridColor.copy(alpha = 0.4f),
                    start = Offset(axisWidth, y),
                    end = Offset(size.width, y),
                )
                val label = if (seconds == 0.0) "今" else "-%.0fs".format(seconds)
                val layout = textMeasurer.measure(label, labelStyle)
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        x = (axisWidth - layout.size.width - 2f).coerceAtLeast(0f),
                        y = (y - layout.size.height / 2f)
                            .coerceIn(0f, plotHeight - layout.size.height),
                    ),
                )
                seconds += step
            }
        }
    }
}

private inline fun DrawScope.drawFrequencyAxis(
    minHz: Double,
    maxHz: Double,
    axisWidth: Float,
    plotWidth: Float,
    plotHeight: Float,
    gridColor: Color,
    draw: (String, Float) -> Unit,
) {
    if (maxHz <= minHz) return
    val span = ln(maxHz / minHz)
    for (hz in GRID_FREQUENCIES) {
        if (hz < minHz || hz > maxHz) continue
        val x = axisWidth + (plotWidth * ln(hz / minHz) / span).toFloat()
        drawLine(
            color = gridColor.copy(alpha = 0.4f),
            start = Offset(x, 0f),
            end = Offset(x, plotHeight),
        )
        draw(formatHz(hz), x)
    }
}

/** 目盛りが混みすぎない間隔を選ぶ */
private fun timeStepSeconds(totalSeconds: Double): Double = when {
    totalSeconds <= 12.0 -> 2.0
    totalSeconds <= 40.0 -> 5.0
    totalSeconds <= 120.0 -> 15.0
    else -> 30.0
}

private fun paletteIndex(db: Float, range: SpectrumRange): Int {
    if (!db.isFinite()) return 0
    val normalized = (db - range.bottomDb) / (range.topDb - range.bottomDb)
    return (normalized * (PALETTE_SIZE - 1)).roundToInt().coerceIn(0, PALETTE_SIZE - 1)
}

/**
 * 暗→中→明の3点を補間した色表。
 *
 * テーマの色から作っているので、暗所モード（赤）でも屋外モードでも
 * 画面全体と地続きに見える。虹色（jet）は境目が目に付いて、実際には
 * 無い段差が見える色使いなので採用していない。
 */
private fun buildPalette(cold: Color, mid: Color, hot: Color): IntArray = IntArray(PALETTE_SIZE) {
    val t = it.toFloat() / (PALETTE_SIZE - 1)
    val color = if (t < 0.5f) {
        lerpColor(cold, mid, t * 2f)
    } else {
        lerpColor(mid, hot, (t - 0.5f) * 2f)
    }
    color.toArgb()
}

private fun lerpColor(from: Color, to: Color, t: Float) = Color(
    red = from.red + (to.red - from.red) * t,
    green = from.green + (to.green - from.green) * t,
    blue = from.blue + (to.blue - from.blue) * t,
    alpha = 1f,
)

private val AXIS_WIDTH = 30.dp
private val LABEL_HEIGHT = 14.dp
private const val PALETTE_SIZE = 64
