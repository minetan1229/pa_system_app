package com.patoolbox.core.calc

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ImpedanceCalculatorTest {

    @Test
    fun `8オーム2台の並列は4オーム`() {
        assertThat(ImpedanceCalculator.parallelIdentical(8.0, 2)).isWithin(1e-9).of(4.0)
        assertThat(ImpedanceCalculator.parallel(listOf(8.0, 8.0))).isWithin(1e-9).of(4.0)
    }

    @Test
    fun `8オーム4台の並列は2オーム`() {
        assertThat(ImpedanceCalculator.parallelIdentical(8.0, 4)).isWithin(1e-9).of(2.0)
    }

    @Test
    fun `異なるインピーダンスの並列`() {
        // 8Ω と 16Ω の並列 = 5.33Ω
        assertThat(ImpedanceCalculator.parallel(listOf(8.0, 16.0))).isWithin(0.01).of(5.33)
    }

    @Test
    fun `直列は足し算`() {
        assertThat(ImpedanceCalculator.series(listOf(8.0, 8.0))).isWithin(1e-9).of(16.0)
        assertThat(ImpedanceCalculator.series(listOf(4.0, 8.0, 16.0))).isWithin(1e-9).of(28.0)
    }

    @Test
    fun `並列で不正な値は無視する`() {
        assertThat(ImpedanceCalculator.parallel(listOf(8.0, 0.0, -4.0)))
            .isWithin(1e-9)
            .of(8.0)
        assertThat(ImpedanceCalculator.parallel(emptyList())).isEqualTo(0.0)
    }

    @Test
    fun `最低負荷4オームなら8オームは2台まで`() {
        assertThat(ImpedanceCalculator.maxParallelSpeakers(8.0, 4.0)).isEqualTo(2)
    }

    @Test
    fun `最低負荷2オームなら8オームは4台まで`() {
        assertThat(ImpedanceCalculator.maxParallelSpeakers(8.0, 2.0)).isEqualTo(4)
    }

    @Test
    fun `負荷の安全判定`() {
        assertThat(ImpedanceCalculator.isSafeLoad(4.0, 4.0)).isTrue()
        assertThat(ImpedanceCalculator.isSafeLoad(8.0, 4.0)).isTrue()
        // 3台並列で 2.67Ω は 4Ω 最低のアンプでは危ない
        assertThat(ImpedanceCalculator.isSafeLoad(2.67, 4.0)).isFalse()
    }

    @Test
    fun `負荷が半分になると出力は倍になる`() {
        assertThat(ImpedanceCalculator.powerAtLoad(200.0, 8.0, 4.0)).isWithin(1e-9).of(400.0)
        assertThat(ImpedanceCalculator.powerAtLoad(200.0, 8.0, 16.0)).isWithin(1e-9).of(100.0)
        assertThat(ImpedanceCalculator.powerAtLoad(200.0, 8.0, 8.0)).isWithin(1e-9).of(200.0)
    }

    @Test
    fun `並列のスピーカーで電力が分配される`() {
        assertThat(ImpedanceCalculator.powerPerSpeaker(400.0, 4)).isWithin(1e-9).of(100.0)
    }

    @Test
    fun `100Vライン60Wタップは約167オーム`() {
        assertThat(ImpedanceCalculator.highImpedanceLoadOhms(60.0, LineVoltage.V100))
            .isWithin(0.1)
            .of(166.7)
    }

    @Test
    fun `70Vライン10Wタップは約500オーム`() {
        assertThat(ImpedanceCalculator.highImpedanceLoadOhms(10.0, LineVoltage.V70))
            .isWithin(1.0)
            .of(499.8)
    }

    @Test
    fun `ハイインピーダンス回線のタップ合計`() {
        val taps = listOf(10.0, 10.0, 20.0, 5.0)

        assertThat(ImpedanceCalculator.highImpedanceTotalWatts(taps)).isWithin(1e-9).of(45.0)
    }

    @Test
    fun `ハイインピーダンス回線の残り容量は8割基準`() {
        // 120W のアンプに 45W ぶら下げている → 96W まで使えるので残り51W
        val remaining = ImpedanceCalculator.highImpedanceRemainingWatts(
            amplifierWatts = 120.0,
            tapWatts = listOf(10.0, 10.0, 20.0, 5.0),
        )

        assertThat(remaining).isWithin(1e-9).of(51.0)
    }

    @Test
    fun `容量を超えると残りが負になる`() {
        val remaining = ImpedanceCalculator.highImpedanceRemainingWatts(
            amplifierWatts = 60.0,
            tapWatts = listOf(30.0, 30.0),
        )

        assertThat(remaining).isLessThan(0.0)
    }

    @Test
    fun `不正な引数は例外`() {
        runCatching { ImpedanceCalculator.parallelIdentical(0.0, 2) }
            .also { assertThat(it.isFailure).isTrue() }
        runCatching { ImpedanceCalculator.parallelIdentical(8.0, 0) }
            .also { assertThat(it.isFailure).isTrue() }
        runCatching { ImpedanceCalculator.highImpedanceLoadOhms(0.0, LineVoltage.V100) }
            .also { assertThat(it.isFailure).isTrue() }
    }
}
