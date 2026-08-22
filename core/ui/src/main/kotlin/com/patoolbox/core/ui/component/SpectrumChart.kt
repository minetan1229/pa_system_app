package com.patoolbox.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * 縦軸の表示範囲。
 *
 * 固定値にしないのは、未校正（dBFS、-90〜0 あたり）と校正済み（dB SPL、40〜120 あたり）で
 * 出てくる数字が 100dB 近くずれるため。固定にすると片方で必ず画面外に出る。
 */
data class SpectrumRange(val bottomDb: Double, val topDb: Double) {
    fun toY(db: Float, plotHeight: Float): Float {
        if (!db.isFinite()) return plotHeight
        val normalized = ((db - bottomDb) / (topDb - bottomDb)).coerceIn(0.0, 1.0)
        return (plotHeight * (1.0 - normalized)).toFloat()
    }

    companion object {
        const val GRID_STEP_DB = 10.0

        /** 山の頭が天井に張り付かないように空ける余白 */
        private const val HEADROOM_DB = 6.0

        /** データが1つも無いときの既定。dBFS を想定した位置 */
        private val EMPTY = SpectrumRange(bottomDb = -90.0, topDb = -10.0)

        /**
         * 一番大きいカラムに合わせて上端を決める。
         *
         * 現場では「絶対値が何dBか」より「どこが飛び出しているか」を見るので、
         * 手で範囲を合わせ直させない方がよい。上端だけ追従させ、
         * 幅（[spanDb]）は利用者に選ばせる。
         */
        fun auto(columnsDb: FloatArray, spanDb: Double): SpectrumRange {
            var max = Float.NEGATIVE_INFINITY
            for (value in columnsDb) {
                if (value.isFinite() && value > max) max = value
            }
            if (!max.isFinite()) return EMPTY.copy(bottomDb = EMPTY.topDb - spanDb)

            val top = ceil((max + HEADROOM_DB) / GRID_STEP_DB) * GRID_STEP_DB
            return SpectrumRange(bottomDb = top - spanDb, topDb = top)
        }
    }
}

/** 縦線と数字を出すオクターブの節目 */
private val GRID_FREQUENCIES = doubleArrayOf(
    31.5, 63.0, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0,
)

/**
 * 図に立てる印。
 *
 * ハウリングしている周波数を波形の上で名指しするために使う。
 * 山があること自体は波形で分かるが、「その山がハウリングだと判断された」ことは
 * 波形には出ない。判断の結果は別のしるしで重ねる必要がある。
 *
 * @param active いま鳴っているか。止まったものは薄い点線にして、
 *   「さっき鳴っていた場所」として残す
 * @param weight 0〜1。線の太さに反映する。長く鳴っているものほど太くする用途
 */
data class SpectrumMarker(
    val frequencyHz: Double,
    val label: String,
    val active: Boolean = true,
    val weight: Float = 1f,
)

/**
 * 対数周波数軸のスペクトラム図。
 *
 * 目盛りの数字を必ず出す。数字の無い波形は「山がある」ことしか伝えず、
 * 現場で次に何をするか（どの帯域を何dB削るか）が決められない。
 *
 * @param columnsDb 対数等間隔に並んだカラムのレベル。[frequencies] と同じ長さ
 * @param cursorHz カーソル位置。null なら出さない
 * @param onCursorChange 触った位置の周波数を返す。null を渡すと触れない図になる
 * @param harmonics カーソルの倍音を何次まで薄く示すか。0 なら出さない。
 *   ハムは 50/100/150Hz と等間隔に並ぶので、基音に合わせると一目で見分けられる
 * @param markers 波形に重ねる印。ハウリング検出の結果を出すのに使う
 */
