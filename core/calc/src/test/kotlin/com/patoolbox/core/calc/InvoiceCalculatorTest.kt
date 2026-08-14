package com.patoolbox.core.calc

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class InvoiceCalculatorTest {

    private fun line(price: Long, quantity: Double = 1.0, rate: TaxRate = TaxRate.STANDARD) =
        InvoiceLine(description = "項目", quantity = quantity, unitPrice = price, taxRate = rate)

    @Test
    fun `税抜の合計に消費税が乗る`() {
        val totals = InvoiceCalculator.calculate(listOf(line(50_000), line(30_000)))

        assertThat(totals.netTotal).isEqualTo(80_000)
        assertThat(totals.taxTotal).isEqualTo(8_000)
        assertThat(totals.grossTotal).isEqualTo(88_000)
    }

    @Test
    fun `端数処理は税率ごとに1回だけ行う`() {
        // インボイス制度の中核の規定。明細ごとに切り捨てると
        // 10円 × 10 = 100円 になるが、正しくは 1080円 × 10% = 108円。
        // 8円ずれた請求書は「合計が合わない請求書」になる
        val lines = List(10) { line(108) }

        val totals = InvoiceCalculator.calculate(lines, rounding = TaxRounding.DOWN)

        assertThat(totals.netTotal).isEqualTo(1_080)
        assertThat(totals.taxTotal).isEqualTo(108)
    }

    @Test
    fun `税率が混ざると内訳が分かれる`() {
        val totals = InvoiceCalculator.calculate(
            listOf(
                line(10_000, rate = TaxRate.STANDARD),
                line(5_000, rate = TaxRate.REDUCED),
            ),
        )

        assertThat(totals.breakdowns).hasSize(2)
        val standard = totals.breakdowns.first { it.rate == TaxRate.STANDARD }
        val reduced = totals.breakdowns.first { it.rate == TaxRate.REDUCED }
        assertThat(standard.taxAmount).isEqualTo(1_000)
        assertThat(reduced.taxAmount).isEqualTo(400)
        assertThat(totals.grossTotal).isEqualTo(16_400)
    }

    @Test
    fun `端数処理の方法で税額が変わる`() {
        val lines = listOf(line(1_055))

        val down = InvoiceCalculator.calculate(lines, rounding = TaxRounding.DOWN)
        val round = InvoiceCalculator.calculate(lines, rounding = TaxRounding.ROUND)
        val up = InvoiceCalculator.calculate(lines, rounding = TaxRounding.UP)

        // 1055 × 10% = 105.5
        assertThat(down.taxTotal).isEqualTo(105)
        assertThat(round.taxTotal).isEqualTo(106)
        assertThat(up.taxTotal).isEqualTo(106)
    }

    @Test
    fun `税込入力では税抜と税額の和が入力額に一致する`() {
        // 割り戻しで端数が出ても、合計が入力した税込額とずれてはいけない。
        // 「請求書の金額と振込額が1円違う」は実際に問い合わせになる
        val lines = listOf(line(9_999), line(1))

        val totals = InvoiceCalculator.calculate(lines, mode = TaxMode.INCLUSIVE)

        assertThat(totals.grossTotal).isEqualTo(10_000)
    }

    @Test
    fun `数量が小数でも計算できる`() {
        // 1.5日ぶんの日当など
        val totals = InvoiceCalculator.calculate(listOf(line(30_000, quantity = 1.5)))

        assertThat(totals.lineAmounts).containsExactly(45_000L)
        assertThat(totals.taxTotal).isEqualTo(4_500)
    }

    @Test
    fun `明細が空でも0を返す`() {
        val totals = InvoiceCalculator.calculate(emptyList())

        assertThat(totals.grossTotal).isEqualTo(0)
        assertThat(totals.breakdowns).isEmpty()
    }

    @Test
    fun `値引きの負の明細も扱える`() {
        val totals = InvoiceCalculator.calculate(
            listOf(line(100_000), line(-10_000)),
        )

        assertThat(totals.netTotal).isEqualTo(90_000)
        assertThat(totals.taxTotal).isEqualTo(9_000)
    }

    @Test
    fun `登録番号の形を検査する`() {
        assertThat(InvoiceCalculator.isRegistrationNumberShaped("T1234567890123")).isTrue()
        // 桁数が足りない・T が無い・数字でない
        assertThat(InvoiceCalculator.isRegistrationNumberShaped("T123")).isFalse()
        assertThat(InvoiceCalculator.isRegistrationNumberShaped("1234567890123")).isFalse()
        assertThat(InvoiceCalculator.isRegistrationNumberShaped("TABCDEFGHIJKLM")).isFalse()
    }

    @Test
    fun `内訳の税抜と税額を足すと税込になる`() {
        val totals = InvoiceCalculator.calculate(
            listOf(line(12_345), line(6_789, rate = TaxRate.REDUCED)),
        )

        for (breakdown in totals.breakdowns) {
            assertThat(breakdown.grossAmount)
                .isEqualTo(breakdown.netAmount + breakdown.taxAmount)
        }
        assertThat(totals.grossTotal).isEqualTo(totals.breakdowns.sumOf { it.grossAmount })
    }
}
