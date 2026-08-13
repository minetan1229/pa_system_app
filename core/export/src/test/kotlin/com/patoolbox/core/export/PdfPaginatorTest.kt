package com.patoolbox.core.export

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PdfPaginatorTest {

    @Test
    fun `1ページに収まるなら1ページ`() {
        val pages = PdfPaginator.paginate(rowCount = 16, rowsPerPage = 30)

        assertThat(pages).hasSize(1)
        assertThat(pages.first().toList()).hasSize(16)
    }

    @Test
    fun `溢れたら次のページに続く`() {
        val pages = PdfPaginator.paginate(rowCount = 48, rowsPerPage = 30)

        assertThat(pages).hasSize(2)
        assertThat(pages[0]).isEqualTo(0 until 30)
        assertThat(pages[1]).isEqualTo(30 until 48)
    }

    @Test
    fun `ちょうど割り切れるときに空ページを作らない`() {
        val pages = PdfPaginator.paginate(rowCount = 60, rowsPerPage = 30)

        assertThat(pages).hasSize(2)
        assertThat(pages.last().last).isEqualTo(59)
    }

    @Test
    fun `全ページの行を合わせると元の行数になる`() {
        val pages = PdfPaginator.paginate(rowCount = 64, rowsPerPage = 25)

        assertThat(pages.sumOf { it.count() }).isEqualTo(64)
    }

    @Test
    fun `行が無くても1ページ出す`() {
        // 空のパッチ表でも見出しだけのページは出したい
        val pages = PdfPaginator.paginate(rowCount = 0, rowsPerPage = 30)

        assertThat(pages).hasSize(1)
        assertThat(pages.first().count()).isEqualTo(0)
    }

    @Test
    fun `1ページ0行は拒否する`() {
        runCatching { PdfPaginator.paginate(10, 0) }
            .also { assertThat(it.isFailure).isTrue() }
    }

    @Test
    fun `列幅は重みの比で配分される`() {
        val columns = listOf(
            PdfColumn("CH", weight = 1f),
            PdfColumn("音源", weight = 3f),
            PdfColumn("備考", weight = 2f),
        )

        val widths = PdfPaginator.columnWidths(columns, availableWidth = 600f)

        assertThat(widths[0]).isWithin(0.01f).of(100f)
        assertThat(widths[1]).isWithin(0.01f).of(300f)
        assertThat(widths[2]).isWithin(0.01f).of(200f)
        assertThat(widths.sum()).isWithin(0.01f).of(600f)
    }

    @Test
    fun `重みが0でも均等に割る`() {
        val columns = listOf(PdfColumn("A", 0f), PdfColumn("B", 0f))

        val widths = PdfPaginator.columnWidths(columns, availableWidth = 500f)

        assertThat(widths[0]).isWithin(0.01f).of(250f)
        assertThat(widths[1]).isWithin(0.01f).of(250f)
    }
}
