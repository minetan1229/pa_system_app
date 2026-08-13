package com.patoolbox.core.calc

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PowerCalculatorTest {

    @Test
    fun `単相100Vで1000Wは10A`() {
        assertThat(PowerCalculator.currentAmps(1000.0, 100.0)).isWithin(0.01).of(10.0)
    }

    @Test
    fun `力率が下がると電流が増える`() {
        val unity = PowerCalculator.currentAmps(1000.0, 100.0, powerFactor = 1.0)
        val poor = PowerCalculator.currentAmps(1000.0, 100.0, powerFactor = 0.8)

        assertThat(poor).isWithin(0.01).of(12.5)
        assertThat(poor).isGreaterThan(unity)
    }

    @Test
    fun `三相200Vで3000Wは約8_7A`() {
        // I = P / (√3 × V × 力率)
        val amps = PowerCalculator.currentAmps(
            watts = 3000.0,
            volts = 200.0,
            system = WiringSystem.THREE_PHASE_3WIRE,
        )

        assertThat(amps).isWithin(0.05).of(8.66)
    }

    @Test
    fun `連続負荷はブレーカー定格の80%まで`() {
        assertThat(PowerCalculator.continuousCapacityAmps(20.0)).isWithin(0.01).of(16.0)
        assertThat(PowerCalculator.isWithinBreaker(16.0, 20.0)).isTrue()
        assertThat(PowerCalculator.isWithinBreaker(18.0, 20.0)).isFalse()
    }

    @Test
    fun `20Aブレーカーの単相100Vで使えるのは1600W`() {
        assertThat(PowerCalculator.usableWatts(20.0, 100.0)).isWithin(0.01).of(1600.0)
    }

    @Test
    fun `三相のほうが同じ電流で多くの電力を送れる`() {
        val single = PowerCalculator.usableWatts(30.0, 200.0)
        val three = PowerCalculator.usableWatts(
            30.0,
            200.0,
            system = WiringSystem.THREE_PHASE_3WIRE,
        )

        assertThat(three / single).isWithin(0.01).of(1.732)
    }

    @Test
    fun `電圧降下が内線規程の式と一致する`() {
        // 単相2線式 e = 35.6 × L × I / (1000 × A)
        // 50m、15A、3.5mm² → 7.63V
        val drop = PowerCalculator.voltageDropVolts(
            lengthMeters = 50.0,
            currentAmps = 15.0,
            crossSectionMm2 = 3.5,
        )

        assertThat(drop).isWithin(0.01).of(7.63)
    }

    @Test
    fun `断面積を上げると電圧降下が減る`() {
        val thin = PowerCalculator.voltageDropVolts(50.0, 15.0, 2.0)
        val thick = PowerCalculator.voltageDropVolts(50.0, 15.0, 8.0)

        assertThat(thick).isWithin(0.001).of(thin / 4.0)
    }

    @Test
    fun `三相のほうが電圧降下が小さい`() {
        val single = PowerCalculator.voltageDropVolts(50.0, 15.0, 3.5)
        val three = PowerCalculator.voltageDropVolts(
            50.0,
            15.0,
            3.5,
            system = WiringSystem.THREE_PHASE_3WIRE,
        )

        assertThat(three).isLessThan(single)
    }

    @Test
    fun `電圧降下の割合と許容判定`() {
        // 100V で 7.63V 落ちると 7.6% で許容外
        val percent = PowerCalculator.voltageDropPercent(50.0, 15.0, 3.5, 100.0)

        assertThat(percent).isWithin(0.05).of(7.63)
        assertThat(PowerCalculator.isVoltageDropAcceptable(percent)).isFalse()
        assertThat(PowerCalculator.isVoltageDropAcceptable(1.5)).isTrue()
    }

    @Test
    fun `100Vで長く引くと極端に太い線が要る`() {
        // 100m・20A を 100V で送ると、22mm² でもまだ 3.2% 落ちる。
        // 2% に収めるには 38mm² が要る。これが長距離を 200V で配る理由。
        val at22 = PowerCalculator.voltageDropPercent(100.0, 20.0, 22.0, 100.0)
        val at38 = PowerCalculator.voltageDropPercent(100.0, 20.0, 38.0, 100.0)

        assertThat(at22).isWithin(0.05).of(3.24)
        assertThat(PowerCalculator.isVoltageDropAcceptable(at22)).isFalse()
        assertThat(PowerCalculator.isVoltageDropAcceptable(at38)).isTrue()
    }

    @Test
    fun `同じ電力なら200Vのほうが電圧降下の割合が小さい`() {
        // 2000W を 100m 送る場合。電流が半分になり、基準電圧も倍になる
        val at100v = PowerCalculator.voltageDropPercent(
            lengthMeters = 100.0,
            currentAmps = PowerCalculator.currentAmps(2000.0, 100.0),
            crossSectionMm2 = 5.5,
            volts = 100.0,
        )
        val at200v = PowerCalculator.voltageDropPercent(
            lengthMeters = 100.0,
            currentAmps = PowerCalculator.currentAmps(2000.0, 200.0),
            crossSectionMm2 = 5.5,
            volts = 200.0,
        )

        // 電流が半分・電圧が倍なので割合は1/4になる
        assertThat(at200v).isWithin(0.01).of(at100v / 4.0)
    }

    @Test
    fun `不正な入力は例外`() {
        runCatching { PowerCalculator.currentAmps(1000.0, 0.0) }
            .also { assertThat(it.isFailure).isTrue() }
        runCatching { PowerCalculator.currentAmps(1000.0, 100.0, powerFactor = 0.0) }
            .also { assertThat(it.isFailure).isTrue() }
        runCatching { PowerCalculator.voltageDropVolts(10.0, 10.0, 0.0) }
            .also { assertThat(it.isFailure).isTrue() }
    }
}
