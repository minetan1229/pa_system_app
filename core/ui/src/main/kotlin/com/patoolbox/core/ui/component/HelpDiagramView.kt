package com.patoolbox.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patoolbox.core.reference.HelpDiagram
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin

/**
 * 解説に付けた [HelpDiagram] を描く。
 *
 * HelpSheet からしか呼ばない前提の小さな部品なので、
 * 見た目の凝った作り込みはせず、NoiseSlopeChart / BandRuler と同じ
 * 「軸・目盛り・数字」だけの最小限のスタイルに揃えている。
 */
@Composable
fun HelpDiagramView(
    diagram: HelpDiagram,
    modifier: Modifier = Modifier,
) {
    when (diagram) {
        is HelpDiagram.BarSeries -> BarSeriesView(diagram, modifier)
        is HelpDiagram.LineCurve -> LineCurveView(diagram, modifier)
        is HelpDiagram.PolarPattern -> PolarPatternView(diagram, modifier)
    }
}

@Composable
private fun BarSeriesView(
    diagram: HelpDiagram.BarSeries,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val valueColor = MaterialTheme.colorScheme.onSurface
    val barColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(BAR_CHART_HEIGHT),
    ) {
        val labelHeightPx = with(density) { BAR_LABEL_HEIGHT.toPx() }
        val baseline = size.height - labelHeightPx
        if (baseline <= 0f) return@Canvas

        val maxAbs = diagram.bars.maxOf { kotlin.math.abs(it.value) }.coerceAtLeast(0.001f)
        val hasNegative = diagram.bars.any { it.value < 0f }
        // 負の値があるバー（GBFの目減りなど）は 0 を中央に置き、無ければ 0 を下端に置く
        val zeroY = if (hasNegative) baseline / 2f else baseline
        val usableHalf = if (hasNegative) baseline / 2f else baseline

        drawLine(
            color = axisColor,
            start = Offset(0f, zeroY),
            end = Offset(size.width, zeroY),
            strokeWidth = 2f,
        )

        val slot = size.width / diagram.bars.size
        val barWidth = (slot * 0.42f).coerceAtLeast(4f)
        val valueStyle = TextStyle(fontSize = 10.sp, color = valueColor)
        val labelStyle = TextStyle(fontSize = 9.sp, color = labelColor)

        diagram.bars.forEachIndexed { index, bar ->
            val centerX = slot * index + slot / 2f
            val barHeight = (kotlin.math.abs(bar.value) / maxAbs) * usableHalf * 0.85f
            val top = if (bar.value >= 0f) zeroY - barHeight else zeroY
            drawRoundRect(
                color = barColor,
                topLeft = Offset(centerX - barWidth / 2f, top),
                size = Size(barWidth, barHeight.coerceAtLeast(2f)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f),
            )

            val valueText = formatBarValue(bar.value, diagram.unit)
            val valueLayout = textMeasurer.measure(valueText, valueStyle)
            val valueY = if (bar.value >= 0f) {
                top - valueLayout.size.height - 2f
            } else {
                top + barHeight + 2f
            }
            drawText(
                textLayoutResult = valueLayout,
                topLeft = Offset(
                    (centerX - valueLayout.size.width / 2f)
                        .coerceIn(0f, size.width - valueLayout.size.width),
                    valueY.coerceIn(0f, baseline - valueLayout.size.height),
                ),
            )

            val labelLayout = textMeasurer.measure(bar.label, labelStyle)
            drawText(
                textLayoutResult = labelLayout,
                topLeft = Offset(
                    (centerX - labelLayout.size.width / 2f)
                        .coerceIn(0f, size.width - labelLayout.size.width),
                    baseline + 2f,
                ),
            )
        }
    }
}

private fun formatBarValue(value: Float, unit: String): String {
    val text = if (value == value.toInt().toFloat()) {
        "%+d".format(value.toInt())
    } else {
        "%+.2f".format(value)
    }
    return if (unit.isEmpty()) text else "$text$unit"
}

