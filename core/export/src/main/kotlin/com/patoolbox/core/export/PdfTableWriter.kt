package com.patoolbox.core.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 表を A4 の PDF に書き出す。
 *
 * Android 標準の [PdfDocument] だけで完結させている。日本語は端末のシステムフォントで
 * 描画されるので、フォントを同梱する必要がない（＝商用配布でのフォントライセンス確認が不要）。
 */
@Singleton
class PdfTableWriter @Inject constructor() {

    /**
     * @param output 呼び出し側が閉じる
     */
    fun write(table: PdfTable, output: OutputStream) {
        val document = PdfDocument()
        try {
            val pages = PdfPaginator.paginate(table.rows.size, ROWS_PER_PAGE)
            pages.forEachIndexed { index, range ->
                writePage(
                    document = document,
                    table = table,
                    rowRange = range,
                    pageNumber = index + 1,
                    pageCount = pages.size,
                )
            }
            document.writeTo(output)
        } finally {
            document.close()
        }
    }

    private fun writePage(
        document: PdfDocument,
        table: PdfTable,
        rowRange: IntRange,
        pageNumber: Int,
        pageCount: Int,
    ) {
        val pageInfo = PdfDocument.PageInfo
            .Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber)
            .create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val contentWidth = (PAGE_WIDTH - MARGIN * 2).toFloat()
        val widths = PdfPaginator.columnWidths(table.columns, contentWidth)

        var y = MARGIN.toFloat()

        // 見出しは全ページに出す。2ページ目以降だけ見て何の表か分からないのは困る
        canvas.drawText(table.title, MARGIN.toFloat(), y + TITLE_SIZE, titlePaint)
        y += TITLE_SIZE + 6f

        if (table.subtitle.isNotBlank()) {
            canvas.drawText(table.subtitle, MARGIN.toFloat(), y + BODY_SIZE, subtitlePaint)
            y += BODY_SIZE + 4f
        }

        y += 8f
        y = drawHeaderRow(canvas, table.columns, widths, y)

        for (index in rowRange) {
            y = drawRow(canvas, table.columns, widths, table.rows[index], y)
        }

        drawFooter(canvas, table.footer, pageNumber, pageCount)

        document.finishPage(page)
    }

    private fun drawHeaderRow(
        canvas: Canvas,
        columns: List<PdfColumn>,
        widths: FloatArray,
        top: Float,
    ): Float {
        var x = MARGIN.toFloat()
        columns.forEachIndexed { index, column ->
            drawCellText(canvas, column.header, x, top, widths[index], column.alignEnd, headerPaint)
            x += widths[index]
        }
        val bottom = top + ROW_HEIGHT
        canvas.drawLine(
            MARGIN.toFloat(),
            bottom - 3f,
            (PAGE_WIDTH - MARGIN).toFloat(),
            bottom - 3f,
            rulePaint,
        )
        return bottom
    }

    private fun drawRow(
        canvas: Canvas,
        columns: List<PdfColumn>,
        widths: FloatArray,
        values: List<String>,
        top: Float,
    ): Float {
        var x = MARGIN.toFloat()
        columns.forEachIndexed { index, column ->
            val text = values.getOrElse(index) { "" }
            drawCellText(canvas, text, x, top, widths[index], column.alignEnd, bodyPaint)
            x += widths[index]
        }
        val bottom = top + ROW_HEIGHT
        canvas.drawLine(
            MARGIN.toFloat(),
            bottom - 3f,
            (PAGE_WIDTH - MARGIN).toFloat(),
            bottom - 3f,
            faintRulePaint,
        )
        return bottom
    }

    /** セル幅に収まらない文字は末尾を省略する。 */
    private fun drawCellText(
        canvas: Canvas,
        text: String,
        left: Float,
        top: Float,
        width: Float,
        alignEnd: Boolean,
        paint: Paint,
    ) {
        val available = width - CELL_PADDING * 2
        val clipped = if (paint.measureText(text) <= available) {
            text
        } else {
            var cut = text
            while (cut.isNotEmpty() && paint.measureText("$cut…") > available) {
                cut = cut.dropLast(1)
            }
            "$cut…"
        }

        val x = if (alignEnd) {
            left + width - CELL_PADDING - paint.measureText(clipped)
        } else {
            left + CELL_PADDING
        }
        canvas.drawText(clipped, x, top + ROW_HEIGHT - 6f, paint)
    }

    private fun drawFooter(canvas: Canvas, footer: String, pageNumber: Int, pageCount: Int) {
        val y = (PAGE_HEIGHT - MARGIN / 2).toFloat()
        if (footer.isNotBlank()) {
            canvas.drawText(footer, MARGIN.toFloat(), y, footerPaint)
        }
        val pageLabel = "$pageNumber / $pageCount"
        canvas.drawText(
            pageLabel,
            PAGE_WIDTH - MARGIN - footerPaint.measureText(pageLabel),
            y,
            footerPaint,
        )
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

    private val headerPaint = Paint().apply {
        color = Color.BLACK
        textSize = BODY_SIZE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    private val bodyPaint = Paint().apply {
        color = Color.BLACK
        textSize = BODY_SIZE
        isAntiAlias = true
    }

    private val footerPaint = Paint().apply {
        color = Color.GRAY
        textSize = FOOTER_SIZE
        isAntiAlias = true
    }

    private val rulePaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 1f
    }

    private val faintRulePaint = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 0.5f
    }

    private companion object {
        // A4 を 72dpi のポイントで表した大きさ
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN = 36

        const val TITLE_SIZE = 18f
        const val BODY_SIZE = 10f
        const val FOOTER_SIZE = 8f
        const val ROW_HEIGHT = 18f
        const val CELL_PADDING = 4f

        /** 見出しとフッタを除いて1ページに入る行数 */
        const val ROWS_PER_PAGE = 36
    }
}