@Composable
fun SpectrumChart(
    columnsDb: FloatArray,
    frequencies: DoubleArray,
    range: SpectrumRange,
    modifier: Modifier = Modifier,
    peakHoldDb: FloatArray = FloatArray(0),
    cursorHz: Double? = null,
    onCursorChange: ((Double?) -> Unit)? = null,
    harmonics: Int = 0,
    markers: List<SpectrumMarker> = emptyList(),
    showAxis: Boolean = true,
    height: Dp = 240.dp,
) {
    val textMeasurer = rememberTextMeasurer()
    val lineColor = MaterialTheme.colorScheme.primary
    val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    val peakColor = MaterialTheme.colorScheme.tertiary
    val cursorColor = MaterialTheme.colorScheme.error
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = remember(labelColor) { TextStyle(fontSize = 9.sp, color = labelColor) }

    // 印の色。いま鳴っているものは警告色、止まったものは無彩色に落とす。
    // 色だけで区別せず、線種（実線／点線）でも分けているのは暗所モードのため
    val markerColor = MaterialTheme.colorScheme.error
    val markerPastColor = MaterialTheme.colorScheme.onSurfaceVariant
    val markerInkOnActive = MaterialTheme.colorScheme.onError
    val markerLabelBackground = MaterialTheme.colorScheme.surface

    val density = LocalDensity.current
    val axisWidth = if (showAxis) with(density) { AXIS_WIDTH.toPx() } else 0f
    val labelHeight = if (showAxis) with(density) { LABEL_HEIGHT.toPx() } else 0f

    val minHz = frequencies.firstOrNull() ?: DEFAULT_MIN_HZ
    val maxHz = frequencies.lastOrNull() ?: DEFAULT_MAX_HZ

    // 触った x から周波数を出す。描画と同じ座標系を使わないとカーソルがずれるので、
    // plot の左端と幅はここで一度だけ決めて両方に配る
    val gestureModifier = if (onCursorChange == null) {
        Modifier
    } else {
        Modifier
            .pointerInput(minHz, maxHz, axisWidth) {
                detectTapGestures { offset ->
                    onCursorChange(xToHz(offset.x, axisWidth, size.width.toFloat(), minHz, maxHz))
                }
            }
            .pointerInput(minHz, maxHz, axisWidth) {
                detectHorizontalDragGestures { change, _ ->
                    onCursorChange(
                        xToHz(change.position.x, axisWidth, size.width.toFloat(), minHz, maxHz),
                    )
                }
            }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .then(gestureModifier),
    ) {
        val plotWidth = size.width - axisWidth
        val plotHeight = size.height - labelHeight
        if (plotWidth <= 0f || plotHeight <= 0f) return@Canvas

        drawLevelGrid(range, axisWidth, plotWidth, plotHeight, gridColor, textMeasurer, labelStyle, showAxis)
        drawFrequencyGrid(
            minHz, maxHz, axisWidth, plotWidth, plotHeight, size.height,
            gridColor, textMeasurer, labelStyle, showAxis,
        )

        if (columnsDb.isNotEmpty()) {
            drawTrace(columnsDb, range, axisWidth, plotWidth, plotHeight, lineColor, fillColor, TRACE_WIDTH)
        }
        if (peakHoldDb.isNotEmpty()) {
            drawTrace(peakHoldDb, range, axisWidth, plotWidth, plotHeight, peakColor, null, PEAK_WIDTH)
        }

        if (markers.isNotEmpty() && maxHz > minHz) {
            drawMarkers(
                markers = markers,
                minHz = minHz,
                maxHz = maxHz,
                axisWidth = axisWidth,
                plotWidth = plotWidth,
                plotHeight = plotHeight,
                activeColor = markerColor,
                pastColor = markerPastColor,
                activeInk = markerInkOnActive,
                labelBackground = markerLabelBackground,
                textMeasurer = textMeasurer,
            )
        }

        if (cursorHz != null && maxHz > minHz) {
            // 倍音を先に薄く引く。基音の線が倍音に埋もれないよう順番を守る
            for (order in 2..harmonics) {
                val hz = cursorHz * order
                if (hz > maxHz) break
                val ht = (ln(hz / minHz) / ln(maxHz / minHz)).coerceIn(0.0, 1.0)
                drawLine(
                    color = cursorColor.copy(alpha = HARMONIC_ALPHA),
                    start = Offset(axisWidth + plotWidth * ht.toFloat(), 0f),
                    end = Offset(axisWidth + plotWidth * ht.toFloat(), plotHeight),
                )
            }

            val t = (ln(cursorHz / minHz) / ln(maxHz / minHz)).coerceIn(0.0, 1.0)
            val x = axisWidth + plotWidth * t.toFloat()
            drawLine(
                color = cursorColor,
                start = Offset(x, 0f),
                end = Offset(x, plotHeight),
                strokeWidth = CURSOR_WIDTH,
            )
            if (columnsDb.isNotEmpty()) {
                val index = (t * (columnsDb.size - 1)).roundToInt().coerceIn(0, columnsDb.size - 1)
                drawCircle(
                    color = cursorColor,
                    radius = CURSOR_DOT_RADIUS,
                    center = Offset(x, range.toY(columnsDb[index], plotHeight)),
                )
            }
        }
    }
}

