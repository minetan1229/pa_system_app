package com.patoolbox.core.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.patoolbox.core.model.StagePlot
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ステージプロットを A4 横の PDF に書き出す。
 *
 * 横向きにしているのは、ステージが横長だから。縦にすると図が小さくなって
 * 記号の名前が読めなくなる。
 *
 * 白地に黒で描く。現場で配る図は白黒でコピーされることが多く、
 * 色に意味を持たせると1回コピーした時点で情報が落ちる。
 */
@Singleton
class StagePlotPdfWriter @Inject constructor() {

    /**
     * @param output 呼び出し側が閉じる
     */
    fun write(plot: StagePlot, output: OutputStream, subtitle: String = "") {
        val document = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo
                .Builder(PAGE_WIDTH, PAGE_HEIGHT, 1)
                .create()
            val page = document.startPage(pageInfo)
            drawPage(page.canvas, plot, subtitle)
            document.finishPage(page)
            document.writeTo(output)
        } finally {
            document.close()
        }
    }

    private fun drawPage(canvas: Canvas, plot: StagePlot, subtitle: String) {
        var y = MARGIN.toFloat()

        canvas.drawText(plot.name, MARGIN.toFloat(), y + TITLE_SIZE, titlePaint)
        y += TITLE_SIZE + 6f

        val dimensions = "ステージ %.1fm × %.1fm".format(
            plot.stageWidthMeters,
            plot.stageDepthMeters,
        )
        val heading = if (subtitle.isBlank()) dimensions else "$subtitle ／ $dimensions"
        canvas.drawText(heading, MARGIN.toFloat(), y + BODY_SIZE, subtitlePaint)
        y += BODY_SIZE + 12f

        // 備考の行数ぶんを先に確保してから図を配置する。
        // 図を先に大きく取ると、備考が長い現場で下にはみ出す
        val noteLines = wrapNotes(plot.notes)
        val notesHeight = if (noteLines.isEmpty()) {
            0f
        } else {
            noteLines.size * (BODY_SIZE + 3f) + 10f
        }

        val bounds = RectF(
            MARGIN.toFloat(),
            y + CAPTION_ROOM,
            (PAGE_WIDTH - MARGIN).toFloat(),
            PAGE_HEIGHT - MARGIN - notesHeight - CAPTION_ROOM,
        )
        StagePlotRenderer().draw(canvas, plot, bounds, PRINT_COLORS)

        if (noteLines.isNotEmpty()) {
            var noteY = PAGE_HEIGHT - MARGIN - notesHeight + BODY_SIZE
            for (line in noteLines) {
                canvas.drawText(line, MARGIN.toFloat(), noteY, bodyPaint)
                noteY += BODY_SIZE + 3f
            }
        }
    }

    /** 備考を行に割る。用紙幅に収まらない行はそこで折る。 */
    private fun wrapNotes(notes: String): List<String> {
        if (notes.isBlank()) return emptyList()
        val available = (PAGE_WIDTH - MARGIN * 2).toFloat()
        val lines = mutableListOf<String>()

        for (paragraph in notes.split('\n')) {
            if (paragraph.isEmpty()) continue
            var current = StringBuilder()
            for (char in paragraph) {
                current.append(char)
                if (bodyPaint.measureText(current.toString()) > available) {
                    // はみ出した1文字を次の行へ送る
                    val overflow = current.last()
                    current.deleteCharAt(current.length - 1)
                    lines += current.toString()
                    current = StringBuilder().append(overflow)
                }
            }
            if (current.isNotEmpty()) lines += current.toString()
            if (lines.size >= MAX_NOTE_LINES) break
        }
        return lines.take(MAX_NOTE_LINES)
    }

    private val titlePaint = Paint().apply {
        color = Color.BLACK
        textSize = TITLE_SIZE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    private val subtitlePaint = Paint().apply {
        color = Color.DKGRAY
        textSize = BODY_SIZE
        isAntiAlias = true
    }

    private val bodyPaint = Paint().apply {
        color = Color.BLACK
        textSize = BODY_SIZE
        isAntiAlias = true
    }

    private companion object {
        /** A4 横（72dpi） */
        const val PAGE_WIDTH = 842
        const val PAGE_HEIGHT = 595
        const val MARGIN = 36

        const val TITLE_SIZE = 16f
        const val BODY_SIZE = 9f

        /** 図の上下に出す「客席側」「上手/下手」のぶん */
        const val CAPTION_ROOM = 18f
        const val MAX_NOTE_LINES = 6

        // 印刷用は白地に映える薄い色調にしてある。フチと文字を黒で統一しているのは、
        // モノクロ印刷でも記号どうしが判別できるようにするため
        // （淡色だけで区別する作りだと白黒印刷で全部同じ灰色に潰れる）
        private val PRINT_ITEM_PALETTE = listOf(
            Color.rgb(0xDC, 0xE6, 0xF2), // 青
            Color.rgb(0xDE, 0xEC, 0xE2), // 緑
            Color.rgb(0xF5, 0xE6, 0xCE), // 橙
            Color.rgb(0xF3, 0xDC, 0xE4), // 桃
            Color.rgb(0xE6, 0xDF, 0xF0), // 紫
            Color.rgb(0xDA, 0xEB, 0xEE), // 水
        )

        val PRINT_COLORS = StagePlotRenderer.Colors(
            stageOutline = Color.BLACK,
            stageFill = Color.WHITE,
            itemPalette = PRINT_ITEM_PALETTE,
            // 淡色の上なので、どの色も黒文字で足りる
            itemTextPalette = List(PRINT_ITEM_PALETTE.size) { Color.BLACK },
            itemOutline = Color.BLACK,
            label = Color.DKGRAY,
            selectedOutline = Color.BLACK,
        )
    }
}
