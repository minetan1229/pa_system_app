package com.patoolbox.feature.business

import com.patoolbox.core.calc.InvoiceCalculator
import com.patoolbox.core.calc.InvoiceTotals
import com.patoolbox.core.calc.TaxMode
import com.patoolbox.core.calc.TaxRounding
import com.patoolbox.core.export.PdfColumn
import com.patoolbox.core.export.PdfTable
import com.patoolbox.core.export.PdfTableWriter
import com.patoolbox.core.model.Invoice
import com.patoolbox.core.ui.DateTimeText
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 請求書・見積書の PDF。
 *
 * 適格請求書として必要な項目（発行者名と登録番号、取引年月日、取引内容、
 * **税率ごとに区分した合計額と消費税額**、交付を受ける者の氏名）が
 * 出るようにしている。税額の内訳が無いと適格請求書として認められない。
 *
 * ただし要件を満たしているかの最終確認は利用者の責任。
 * 制度は改定されるし、事業の形態によって必要な記載が増えることもある。
 */
@Singleton
class InvoiceDocumentWriter @Inject constructor(
    private val writer: PdfTableWriter,
) {

    fun write(invoice: Invoice, totals: InvoiceTotals, output: OutputStream) {
        val rows = invoice.lines.mapIndexed { index, line ->
            listOf(
                line.description,
                formatQuantity(line.quantity) + line.unit,
                formatYen(line.unitPrice),
                formatYen(totals.lineAmounts.getOrElse(index) { 0 }),
                taxLabel(line.taxRateName),
            )
        }

        writer.write(
            table = PdfTable(
                title = "${invoice.documentLabel}  ${invoice.number}".trim(),
                subtitle = buildSubtitle(invoice),
                columns = listOf(
                    PdfColumn("品目", weight = 3f),
                    PdfColumn("数量", weight = 1f, alignEnd = true),
                    PdfColumn("単価", weight = 1.2f, alignEnd = true),
                    PdfColumn("金額", weight = 1.4f, alignEnd = true),
                    PdfColumn("税率", weight = 0.8f, alignEnd = true),
                ),
                rows = rows + summaryRows(invoice, totals),
                footer = buildFooter(invoice),
            ),
            output = output,
        )
    }

    /**
     * 合計と税率ごとの内訳を、明細の続きの行として出す。
     * 適格請求書では **税率ごとの区分と、それぞれの消費税額** の記載が必要。
     */
    private fun summaryRows(invoice: Invoice, totals: InvoiceTotals): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        rows += listOf("", "", "", "", "")

        for (breakdown in totals.breakdowns) {
            rows += listOf(
                "${breakdown.rate.label} 対象",
                "",
                "",
                formatYen(breakdown.netAmount),
                "",
            )
            rows += listOf(
                "　消費税（${breakdown.rate.label}）",
                "",
                "",
                formatYen(breakdown.taxAmount),
                "",
            )
        }

        rows += listOf("小計（税抜）", "", "", formatYen(totals.netTotal), "")
        rows += listOf("消費税", "", "", formatYen(totals.taxTotal), "")
        rows += listOf("合計（税込）", "", "", formatYen(totals.grossTotal), "")

        val mode = runCatching { TaxMode.valueOf(invoice.taxModeName) }
            .getOrDefault(TaxMode.EXCLUSIVE)
        val rounding = runCatching { TaxRounding.valueOf(invoice.taxRoundingName) }
            .getOrDefault(TaxRounding.DOWN)
        rows += listOf("", "", "", "", "")
        rows += listOf("単価は${mode.label} / 端数は${rounding.label}", "", "", "", "")

        return rows
    }

    private fun buildSubtitle(invoice: Invoice): String = buildList {
        if (invoice.clientName.isNotBlank()) add("${invoice.clientName} 御中")
        if (invoice.subject.isNotBlank()) add("件名: ${invoice.subject}")
        add("発行日: ${DateTimeText.formatDate(invoice.issueDateEpochMs)}")
    }.joinToString("　")

    private fun buildFooter(invoice: Invoice): String = buildList {
        if (invoice.issuerName.isNotBlank()) add(invoice.issuerName)
        if (invoice.registrationNumber.isNotBlank()) {
            val suffix = if (InvoiceCalculator.isRegistrationNumberShaped(invoice.registrationNumber)) {
                ""
            } else {
                "（形式を確認してください）"
            }
            add("登録番号: ${invoice.registrationNumber}$suffix")
        }
        if (invoice.note.isNotBlank()) add(invoice.note)
    }.joinToString("　")

    private fun taxLabel(name: String): String = when (name) {
        "REDUCED" -> "8%"
        else -> "10%"
    }

    private fun formatQuantity(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)

    private fun formatYen(value: Long): String = "%,d".format(value)
}
