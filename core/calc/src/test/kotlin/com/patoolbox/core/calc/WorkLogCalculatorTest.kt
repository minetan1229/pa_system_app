package com.patoolbox.core.calc

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorkLogCalculatorTest {

    private fun entry(
        start: String,
        end: String,
        breakMinutes: Int = 0,
        rateType: RateType = RateType.DAILY,
        rate: Long = 0,
        multiplier: Double = 1.0,
        transportFee: Long = 0,
    ) = WorkEntry(
        dateEpochMs = 0,
        startMinutesOfDay = WorkLogCalculator.parseTimeOfDay(start)!!,
        endMinutesOfDay = WorkLogCalculator.parseTimeOfDay(end)!!,
        breakMinutes = breakMinutes,
        rateType = rateType,
        rate = rate,
        multiplier = multiplier,
        transportFee = transportFee,
    )

    @Test
    fun `同じ日の中の稼働時間`() {
        val summary = WorkLogCalculator.summarize(entry("9:00", "18:00"))

        assertThat(summary.workedMinutes).isEqualTo(540)
        assertThat(summary.hoursLabel).isEqualTo("9:00")
    }

    @Test
    fun `日をまたぐ現場を負の時間にしない`() {
        // 撤収が翌 2:00 になる現場は普通にある。素直に引き算すると
        // 負の稼働時間になり、月の集計が静かに狂う
        val summary = WorkLogCalculator.summarize(entry("13:00", "2:00"))

        assertThat(summary.workedMinutes).isEqualTo(13 * 60)
    }

    @Test
    fun `休憩を差し引く`() {
        val summary = WorkLogCalculator.summarize(entry("9:00", "18:00", breakMinutes = 60))

        assertThat(summary.workedMinutes).isEqualTo(480)
    }

    @Test
    fun `休憩が長すぎても負にはしない`() {
        val summary = WorkLogCalculator.summarize(entry("9:00", "10:00", breakMinutes = 600))

        assertThat(summary.workedMinutes).isEqualTo(0)
    }

    @Test
    fun `日当は時間に関係なく同額`() {
        val short = WorkLogCalculator.summarize(
            entry("9:00", "12:00", rateType = RateType.DAILY, rate = 30_000),
        )
        val long = WorkLogCalculator.summarize(
            entry("9:00", "23:00", rateType = RateType.DAILY, rate = 30_000),
        )

        assertThat(short.amount).isEqualTo(30_000)
        assertThat(long.amount).isEqualTo(30_000)
    }

    @Test
    fun `時間単価は実働時間に比例する`() {
        val summary = WorkLogCalculator.summarize(
            entry("9:00", "18:00", breakMinutes = 60, rateType = RateType.HOURLY, rate = 3_000),
        )

        assertThat(summary.amount).isEqualTo(24_000)
    }

    @Test
    fun `割増率が金額に乗る`() {
        val summary = WorkLogCalculator.summarize(
            entry("22:00", "2:00", rateType = RateType.HOURLY, rate = 3_000, multiplier = 1.25),
        )

        // 4時間 × 3000 × 1.25
        assertThat(summary.amount).isEqualTo(15_000)
    }

    @Test
    fun `交通費には割増を掛けない`() {
        // 実費なので割増の対象外
        val summary = WorkLogCalculator.summarize(
            entry(
                "9:00", "17:00",
                rateType = RateType.DAILY, rate = 20_000,
                multiplier = 1.5, transportFee = 1_200,
            ),
        )

        assertThat(summary.amount).isEqualTo(30_000 + 1_200)
    }

    @Test
    fun `複数件を合計できる`() {
        val entries = listOf(
            entry("9:00", "18:00", rateType = RateType.DAILY, rate = 25_000),
            entry("13:00", "2:00", rateType = RateType.DAILY, rate = 30_000, transportFee = 800),
        )

        val total = WorkLogCalculator.total(entries)

        assertThat(total.workedMinutes).isEqualTo(540 + 780)
        assertThat(total.amount).isEqualTo(25_000 + 30_000 + 800)
    }

    @Test
    fun `時刻の読み書きが往復する`() {
        assertThat(WorkLogCalculator.parseTimeOfDay("9:30")).isEqualTo(570)
        assertThat(WorkLogCalculator.formatTimeOfDay(570)).isEqualTo("9:30")
        assertThat(WorkLogCalculator.parseTimeOfDay("0:00")).isEqualTo(0)
        assertThat(WorkLogCalculator.parseTimeOfDay("23:59")).isEqualTo(1439)
    }

    @Test
    fun `おかしな時刻は読まない`() {
        assertThat(WorkLogCalculator.parseTimeOfDay("25:00")).isNull()
        assertThat(WorkLogCalculator.parseTimeOfDay("9:70")).isNull()
        assertThat(WorkLogCalculator.parseTimeOfDay("930")).isNull()
        assertThat(WorkLogCalculator.parseTimeOfDay("")).isNull()
    }

    @Test
    fun `全角コロンも読める`() {
        // 日本語入力のままだと全角になることがある
        assertThat(WorkLogCalculator.parseTimeOfDay("9：30")).isEqualTo(570)
    }
}
