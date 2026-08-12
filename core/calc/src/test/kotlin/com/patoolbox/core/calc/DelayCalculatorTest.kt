package com.patoolbox.core.calc

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DelayCalculatorTest {

    private val speed = 344.0

    @Test
    fun `34_4mは100ms`() {
        assertThat(DelayCalculator.millisecondsForDistance(34.4, speed))
            .isWithin(0.01)
            .of(100.0)
    }

    @Test
    fun `1mは約2_9ms`() {
        assertThat(DelayCalculator.millisecondsForDistance(1.0, speed))
            .isWithin(0.01)
            .of(2.91)
    }

    @Test
    fun `距離とmsが往復で一致する`() {
        val meters = 23.7
        val ms = DelayCalculator.millisecondsForDistance(meters, speed)

        assertThat(DelayCalculator.distanceForMilliseconds(ms, speed))
            .isWithin(1e-9)
            .of(meters)
    }

    @Test
    fun `フィートとメートルの換算`() {
        assertThat(DelayCalculator.feetToMeters(1.0)).isWithin(1e-9).of(0.3048)
        assertThat(DelayCalculator.metersToFeet(0.3048)).isWithin(1e-9).of(1.0)
        assertThat(DelayCalculator.feetToMeters(100.0)).isWithin(0.01).of(30.48)
    }

    @Test
    fun `48kHzで10msは480サンプル`() {
        assertThat(DelayCalculator.samplesForMilliseconds(10.0, 48000)).isWithin(1e-9).of(480.0)
        assertThat(DelayCalculator.millisecondsForSamples(480.0, 48000)).isWithin(1e-9).of(10.0)
    }

    @Test
    fun `ディレイタワーは距離ぶん遅らせる`() {
        val delay = DelayCalculator.towerDelayMs(30.0, speed)

        assertThat(delay).isWithin(0.1).of(87.2)
    }

    @Test
    fun `ディレイタワーに追加オフセットを足せる`() {
        val base = DelayCalculator.towerDelayMs(30.0, speed)
        val offset = DelayCalculator.towerDelayMs(30.0, speed, alignmentOffsetMs = 5.0)

        assertThat(offset - base).isWithin(1e-9).of(5.0)
    }

    @Test
    fun `到達時間差が距離差から出る`() {
        val difference = DelayCalculator.arrivalDifferenceMs(10.0, 20.0, speed)

        assertThat(difference).isWithin(0.01).of(
            DelayCalculator.millisecondsForDistance(10.0, speed),
        )
    }

    @Test
    fun `1msの時間差は500Hzで打ち消す`() {
        assertThat(DelayCalculator.firstCancellationHz(1.0)).isWithin(0.01).of(500.0)
        assertThat(DelayCalculator.firstCancellationHz(2.0)).isWithin(0.01).of(250.0)
    }

    @Test
    fun `時間差がなければ打ち消しは起きない`() {
        assertThat(DelayCalculator.firstCancellationHz(0.0)).isPositiveInfinity()
    }

    @Test
    fun `音速を変えるとディレイ量も変わる`() {
        val cold = DelayCalculator.millisecondsForDistance(50.0, SpeedOfSound.forConditions(0.0))
        val hot = DelayCalculator.millisecondsForDistance(50.0, SpeedOfSound.forConditions(35.0))

        assertThat(cold).isGreaterThan(hot)
    }
}
