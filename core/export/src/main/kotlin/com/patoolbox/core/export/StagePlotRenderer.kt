package com.patoolbox.core.export

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.patoolbox.core.model.StageItem
import com.patoolbox.core.model.StageItemColor
import com.patoolbox.core.model.StagePlot
import com.patoolbox.core.model.StageShape

/**
 * ステージプロットの当たり判定用の座標計算。
 *
 * 描画（[StagePlotRenderer]）と画面のドラッグ処理の両方から使う。
 * 別々に計算すると、見えている位置と掴める位置がずれる。
 */
object StageGeometry {

    /** ステージの縦横比を保ったまま [bounds] に収めた矩形。 */
    fun fitStage(plot: StagePlot, bounds: RectF): RectF {
        val aspect = plot.aspectRatio.coerceAtLeast(MIN_ASPECT)
        var width = bounds.width()
        var height = width / aspect
        if (height > bounds.height()) {
            height = bounds.height()
            width = height * aspect
        }
        val left = bounds.left + (bounds.width() - width) / 2f
        val top = bounds.top + (bounds.height() - height) / 2f
        return RectF(left, top, left + width, top + height)
    }

    /** 記号が占める矩形。 */
    fun itemBounds(item: StageItem, stage: RectF): RectF {
        val width = stage.width() * item.symbol.widthRatio
        val height = stage.height() * item.symbol.heightRatio
        val centerX = stage.left + stage.width() * item.x
        val centerY = stage.top + stage.height() * item.y
        return RectF(
            centerX - width / 2f,
            centerY - height / 2f,
            centerX + width / 2f,
            centerY + height / 2f,
        )
    }

    /**
     * 座標にある記号を探す。手前に描いたもの（＝リストの後ろ）を優先する。
     * 重なっている場合、上に見えている方を掴めないと操作できない。
     *
     * @param minHitSize 記号の見た目のサイズに関わらず、最低これだけの正方形を
     *   当たり判定にする（px）。ステージ全体に対して小さく描かれる記号（[com.patoolbox.core.model.StageSymbol.widthRatio]
     *   が小さいもの）は、見た目どおりの矩形だけだと指では拾えないサイズになる
     */
    fun itemAt(plot: StagePlot, stage: RectF, x: Float, y: Float, minHitSize: Float = 0f): StageItem? =
        plot.items.lastOrNull { hitBounds(it, stage, minHitSize).contains(x, y) }

    private fun hitBounds(item: StageItem, stage: RectF, minHitSize: Float): RectF {
        val bounds = itemBounds(item, stage)
        if (minHitSize <= 0f) return bounds
        val width = bounds.width().coerceAtLeast(minHitSize)
        val height = bounds.height().coerceAtLeast(minHitSize)
        val centerX = bounds.centerX()
        val centerY = bounds.centerY()
        return RectF(
            centerX - width / 2f,
            centerY - height / 2f,
            centerX + width / 2f,
            centerY + height / 2f,
        )
    }

    private const val MIN_ASPECT = 0.1f
}

/**
 * ステージプロットの描画。**画面と PDF で同じコードを使う。**
 *
 * 別々に書くと必ずずれる。「画面で確認した図」と「渡した図」が違うのは
 * 現場では致命的なので、Compose 側も DrawScope から native canvas を借りてここを呼ぶ。
 *
 * 色は呼び出し側から渡す。PDF は白地に黒、画面はテーマ色に従う必要がある。
 *
 * Paint を持ち回すのでインスタンスを共有しないこと。画面（UIスレッド）と
 * PDF書き出し（IOスレッド）が同時に動く場面があり、共有すると描画が壊れる。
 */
class StagePlotRenderer {

    /**
     * @param itemPalette 記号の地色。[StageItem.colorIndex] で引く
     *   （[StageItemColor.COUNT] 個ぶん必要）
     * @param itemTextPalette [itemPalette] それぞれに乗せる文字色。
     *   固定1色にしないのは、色によって白文字と黒文字のどちらが読めるかが変わるため
     *   （呼び出し側で `contrastingInk` を通した値を渡す）
     */
    data class Colors(
        val stageOutline: Int,
        val stageFill: Int,
        val itemPalette: List<Int>,
        val itemTextPalette: List<Int>,
        val itemOutline: Int,
        val label: Int,
        val selectedOutline: Int,
    ) {
        init {
            require(itemPalette.size == StageItemColor.COUNT) {
                "itemPalette は ${StageItemColor.COUNT} 色ぶん必要"
            }
            require(itemTextPalette.size == StageItemColor.COUNT) {
                "itemTextPalette は ${StageItemColor.COUNT} 色ぶん必要"
            }
        }
    }

