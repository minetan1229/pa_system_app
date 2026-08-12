package com.patoolbox.core.dsp

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import kotlin.math.abs

/**
 * A/C 特性が IEC 61672-1 の規定値に一致するかを検証する。
 * ここが狂うと SPL メーターと RTA の値すべてが狂うので、最も重要なテスト。
 */
class FrequencyWeightingTest {

    private val aWeighting = WeightingFilter.create(FrequencyWeighting.A, TEST_SAMPLE_RATE)
    private val cWeighting = WeightingFilter.create(FrequencyWeighting.C, TEST_SAMPLE_RATE)
    private val zWeighting = WeightingFilter.create(FrequencyWeighting.Z, TEST_SAMPLE_RATE)

    @Test
    fun `A特性は1kHzで0dB`() {
        assertThat(aWeighting.magnitudeDbAt(1000.0)).isWithin(0.05).of(0.0)
    }

    @Test
    fun `C特性は1kHzで0dB`() {
        assertThat(cWeighting.magnitudeDbAt(1000.0)).isWithin(0.05).of(0.0)
    }

    @Test
    fun `Z特性は全帯域でフラット`() {
        for (hz in listOf(20.0, 100.0, 1000.0, 10000.0, 20000.0)) {
            assertThat(zWeighting.magnitudeDbAt(hz)).isEqualTo(0.0)
        }
    }

    @Test
    fun `A特性がIEC 61672の規定量と一致する`() {
        // IEC 61672-1 表2（呼び中心周波数に対する A 特性の重み）
        val expected = mapOf(
            31.5 to -39.4,
            63.0 to -26.2,
            125.0 to -16.1,
            250.0 to -8.6,
            500.0 to -3.2,
            1000.0 to 0.0,
            2000.0 to 1.2,
            4000.0 to 1.0,
        )
        expected.forEach { (hz, db) ->
            assertWithMessage("%s Hz の A 特性", hz)
                .that(aWeighting.magnitudeDbAt(hz))
                .isWithin(0.5)
                .of(db)
        }
    }

    @Test
    fun `A特性の8kHzは双一次変換のずれを含めて許容内`() {
        // 12194Hz の極が 48kHz サンプリングだとわずかに下へ寄るため高域はずれる。
        // クラス1の許容差（8kHz で +2.1 / -3.1 dB）には収まる。
        assertThat(aWeighting.magnitudeDbAt(8000.0)).isWithin(1.5).of(-1.1)
    }

    @Test
    fun `C特性がIEC 61672の規定量と一致する`() {
        val expected = mapOf(
            31.5 to -3.0,
            63.0 to -0.8,
            125.0 to -0.2,
            250.0 to 0.0,
            500.0 to 0.0,
            1000.0 to 0.0,
            2000.0 to -0.2,
            4000.0 to -0.8,
        )
        expected.forEach { (hz, db) ->
            assertWithMessage("%s Hz の C 特性", hz)
                .that(cWeighting.magnitudeDbAt(hz))
                .isWithin(0.4)
                .of(db)
        }
    }

    @Test
    fun `A特性は低域を強く減衰させる`() {
        assertThat(aWeighting.magnitudeDbAt(20.0)).isLessThan(-45.0)
        assertThat(aWeighting.magnitudeDbAt(10.0)).isLessThan(-65.0)
    }

    @Test
    fun `時間波形を通しても1kHzのレベルが変わらない`() {
        // 解析的な振幅特性ではなく、実際にサンプルを流す経路を検証する
        val input = sineAtLevel(1000.0, levelDbFs = -20.0, lengthSamples = TEST_SAMPLE_RATE)
        val output = input.copyOf()

        WeightingFilter.create(FrequencyWeighting.A, TEST_SAMPLE_RATE).processInPlace(output)

        // 先頭はフィルタの立ち上がりが乗るので後半だけで比較する
        val outputTail = output.copyOfRange(output.size / 2, output.size)
        val inputTail = input.copyOfRange(input.size / 2, input.size)

        assertThat(abs(levelDbFs(outputTail) - levelDbFs(inputTail))).isLessThan(0.1)
    }

    @Test
    fun `時間波形で125Hzが規定どおり減衰する`() {
        val input = sineAtLevel(125.0, levelDbFs = -20.0, lengthSamples = TEST_SAMPLE_RATE * 2)
        val output = input.copyOf()
        WeightingFilter.create(FrequencyWeighting.A, TEST_SAMPLE_RATE).processInPlace(output)

        val tail = output.copyOfRange(output.size / 2, output.size)
        val attenuation = levelDbFs(tail) - (-20.0)

        assertThat(attenuation).isWithin(0.5).of(-16.1)
    }
}
