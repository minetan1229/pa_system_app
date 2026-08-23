package com.patoolbox.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.patoolbox.core.designsystem.theme.LocalPaThemeMode
import com.patoolbox.core.designsystem.theme.PaSoft
import com.patoolbox.core.designsystem.theme.isDarkAppearance
import com.patoolbox.core.model.ThemeMode

/**
 * 挿絵。
 *
 * 絵文字も画像素材も使わず、Canvas に図形で描いている。理由は3つ。
 *
 * 1. 絵文字は端末のフォントで形が変わる。Pixel と Galaxy で別の絵が出る画面は作れない。
 * 2. 配布画像を持つと、素材のライセンスを THIRD_PARTY.md に足して追い続けることになる。
 *    自分で描いた図形なら、その管理が丸ごと要らない。
 * 3. テーマが4つある。暗所モード（赤以外の光を出せない）と屋外モード（淡色が飛ぶ）で
 *    同じ PNG は成立しないが、図形なら色だけ差し替えられる。
 *
 * 絵は 160 x 100 の座標で描き、与えられた領域に合わせて等倍で拡大縮小する。
 * 縦横比は呼び出し側が `Modifier.aspectRatio(1.6f)` などで決める。
 */
@Composable
fun PaIllustration(
    scene: PaScene,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val palette = paIllustrationPalette()

    Canvas(
        modifier = if (contentDescription == null) {
            modifier
        } else {
            modifier.semantics { this.contentDescription = contentDescription }
        },
    ) {
        val scale = minOf(size.width / DESIGN_WIDTH, size.height / DESIGN_HEIGHT)
        if (scale <= 0f) return@Canvas

        withTransform({
            translate(
                left = (size.width - DESIGN_WIDTH * scale) / 2f,
                top = (size.height - DESIGN_HEIGHT * scale) / 2f,
            )
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            when (scene) {
                PaScene.STAGE -> drawStage(palette)
                PaScene.MEASURE -> drawMeasure(palette)
                PaScene.CALC -> drawCalc(palette)
                PaScene.DOCUMENT -> drawDocument(palette)
                PaScene.BUSINESS -> drawBusiness(palette)
                PaScene.CALIBRATION -> drawCalibration(palette)
                PaScene.ONE_PHONE -> drawOnePhone(palette)
                PaScene.OPEN_ACCESS -> drawOpenAccess(palette)
                PaScene.SEARCH_EMPTY -> drawSearchEmpty(palette)
            }
        }
    }
}

/** 描ける絵の種類。増やすときは `draw*` を足して [PaIllustration] の when に繋ぐ。 */
enum class PaScene {
    /** ホームの見出し。スピーカー2本とマイクスタンドの舞台 */
    STAGE,

    /** 計測。レベルメーターの棒 */
    MEASURE,

    /** 計算・リファレンス。定規と波形 */
    CALC,

    /** 現場ドキュメント。チャンネル表の紙 */
    DOCUMENT,

    /** 見積・稼働。棒グラフの紙 */
    BUSINESS,

    /** 校正。端末と基準の騒音計を並べる */
    CALIBRATION,

    /** 端末1台だけの校正。スピーカーと端末の距離 */
    ONE_PHONE,

    /**
     * 鍵が開いている図。
     * 「上級」「PRO」の札が鍵ではないこと（今どれも開けること）を、
     * 文章より先に伝えるために使う。
     */
    OPEN_ACCESS,

    /** 検索して何も出なかったとき。虫めがねと空の紙 */
    SEARCH_EMPTY,
}

/**
 * 挿絵の色。
 *
 * [PaTone] と同じ考え方で、呼び出し側に色そのものを持たせない。
 * 暗所モードと屋外モードでは彩度のある淡色が使えないので、
 * その2つでは面の色を無彩色（明度差だけ）に落とす。
 */
@Immutable
data class PaIllustrationPalette(
    /** 輪郭線 */
    val line: Color,
    /** 一番奥の地（床・紙） */
    val ground: Color,
    /** 影・裏側の面 */
    val muted: Color,
    /** 主役の面 */
    val primary: Color,
    /** 副の面 */
    val secondary: Color,
    /** 1か所だけ効かせる色。ここが視線の着地点になる */
    val accent: Color,
    /** 線の太さの倍率。屋外モードだけ太くする */
    val strokeScale: Float,
)

@Composable
fun paIllustrationPalette(): PaIllustrationPalette {
    val scheme = MaterialTheme.colorScheme
    val mode = LocalPaThemeMode.current
    return when (mode) {
        // 赤以外の光を出さない。面は明度差だけで分ける
        ThemeMode.NIGHT_RED -> PaIllustrationPalette(
            line = scheme.onSurfaceVariant,
            ground = scheme.surfaceContainerLow,
            muted = scheme.surfaceContainer,
            primary = scheme.surfaceContainerHigh,
            secondary = scheme.surfaceContainerHighest,
            accent = scheme.primary,
            strokeScale = 1f,
        )

        // 直射日光下。淡い色は飛ぶので無彩色 + 太い線
        ThemeMode.OUTDOOR -> PaIllustrationPalette(
            line = scheme.onSurface,
            ground = scheme.surfaceContainerLow,
            muted = scheme.surfaceContainerHigh,
            primary = scheme.surfaceContainerHighest,
            secondary = scheme.surfaceContainer,
            accent = scheme.primary,
            strokeScale = 1.6f,
        )

        else -> if (mode.isDarkAppearance()) {
            PaIllustrationPalette(
                line = PaSoft.lineDark,
                ground = PaSoft.sandDark,
                muted = PaSoft.mutedDark,
                primary = PaSoft.skyDark,
                secondary = PaSoft.sageDark,
                accent = PaSoft.clayDark,
                strokeScale = 1f,
            )
        } else {
            PaIllustrationPalette(
                line = PaSoft.lineLight,
                ground = PaSoft.sandLight,
                muted = PaSoft.mutedLight,
                primary = PaSoft.skyLight,
                secondary = PaSoft.sageLight,
                accent = PaSoft.clayLight,
                strokeScale = 1f,
            )
        }
    }
}

/**
 * アプリの印。上帯とホームの見出しに出す小さな図形。
 *
 * 文字だけの上帯は、どのツールを開いていても同じ顔になる。
 * 印を1つ置くと「このアプリの中にいる」ことが常に見えるので、
 * 38画面を行き来しても迷子になりにくい。
 *
 * 中心の点から広がる弧（＝1点から出る音）で描いてある。
 * 既製のロゴやアイコンフォントを持ち込まないのは、[PaIllustration] と同じ理由。
 *
 * 色は [androidx.compose.material3.ColorScheme.primary] を通すので、
 * 暗所モードでは赤、屋外モードでは濃紺になる。
 */
@Composable
fun PaAppMark(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(modifier = modifier) {
        val unit = minOf(size.width, size.height)
        if (unit <= 0f) return@Canvas

        // 左下を原点にして右上へ広がる。中央から同心円にすると的に見えてしまう
        val origin = Offset(size.width / 2f - unit * 0.34f, size.height / 2f + unit * 0.30f)
        val stroke = unit * 0.09f

        drawCircle(color = color, radius = unit * 0.11f, center = origin)

        listOf(0.34f, 0.56f, 0.78f).forEachIndexed { index, factor ->
            val radius = unit * factor
            drawArc(
                // 外側ほど薄くする。等しい濃さだと3本の弧が縞模様に見える
                color = color.copy(alpha = 1f - index * 0.22f),
                startAngle = -78f,
                sweepAngle = 66f,
                useCenter = false,
                topLeft = Offset(origin.x - radius, origin.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}

private const val DESIGN_WIDTH = 160f
private const val DESIGN_HEIGHT = 100f

// --- 絵ごとの描画 -----------------------------------------------------------
// どれも「面を塗る → 輪郭を引く」の順。輪郭を先に引くと面に隠れる。

/** ホームの見出し。左右のスピーカー、中央のマイクスタンド、床の線。 */
private fun DrawScope.drawStage(palette: PaIllustrationPalette) {
    val floorY = 84f

    // 舞台の奥。面を1枚敷くと、上に置いた物が「立っている」ように見える
    drawRect(
        color = palette.ground,
        topLeft = Offset(28f, 46f),
        size = Size(104f, floorY - 46f),
    )

    drawLine(
        color = palette.line,
        start = Offset(6f, floorY),
        end = Offset(154f, floorY),
        strokeWidth = 1.5f * palette.strokeScale,
    )

    micStand(palette, x = 78f, floorY = floorY)

    speakerStack(palette, x = 12f, floorY = floorY)
    speakerStack(palette, x = 126f, floorY = floorY)

    // 音の広がり。左のスピーカーからだけ出す（左右に出すと中央で交差して汚れる）
    soundArcs(palette, origin = Offset(36f, 42f), count = 3, start = 12f, step = 9f)
}

/** スピーカー1本（台形の箱 + スタンド）。 */
private fun DrawScope.speakerStack(palette: PaIllustrationPalette, x: Float, floorY: Float) {
    // スタンドの支柱と脚
    drawLine(
        color = palette.line,
        start = Offset(x + 11f, 58f),
        end = Offset(x + 11f, floorY),
        strokeWidth = 2f * palette.strokeScale,
    )
    drawLine(
        color = palette.line,
        start = Offset(x + 3f, floorY),
        end = Offset(x + 19f, floorY),
        strokeWidth = 2f * palette.strokeScale,
    )

    // 箱。上を少し狭めた台形にすると、正面向きの箱より「振っている」感じが出る
    val box = Path().apply {
        moveTo(x + 1f, 24f)
        lineTo(x + 21f, 20f)
        lineTo(x + 21f, 56f)
        lineTo(x + 1f, 58f)
        close()
    }
    drawPath(box, color = palette.primary)
    drawPath(box, color = palette.line, style = Stroke(width = 1.5f * palette.strokeScale))

    // ウーファーとツイーター
    drawCircle(palette.muted, radius = 6.5f, center = Offset(x + 11f, 44f))
    drawCircle(
        color = palette.line,
        radius = 6.5f,
        center = Offset(x + 11f, 44f),
        style = Stroke(1.2f * palette.strokeScale),
    )
    drawCircle(palette.secondary, radius = 3f, center = Offset(x + 11f, 30f))
    drawCircle(
        color = palette.line,
        radius = 3f,
        center = Offset(x + 11f, 30f),
        style = Stroke(1.2f * palette.strokeScale),
    )
}

/** マイクスタンド。 */
private fun DrawScope.micStand(palette: PaIllustrationPalette, x: Float, floorY: Float) {
    drawLine(
        color = palette.line,
        start = Offset(x, 48f),
        end = Offset(x, floorY),
        strokeWidth = 1.8f * palette.strokeScale,
    )
    drawLine(
        color = palette.line,
        start = Offset(x - 9f, floorY),
        end = Offset(x + 9f, floorY),
        strokeWidth = 1.8f * palette.strokeScale,
    )
    // ブーム
    drawLine(
        color = palette.line,
        start = Offset(x, 48f),
        end = Offset(x + 13f, 42f),
        strokeWidth = 1.8f * palette.strokeScale,
    )
    drawCircle(palette.secondary, radius = 4.5f, center = Offset(x + 16f, 41f))
    drawCircle(
        color = palette.line,
        radius = 4.5f,
        center = Offset(x + 16f, 41f),
        style = Stroke(1.3f * palette.strokeScale),
    )
}

/** 計測。レベルメーターの棒と目盛り線。 */
private fun DrawScope.drawMeasure(palette: PaIllustrationPalette) {
    val baseY = 82f
    val heights = listOf(22f, 38f, 30f, 52f, 44f, 60f, 34f, 26f)

    // 目盛り。3本だけ引く（多いと絵ではなく図に見える）
    listOf(30f, 48f, 66f).forEach { y ->
        drawLine(
            color = palette.muted,
            start = Offset(14f, y),
            end = Offset(146f, y),
            strokeWidth = 1f * palette.strokeScale,
        )
    }

    heights.forEachIndexed { index, height ->
        val x = 18f + index * 16f
        val rect = Rect(x, baseY - height, x + 11f, baseY)
        fillRoundRect(if (height >= 52f) palette.accent else palette.primary, rect, 2f)
        strokeRoundRect(palette.line, rect, 2f, palette.strokeScale)
    }

    drawLine(
        color = palette.line,
        start = Offset(12f, baseY),
        end = Offset(148f, baseY),
        strokeWidth = 1.8f * palette.strokeScale,
    )
}

/** 計算・リファレンス。定規と波形。 */
private fun DrawScope.drawCalc(palette: PaIllustrationPalette) {
    val ruler = Rect(14f, 60f, 146f, 82f)
    fillRoundRect(palette.secondary, ruler, 3f)
    strokeRoundRect(palette.line, ruler, 3f, palette.strokeScale)
    repeat(12) { index ->
        val x = 24f + index * 11f
        val tick = if (index % 3 == 0) 12f else 7f
        drawLine(
            color = palette.line,
            start = Offset(x, 60f),
            end = Offset(x, 60f + tick),
            strokeWidth = 1.2f * palette.strokeScale,
        )
    }

    drawLine(
        color = palette.muted,
        start = Offset(14f, 36f),
        end = Offset(146f, 36f),
        strokeWidth = 1f * palette.strokeScale,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)),
    )

    // 波形。1周期だけ描く
    val wave = Path().apply {
        moveTo(14f, 36f)
        cubicTo(36f, 8f, 58f, 8f, 80f, 36f)
        cubicTo(102f, 64f, 124f, 64f, 146f, 36f)
    }
    drawPath(wave, color = palette.accent, style = Stroke(width = 2.4f * palette.strokeScale))
}

/** 現場ドキュメント。チャンネル表の紙。 */
private fun DrawScope.drawDocument(palette: PaIllustrationPalette) {
    // 後ろにもう1枚。重なりで「複数の書類」だと分かる
    val back = Rect(44f, 16f, 134f, 90f)
    fillRoundRect(palette.muted, back, 3f)
    strokeRoundRect(palette.line, back, 3f, palette.strokeScale)

    val front = Rect(26f, 10f, 116f, 84f)
    fillRoundRect(palette.ground, front, 3f)
    strokeRoundRect(palette.line, front, 3f, palette.strokeScale)

    // 見出し帯
    fillRoundRect(palette.accent, Rect(34f, 18f, 108f, 26f), 2f)

    // 行。左に短い列（ch 番号）、右に長い列（名前）
    repeat(5) { index ->
        val y = 34f + index * 10f
        fillRoundRect(palette.primary, Rect(34f, y, 48f, y + 5f), 1.5f)
        fillRoundRect(palette.muted, Rect(54f, y, 108f, y + 5f), 1.5f)
    }
}

/** 見積・稼働。棒グラフの紙。 */
private fun DrawScope.drawBusiness(palette: PaIllustrationPalette) {
    val paper = Rect(24f, 12f, 136f, 88f)
    fillRoundRect(palette.ground, paper, 3f)
    strokeRoundRect(palette.line, paper, 3f, palette.strokeScale)

    val baseY = 74f
    listOf(18f, 30f, 24f, 42f).forEachIndexed { index, height ->
        val x = 40f + index * 22f
        val rect = Rect(x, baseY - height, x + 14f, baseY)
        fillRoundRect(if (index == 3) palette.accent else palette.primary, rect, 2f)
        strokeRoundRect(palette.line, rect, 2f, palette.strokeScale)
    }

    drawLine(
        color = palette.line,
        start = Offset(34f, baseY),
        end = Offset(126f, baseY),
        strokeWidth = 1.5f * palette.strokeScale,
    )
    // 合計行
    fillRoundRect(palette.muted, Rect(34f, 80f, 126f, 84f), 1.5f)
}

/** 校正。左に基準の騒音計、右に端末。同じ音を同じ位置で聞かせる図。 */
private fun DrawScope.drawCalibration(palette: PaIllustrationPalette) {
    soundArcs(palette, origin = Offset(8f, 52f), count = 3, start = 14f, step = 9f)

    // 基準の騒音計。上が細く下が太い、あの形
    val meter = Path().apply {
        moveTo(58f, 22f)
        lineTo(72f, 22f)
        lineTo(72f, 46f)
        lineTo(78f, 46f)
        lineTo(78f, 86f)
        lineTo(52f, 86f)
        lineTo(52f, 46f)
        lineTo(58f, 46f)
        close()
    }
    drawPath(meter, color = palette.secondary)
    drawPath(meter, color = palette.line, style = Stroke(width = 1.5f * palette.strokeScale))
    val display = Rect(56f, 52f, 74f, 64f)
    fillRoundRect(palette.ground, display, 1.5f)
    strokeRoundRect(palette.line, display, 1.5f, palette.strokeScale)

    // 端末
    phone(palette, left = 98f, top = 22f, width = 34f, height = 64f)

    // 2つが同じ高さ・同じ向きであることを示す線。
    // 物の上端と同じ高さに引くと線が本体に重なって見えるので、少し上に離す
    drawLine(
        color = palette.muted,
        start = Offset(44f, 14f),
        end = Offset(142f, 14f),
        strokeWidth = 1f * palette.strokeScale,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)),
    )
}

/** 端末1台だけの校正。スピーカーと端末の距離を測る図。 */
private fun DrawScope.drawOnePhone(palette: PaIllustrationPalette) {
    val floorY = 80f

    drawLine(
        color = palette.line,
        start = Offset(8f, floorY),
        end = Offset(152f, floorY),
        strokeWidth = 1.5f * palette.strokeScale,
    )

    // スピーカー（床置き）
    val box = Rect(14f, 38f, 44f, floorY)
    fillRoundRect(palette.primary, box, 3f)
    strokeRoundRect(palette.line, box, 3f, palette.strokeScale)
    drawCircle(palette.muted, radius = 8f, center = Offset(29f, 62f))
    drawCircle(
        color = palette.line,
        radius = 8f,
        center = Offset(29f, 62f),
        style = Stroke(1.2f * palette.strokeScale),
    )
    drawCircle(palette.secondary, radius = 3.5f, center = Offset(29f, 46f))
    drawCircle(
        color = palette.line,
        radius = 3.5f,
        center = Offset(29f, 46f),
        style = Stroke(1.2f * palette.strokeScale),
    )

    soundArcs(palette, origin = Offset(46f, 56f), count = 3, start = 13f, step = 10f)

    // 端末。マイクのある下端をスピーカーへ向けている
    phone(palette, left = 112f, top = 28f, width = 30f, height = 52f)
    drawCircle(palette.accent, radius = 2.6f, center = Offset(127f, 76f))

    // 距離の寸法線。「毎回同じ距離に戻せること」がこの手順の肝なので、絵でもそこを描く
    val y = 92f
    drawLine(
        color = palette.line,
        start = Offset(29f, y),
        end = Offset(127f, y),
        strokeWidth = 1.2f * palette.strokeScale,
    )
    listOf(29f, 127f).forEach { x ->
        drawLine(
            color = palette.line,
            start = Offset(x, y - 4f),
            end = Offset(x, y + 4f),
            strokeWidth = 1.2f * palette.strokeScale,
        )
    }
}

/** 端末の外形。画面の中は文字を描かず、桁の塊として置く。 */
private fun DrawScope.phone(
    palette: PaIllustrationPalette,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
) {
    val body = Rect(left, top, left + width, top + height)
    fillRoundRect(palette.ground, body, 4f)
    strokeRoundRect(palette.line, body, 4f, palette.strokeScale)

    val screen = Rect(left + 4f, top + 6f, left + width - 4f, top + height - 8f)
    fillRoundRect(palette.muted, screen, 2f)

    fillRoundRect(
        color = palette.accent,
        rect = Rect(screen.left + 3f, screen.top + 6f, screen.right - 3f, screen.top + 16f),
        corner = 1.5f,
    )
    repeat(3) { index ->
        val y = screen.top + 22f + index * 6f
        if (y + 3f < screen.bottom) {
            fillRoundRect(
                color = palette.secondary,
                rect = Rect(screen.left + 3f, y, screen.right - 6f, y + 3f),
                corner = 1f,
            )
        }
    }
}

/**
 * 鍵が開いている図。
 *
 * **閉じた鍵を描かない。** 「制限がある」ことではなく「制限が無い」ことを言う絵なので、
 * つるを外して持ち上げた形にして、掛け金が本体から離れているのが見えるようにする。
 */
private fun DrawScope.drawOpenAccess(palette: PaIllustrationPalette) {
    // つる。左上に開いた状態。本体の左肩にだけ刺さっていて、右肩からは離れている
    val shackle = Path().apply {
        moveTo(62f, 48f)
        lineTo(62f, 34f)
        cubicTo(62f, 16f, 92f, 16f, 92f, 34f)
        lineTo(92f, 40f)
    }
    drawPath(
        shackle,
        color = palette.line,
        style = Stroke(width = 5f * palette.strokeScale),
    )

    // 本体
    val body = Rect(56f, 48f, 116f, 88f)
    fillRoundRect(palette.accent, body, 6f)
    strokeRoundRect(palette.line, body, 6f, palette.strokeScale)

    // 鍵穴。中心を少し上に置くと錠前に見える
    drawCircle(palette.ground, radius = 5f, center = Offset(86f, 63f))
    val slot = Rect(83.5f, 63f, 88.5f, 76f)
    fillRoundRect(palette.ground, slot, 2f)

    // 開いていることの強調。つるの外れた側に短い線を3本
    listOf(0f, 7f, 14f).forEach { dy ->
        drawLine(
            color = palette.muted,
            start = Offset(104f, 22f + dy),
            end = Offset(118f, 22f + dy),
            strokeWidth = 2f * palette.strokeScale,
        )
    }
}

/** 検索して何も出なかったとき。空の紙と虫めがね。 */
private fun DrawScope.drawSearchEmpty(palette: PaIllustrationPalette) {
    val paper = Rect(30f, 12f, 118f, 88f)
    fillRoundRect(palette.ground, paper, 4f)
    strokeRoundRect(palette.line, paper, 4f, palette.strokeScale)

    // 行。空振りの絵なので薄い1色だけにする（色を足すと「何かある」ように見える）
    repeat(4) { index ->
        val y = 26f + index * 13f
        fillRoundRect(palette.muted, Rect(40f, y, 96f, y + 5f), 1.5f)
    }

    // 虫めがね。紙の右下に重ねて、紙より手前にあることを示す
    val center = Offset(108f, 62f)
    drawCircle(palette.ground, radius = 20f, center = center)
    drawCircle(
        color = palette.accent,
        radius = 20f,
        center = center,
        style = Stroke(width = 3.2f * palette.strokeScale),
    )
    drawLine(
        color = palette.accent,
        start = Offset(122f, 76f),
        end = Offset(138f, 92f),
        strokeWidth = 4.5f * palette.strokeScale,
    )
}

/** 音の広がり。原点から右向きの弧を [count] 本。 */
private fun DrawScope.soundArcs(
    palette: PaIllustrationPalette,
    origin: Offset,
    count: Int,
    start: Float,
    step: Float,
) {
    repeat(count) { index ->
        val radius = start + index * step
        drawArc(
            color = palette.accent.copy(alpha = 0.55f - index * 0.13f),
            startAngle = -50f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = Offset(origin.x - radius, origin.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = 1.6f * palette.strokeScale),
        )
    }
}

private fun DrawScope.fillRoundRect(color: Color, rect: Rect, corner: Float) {
    drawRoundRect(
        color = color,
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.width, rect.height),
        cornerRadius = CornerRadius(corner, corner),
    )
}

private fun DrawScope.strokeRoundRect(
    color: Color,
    rect: Rect,
    corner: Float,
    strokeScale: Float,
) {
    drawRoundRect(
        color = color,
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.width, rect.height),
        cornerRadius = CornerRadius(corner, corner),
        style = Stroke(width = 1.4f * strokeScale),
    )
}
