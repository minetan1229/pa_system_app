package com.patoolbox.core.dsp

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReverbTimeTest {

    private val sampleRate = TEST_SAMPLE_RATE

    @Test
    fun `既知の減衰から残響時間が出る`() {
        val ir = decayingNoise(rtSeconds = 1.0, lengthSamples = sampleRate * 3)

        val result = ReverbTime.analyze(ir, sampleRate)

        assertThat(result.t30).isNotNull()
        assertThat(result.t30!!.rtSeconds).isWithin(0.05).of(1.0)
        assertThat(result.t20!!.rtSeconds).isWithin(0.05).of(1.0)
    }

    @Test
    fun `短い残響も長い残響も同じ精度で出る`() {
        val short = ReverbTime.analyze(
            decayingNoise(rtSeconds = 0.35, lengthSamples = sampleRate * 2),
            sampleRate,
        )
        val long = ReverbTime.analyze(
            decayingNoise(rtSeconds = 2.5, lengthSamples = sampleRate * 6),
            sampleRate,
        )

        assertThat(short.t30!!.rtSeconds).isWithin(0.03).of(0.35)
        assertThat(long.t30!!.rtSeconds).isWithin(0.2).of(2.5)
    }

    @Test
    fun `減衰直線のあてはまりが良いことを確認できる`() {
        val result = ReverbTime.analyze(
            decayingNoise(rtSeconds = 1.2, lengthSamples = sampleRate * 4),
            sampleRate,
        )

        val t30 = result.t30!!
        assertThat(t30.isReliable).isTrue()
        assertThat(t30.slopeDbPerSecond).isLessThan(0.0)
    }

    @Test
    fun `暗騒音があっても打ち切りで残響時間が伸びない`() {
        // 打ち切らないとカーブの後半が水平になり、残響が実際より長く出る。
        // -45dB の暗騒音を足しても T20 が保たれることを確認する
        val ir = decayingNoise(rtSeconds = 1.0, lengthSamples = sampleRate * 3)
            .plusNoise(amplitude = 0.0056)

        val result = ReverbTime.analyze(ir, sampleRate)

        assertThat(result.t20).isNotNull()
        assertThat(result.t20!!.rtSeconds).isWithin(0.15).of(1.0)
    }

    @Test
    fun `暗騒音が高いと出せない値は出さない`() {
        // -25dB 程度までしか下がらない測定。T30 は原理的に読めないので
        // 外挿して数字を出すのではなく null を返す
        val ir = decayingNoise(rtSeconds = 1.0, lengthSamples = sampleRate * 3)
            .plusNoise(amplitude = 0.09)

        val result = ReverbTime.analyze(ir, sampleRate)

        assertThat(result.t30).isNull()
        assertThat(result.decayRangeDb).isLessThan(35.0)
    }

    @Test
    fun `代表値はT30があればT30を使う`() {
        val result = ReverbTime.analyze(
            decayingNoise(rtSeconds = 0.8, lengthSamples = sampleRate * 3),
            sampleRate,
        )

        assertThat(result.bestLabel).isEqualTo("T30")
        assertThat(result.bestFit).isEqualTo(result.t30)
    }

    @Test
    fun `減衰カーブは単調に下がる`() {
        val result = ReverbTime.analyze(
            decayingNoise(rtSeconds = 1.0, lengthSamples = sampleRate * 3),
            sampleRate,
        )

        assertThat(result.curveDb.first()).isWithin(1e-9).of(0.0)
        for (i in 1 until result.curveDb.size) {
            assertThat(result.curveDb[i]).isAtMost(result.curveDb[i - 1])
        }
    }

    @Test
    fun `オクターブバンドごとに残響時間が出る`() {
        // 全帯域を同じ速さで減衰させた応答なので、どの帯域でも同じ値になるはず。
        // 帯域分割そのものが減衰を汚していないことの確認になる
        val ir = decayingNoise(rtSeconds = 1.0, lengthSamples = sampleRate * 3)

        val bands = ReverbTime.analyzeBands(
            ir,
            sampleRate,
            bands = OctaveBands.bands(BandResolution.FULL, 125.0, 4000.0),
        )

        assertThat(bands).hasSize(6)
        for (band in bands) {
            val t20 = band.result.t20
            assertThat(t20).isNotNull()
            assertThat(t20!!.rtSeconds).isWithin(0.12).of(1.0)
        }
    }

    @Test
    fun `低域だけ響く部屋は低域だけ長く出る`() {
        val fast = decayingNoise(rtSeconds = 0.4, lengthSamples = sampleRate * 4, seed = 3L)
        val slow = decayingNoise(rtSeconds = 1.6, lengthSamples = sampleRate * 4, seed = 5L)
        // 250Hz 以下だけ長い減衰、それ以外は短い減衰にした応答を合成する
        val lowCascade = ButterworthBand.bandPass(30.0, 250.0, sampleRate)
        val highCascade = ButterworthBand.bandPass(250.0, 16000.0, sampleRate)
        val lowPart = ButterworthBand.filterForward(slow, lowCascade)
        val highPart = ButterworthBand.filterForward(fast, highCascade)
        val ir = DoubleArray(fast.size) { lowPart[it] + highPart[it] }

        val bands = ReverbTime.analyzeBands(
            ir,
            sampleRate,
            bands = OctaveBands.bands(BandResolution.FULL, 125.0, 2000.0),
        )

        val low = bands.first { it.band.centerHz < 200 }.result.t20!!.rtSeconds
        val high = bands.first { it.band.centerHz > 1500 }.result.t20!!.rtSeconds
        assertThat(low).isGreaterThan(high * 2.0)
    }

    @Test
    fun `無音を渡しても落ちない`() {
        val result = ReverbTime.analyze(DoubleArray(sampleRate), sampleRate)

        assertThat(result.t30).isNull()
        assertThat(result.t20).isNull()
        assertThat(result.edt).isNull()
    }
}
