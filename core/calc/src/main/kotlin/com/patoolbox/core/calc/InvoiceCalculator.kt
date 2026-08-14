package com.patoolbox.core.calc

import kotlin.math.floor
import kotlin.math.roundToLong

/** 消費税の端数処理。どれを使うかは事業者が選んでよい（法令で指定されていない）。 */
enum class TaxRounding(val label: String) {
    /** 切り捨て。もっとも一般的 */
    DOWN("切り捨て"),
    ROUND("四捨五入"),
    UP("切り上げ"),
    ;

    fun apply(value: Double): Long = when (this) {
        DOWN -> floor(value).toLong()
        ROUND -> value.roundToLong()
        UP -> kotlin.math.ceil(value).toLong()
    }
}

/** 消費税率。PA の請求では 10% がほとんどだが、軽減税率の品目が混ざることはある。 */
enum class TaxRate(val percent: Int, val label: String) {
    STANDARD(10, "10%"),
    REDUCED(8, "8%（軽減）"),
    ;

    val fraction: Double get() = percent / 100.0
}

/** 税の扱い。単価を税抜で入れるか税込で入れるか。 */
enum class TaxMode(val label: String) {
    /** 単価は税抜。合計に消費税を足す */
    EXCLUSIVE("税抜"),

    /** 単価は税込。合計から消費税を割り戻す */
    INCLUSIVE("税込"),
}

data class InvoiceLine(
    val description: String,
    val quantity: Double,
    val unit: String = "式",
    /** 単価（円）。[TaxMode] によって税抜／税込が変わる */
    val unitPrice: Long,
    val taxRate: TaxRate = TaxRate.STANDARD,
)

/** 税率ごとの内訳。適格請求書に記載が必要な単位。 */
data class TaxBreakdown(
    val rate: TaxRate,
    /** その税率の対象になる税抜合計 */
    val netAmount: Long,
    val taxAmount: Long,
) {
    val grossAmount: Long get() = netAmount + taxAmount
}

data class InvoiceTotals(
    val lineAmounts: List<Long>,
    val breakdowns: List<TaxBreakdown>,
) {
    val netTotal: Long get() = breakdowns.sumOf { it.netAmount }
    val taxTotal: Long get() = breakdowns.sumOf { it.taxAmount }
    val grossTotal: Long get() = netTotal + taxTotal
}

/**
 * 請求書の金額計算。
 *
 * ### 端数処理は「税率ごとに1回」
 *
 * 適格請求書（インボイス）制度では、1枚の請求書につき **税率ごとに1回** だけ
 * 消費税の端数処理を行う。明細ごとに端数処理して足し上げる方式は認められていない。
 *
 * この違いは実際に金額として現れる。108円の品目が10個並ぶと、
 * 明細ごとに切り捨てると 10円×10 = 100円、税率ごとにまとめると
 * 1080円×10% = 108円 で、8円ずれる。**請求額が合わない請求書になる。**
 *
 * 端数処理の方法（切上げ・切捨て・四捨五入）は事業者が選んでよい。
 * 既定は切り捨てにしてあるが、設定で変えられる。
 */
object InvoiceCalculator {

    fun calculate(
        lines: List<InvoiceLine>,
        mode: TaxMode = TaxMode.EXCLUSIVE,
        rounding: TaxRounding = TaxRounding.DOWN,
    ): InvoiceTotals {
        // 明細の金額は数量×単価。ここでの端数は消費税の端数処理とは別物なので、
        // 同じ丸め方を使うが税率ごとの集計より前に行う
        val lineAmounts = lines.map { rounding.apply(it.quantity * it.unitPrice) }

        val byRate = linkedMapOf<TaxRate, Long>()
        lines.forEachIndexed { index, line ->
            byRate[line.taxRate] = (byRate[line.taxRate] ?: 0L) + lineAmounts[index]
        }

        val breakdowns = byRate.map { (rate, amount) ->
            when (mode) {
                TaxMode.EXCLUSIVE -> TaxBreakdown(
                    rate = rate,
                    netAmount = amount,
                    // 税率ごとに1回だけ端数処理する
                    taxAmount = rounding.apply(amount * rate.fraction),
                )

                TaxMode.INCLUSIVE -> {
                    // 税込から割り戻す。税抜額を先に出し、差額を税額にすることで
                    // 税抜 + 税額 = 入力した税込額 が必ず成り立つようにする
                    val net = rounding.apply(amount / (1.0 + rate.fraction))
                    TaxBreakdown(rate = rate, netAmount = net, taxAmount = amount - net)
                }
            }
        }

        return InvoiceTotals(lineAmounts = lineAmounts, breakdowns = breakdowns)
    }

    /**
     * 適格請求書発行事業者の登録番号として形が正しいか。
     *
     * 「T」＋13桁の数字。**実在するかどうかは確認しない**（国税庁の公表サイトで
     * 確認する必要がある）。形だけ見て、打ち間違いに気づけるようにするもの。
     */
    fun isRegistrationNumberShaped(value: String): Boolean {
        val trimmed = value.trim()
        if (trimmed.length != REGISTRATION_LENGTH) return false
        if (trimmed.first() != 'T') return false
        return trimmed.drop(1).all { it.isDigit() }
    }

    private const val REGISTRATION_LENGTH = 14
}
