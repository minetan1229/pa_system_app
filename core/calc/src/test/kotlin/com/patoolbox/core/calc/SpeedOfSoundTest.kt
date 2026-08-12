package com.patoolbox.core.calc

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SpeedOfSoundTest {

    @Test
    fun `0度の乾燥空気は約331m毎秒`() {
        assertThat(SpeedOfSound.forConditions(0.0, 0.0)).isWithin(0.1).of(331.4)
    }

    @Test
    fun `20度湿度50%は約344m毎秒`() {
        // PAの計算で既定値として使われる値
        assertThat(SpeedOfSound.forConditions(20.0, 50.0)).isWithin(0.1).of(344.0)
    }

    @Test
    fun `既定値が20度50%と一致する`() {
        assertThat(SpeedOfSound.DEFAULT_M_PER_SEC)
            .isWithin(0.1)
            .of(SpeedOfSound.forConditions(20.0, 50.0))
    }

    @Test
    fun `気温が上がると音速も上がる`() {
        val cold = SpeedOfSound.forConditions(5.0)
        val hot = SpeedOfSound.forConditions(35.0)

        assertThat(hot).isGreaterThan(cold)
        // 30度差で約18m/s
        assertThat(hot - cold).isWithin(0.1).of(18.0)
    }

    @Test
    fun `湿度の影響は気温よりずっと小さい`() {
        val dry = SpeedOfSound.forConditions(20.0, 0.0)
        val humid = SpeedOfSound.forConditions(20.0, 100.0)

        // 湿度0%から100%でも 1.24m/s しか変わらない
        assertThat(humid - dry).isWithin(0.01).of(1.24)
    }

    @Test
    fun `湿度は0から100の範囲に丸める`() {
        assertThat(SpeedOfSound.forConditions(20.0, -50.0))
            .isEqualTo(SpeedOfSound.forConditions(20.0, 0.0))
        assertThat(SpeedOfSound.forConditions(20.0, 200.0))
            .isEqualTo(SpeedOfSound.forConditions(20.0, 100.0))
    }

    @Test
    fun `気温差がディレイタワーに与える影響が実用上無視できない`() {
        // 朝10度・本番25度、タワーまで30mのケース
        val morning = DelayCalculator.millisecondsForDistance(
            30.0,
            SpeedOfSound.forConditions(10.0),
        )
        val show = DelayCalculator.millisecondsForDistance(
            30.0,
            SpeedOfSound.forConditions(25.0),
        )

        // 3.5ms 以上ずれる。時間合わせをやり直す必要がある水準
        assertThat(morning - show).isGreaterThan(2.0)
    }
}