private fun xToHz(x: Float, axisWidth: Float, width: Float, minHz: Double, maxHz: Double): Double {
    val plotWidth = width - axisWidth
    if (plotWidth <= 0f || maxHz <= minHz) return minHz
    val t = ((x - axisWidth) / plotWidth).coerceIn(0f, 1f)
    return minHz * exp(t.toDouble() * ln(maxHz / minHz))
}

/**
 * ハウリングの印を波形の上に立てる。
 *
 * 線だけでは「どこか」しか伝わらないので、**周波数の数字を必ず添える**。
 * 数字が無いと、図を見た人が卓の EQ に入れる値を決められない。
 *
 * ラベルは近いものどうしが重なるので、上から3段に振り分ける。
 * 重ねて描くと、2本が近いとき（1/6 オクターブ差など）にどちらも読めなくなる。
 */
private fun DrawScope.drawMarkers(
    markers: List<SpectrumMarker>,
    minHz: Double,
    maxHz: Double,
    axisWidth: Float,
    plotWidth: Float,
    plotHeight: Float,
    activeColor: Color,
    pastColor: Color,
    activeInk: Color,
    labelBackground: Color,
    textMeasurer: TextMeasurer,
) {
    val span = ln(maxHz / minHz)
    // 段ごとの「ここまで文字が埋まっている」x。次の段へ送るかの判定に使う
    val rowRightEdge = FloatArray(MARKER_LABEL_ROWS) { -1f }

    markers
        .sortedBy { it.frequencyHz }
        .forEach { marker ->
            val hz = marker.frequencyHz.coerceIn(minHz, maxHz)
            val x = axisWidth + (plotWidth * ln(hz / minHz) / span).toFloat()
            val color = if (marker.active) activeColor else pastColor

            val strokeWidth = if (marker.active) {
                MARKER_WIDTH + MARKER_WIDTH_PER_WEIGHT * marker.weight.coerceIn(0f, 1f)
            } else {
                MARKER_PAST_WIDTH
            }

            drawLine(
                color = if (marker.active) color else color.copy(alpha = MARKER_PAST_ALPHA),
                start = Offset(x, 0f),
                end = Offset(x, plotHeight),
                strokeWidth = strokeWidth,
                pathEffect = if (marker.active) {
                    null
                } else {
                    PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                },
            )

            val layout = textMeasurer.measure(
                text = marker.label,
                style = TextStyle(
                    fontSize = 10.sp,
                    color = if (marker.active) activeInk else pastColor,
                ),
            )

            // 線の右に置く。右端に寄りすぎたら左へ返す
            var labelX = x + MARKER_LABEL_GAP
            if (labelX + layout.size.width > axisWidth + plotWidth) {
                labelX = x - MARKER_LABEL_GAP - layout.size.width
            }

            val row = (0 until MARKER_LABEL_ROWS)
                .firstOrNull { labelX > rowRightEdge[it] }
                ?: (0 until MARKER_LABEL_ROWS).minBy { rowRightEdge[it] }
            rowRightEdge[row] = labelX + layout.size.width + MARKER_LABEL_GAP

            val labelY = MARKER_LABEL_GAP + row * (layout.size.height + MARKER_LABEL_GAP)

            drawRoundRect(
                color = if (marker.active) activeColor else labelBackground,
                topLeft = Offset(labelX - 3f, labelY - 2f),
                size = Size(
                    layout.size.width + 6f,
                    layout.size.height + 4f,
                ),
                cornerRadius = CornerRadius(3f, 3f),
            )
            drawText(textLayoutResult = layout, topLeft = Offset(labelX, labelY))
        }
}

