package com.patoolbox.core.calc

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CoverageCalculatorTest {

    @Test
    fun `距離が2倍で6dB下がる`() {
        val near = CoverageCalculator.splFromOneMeter(100.0, 10.0)
        val far = CoverageCalculator.splFromOneMeter(100.0, 20.0)

        assertThat(near - far).isWithin(0.01).of(6.02)
    }

    @Test
    fun `1mでの音圧がそのまま出る`() {
        assertThat(CoverageCalculator.splFromOneMeter(100.0, 1.0)).isWithin(0.01).of(100.0)
    }

    @Test
    fun `感度と電力から距離での音圧が出る`() {
        // 感度 100dB(1W/1m)、100W 入力 → 1m で 120dB、20m で 94dB
        assertThat(CoverageCalculator.splAtDistance(100.0, 100.0, 1.0)).isWithin(0.01).of(120.0)
        assertThat(CoverageCalculator.splAtDistance(100.0, 100.0, 20.0)).isWithin(0.05).of(93.98)
    }

    @Test
    fun `目標音圧から1mで必要な音圧が出る`() {
        // 30m で 100dB 欲しいなら 1m で 129.5dB 必要
        assertThat(CoverageCalculator.requiredSplAtOneMeter(100.0, 30.0))
            .isWithin(0.05)
            .of(129.54)
    }

    @Test
    fun `必要な電力が出る`() {
        // 感度 100dB、20m で 100dB 欲しい → 1m で 126dB → 約400W
        val watts = CoverageCalculator.requiredWatts(
            sensitivityDb = 100.0,
            targetDb = 100.0,
            meters = 20.0,
        )

        assertThat(watts).isWithin(5.0).of(400.0)
    }

    @Test
    fun `必要な電力と音圧が往復で一致する`() {
        val watts = CoverageCalculator.requiredWatts(98.0, 105.0, 15.0)
        val achieved = CoverageCalculator.splAtDistance(98.0, watts, 15.0)

        assertThat(achieved).isWithin(0.01).of(105.0)
    }

    @Test
    fun `90度のスピーカーは距離と同じ幅をカバーする`() {
        // 幅 = 2 × d × tan(45°) = 2d
        assertThat(CoverageCalculator.coverageWidthMeters(90.0, 10.0)).isWithin(0.01).of(20.0)
    }

    @Test
    fun `60度なら距離の約1_15倍`() {
        assertThat(CoverageCalculator.coverageWidthMeters(60.0, 10.0)).isWithin(0.05).of(11.55)
    }

    @Test
    fun `幅から必要な距離が逆算できる`() {
        val distance = CoverageCalculator.distanceForWidth(90.0, 20.0)

        assertThat(distance).isWithin(0.01).of(10.0)
        assertThat(CoverageCalculator.coverageWidthMeters(90.0, distance))
            .isWithin(0.01)
            .of(20.0)
    }

    @Test
    fun `無相関なら倍で3dB 同相なら倍で6dB`() {
        assertThat(CoverageCalculator.gainFromMultipleSources(2)).isWithin(0.01).of(3.01)
        assertThat(CoverageCalculator.gainFromMultipleSources(4)).isWithin(0.01).of(6.02)
        assertThat(CoverageCalculator.gainFromMultipleSources(2, coherent = true))
            .isWithin(0.01)
            .of(6.02)
    }

    @Test
    fun `1台なら増分は0`() {
        assertThat(CoverageCalculator.gainFromMultipleSources(1)).isWithin(1e-9).of(0.0)
    }

    @Test
    fun `前後の音圧差からディレイの要否が判断できる`() {
        // 5m と 40m なら 18dB 差。ディレイタワーの検討対象
        val difference = CoverageCalculator.frontToBackDifferenceDb(5.0, 40.0)

        assertThat(difference).isWithin(0.05).of(18.06)
        assertThat(difference).isGreaterThan(10.0)
    }

    @Test
    fun `不正な入力は例外`() {
        runCatching { CoverageCalculator.splFromOneMeter(100.0, 0.0) }
            .also { assertThat(it.isFailure).isTrue() }
        runCatching { CoverageCalculator.coverageWidthMeters(180.0, 10.0) }
            .also { assertThat(it.isFailure).isTrue() }
        runCatching { CoverageCalculator.gainFromMultipleSources(0) }
            .also { assertThat(it.isFailure).isTrue() }
    }
}
