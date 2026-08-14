package com.patoolbox.core.dsp

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

class ImpulseResponseTest {

    private val sampleRate = TEST_SAMPLE_RATE
    private val sweepLength = 1 shl 15 // 約 0.68 秒

    @Test
    fun `遅延したスイープからその遅延がそのまま出る`() {
        val delay = 1234
        val reference = logSweepAt(0.0, sweepLength)
        val recorded = logSweepAt(delay.toDouble(), sweepLength)

        val ir = ImpulseResponse.deconvolve(reference, recorded)
        val estimate = ImpulseResponse.estimateDelay(ir, sampleRate)

        assertThat(estimate.delaySamples).isWithin(0.05).of(delay.toDouble())
        assertThat(estimate.delayMs).isWithin(0.01).of(delay * 1000.0 / sampleRate)
    }

    @Test
    fun `小数サンプルの遅延も放物線補間で拾える`() {
        // 48kHz の1サンプルは音速で約 7mm。整数だけだとスピーカー間の
        // 追い込みで丸めが効いてくるので、サンプル未満まで読めることを確かめる
        val delay = 800.4
        val reference = logSweepAt(0.0, sweepLength)
        val recorded = logSweepAt(delay, sweepLength)

        val estimate = ImpulseResponse.estimateDelay(
            ImpulseResponse.deconvolve(reference, recorded),
            sampleRate,
        )

        assertThat(estimate.delaySamples).isWithin(0.1).of(delay)
    }

    @Test
    fun `相互相関でも同じ遅延になる`() {
        val delay = 2000
        val reference = logSweepAt(0.0, sweepLength)
        val recorded = logSweepAt(delay.toDouble(), sweepLength)

        val correlation = ImpulseResponse.crossCorrelation(reference, recorded)

        assertThat(ImpulseResponse.findArrival(correlation).index).isEqualTo(delay)
    }

    @Test
    fun `逆相の応答は逆相と判定する`() {
        val delay = 500
        val reference = logSweepAt(0.0, sweepLength)
        val inverted = logSweepAt(delay.toDouble(), sweepLength).map { -it }.toDoubleArray()

        val ir = ImpulseResponse.deconvolve(reference, inverted)
        val polarity = ImpulseResponse.estimatePolarity(ir, sampleRate)

        assertThat(polarity.polarity).isEqualTo(ImpulseResponse.Polarity.INVERTED)
        assertThat(polarity.arrivalIndex).isEqualTo(delay)
    }

    @Test
    fun `正相の応答は正相と判定する`() {
        val delay = 500
        val reference = logSweepAt(0.0, sweepLength)
        val recorded = logSweepAt(delay.toDouble(), sweepLength)

        val polarity = ImpulseResponse.estimatePolarity(
            ImpulseResponse.deconvolve(reference, recorded),
            sampleRate,
        )

        assertThat(polarity.polarity).isEqualTo(ImpulseResponse.Polarity.NORMAL)
        assertThat(polarity.marginDb).isGreaterThan(6.0)
    }

    @Test
    fun `対称な波形では極性を断定しない`() {
        // 正負が同じ高さの応答。実際にこうなるのは判定に足る情報が無いときで、
        // ここで無理に NORMAL か INVERTED を返すと現場を誤らせる
        val ir = DoubleArray(4096)
        ir[1000] = 1.0
        ir[1010] = -1.0

        val polarity = ImpulseResponse.estimatePolarity(ir, sampleRate)

        assertThat(polarity.polarity).isEqualTo(ImpulseResponse.Polarity.UNCERTAIN)
    }

    @Test
    fun `反射より直接音を先に拾う`() {
        val direct = 600
        val reflection = 900
        val reference = logSweepAt(0.0, sweepLength)
        val a = logSweepAt(direct.toDouble(), sweepLength)
        val b = logSweepAt(reflection.toDouble(), sweepLength)
        val recorded = DoubleArray(sweepLength) { a[it] + 0.5 * b[it] }

        val ir = ImpulseResponse.deconvolve(reference, recorded)
        val arrival = ImpulseResponse.findArrival(ir, searchLength = sweepLength)

        assertThat(arrival.index).isEqualTo(direct)
        // 反射も残っていること（IR が直接音だけに潰れていない）
        assertThat(abs(ir[reflection])).isGreaterThan(abs(ir[direct]) * 0.3)
    }

