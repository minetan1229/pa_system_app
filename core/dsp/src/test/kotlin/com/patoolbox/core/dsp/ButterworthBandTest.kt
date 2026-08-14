package com.patoolbox.core.dsp

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

class ButterworthBandTest {

    private val sampleRate = TEST_SAMPLE_RATE

    private fun BiquadCascade.gainDb(hz: Double) = amplitudeToDb(magnitudeAt(hz, sampleRate))

    private fun List<Biquad>.gainDb(hz: Double) =
        amplitudeToDb(BiquadCascade(this).magnitudeAt(hz, sampleRate))

    @Test
    fun `ローパスの遮断周波数は3dB落ち`() {
        val lp = ButterworthBand.lowPass(cutoffHz = 1000.0, sampleRate = sampleRate)

        assertThat(lp.gainDb(1000.0)).isWithin(0.1).of(-3.01)
        assertThat(lp.gainDb(100.0)).isWithin(0.05).of(0.0)
    }

    @Test
    fun `高い周波数でもプリワープで遮断位置がずれない`() {
        // 双一次変換は高域ほど周波数を圧縮する。プリワープを省くと
        // 10kHz 指定が 48kHz サンプリングで 1kHz 近く低い側にずれる
        val lp = ButterworthBand.lowPass(cutoffHz = 10000.0, sampleRate = sampleRate)

        assertThat(lp.gainDb(10000.0)).isWithin(0.2).of(-3.01)
    }

    @Test
    fun `ハイパスの遮断周波数は3dB落ち`() {
        val hp = ButterworthBand.highPass(cutoffHz = 500.0, sampleRate = sampleRate)

        assertThat(hp.gainDb(500.0)).isWithin(0.1).of(-3.01)
        assertThat(hp.gainDb(5000.0)).isWithin(0.05).of(0.0)
    }

    @Test
    fun `4次は1オクターブで24dB落ちる`() {
        val lp = ButterworthBand.lowPass(cutoffHz = 1000.0, sampleRate = sampleRate)

        val oneOctave = lp.gainDb(2000.0)
        val twoOctaves = lp.gainDb(4000.0)
        assertThat(twoOctaves - oneOctave).isWithin(1.0).of(-24.0)
    }

    @Test
    fun `オクターブバンドの端は3dB落ち`() {
        val band = OctaveBands.bands(BandResolution.FULL, 900.0, 1100.0).single()
        val cascade = ButterworthBand.bandPass(band, sampleRate)

        assertThat(cascade.gainDb(band.lowerHz)).isWithin(0.15).of(-3.01)
        assertThat(cascade.gainDb(band.upperHz)).isWithin(0.15).of(-3.01)
    }

    @Test
    fun `バンド中心の損失は0_5dB程度にとどまる`() {
        // ハイパスとローパスを直列にした構成なので、中心でも互いの肩が
        // わずかに掛かる。実測して 1dB 以内であることを固定しておく
        val band = OctaveBands.bands(BandResolution.FULL, 900.0, 1100.0).single()
        val cascade = ButterworthBand.bandPass(band, sampleRate)

        val centerDb = cascade.gainDb(band.centerHz)
        assertThat(centerDb).isLessThan(0.0)
        assertThat(centerDb).isGreaterThan(-1.0)
    }

    @Test
    fun `隣の帯域は十分に落ちる`() {
        val band = OctaveBands.bands(BandResolution.FULL, 900.0, 1100.0).single()
        val cascade = ButterworthBand.bandPass(band, sampleRate)

        // 2オクターブ外で 40dB 以上。IEC 61260 のクラス1には届かないが、
        // 隣接帯域の漏れ込みで残響時間が壊れない程度は確保できている
        assertThat(cascade.gainDb(band.upperHz * 4)).isLessThan(-40.0)
        assertThat(cascade.gainDb(band.lowerHz / 4)).isLessThan(-40.0)
    }

    @Test
    fun `ナイキストに近い上端はローパスを省く`() {
        // 20k 帯の上端 22.4kHz は 48kHz では作れない。落ちずに
        // ハイパスだけの構成になることを確認する
        val cascade = ButterworthBand.bandPass(17800.0, 22400.0, sampleRate)

        assertThat(cascade.isEmpty).isFalse()
        assertThat(cascade.gainDb(20000.0)).isGreaterThan(-1.0)
    }

    @Test
    fun `時間反転して掛けると尾は音より前に出る`() {
        // 残響解析ではこれが要る。フィルタの尾が減衰側に足されると
        // 残響時間が長く出るため、到来より前に置く
        val impulse = DoubleArray(4096).also { it[2048] = 1.0 }
        val cascade = ButterworthBand.bandPass(500.0, 1000.0, sampleRate)

        val reversed = ButterworthBand.filterReversed(impulse, cascade)

        val before = (0 until 2048).sumOf { abs(reversed[it]) }
        val after = (2049 until 4096).sumOf { abs(reversed[it]) }
        assertThat(before).isGreaterThan(after * 100)
    }

    @Test
    fun `順方向に掛けると尾は音の後に出る`() {
        val impulse = DoubleArray(4096).also { it[2048] = 1.0 }
        val cascade = ButterworthBand.bandPass(500.0, 1000.0, sampleRate)

        val forward = ButterworthBand.filterForward(impulse, cascade)

        val before = (0 until 2048).sumOf { abs(forward[it]) }
        val after = (2049 until 4096).sumOf { abs(forward[it]) }
        assertThat(after).isGreaterThan(before * 100)
    }

    @Test
    fun `奇数次は拒否する`() {
        val result = runCatching { ButterworthBand.lowPass(1000.0, sampleRate, order = 3) }

        assertThat(result.isFailure).isTrue()
    }
}
