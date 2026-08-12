package com.patoolbox.core.dsp

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.pow

class OctaveBandsTest {

    @Test
    fun `1_3オクターブは20Hzから20kHzで31バンド`() {
        val bands = OctaveBands.bands(BandResolution.THIRD)
        assertThat(bands).hasSize(31)
        assertThat(bands.first().label).isEqualTo("20")
        assertThat(bands.last().label).isEqualTo("20k")
    }

    @Test
    fun `1_1オクターブは呼び値で並ぶ`() {
        val labels = OctaveBands.bands(BandResolution.FULL).map { it.label }
        assertThat(labels).containsExactly(
            "31.5", "63", "125", "250", "500", "1k", "2k", "4k", "8k", "16k",
        ).inOrder()
    }

    @Test
    fun `1kHzのバンドが必ず存在する`() {
        for (resolution in BandResolution.entries) {
            val bands = OctaveBands.bands(resolution)
            val center = bands.first { it.index == 0 }
            assertThat(center.centerHz).isWithin(0.001).of(1000.0)
        }
    }

    @Test
    fun `中心周波数がIEC 61260の計算値と一致する`() {
        val bands = OctaveBands.bands(BandResolution.THIRD).associateBy { it.label }

        assertThat(bands.getValue("31.5").centerHz).isWithin(0.2).of(31.62)
        assertThat(bands.getValue("125").centerHz).isWithin(0.5).of(125.89)
        assertThat(bands.getValue("1k").centerHz).isWithin(0.001).of(1000.0)
        assertThat(bands.getValue("4k").centerHz).isWithin(5.0).of(3981.07)
    }

    @Test
    fun `帯域端の比がIEC 61260の10を底とする体系に一致する`() {
        for (resolution in BandResolution.entries) {
            val band = OctaveBands.bands(resolution).first { it.index == 0 }
            val ratio = band.upperHz / band.lowerHz
            val b = resolution.bandsPerOctave.toDouble()

            // IEC 61260 の 10 を底とする体系: 10^(3/(10b))
            assertThat(ratio).isWithin(1e-9).of(10.0.pow(3.0 / (10.0 * b)))

            // 2 を底とする体系（2^(1/b)）とは 0.25% 弱ずれる。これは規格上の差
            val base2 = 2.0.pow(1.0 / b)
            assertThat(ratio).isWithin(base2 * 0.003).of(base2)
        }
    }

    @Test
    fun `隣接バンドの端が連続している`() {
        val bands = OctaveBands.bands(BandResolution.THIRD)
        for (i in 0 until bands.size - 1) {
            assertThat(bands[i].upperHz).isWithin(bands[i].upperHz * 1e-9)
                .of(bands[i + 1].lowerHz)
        }
    }

    @Test
    fun `分解能を上げるとバンド数が比例して増える`() {
        val third = OctaveBands.bands(BandResolution.THIRD).size
        val sixth = OctaveBands.bands(BandResolution.SIXTH).size
        val twelfth = OctaveBands.bands(BandResolution.TWELFTH).size

        assertThat(sixth).isAtLeast(third * 2 - 2)
        assertThat(twelfth).isAtLeast(third * 4 - 4)
    }

    @Test
    fun `1_6と1_12はProが必要`() {
        assertThat(BandResolution.FULL.requiresPro).isFalse()
        assertThat(BandResolution.THIRD.requiresPro).isFalse()
        assertThat(BandResolution.SIXTH.requiresPro).isTrue()
        assertThat(BandResolution.TWELFTH.requiresPro).isTrue()
    }
}
