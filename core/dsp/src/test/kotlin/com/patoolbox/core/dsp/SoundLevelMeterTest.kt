package com.patoolbox.core.dsp

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SoundLevelMeterTest {

    /** 校正オフセット 0 = dBFS をそのまま読む状態でテストする */
    private fun meter(
        weighting: FrequencyWeighting = FrequencyWeighting.A,
        timeWeighting: TimeWeighting = TimeWeighting.FAST,
        offsetDb: Double = 0.0,
    ) = SoundLevelMeter(TEST_SAMPLE_RATE, weighting, timeWeighting, offsetDb)

    @Test
    fun `1kHzの定常音は指定レベルどおりに読める`() {
        // A特性は1kHzで0dBなので、-20dBFS がそのまま出る
        val signal = sineAtLevel(1000.0, levelDbFs = -20.0, lengthSamples = TEST_SAMPLE_RATE * 2)
        val reading = meter().process(signal)

        assertThat(reading.instantDb).isWithin(0.1).of(-20.0)
    }

    @Test
    fun `定常音ではLeqが瞬時値に一致する`() {
        val signal = sineAtLevel(1000.0, levelDbFs = -20.0, lengthSamples = TEST_SAMPLE_RATE * 2)
        val reading = meter().process(signal)

        assertThat(reading.leqDb).isWithin(0.1).of(reading.instantDb)
    }

    @Test
    fun `校正オフセットがそのまま加算される`() {
        val signal = sineAtLevel(1000.0, levelDbFs = -20.0, lengthSamples = TEST_SAMPLE_RATE * 2)
        val reading = meter(offsetDb = 120.0).process(signal)

        assertThat(reading.instantDb).isWithin(0.1).of(100.0)
    }

    @Test
    fun `ピーク値はRMSより3dB高い`() {
        val signal = sineAtLevel(1000.0, levelDbFs = -20.0, lengthSamples = TEST_SAMPLE_RATE)
        val reading = meter().process(signal)

        assertThat(reading.peakDb).isWithin(0.1).of(-20.0 + 3.01)
    }

    @Test
    fun `A特性は125Hzを規定どおり下げる`() {
        val signal = sineAtLevel(125.0, levelDbFs = -20.0, lengthSamples = TEST_SAMPLE_RATE * 2)
        val reading = meter().process(signal)

        assertThat(reading.instantDb).isWithin(0.5).of(-20.0 - 16.1)
    }

    @Test
    fun `Z特性は125Hzを下げない`() {
        val signal = sineAtLevel(125.0, levelDbFs = -20.0, lengthSamples = TEST_SAMPLE_RATE * 2)
        val reading = meter(weighting = FrequencyWeighting.Z).process(signal)

        assertThat(reading.instantDb).isWithin(0.1).of(-20.0)
    }

    @Test
    fun `LmaxとLminが変動を挟み込む`() {
        val loud = sineAtLevel(1000.0, levelDbFs = -10.0, lengthSamples = TEST_SAMPLE_RATE * 2)
        val quiet = sineAtLevel(1000.0, levelDbFs = -40.0, lengthSamples = TEST_SAMPLE_RATE * 2)

        val meter = meter()
        meter.process(loud)
        val reading = meter.process(quiet)

        assertThat(reading.maxDb).isWithin(0.3).of(-10.0)
        assertThat(reading.minDb).isWithin(0.5).of(-40.0)
    }

    @Test
    fun `Leqは大小の混在をエネルギー平均する`() {
        // 同じ長さで -10dB と -40dB → エネルギー平均は -13dB 付近
        val loud = sineAtLevel(1000.0, levelDbFs = -10.0, lengthSamples = TEST_SAMPLE_RATE)
        val quiet = sineAtLevel(1000.0, levelDbFs = -40.0, lengthSamples = TEST_SAMPLE_RATE)

        val meter = meter()
        meter.process(loud)
        val reading = meter.process(quiet)

        val expected = powerToDb((dbToPower(-10.0) + dbToPower(-40.0)) / 2.0)
        assertThat(reading.leqDb).isWithin(0.2).of(expected)
    }

    @Test
    fun `統計レベルL10とL90が高低を分ける`() {
        val loud = sineAtLevel(1000.0, levelDbFs = -10.0, lengthSamples = TEST_SAMPLE_RATE * 4)
        val quiet = sineAtLevel(1000.0, levelDbFs = -40.0, lengthSamples = TEST_SAMPLE_RATE * 4)

        val meter = meter()
        meter.process(loud)
        meter.process(quiet)

        assertThat(meter.percentileDb(10.0)).isWithin(1.0).of(-10.0)
        assertThat(meter.percentileDb(90.0)).isWithin(1.5).of(-40.0)
    }

    @Test
    fun `クリップを検出する`() {
        val clipping = FloatArray(1000) { if (it % 2 == 0) 1.0f else -1.0f }
        val reading = meter().process(clipping)

        assertThat(reading.clipped).isTrue()
    }

    @Test
    fun `クリップしていなければ検出しない`() {
        val signal = sineAtLevel(1000.0, levelDbFs = -6.0, lengthSamples = TEST_SAMPLE_RATE)
        val reading = meter().process(signal)

        assertThat(reading.clipped).isFalse()
    }

    @Test
    fun `経過時間がサンプル数と一致する`() {
        val signal = sineAtLevel(1000.0, levelDbFs = -20.0, lengthSamples = TEST_SAMPLE_RATE * 3)
        val reading = meter().process(signal)

        assertThat(reading.elapsedSeconds).isWithin(0.001).of(3.0)
    }

    @Test
    fun `resetで測定がやり直せる`() {
        val loud = sineAtLevel(1000.0, levelDbFs = -6.0, lengthSamples = TEST_SAMPLE_RATE)
        val quiet = sineAtLevel(1000.0, levelDbFs = -30.0, lengthSamples = TEST_SAMPLE_RATE * 2)

        val meter = meter()
        meter.process(loud)
        meter.reset()
        val reading = meter.process(quiet)

        assertThat(reading.maxDb).isWithin(0.3).of(-30.0)
        assertThat(reading.elapsedSeconds).isWithin(0.001).of(2.0)
    }

    @Test
    fun `Slow重み付けはFastより追従が遅い`() {
        val step = FloatArray(TEST_SAMPLE_RATE / 4) // 0.25秒の無音
            .plus(sineAtLevel(1000.0, levelDbFs = -20.0, lengthSamples = TEST_SAMPLE_RATE / 4))

        val fast = meter(timeWeighting = TimeWeighting.FAST).process(step)
        val slow = meter(timeWeighting = TimeWeighting.SLOW).process(step)

        // 立ち上がり途中で切ってあるので、Fast のほうが目標値に近い
        assertThat(fast.instantDb).isGreaterThan(slow.instantDb)
    }

    @Test
    fun `入力バッファを書き換えない`() {
        val signal = sineAtLevel(1000.0, levelDbFs = -20.0, lengthSamples = 1024)
        val copy = signal.copyOf()

        meter().process(signal)

        assertThat(signal).isEqualTo(copy)
    }
}