    private val paint = Paint().apply { isAntiAlias = true }
    private val textPaint = Paint().apply { isAntiAlias = true }
    private val path = Path()

    /**
     * [bounds] にステージの枠を収めて描く。
     *
     * @param selectedItemId 選択中の記号。PDF では null
     */
    fun draw(
        canvas: Canvas,
        plot: StagePlot,
        bounds: RectF,
        colors: Colors,
        selectedItemId: Long? = null,
    ) {
        val stage = StageGeometry.fitStage(plot, bounds)
        val outlineWidth = (stage.height() * OUTLINE_RATIO).coerceAtLeast(1f)

        paint.style = Paint.Style.FILL
        paint.color = colors.stageFill
        canvas.drawRect(stage, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = outlineWidth
        paint.color = colors.stageOutline
        canvas.drawRect(stage, paint)

        drawOrientation(canvas, stage, colors)

        for (item in plot.items) {
            drawItem(
                canvas = canvas,
                item = item,
                stage = stage,
                colors = colors,
                outlineWidth = outlineWidth,
                selected = item.id == selectedItemId,
            )
        }
    }

    /**
     * 客席がどちら側かと、上手・下手を図に書く。
     *
     * これが無い配置図は左右が読めない。渡された側が figure を回して見ることになり、
     * 実際に取り違いが起きる。
     */
    private fun drawOrientation(canvas: Canvas, stage: RectF, colors: Colors) {
        val fontSize = stage.height() * CAPTION_FONT_RATIO
        textPaint.textSize = fontSize
        textPaint.color = colors.label
        textPaint.typeface = Typeface.DEFAULT

        drawCentered(canvas, AUDIENCE_LABEL, stage.centerX(), stage.bottom + fontSize * 1.4f)
        drawCentered(canvas, WING_LABEL, stage.centerX(), stage.top - fontSize * 0.6f)
    }

    private fun drawItem(
        canvas: Canvas,
        item: StageItem,
        stage: RectF,
        colors: Colors,
        outlineWidth: Float,
        selected: Boolean,
    ) {
        val rect = StageGeometry.itemBounds(item, stage)

        val colorIndex = StageItemColor.coerce(item.colorIndex)

        paint.style = Paint.Style.FILL
        paint.color = colors.itemPalette[colorIndex]
        drawShape(canvas, item, rect)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = outlineWidth * if (selected) SELECTED_STROKE_SCALE else 1f
        paint.color = if (selected) colors.selectedOutline else colors.itemOutline
        drawShape(canvas, item, rect)

        // バッジは記号の中、名前はその下。図が混んでも名前だけは読めるようにする
        val badgeSize = (rect.height() * BADGE_FONT_RATIO).coerceAtMost(rect.width() * 0.4f)
        textPaint.textSize = badgeSize
        textPaint.color = colors.itemTextPalette[colorIndex]
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        drawCentered(canvas, item.symbol.badge, rect.centerX(), rect.centerY() + badgeSize * 0.35f)

        val labelSize = stage.height() * LABEL_FONT_RATIO
        textPaint.textSize = labelSize
        textPaint.color = colors.label
        textPaint.typeface = Typeface.DEFAULT
        drawCentered(canvas, item.displayLabel, rect.centerX(), rect.bottom + labelSize)
    }

    private fun drawShape(canvas: Canvas, item: StageItem, rect: RectF) {
        when (item.symbol.shape) {
            StageShape.RECT -> canvas.drawRect(rect, paint)
            StageShape.CIRCLE -> canvas.drawOval(rect, paint)
            StageShape.WEDGE -> {
                // 客席側（下）が広い台形。モニターがどちらを向いているかが図で分かる
                path.reset()
                val inset = rect.width() * WEDGE_INSET_RATIO
                path.moveTo(rect.left + inset, rect.top)
                path.lineTo(rect.right - inset, rect.top)
                path.lineTo(rect.right, rect.bottom)
                path.lineTo(rect.left, rect.bottom)
                path.close()
                canvas.drawPath(path, paint)
            }
        }
    }

    private fun drawCentered(canvas: Canvas, text: String, centerX: Float, baselineY: Float) {
        canvas.drawText(text, centerX - textPaint.measureText(text) / 2f, baselineY, textPaint)
    }

    private companion object {
        const val AUDIENCE_LABEL = "客席側"
        const val WING_LABEL = "上手袖 ←        → 下手袖"

        const val OUTLINE_RATIO = 0.004f
        const val SELECTED_STROKE_SCALE = 3f
        const val BADGE_FONT_RATIO = 0.45f
        const val LABEL_FONT_RATIO = 0.035f
        const val CAPTION_FONT_RATIO = 0.045f
        const val WEDGE_INSET_RATIO = 0.22f
    }
}
