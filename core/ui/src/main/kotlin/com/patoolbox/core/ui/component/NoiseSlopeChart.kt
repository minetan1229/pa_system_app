package com.patoolbox.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ln

/**
 * ノイズの「色」を1枚の図にする。
 *
 * ピンクノイズが -3dB/oct だと文章で読んでも身に付かないのは、
 * **同じ信号が測り方で違う形に見える**からで、そこが混乱の元になっている。
 * そこで2本の線を重ねて出す。
 *
 * 　実線＝FFT（周波数を等間隔に見る）での傾き。信号そのものの性質
 * 　点線＝RTA（1/1・1/3 オクターブで見る）での傾き。実線 +3dB/oct
 *
 * ピンクノイズは点線が水平になる。これが「RTA で平らになるのが基準」の理由で、
 * 同じ信号を FFT で見れば右下がりに見えるのも同時に分かる。
 *
 * 縦軸は 1kHz を 0dB とした相対値。**目盛りの数字を必ず入れる**——
 * 傾きの向きだけ分かっても、何dB/oct なのかが読めないと現場では使えない。
 */
@Composable
fun NoiseSlopeChart(
    slopeDbPerOctave: Double,
    modifier: Modifier = Modifier,
    fftLabel: String = "FFT表示",
    octaveLabel: String = "1/3oct表示",
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val fftColor = MaterialTheme.colorScheme.primary
    val octaveColor = MaterialTheme.colorScheme.tertiary

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(CHART_HEIGHT),
    ) {
        val axisWidth = with(density) { AXIS_WIDTH.toPx() }
        val labelHeight = with(density) { LABEL_HEIGHT.toPx() }
        val plotWidth = size.width - axisWidth
        val plotHeight = size.height - labelHeight
        if (plotWidth <= 0f || plotHeight <= 0f) return@Canvas

        val labelStyle = TextStyle(fontSize = 9.sp, color = labelColor)

        // --- 縦軸（dB）。0dB を中央に置く ---
        var db = -RANGE_DB
        while (db <= RANGE_DB) {
            val y = dbToY(db, plotHeight)
            drawLine(
                color = gridColor.copy(alpha = if (db == 0.0) 1f else 0.5f),
                start = Offset(axisWidth, y),
                end = Offset(size.width, y),
                strokeWidth = if (db == 0.0) 2f else 1f,
            )
            val layout = textMeasurer.measure("%+.0f".format(db), labelStyle)
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    x = (axisWidth - layout.size.width - 2f).coerceAtLeast(0f),
                    y = (y - layout.size.height / 2f).coerceIn(0f, plotHeight - layout.size.height),
                ),
            )
            db += GRID_STEP_DB
        }

        // --- 横軸（Hz） ---
        TICKS.forEach { hz ->
            val x = hzToX(hz, axisWidth, plotWidth)
            drawLine(
                color = gridColor.copy(alpha = 0.5f),
                start = Offset(x, 0f),
                end = Offset(x, plotHeight),
            )
            val layout = textMeasurer.measure(formatHz(hz), labelStyle)
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    x = (x - layout.size.width / 2f).coerceIn(0f, size.width - layout.size.width),
                    y = plotHeight + 2f,
                ),
            )
        }

        // --- 2本の傾き ---
        drawSlope(
            slopeDbPerOctave = slopeDbPerOctave + OCTAVE_VIEW_OFFSET,
            color = octaveColor,
            axisWidth = axisWidth,
            plotWidth = plotWidth,
            plotHeight = plotHeight,
            dashed = true,
        )
        drawSlope(
            slopeDbPerOctave = slopeDbPerOctave,
            color = fftColor,
            axisWidth = axisWidth,
            plotWidth = plotWidth,
            plotHeight = plotHeight,
            dashed = false,
        )

        // --- 凡例は線の傍に置く。図と表を目で往復させないため ---
        val legendStyle = TextStyle(fontSize = 9.sp, color = labelColor)
        listOf(
            fftLabel to slopeDbPerOctave,
            octaveLabel to slopeDbPerOctave + OCTAVE_VIEW_OFFSET,
        ).forEach { (label, slope) ->
            val text = "$label %+.0f dB/oct".format(slope)
            val layout = textMeasurer.measure(text, legendStyle)
            val y = dbToY(slope * OCTAVES_FROM_CENTER_RIGHT, plotHeight)
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    x = (size.width - layout.size.width - 2f).coerceAtLeast(axisWidth),
                    y = (y - layout.size.height - 2f).coerceIn(0f, plotHeight - layout.size.height),
                ),
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSlope(
    slopeDbPerOctave: Double,
    color: Color,
    axisWidth: Float,
    plotWidth: Float,
    plotHeight: Float,
    dashed: Boolean,
) {
    val leftDb = slopeDbPerOctave * -OCTAVES_FROM_CENTER_LEFT
    val rightDb = slopeDbPerOctave * OCTAVES_FROM_CENTER_RIGHT

    drawLine(
        color = color,
        start = Offset(axisWidth, dbToY(leftDb, plotHeight)),
        end = Offset(axisWidth + plotWidth, dbToY(rightDb, plotHeight)),
        strokeWidth = if (dashed) 2f else 3f,
        pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(8f, 6f)) else null,
    )
}

/** 1kHz を 0dB として、±[RANGE_DB] を縦幅いっぱいに割り当てる */
private fun dbToY(db: Double, plotHeight: Float): Float {
    val normalized = ((db + RANGE_DB) / (2 * RANGE_DB)).coerceIn(0.0, 1.0)
    return (plotHeight * (1.0 - normalized)).toFloat()
}

private fun hzToX(hz: Double, axisWidth: Float, plotWidth: Float): Float {
    val ratio = ln(hz / MIN_HZ) / ln(MAX_HZ / MIN_HZ)
    return axisWidth + (ratio * plotWidth).toFloat()
}

private val TICKS = doubleArrayOf(20.0, 100.0, 1_000.0, 10_000.0, 20_000.0)

private const val MIN_HZ = 20.0
private const val MAX_HZ = 20_000.0

/** 1kHz から左端（20Hz）までのオクターブ数。log2(1000/20) ≒ 5.6 */
private const val OCTAVES_FROM_CENTER_LEFT = 5.64

/** 1kHz から右端（20kHz）までのオクターブ数。log2(20000/1000) ≒ 4.3 */
private const val OCTAVES_FROM_CENTER_RIGHT = 4.32

/**
 * オクターブ表示にすると見かけの傾きが +3dB/oct される。
 * 帯域幅がオクターブごとに倍になるぶん、そのバンドに入るエネルギーが倍（+3dB）になるため
 */
private const val OCTAVE_VIEW_OFFSET = 3.0

private const val RANGE_DB = 18.0
private const val GRID_STEP_DB = 6.0

private val CHART_HEIGHT = 150.dp
private val AXIS_WIDTH = 30.dp
private val LABEL_HEIGHT = 14.dp
