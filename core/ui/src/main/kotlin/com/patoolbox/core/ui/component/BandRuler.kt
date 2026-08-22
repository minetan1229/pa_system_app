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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patoolbox.core.designsystem.component.PaTone
import com.patoolbox.core.designsystem.component.content
import com.patoolbox.core.reference.BandAction
import com.patoolbox.core.reference.BandTip
import kotlin.math.ln

/**
 * 帯域の物差し。
 *
 * 20Hz〜20kHz を対数で1本の帯にして、その楽器の基音と、触る帯域を重ねて出す。
 * **目盛りの数字を必ず入れる**のがこの図の要点で、
 * 「山があること」だけ分かっても、卓の前では何Hzを触るか決められない。
 *
 * リニア軸にしないのは、可聴域の下半分（20Hz〜1kHz）に音楽の情報の大半があり、
 * リニアだとその全部が左端の 5% に潰れるため。
 */
@Composable
fun BandRuler(
    fundamentalFromHz: Double,
    fundamentalToHz: Double,
    tips: List<BandTip>,
    modifier: Modifier = Modifier,
    /** 上段の帯に付ける名前。楽器なら「基音」、帯域辞書なら「この帯域」 */
    fundamentalLabel: String = "基音",
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant
    val fundamentalColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurface

    // 色は PaTone 経由で引く。4テーマぶんの出し分けをここに書かないため
    val actionColors = mapOf(
        BandAction.CUT to PaTone.INFO.content(),
        BandAction.BOOST to PaTone.SUCCESS.content(),
        BandAction.WATCH to PaTone.DANGER.content(),
        BandAction.EITHER to PaTone.NEUTRAL.content(),
    )

    val laneHeight = 18.dp
    val axisHeight = 22.dp
    val fundamentalLane = 14.dp
    val totalHeight = axisHeight + fundamentalLane + laneHeight * tips.size

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight),
    ) {
        val laneHeightPx = with(density) { laneHeight.toPx() }
        val axisHeightPx = with(density) { axisHeight.toPx() }
        val fundamentalLanePx = with(density) { fundamentalLane.toPx() }
        val barHeight = laneHeightPx * 0.55f
        val plotWidth = size.width

        // --- 目盛り。縦線を先に引いて、帯をその上に重ねる ---
        val gridTop = 0f
        val gridBottom = size.height - axisHeightPx
        TICKS.forEach { hz ->
            val x = hzToX(hz, plotWidth)
            drawLine(
                color = axisColor,
                start = Offset(x, gridTop),
                end = Offset(x, gridBottom),
                strokeWidth = 1f,
            )
            val label = textMeasurer.measure(
                text = formatHz(hz),
                style = TextStyle(fontSize = 9.sp, color = tickColor),
            )
            // 端の数字は内側に寄せる。はみ出すと切れて読めなくなる
            val labelX = (x - label.size.width / 2f)
                .coerceIn(0f, plotWidth - label.size.width)
            drawText(
                textLayoutResult = label,
                topLeft = Offset(labelX, gridBottom + 4f),
            )
        }
        drawLine(
            color = axisColor,
            start = Offset(0f, gridBottom),
            end = Offset(plotWidth, gridBottom),
            strokeWidth = 2f,
        )

        // --- 基音の範囲 ---
        val fundamentalTop = 0f
        drawBand(
            fromHz = fundamentalFromHz,
            toHz = fundamentalToHz,
            top = fundamentalTop + (fundamentalLanePx - barHeight) / 2f,
            height = barHeight,
            plotWidth = plotWidth,
            color = fundamentalColor.copy(alpha = 0.35f),
        )
        val fundamentalLayout = textMeasurer.measure(
            text = fundamentalLabel,
            style = TextStyle(fontSize = 9.sp, color = tickColor),
        )
        val fundamentalEndX = hzToX(fundamentalToHz, plotWidth)
        drawText(
            textLayoutResult = fundamentalLayout,
            topLeft = Offset(
                (fundamentalEndX + 4f)
                    .coerceAtMost(plotWidth - fundamentalLayout.size.width),
                fundamentalTop,
            ),
        )

        // --- 触る帯域。順番はリストと同じにする ---
        tips.forEachIndexed { index, tip ->
            val laneTop = fundamentalLanePx + laneHeightPx * index
            val color = actionColors[tip.action] ?: tickColor
            drawBand(
                fromHz = tip.fromHz,
                toHz = tip.toHz,
                top = laneTop + (laneHeightPx - barHeight) / 2f,
                height = barHeight,
                plotWidth = plotWidth,
                color = color,
            )

            val label = textMeasurer.measure(
                text = tip.label,
                style = TextStyle(fontSize = 10.sp, color = labelColor),
            )
            val barEnd = hzToX(tip.toHz, plotWidth)
            val barStart = hzToX(tip.fromHz, plotWidth)
            // 帯の右に置けるなら右、入らなければ左に出す。
            // 帯の中に重ねると、細い帯（ノッチ）では必ず読めなくなる
            val labelX = if (barEnd + 4f + label.size.width <= plotWidth) {
                barEnd + 4f
            } else {
                (barStart - 4f - label.size.width).coerceAtLeast(0f)
            }
            drawText(
                textLayoutResult = label,
                topLeft = Offset(labelX, laneTop + (laneHeightPx - label.size.height) / 2f),
            )
        }
    }
}

private fun DrawScope.drawBand(
    fromHz: Double,
    toHz: Double,
    top: Float,
    height: Float,
    plotWidth: Float,
    color: Color,
) {
    val start = hzToX(fromHz, plotWidth)
    val end = hzToX(toHz, plotWidth)
    // 最低幅を持たせる。20Hz 幅のノッチは1px 未満になって見えなくなる
    val width = (end - start).coerceAtLeast(3f)
    drawRoundRect(
        color = color,
        topLeft = Offset(start, top),
        size = Size(width, height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(height / 2f),
    )
}

/** 可聴域を対数で横幅に写す。 */
private fun hzToX(hz: Double, plotWidth: Float): Float {
    val clamped = hz.coerceIn(MIN_HZ, MAX_HZ)
    val ratio = ln(clamped / MIN_HZ) / ln(MAX_HZ / MIN_HZ)
    return (ratio * plotWidth).toFloat()
}

/** 数字を出す節目。オクターブの区切りに合わせてある */
private val TICKS = doubleArrayOf(
    20.0, 50.0, 100.0, 200.0, 500.0, 1_000.0, 2_000.0, 5_000.0, 10_000.0, 20_000.0,
)

private const val MIN_HZ = 20.0
private const val MAX_HZ = 20_000.0

/** [BandAction] の凡例に使う色。図と一覧で同じ色にするために公開している */
@Composable
fun BandAction.tone(): PaTone = when (this) {
    BandAction.CUT -> PaTone.INFO
    BandAction.BOOST -> PaTone.SUCCESS
    BandAction.WATCH -> PaTone.DANGER
    BandAction.EITHER -> PaTone.NEUTRAL
}

/** 図と同じ描き方の凡例。図の下に1行で置く（[ChartLegend] に渡す） */
@Composable
fun bandActionLegend(): List<Pair<String, Color>> = listOf(
    BandAction.CUT.label to PaTone.INFO.content(),
    BandAction.BOOST.label to PaTone.SUCCESS.content(),
    BandAction.WATCH.label to PaTone.DANGER.content(),
    BandAction.EITHER.label to PaTone.NEUTRAL.content(),
)
