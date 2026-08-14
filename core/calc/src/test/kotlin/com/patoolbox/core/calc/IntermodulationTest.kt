package com.patoolbox.core.calc

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IntermodulationTest {

    private fun mhz(value: Double) = Intermodulation.mhzToKHz(value)

    @Test
    fun `3次の積は教科書どおりの位置に出る`() {
        // f1=470.000, f2=470.400 → 2f1-f2 = 469.600, 2f2-f1 = 470.800
        val products = Intermodulation.products(
            listOf(mhz(470.0), mhz(470.4)),
            orders = setOf(Intermodulation.Order.THIRD_TWO_TONE),
        )

        val values = products.map { it.frequencyKHz }.sorted()
        assertThat(values).containsExactly(mhz(469.6), mhz(470.8))
    }

    @Test
    fun `5次の積も教科書どおり`() {
        // 3f1-2f2 = 3*470.0 - 2*470.4 = 469.2
        val products = Intermodulation.products(
            listOf(mhz(470.0), mhz(470.4)),
            orders = setOf(Intermodulation.Order.FIFTH_TWO_TONE),
        )

        assertThat(products.map { it.frequencyKHz }).contains(mhz(469.2))
    }

    @Test
    fun `3波の積は3つの周波数から作られる`() {
        val products = Intermodulation.products(
            listOf(mhz(470.0), mhz(471.0), mhz(472.0)),
            orders = setOf(Intermodulation.Order.THIRD_THREE_TONE),
        )

        // 470 + 471 - 472 = 469
        assertThat(products.map { it.frequencyKHz }).contains(mhz(469.0))
        assertThat(products.all { it.sources.size == 3 }).isTrue()
    }

    @Test
    fun `負になる組み合わせは積として出さない`() {
        // 2*470.0 - 480.0 = 460 は正だが、2*100 - 500 のような負の値は捨てる
        val products = Intermodulation.products(
            listOf(mhz(100.0), mhz(500.0)),
            orders = setOf(Intermodulation.Order.THIRD_TWO_TONE),
        )

        assertThat(products.all { it.frequencyKHz > 0 }).isTrue()
    }

    @Test
    fun `等間隔に並べると必ず干渉する`() {
        // 等間隔は最悪の並べ方。470.0 / 470.4 / 470.8 だと
        // 2*470.4 - 470.0 = 470.8 が3本目に直撃する
        val report = Intermodulation.analyze(
            listOf(mhz(470.0), mhz(470.4), mhz(470.8)),
            minSpacingKHz = 300,
        )

        assertThat(report.isClean).isFalse()
        assertThat(report.conflicts.map { it.victimKHz }).contains(mhz(470.8))
    }

    @Test
    fun `直撃した積はずれ0として出る`() {
        val report = Intermodulation.analyze(
            listOf(mhz(470.0), mhz(470.4), mhz(470.8)),
            minSpacingKHz = 300,
        )

        val direct = report.conflicts.first { it.victimKHz == mhz(470.8) }
        assertThat(direct.offsetKHz).isEqualTo(0L)
    }

    @Test
    fun `ガード幅を広げると引っかかる積が増える`() {
        val frequencies = listOf(mhz(470.0), mhz(470.425), mhz(470.8))

        val narrow = Intermodulation.analyze(frequencies, guardKHz = 10).conflicts.size
        val wide = Intermodulation.analyze(frequencies, guardKHz = 100).conflicts.size

        assertThat(wide).isGreaterThan(narrow)
    }

    @Test
    fun `間隔が近すぎる組は別に報告する`() {
        val report = Intermodulation.analyze(
            listOf(mhz(470.0), mhz(470.1)),
            minSpacingKHz = 300,
        )

        assertThat(report.spacingViolations).containsExactly(mhz(470.0) to mhz(470.1))
    }

    @Test
    fun `問題の多い周波数が上位に出る`() {
        val report = Intermodulation.analyze(
            listOf(mhz(470.0), mhz(470.4), mhz(470.8), mhz(475.0)),
            minSpacingKHz = 300,
        )

        val scores = report.troubleScores()
        assertThat(scores).isNotEmpty()
        // 干渉に関わっていない 475.0 は、関わっている周波数より下に来る
        val topScore = scores.first().second
        val lonely = scores.firstOrNull { it.first == mhz(475.0) }?.second ?: 0
        assertThat(lonely).isLessThan(topScore)
    }

    @Test
    fun `自動生成した組み合わせは干渉しない`() {
        val result = Intermodulation.plan(
            Intermodulation.PlanRequest(
                fromKHz = mhz(470.0),
                toKHz = mhz(490.0),
                stepKHz = 25,
                count = 8,
            ),
        )

        assertThat(result.isComplete).isTrue()
        assertThat(result.frequenciesKHz).hasSize(8)

        val report = Intermodulation.analyze(result.frequenciesKHz)
        assertThat(report.isClean).isTrue()
    }

    @Test
    fun `生成結果は毎回同じ`() {
        // 「昨日と違う周波数が出た」が現場では一番困る
        val request = Intermodulation.PlanRequest(
            fromKHz = mhz(470.0),
            toKHz = mhz(490.0),
            stepKHz = 25,
            count = 6,
        )

        assertThat(Intermodulation.plan(request).frequenciesKHz)
            .isEqualTo(Intermodulation.plan(request).frequenciesKHz)
    }

    @Test
    fun `動かせない周波数を避けて生成する`() {
        // 他社が使っている、あるいは放送が乗っている周波数
        val fixed = listOf(mhz(475.0), mhz(480.0))
        val result = Intermodulation.plan(
            Intermodulation.PlanRequest(
                fromKHz = mhz(470.0),
                toKHz = mhz(490.0),
                stepKHz = 25,
                count = 5,
                fixedKHz = fixed,
            ),
        )

        // 固定分は結果に含めない（利用者が選ぶのは新しく足す分だけ）
        assertThat(result.frequenciesKHz).containsNoneIn(fixed)
        // 固定分と合わせても干渉しない
        val report = Intermodulation.analyze(result.frequenciesKHz + fixed)
        assertThat(report.conflicts).isEmpty()
    }

    @Test
    fun `本数が足りないときは足りないと分かる形で返す`() {
        // 1MHz の幅に 300kHz 間隔で20本は入らない。
        // 黙って少ない本数を返すと、現場で足りないことに直前まで気づけない
        val result = Intermodulation.plan(
            Intermodulation.PlanRequest(
                fromKHz = mhz(470.0),
                toKHz = mhz(471.0),
                stepKHz = 25,
                count = 20,
            ),
        )

        assertThat(result.isComplete).isFalse()
        assertThat(result.shortfall).isGreaterThan(0)
        assertThat(result.frequenciesKHz.size + result.shortfall).isEqualTo(20)
    }

    @Test
    fun `5次まで見ると3次だけより厳しくなる`() {
        val request = Intermodulation.PlanRequest(
            fromKHz = mhz(470.0),
            toKHz = mhz(480.0),
            stepKHz = 25,
            count = 12,
        )

        val thirdOnly = Intermodulation.plan(request).frequenciesKHz.size
        val withFifth = Intermodulation.plan(
            request.copy(
                orders = Intermodulation.DEFAULT_ORDERS +
                    Intermodulation.Order.FIFTH_TWO_TONE,
            ),
        ).frequenciesKHz.size

        assertThat(withFifth).isAtMost(thirdOnly)
    }

    @Test
    fun `MHzとkHzの変換で桁が狂わない`() {
        // 0.025MHz のような刻みを Double で持つと下位桁が揺れる。
        // 整数 kHz に落として扱う設計の確認
        assertThat(Intermodulation.mhzToKHz(470.425)).isEqualTo(470_425L)
        assertThat(Intermodulation.kHzToMhz(470_425L)).isWithin(1e-9).of(470.425)
    }

    @Test
    fun `1本だけなら何も起きない`() {
        val report = Intermodulation.analyze(listOf(mhz(470.0)))

        assertThat(report.isClean).isTrue()
    }

    @Test
    fun `空でも落ちない`() {
        val report = Intermodulation.analyze(emptyList())

        assertThat(report.isClean).isTrue()
    }
}
