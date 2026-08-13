package com.patoolbox.core.export

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CsvWriterTest {

    @Test
    fun `普通の値は引用しない`() {
        assertThat(CsvWriter.escape("100.5")).isEqualTo("100.5")
        assertThat(CsvWriter.escape("LAeq")).isEqualTo("LAeq")
    }

    @Test
    fun `カンマを含む値は引用する`() {
        // 引用しないと列がずれる
        assertThat(CsvWriter.escape("1曲目, MC")).isEqualTo("\"1曲目, MC\"")
    }

    @Test
    fun `引用符は2つ重ねる`() {
        // RFC 4180: 値の中の " は "" にして全体を " で囲む
        val escaped = CsvWriter.escape("say \"hi\"")

        assertThat(escaped).isEqualTo("\"say \"\"hi\"\"\"")
    }

    @Test
    fun `改行を含む値は引用する`() {
        assertThat(CsvWriter.escape("1行目\n2行目")).startsWith("\"")
        assertThat(CsvWriter.escape("1行目\n2行目")).endsWith("\"")
    }

    @Test
    fun `行はカンマで連結される`() {
        assertThat(CsvWriter.row(listOf("a", "b", "c"))).isEqualTo("a,b,c")
    }

    @Test
    fun `日本語が化けないよう先頭にBOMを付ける`() {
        val csv = CsvWriter.build(header = listOf("時刻"), rows = emptyList())

        assertThat(csv.first()).isEqualTo('\uFEFF')
    }

    @Test
    fun `ヘッダと本体が改行で並ぶ`() {
        val csv = CsvWriter.build(
            header = listOf("経過", "dB"),
            rows = listOf(listOf("0", "95.2"), listOf("1", "97.8")),
        )

        val lines = csv.removePrefix(CsvWriter.UTF8_BOM).split("\r\n").filter { it.isNotEmpty() }
        assertThat(lines).containsExactly("経過,dB", "0,95.2", "1,97.8").inOrder()
    }

    @Test
    fun `注記は先頭に出る`() {
        val csv = CsvWriter.build(
            header = listOf("経過"),
            rows = emptyList(),
            comments = listOf("A特性 / Fast", "未校正のため参考値"),
        )

        val lines = csv.removePrefix(CsvWriter.UTF8_BOM).split("\r\n")
        assertThat(lines[0]).isEqualTo("# A特性 / Fast")
        assertThat(lines[1]).isEqualTo("# 未校正のため参考値")
        assertThat(lines[2]).isEqualTo("経過")
    }

    @Test
    fun `改行はCRLFにする`() {
        // Excel が素直に読めるようにする
        val csv = CsvWriter.build(listOf("a"), listOf(listOf("1")))

        assertThat(csv).contains("\r\n")
    }

    @Test
    fun `空の測定でもヘッダだけ出る`() {
        val csv = CsvWriter.build(listOf("経過", "dB"), emptyList())
        val lines = csv.removePrefix(CsvWriter.UTF8_BOM).split("\r\n").filter { it.isNotEmpty() }

        assertThat(lines).containsExactly("経過,dB")
    }
}