@Composable
private fun LineCurveView(
    diagram: HelpDiagram.LineCurve,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val seriesColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
    )

    val allPoints = diagram.series.flatMap { it.points }
    val minX = allPoints.minOf { it.first }
    val maxX = allPoints.maxOf { it.first }
    val minY = allPoints.minOf { it.second }
    val maxY = allPoints.maxOf { it.second }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(LINE_CHART_HEIGHT),
    ) {
        val axisWidth = with(density) { AXIS_LABEL_WIDTH.toPx() }
        val bottomHeight = with(density) { BOTTOM_LABEL_HEIGHT.toPx() }
        val plotWidth = size.width - axisWidth
        val plotHeight = size.height - bottomHeight
        if (plotWidth <= 0f || plotHeight <= 0f) return@Canvas

        fun mapX(x: Float): Float = axisWidth + if (diagram.logX) {
            val ratio = (ln(x / minX) / ln(maxX / minX)).toFloat()
            ratio * plotWidth
        } else {
            ((x - minX) / (maxX - minX)) * plotWidth
        }

        fun mapY(y: Float): Float {
            val ratio = (y - minY) / (maxY - minY)
            return plotHeight * (1f - ratio)
        }

        val labelStyle = TextStyle(fontSize = 9.sp, color = labelColor)

        // --- 縦軸の目盛り（4分割） ---
        val ySteps = 4
        for (i in 0..ySteps) {
            val y = minY + (maxY - minY) * i / ySteps
            val py = mapY(y)
            drawLine(
                color = gridColor.copy(alpha = 0.5f),
                start = Offset(axisWidth, py),
                end = Offset(size.width, py),
            )
            val layout = textMeasurer.measure("%.0f".format(y), labelStyle)
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    (axisWidth - layout.size.width - 2f).coerceAtLeast(0f),
                    (py - layout.size.height / 2f).coerceIn(0f, plotHeight - layout.size.height),
                ),
            )
        }

        // --- 横軸の目盛り。log の場合は元データの x をそのまま節目にする ---
        val xTicks = if (diagram.logX) {
            allPoints.map { it.first }.distinct().sorted()
        } else {
            (0..4).map { minX + (maxX - minX) * it / 4 }
        }
        xTicks.forEach { x ->
            val px = mapX(x)
            drawLine(
                color = gridColor.copy(alpha = 0.5f),
                start = Offset(px, 0f),
                end = Offset(px, plotHeight),
            )
            val layout = textMeasurer.measure("%.0f".format(x), labelStyle)
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    (px - layout.size.width / 2f).coerceIn(axisWidth, size.width - layout.size.width),
                    plotHeight + 2f,
                ),
            )
        }

        // --- 各系列の折れ線 ---
        diagram.series.forEachIndexed { index, s ->
            val color = seriesColors[index % seriesColors.size]
            val points = s.points.map { Offset(mapX(it.first), mapY(it.second)) }
            for (i in 0 until points.size - 1) {
                drawLine(
                    color = color,
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = 3f,
                )
            }
            // 凡例は最後の点の傍に置く。図と表を目で往復させないため
            val last = points.last()
            val legend = textMeasurer.measure(s.label, TextStyle(fontSize = 9.sp, color = color))
            drawText(
                textLayoutResult = legend,
                topLeft = Offset(
                    (last.x - legend.size.width).coerceIn(axisWidth, size.width - legend.size.width),
                    (last.y - legend.size.height - 4f).coerceIn(0f, plotHeight - legend.size.height),
                ),
            )
        }
    }
}

@Composable
private fun PolarPatternView(
    diagram: HelpDiagram.PolarPattern,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val lineColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(POLAR_CHART_HEIGHT),
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = (kotlin.math.min(size.width, size.height) / 2f) * 0.78f

        // --- 目盛りの同心円と十字線 ---
        listOf(0.25f, 0.5f, 0.75f, 1.0f).forEach { fraction ->
            drawCircle(
                color = gridColor.copy(alpha = 0.5f),
                radius = radius * fraction,
                center = Offset(cx, cy),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f),
            )
        }
        drawLine(gridColor.copy(alpha = 0.5f), Offset(cx - radius, cy), Offset(cx + radius, cy))
        drawLine(gridColor.copy(alpha = 0.5f), Offset(cx, cy - radius), Offset(cx, cy + radius))

        // --- 指向性の形。0°を上（正面）にして時計回りに1周する ---
        val steps = 180
        val path = androidx.compose.ui.graphics.Path()
        for (i in 0..steps) {
            val theta = 2 * PI * i / steps
            val r = polarRadius(diagram.pattern, theta) * radius
            val x = cx + (r * sin(theta)).toFloat()
            val y = cy - (r * cos(theta)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path = path, color = lineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))

        val labelStyle = TextStyle(fontSize = 10.sp, color = labelColor)
        val front = textMeasurer.measure("正面 0°", labelStyle)
        drawText(front, topLeft = Offset(cx - front.size.width / 2f, cy - radius - front.size.height - 2f))
        val back = textMeasurer.measure("背面 180°", labelStyle)
        drawText(back, topLeft = Offset(cx - back.size.width / 2f, cy + radius + 2f))
    }
}

/**
 * 各指向性の極形式の式。θ=0 が正面。
 * カーディオイド族は r = a + (1-a)cosθ の形で、a を変えるだけで作れる
 * （a=1: 無指向性, a=0.5: カーディオイド, a=0.37: スーパー/ハイパーカーディオイド, a=0: 双指向性）。
 */
private fun polarRadius(pattern: HelpDiagram.PolarPattern.Pattern, theta: Double): Double {
    val a = when (pattern) {
        HelpDiagram.PolarPattern.Pattern.OMNI -> 1.0
        HelpDiagram.PolarPattern.Pattern.CARDIOID -> 0.5
        HelpDiagram.PolarPattern.Pattern.SUPERCARDIOID -> 0.37
        HelpDiagram.PolarPattern.Pattern.FIGURE_8 -> 0.0
    }
    return kotlin.math.abs(a + (1 - a) * cos(theta))
}

private val BAR_CHART_HEIGHT = 150.dp
private val BAR_LABEL_HEIGHT = 16.dp

private val LINE_CHART_HEIGHT = 160.dp
private val AXIS_LABEL_WIDTH = 26.dp
private val BOTTOM_LABEL_HEIGHT = 16.dp

private val POLAR_CHART_HEIGHT = 200.dp
