package com.patoolbox.core.export

/**
 * CSV の組み立て。
 *
 * Excel で開かれる前提で作る。区切りと引用の扱いを間違えると
 * 現場の資料が1列に潰れるので、ここは自前で組んでテストしておく。
 */
object CsvWriter {

    /** Excel が UTF-8 と判定できるようにする印。無いと日本語が化ける */
    const val UTF8_BOM = "\uFEFF"

    private const val SEPARATOR = ","
    private const val NEWLINE = "\r\n"

    /**
     * 1セルを CSV の書式にする。
     * 区切り・引用符・改行を含むときだけ引用し、中の引用符は2つ重ねる（RFC 4180）。
     */
    fun escape(value: String): String {
        val needsQuoting = value.contains(SEPARATOR) ||
            value.contains('"') ||
            value.contains('\n') ||
            value.contains('\r')

        if (!needsQuoting) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    fun row(values: List<String>): String = values.joinToString(SEPARATOR) { escape(it) }

    /**
     * ヘッダ行と本体から CSV 全体を作る。
     * @param comments 先頭に置く注記（測定条件や免責）。`#` を付けて出す
     */
    fun build(
        header: List<String>,
        rows: List<List<String>>,
        comments: List<String> = emptyList(),
    ): String = buildString {
        append(UTF8_BOM)
        comments.forEach { comment ->
            append(escape("# $comment"))
            append(NEWLINE)
        }
        append(row(header))
        append(NEWLINE)
        rows.forEach {
            append(row(it))
            append(NEWLINE)
        }
    }
}
