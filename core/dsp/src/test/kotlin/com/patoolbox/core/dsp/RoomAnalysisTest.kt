package com.patoolbox.core.dsp

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * 部屋を丸ごと模擬して、スイープ1回から遅延・極性・残響が同時に読めることを確かめる。
 *
 * 既知のインパルス応答（直接音＋反射＋指数減衰の残響）を作り、スイープに畳み込んで
 * 「録音」を合成する。解析側はその応答を復元できていなければならない。
 */
class RoomAnalysisTest {

    private val sampleRate = TEST_SAMPLE_RATE
    private val sweepSamples = 24000 // 0.5 秒
    private val bands = OctaveBands.bands(BandResolution.FULL, 250.0, 2000.0)

    /**
     * 直接音・反射・残響を持つ部屋の応答。
     * @param inverted スピーカーが逆相で繋がっている状態
     */
    private fun roomIr(
        delaySamples: Int,
        rtSeconds: Double,
        lengthSamples: Int,
        inverted: Boolean = false,
        reflectionSamples: Int = 0,
        reflectionLevel: Double = 0.0,
    ): DoubleArray {
        val ir = DoubleArray(lengthSamples)
        val sign = if (inverted) -1.0 else 1.0
        ir[delaySamples] = sign
        if (reflectionLevel > 0.0) {
            ir[delaySamples + reflectionSamples] = sign * reflectionLevel
        }
        val tail = decayingNoise(rtSeconds, lengthSamples - delaySamples, sampleRate)
        for (i in tail.indices) {
            ir[delaySamples + i] += sign * tail[i] * 0.3
        }
        return ir
    }

    /** FFT による畳み込み。部屋を通した「録音」を作る。 */
    private fun convolve(signal: DoubleArray, ir: DoubleArray): DoubleArray {
        val n = nextPowerOfTwo(signal.size + ir.size)
        val fft = Fft(n)
        val ar = DoubleArray(n)
        val ai = DoubleArray(n)
        val br = DoubleArray(n)
        val bi = DoubleArray(n)
        signal.copyInto(ar)
        ir.copyInto(br)
        fft.transform(ar, ai)
        fft.transform(br, bi)
        for (i in 0 until n) {
            val re = ar[i] * br[i] - ai[i] * bi[i]
            val im = ar[i] * bi[i] + ai[i] * br[i]
            ar[i] = re
            ai[i] = im
        }
        fft.transform(ar, ai, inverse = true)
        return ar.copyOf(signal.size + ir.size)
    }

    private fun measure(ir: DoubleArray): RoomAnalysis.Result {
        val reference = logSweepAt(0.0, sweepSamples)
        return RoomAnalysis.analyze(
            reference = reference,
            recorded = convolve(reference, ir),
            sampleRate = sampleRate,
            analysisSeconds = 1.0,
            bands = bands,
        )
    }

    @Test
    fun `既知の部屋から遅延と残響が同時に出る`() {
        val result = measure(roomIr(delaySamples = 4800, rtSeconds = 0.5, lengthSamples = 48000))

        assertThat(result.delay.delaySamples).isWithin(1.0).of(4800.0)
        assertThat(result.delay.isReliable).isTrue()
        assertThat(result.reverb.t20).isNotNull()
        assertThat(result.reverb.t20!!.rtSeconds).isWithin(0.08).of(0.5)
    }

    @Test
    fun `正相のスピーカーは正相と出る`() {
        val result = measure(roomIr(delaySamples = 2400, rtSeconds = 0.4, lengthSamples = 48000))

        assertThat(result.polarity.polarity).isEqualTo(ImpulseResponse.Polarity.NORMAL)
    }

    @Test
    fun `逆相のスピーカーは逆相と出る`() {
        val result = measure(
            roomIr(delaySamples = 2400, rtSeconds = 0.4, lengthSamples = 48000, inverted = true),
        )

        assertThat(result.polarity.polarity).isEqualTo(ImpulseResponse.Polarity.INVERTED)
    }

    @Test
    fun `静かな部屋の遅延はデコンボリューションから読む`() {
        val result = measure(roomIr(delaySamples = 4800, rtSeconds = 0.5, lengthSamples = 48000))

        assertThat(result.delayMethod).isEqualTo(RoomAnalysis.DelayMethod.DECONVOLUTION)
    }

    @Test
    fun `暗騒音が大きいと相互相関に切り替わる`() {
        val reference = logSweepAt(0.0, sweepSamples)
        val ir = roomIr(delaySamples = 4800, rtSeconds = 0.5, lengthSamples = 48000)
        val recorded = convolve(reference, ir).plusNoise(amplitude = 0.5)

        val result = RoomAnalysis.analyze(
            reference = reference,
            recorded = recorded,
            sampleRate = sampleRate,
            analysisSeconds = 1.0,
            bands = bands,
        )

        assertThat(result.delayMethod).isEqualTo(RoomAnalysis.DelayMethod.CORRELATION)
        // 相互相関は残響で遅れ側に寄るが、それでも数ms以内には収まる
        assertThat(result.delay.delaySamples).isWithin(50.0).of(4800.0)
    }

    @Test
    fun `響きが短い部屋ほど明瞭度が高く出る`() {
        val dead = measure(roomIr(delaySamples = 2400, rtSeconds = 0.3, lengthSamples = 48000))
        val live = measure(roomIr(delaySamples = 2400, rtSeconds = 1.5, lengthSamples = 96000))

        assertThat(dead.clarityC50Db!!).isGreaterThan(live.clarityC50Db!!)
        assertThat(dead.definitionPercent!!).isGreaterThan(live.definitionPercent!!)
    }

    @Test
    fun `C80はC50以上になる`() {
        // 定義上の関係。区間を広げれば分子が増えて分母が減るので必ずこうなる
        val result = measure(roomIr(delaySamples = 2400, rtSeconds = 0.8, lengthSamples = 96000))

        assertThat(result.clarityC80Db!!).isAtLeast(result.clarityC50Db!!)
    }

    @Test
    fun `D50は0から100の範囲に収まる`() {
        val result = measure(roomIr(delaySamples = 2400, rtSeconds = 0.8, lengthSamples = 96000))

        assertThat(result.definitionPercent!!).isIn(com.google.common.collect.Range.closed(0.0, 100.0))
    }

    @Test
    fun `帯域ごとの残響時間が揃って出る`() {
        val result = measure(roomIr(delaySamples = 2400, rtSeconds = 0.6, lengthSamples = 48000))

        assertThat(result.bands).hasSize(bands.size)
        for (band in result.bands) {
            assertThat(band.result.t20).isNotNull()
            assertThat(band.result.t20!!.rtSeconds).isWithin(0.12).of(0.6)
        }
    }

    @Test
    fun `IR は正規化されて返る`() {
        val result = measure(roomIr(delaySamples = 2400, rtSeconds = 0.4, lengthSamples = 48000))

        assertThat(result.impulse.maxOf { kotlin.math.abs(it) }).isWithin(1e-9).of(1.0)
        assertThat(result.impulseOffset).isGreaterThan(0)
    }

    @Test
    fun `包絡は0dBを超えない`() {
        val result = measure(roomIr(delaySamples = 2400, rtSeconds = 0.4, lengthSamples = 48000))

        val envelope = RoomAnalysis.envelopeDb(result.impulse)
        assertThat(envelope.max()).isWithin(1e-9).of(0.0)
        assertThat(envelope.min()).isAtLeast(-60.0)
    }
}