    @Test
    fun `雑音が信号より大きい現場では相互相関の方が遅延を拾える`() {
        // 客入り中のホールのような、暗騒音がスイープを上回る条件。
        //
        // 相互相関は整合フィルタそのものなので、スイープの長さぶんの利得が
        // まるごと乗る。一方デコンボリューションはスペクトルを平坦にする過程で、
        // スイープのエネルギーが薄い帯域の雑音を持ち上げてしまい、同じ条件で破綻する。
        // ディレイ実測に相互相関を使っているのはこのため
        val delay = 700
        val reference = logSweepAt(0.0, sweepLength)
        val noisy = logSweepAt(delay.toDouble(), sweepLength).plusNoise(amplitude = 2.0)

        val byCorrelation = ImpulseResponse
            .estimateDelay(ImpulseResponse.crossCorrelation(reference, noisy), sampleRate)
        val byDeconvolution = ImpulseResponse
            .estimateDelay(ImpulseResponse.deconvolve(reference, noisy), sampleRate)

        assertThat(byCorrelation.delaySamples).isWithin(1.0).of(delay.toDouble())
        assertThat(byCorrelation.isReliable).isTrue()
        assertThat(byDeconvolution.isReliable).isFalse()
    }

    @Test
    fun `雑音が増えるほど信頼度は下がる`() {
        val reference = logSweepAt(0.0, sweepLength)
        val clean = logSweepAt(700.0, sweepLength)

        val confidences = listOf(0.0, 1.0, 4.0, 16.0).map { amplitude ->
            val recorded = if (amplitude == 0.0) clean else clean.plusNoise(amplitude)
            ImpulseResponse
                .estimateDelay(ImpulseResponse.crossCorrelation(reference, recorded), sampleRate)
                .confidenceDb
        }

        for (i in 1 until confidences.size) {
            assertThat(confidences[i]).isLessThan(confidences[i - 1])
        }
    }

    @Test
    fun `スイープが返ってこない測定は信頼できないと出る`() {
        // スピーカーから音が出ていない、マイクが繋がっていない、といった状況。
        // 雑音だけを解析しても最大値はそこそこ立つので、素朴にピーク対RMSで
        // 判定すると「測れた」と誤って出る。まぐれで立つ高さを基準にしてある
        val reference = logSweepAt(0.0, sweepLength)
        val noiseOnly = DoubleArray(sweepLength).plusNoise(amplitude = 0.3)

        val estimate = ImpulseResponse
            .estimateDelay(ImpulseResponse.deconvolve(reference, noiseOnly), sampleRate)

        assertThat(estimate.isReliable).isFalse()
    }

    @Test
    fun `きれいな測定は信頼できると判定される`() {
        val reference = logSweepAt(0.0, sweepLength)
        val recorded = logSweepAt(300.0, sweepLength)

        val estimate = ImpulseResponse.estimateDelay(
            ImpulseResponse.deconvolve(reference, recorded),
            sampleRate,
        )

        assertThat(estimate.isReliable).isTrue()
        assertThat(estimate.confidenceDb).isGreaterThan(30.0)
    }

    @Test
    fun `探索範囲の外は見にいかない`() {
        val reference = logSweepAt(0.0, sweepLength)
        val recorded = logSweepAt(20000.0, sweepLength)
        val ir = ImpulseResponse.deconvolve(reference, recorded)

        val estimate = ImpulseResponse.estimateDelay(ir, sampleRate, maxDelaySeconds = 0.1)

        assertThat(estimate.delaySamples).isLessThan(0.1 * sampleRate)
    }

    @Test
    fun `長すぎる信号は理由を示して拒否する`() {
        val tooLong = DoubleArray(1 shl 21)
        val result = runCatching { ImpulseResponse.deconvolve(tooLong, tooLong) }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).hasMessageThat().contains("長すぎる")
    }

    @Test
    fun `正規化するとピークが1になる`() {
        val ir = doubleArrayOf(0.1, -0.4, 0.2)

        val normalized = ImpulseResponse.normalize(ir)

        assertThat(normalized[1]).isWithin(1e-12).of(-1.0)
        assertThat(normalized[0]).isWithin(1e-12).of(0.25)
    }

    @Test
    fun `無音を渡しても落ちない`() {
        val silent = DoubleArray(1024)

        val normalized = ImpulseResponse.normalize(silent)

        assertThat(normalized.all { it == 0.0 }).isTrue()
    }
}
