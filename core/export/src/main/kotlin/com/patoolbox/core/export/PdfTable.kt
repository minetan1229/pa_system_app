package com.patoolbox.core.export

/** PDF に出す表の定義。 */
data class PdfTable(
    val title: String,
    val subtitle: String,
    val columns: List<PdfColumn>,
    val rows: List<List<String>>,
    val footer: String = "",
)

/**
 * 列の定義。
 * @param weight 列幅の比率。合計が1でなくても内部で正規化する
 */
data class PdfColumn(
    val header: String,
    val weight: Float,
    val alignEnd: Boolean = false,
)

/**
 * 表のページ分割。
 *
 * 描画から切り離してあるので、ページ跨ぎの計算だけをテストできる。
 * 「32ch のパッチ表が2ページ目の1行目から始まってしまう」ような崩れは
 * 印刷して初めて気づくので、ロジックとして固めておきたい。
 */
object PdfPaginator {

    /**
     * @param rowCount 全行数
     * @param rowsPerPage 1ページに入る行数
     * @return 各ページの行範囲
     */
    fun paginate(rowCount: Int, rowsPerPage: Int): List<IntRange> {
        require(rowsPerPage > 0) { "1ページの行数は正の値" }
        if (rowCount <= 0) return listOf(IntRange.EMPTY)

        return (0 until rowCount step rowsPerPage).map { start ->
            start until minOf(start + rowsPerPage, rowCount)
        }
    }

    /** 列の重みを実際の幅（ポイント）に変換する。 */
    fun columnWidths(columns: List<PdfColumn>, availableWidth: Float): FloatArray {
        val total = columns.sumOf { it.weight.toDouble() }.toFloat()
        if (total <= 0f) return FloatArray(columns.size) { availableWidth / columns.size }
        return FloatArray(columns.size) { columns[it].weight / total * availableWidth }
    }
}
