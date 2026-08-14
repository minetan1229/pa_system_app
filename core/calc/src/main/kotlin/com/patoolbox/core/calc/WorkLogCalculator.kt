package com.patoolbox.core.calc

import kotlin.math.roundToLong

/** 単価の形。PA の現場は日当が多いが、リハや長丁場は時間単価になる。 */
enum class RateType(val label: String) {
    DAILY("日当"),
    HOURLY("時間単価"),
}

data class WorkEntry(
    /** 日付（epoch ミリ秒）。集計の月別グループに使う */
    val dateEpochMs: Long,
    val startMinutesOfDay: Int,
    val endMinutesOfDay: Int,
    val breakMinutes: Int = 0,
    val rateType: RateType = RateType.DAILY,
    /** 日当なら1日あたり、時間単価なら1時間あたりの金額（円） */
    val rate: Long = 0,
    /** 割増率。深夜・休日などの上乗せ。1.0 で割増なし */
    val multiplier: Double = 1.0,
    val transportFee: Long = 0,
)

data class WorkSummary(
    val workedMinutes: Int,
    val amount: Long,
) {
    val workedHours: Double get() = workedMinutes / 60.0

    val hoursLabel: String
        get() = "%d:%02d".format(workedMinutes / 60, workedMinutes % 60)
}

/**
 * 稼働時間と金額の計算。
 *
 * 日をまたぐ現場が普通にある（撤収が翌 2:00 など）ので、
 * 終了が開始より小さい場合は翌日として扱う。ここを素直に引き算すると
 * **負の稼働時間**になり、月の集計が静かに狂う。
 */
object WorkLogCalculator {

    fun summarize(entry: WorkEntry): WorkSummary {
        val minutes = workedMinutes(entry)
        return WorkSummary(workedMinutes = minutes, amount = amountOf(entry, minutes))
    }

    fun workedMinutes(entry: WorkEntry): Int {
        val span = if (entry.endMinutesOfDay >= entry.startMinutesOfDay) {
            entry.endMinutesOfDay - entry.startMinutesOfDay
        } else {
            // 日をまたいだ。撤収が翌日になる現場は珍しくない
            MINUTES_PER_DAY - entry.startMinutesOfDay + entry.endMinutesOfDay
        }
        return (span - entry.breakMinutes).coerceAtLeast(0)
    }

    private fun amountOf(entry: WorkEntry, workedMinutes: Int): Long {
        val base = when (entry.rateType) {
            RateType.DAILY -> entry.rate.toDouble()
            RateType.HOURLY -> entry.rate * (workedMinutes / 60.0)
        }
        // 交通費に割増は掛からない。実費なので
        return (base * entry.multiplier).roundToLong() + entry.transportFee
    }

    /** 複数件の合計。月次の請求や稼働時間の把握に使う。 */
    fun total(entries: List<WorkEntry>): WorkSummary {
        var minutes = 0
        var amount = 0L
        for (entry in entries) {
            val summary = summarize(entry)
            minutes += summary.workedMinutes
            amount += summary.amount
        }
        return WorkSummary(workedMinutes = minutes, amount = amount)
    }

    /** "9:30" のような表記 → 0時からの分。 */
    fun parseTimeOfDay(text: String): Int? {
        val parts = text.trim().split(':', '：')
        if (parts.size != 2) return null
        val hours = parts[0].toIntOrNull() ?: return null
        val minutes = parts[1].toIntOrNull() ?: return null
        if (hours !in 0..23 || minutes !in 0..59) return null
        return hours * 60 + minutes
    }

    fun formatTimeOfDay(minutesOfDay: Int): String {
        val normalized = ((minutesOfDay % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY
        return "%d:%02d".format(normalized / 60, normalized % 60)
    }

    private const val MINUTES_PER_DAY = 24 * 60
}