private fun DrawScope.drawTrace(
    valuesDb: FloatArray,
    range: SpectrumRange,
    axisWidth: Float,
    plotWidth: Float,
    plotHeight: Float,
    color: Color,
    fillColor: Color?,
    strokeWidth: Float,
) {
    val last = (valuesDb.size - 1).coerceAtLeast(1)
    val path = Path()
    for (i in valuesDb.indices) {
        val x = axisWidth + plotWidth * i / last
        val y = range.toY(valuesDb[i], plotHeight)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }

    // 塗りは暗所で線だけより形が掴みやすい。線は塗りの上に重ねて輪郭を残す
    if (fillColor != null) {
        val filled = Path().apply {
            addPath(path)
            lineTo(axisWidth + plotWidth, plotHeight)
            lineTo(axisWidth, plotHeight)
            close()
        }
        drawPath(path = filled, color = fillColor)
    }
    drawPath(path = path, color = color, style = Stroke(strokeWidth))
}

private fun DrawScope.drawLevelGrid(
    range: SpectrumRange,
    axisWidth: Float,
    plotWidth: Float,
    plotHeight: Float,
    gridColor: Color,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
    showAxis: Boolean,
) {
    var db = ceil(range.bottomDb / SpectrumRange.GRID_STEP_DB) * SpectrumRange.GRID_STEP_DB
    while (db <= range.topDb) {
        val y = range.toY(db.toFloat(), plotHeight)
        drawRect(
            color = gridColor.copy(alpha = 0.5f),
            topLeft = Offset(axisWidth, y),
            size = Size(plotWidth, 1f),
        )
        if (showAxis) {
            val layout = textMeasurer.measure("%.0f".format(db), labelStyle)
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    x = (axisWidth - layout.size.width - 2f).coerceAtLeast(0f),
                    y = (y - layout.size.height / 2f).coerceIn(0f, plotHeight - layout.size.height),
                ),
            )
        }
        db += SpectrumRange.GRID_STEP_DB
    }
}

private fun DrawScope.drawFrequencyGrid(
    minHz: Double,
    maxHz: Double,
    axisWidth: Float,
    plotWidth: Float,
    plotHeight: Float,
    totalHeight: Float,
    gridColor: Color,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
    showAxis: Boolean,
) {
    if (maxHz <= minHz) return
    val span = ln(maxHz / minHz)

    for (hz in GRID_FREQUENCIES) {
        if (hz < minHz || hz > maxHz) continue
        val x = axisWidth + (plotWidth * ln(hz / minHz) / span).toFloat()
        drawLine(
            color = gridColor.copy(alpha = 0.5f),
            start = Offset(x, 0f),
            end = Offset(x, plotHeight),
        )
        if (!showAxis) continue

        val layout = textMeasurer.measure(formatHz(hz), labelStyle)
        drawText(
            textLayoutResult = layout,
            topLeft = Offset(
                x = (x - layout.size.width / 2f).coerceIn(0f, size.width - layout.size.width),
                y = (totalHeight - layout.size.height).coerceAtLeast(plotHeight),
            ),
        )
    }
}

/** 1000 以上は "1k" 表記。桁が多いと目盛りが重なって読めなくなる */
fun formatHz(hz: Double): String = when {
    hz >= 1000.0 -> {
        val k = hz / 1000.0
        if (k == floor(k)) "%.0fk".format(k) else "%.1fk".format(k)
    }

    hz >= 100.0 -> "%.0f".format(hz)
    hz >= 10.0 -> if (hz == floor(hz)) "%.0f".format(hz) else "%.1f".format(hz)
    else -> "%.1f".format(hz)
}

private val AXIS_WIDTH = 30.dp
private val LABEL_HEIGHT = 14.dp
private const val DEFAULT_MIN_HZ = 20.0
private const val DEFAULT_MAX_HZ = 20000.0
private const val TRACE_WIDTH = 2.5f
private const val PEAK_WIDTH = 1.5f
private const val CURSOR_WIDTH = 2f
private const val CURSOR_DOT_RADIUS = 6f
private const val HARMONIC_ALPHA = 0.35f

/** ハウリングの印 */
private const val MARKER_WIDTH = 2f
private const val MARKER_WIDTH_PER_WEIGHT = 3f
private const val MARKER_PAST_WIDTH = 1.5f
private const val MARKER_PAST_ALPHA = 0.55f
private const val MARKER_LABEL_GAP = 4f

/** ラベルを振り分ける段数。4段以上にすると波形そのものが隠れる */
private const val MARKER_LABEL_ROWS = 3
