package com.patoolbox.feature.analyzer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.patoolbox.core.dsp.SpectrogramBuffer
import kotlin.math.ln
import kotlin.math.roundToInt

/** 表示するレベルの範囲。上下端は現場のダイナミックレンジに合わせてある */
internal const val DISPLAY_TOP_DB = 0.0f
internal const val DISPLAY_BOTTOM_DB = -90.0f

/** 縦線を引くオクターブの節目。数字を書くのはこの中の一部だけ */
private val GRID_FREQUENCIES = doubleArrayOf(
    31.5, 63.0, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0,
)

/**
 * FFT スペクトラム（対数周波数軸）。
 *
 * ピークホールドは同じ座標系に薄い線で重ねる。別の図にすると、
 * 「今どこが出ているか」と「さっきどこまで出たか」を目で往復させることになって使いにくい。
 */
@Composable
internal fun SpectrumChart(
    columnsDb: FloatArray,
    peakHoldDb: FloatArray,
    frequencies: DoubleArray,
    modifier: Modifier = Modifier,
    height: Dp = 220.dp,
) {
    val lineColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
    val peakColor = androidx.compose.material3.MaterialTheme.colorScheme.tertiary
    val gridColor = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        drawFrequencyGrid(frequencies, gridColor)
        drawLevelGrid(gridColor)
        if (columnsDb.isNotEmpty()) drawSpectrum(columnsDb, lineColor, 2.5f)
        if (peakHoldDb.isNotEmpty()) drawSpectrum(peakHoldDb, peakColor, 1.5f)
    }
}

/**
 * スペクトログラム。
 *
 * 毎フレーム Bitmap を作り直さず、1枚を使い回して画素だけ書き換える。
 * 256×300 の画素書き込みは1ミリ秒に満たないので、リングを工夫するより
 * 単純に全面書き直す方が読みやすく、実測でも十分間に合う。
 */
@Composable
internal fun SpectrogramView(
    buffer: SpectrogramBuffer,
    /** これが変わったときだけ描き直す */
    frame: Long,
    modifier: Modifier = Modifier,
    height: Dp = 300.dp,
) {
    val coldColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLowest
    val gridColor = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant
    val hotColor = androidx.compose.material3.MaterialTheme.colorScheme.error
    val midColor = androidx.compose.material3.MaterialTheme.colorScheme.primary

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

        pixels.fill(palette[0])
        // 上を最新にする。時間が下に流れていく見え方の方が、
        // 「さっき何が鳴ったか」を遡るときに直感に合う
        buffer.forEachNewestFirst { index, row ->
            val base = index * buffer.columns
            for (column in 0 until buffer.columns) {
                pixels[base + column] = palette[paletteIndex(row[column])]
            }
        }
        bitmap.setPixels(pixels, 0, buffer.columns, 0, 0, buffer.columns, buffer.historySize)

        drawImage(
            image = bitmap.asImageBitmap(),
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(buffer.columns, buffer.historySize),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
        )
        drawFrequencyGrid(null, gridColor)
    }
}

private fun paletteIndex(db: Float): Int {
    val normalized = (db - DISPLAY_BOTTOM_DB) / (DISPLAY_TOP_DB - DISPLAY_BOTTOM_DB)
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

private fun DrawScope.drawSpectrum(valuesDb: FloatArray, color: Color, width: Float) {
    val path = Path()
    for (i in valuesDb.indices) {
        val x = size.width * i / (valuesDb.size - 1).coerceAtLeast(1)
        val y = levelToY(valuesDb[i])
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path = path, color = color, style = Stroke(width))
}

private fun DrawScope.levelToY(db: Float): Float {
    val normalized = (db - DISPLAY_BOTTOM_DB) / (DISPLAY_TOP_DB - DISPLAY_BOTTOM_DB)
    return size.height * (1f - normalized.coerceIn(0f, 1f))
}

/**
 * オクターブごとの縦線。
 * [frequencies] があればカラム位置から、無ければ 20Hz〜20kHz の対数軸として引く。
 */
private fun DrawScope.drawFrequencyGrid(frequencies: DoubleArray?, color: Color) {
    val minHz = frequencies?.firstOrNull() ?: 20.0
    val maxHz = frequencies?.lastOrNull() ?: 20000.0
    if (maxHz <= minHz) return
    val span = ln(maxHz / minHz)

    for (hz in GRID_FREQUENCIES) {
        if (hz < minHz || hz > maxHz) continue
        val x = (size.width * ln(hz / minHz) / span).toFloat()
        drawLine(color = color, start = Offset(x, 0f), end = Offset(x, size.height))
    }
}

private fun DrawScope.drawLevelGrid(color: Color) {
    val steps = ((DISPLAY_TOP_DB - DISPLAY_BOTTOM_DB) / 10f).toInt()
    for (step in 1 until steps) {
        val y = size.height * step / steps
        drawLine(color = color, start = Offset(0f, y), end = Offset(size.width, y))
    }
}

private const val PALETTE_SIZE = 64
