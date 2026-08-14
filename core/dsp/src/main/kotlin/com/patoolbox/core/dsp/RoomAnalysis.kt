package com.patoolbox.core.dsp

import kotlin.math.abs
import kotlin.math.log10

/**
 * スイープ1回の測定から、ディレイ・極性・残響をまとめて出す。
 *
 * 3つのツール（ディレイ実測 / 極性チェック / 残響測定）は、現場では別々の道具でも
 * 測定行為は同じ「スイープを鳴らして録る」1回。1回の録音から全部読めるようにしてある。
 *
 * 遅延だけ [ImpulseResponse.crossCorrelation] で、極性と残響は
 * [ImpulseResponse.deconvolve] で求める。同じ録音でも問いによって使う変換が違う
 * ためで、理由はそれぞれの関数のコメントに書いてある。
 */
object RoomAnalysis {

    /** 遅延をどちらの変換から読んだか。 */
    enum class DelayMethod {
        /** デコンボリューション。到来位置が正確 */
        DECONVOLUTION,

        /** 相互相関。雑音には強いが、残響が多いと数サンプル遅れ側に寄る */
        CORRELATION,
    }

    data class Result(
        val delay: ImpulseResponse.DelayEstimate,
        val delayMethod: DelayMethod,
        val polarity: ImpulseResponse.PolarityEstimate,
        val reverb: ReverbTime.Result,
        val bands: List<ReverbTime.BandResult>,
        /** 表示用に切り出して正規化した IR */
        val impulse: DoubleArray,
        /** [impulse] の先頭が録音全体のどこか */
        val impulseOffset: Int,
        val sampleRate: Int,
        /** 録音のピーク（1.0 に達していたら過大入力） */
        val peakAmplitude: Double,
        /** C50。話し声の聞き取りやすさ */
        val clarityC50Db: Double?,
        /** C80。音楽の粒立ち */
        val clarityC80Db: Double?,
        /** D50。全体のうち 50ms までに届く割合（%） */
        val definitionPercent: Double?,
    ) {
        val clipped: Boolean get() = peakAmplitude >= CLIP_THRESHOLD
    }

    /**
     * @param analysisSeconds IR のうち解析に使う長さ。残響がこれより長い部屋では
     *   伸ばす必要がある。既定の3秒はライブハウス〜中ホールを想定
     */
    fun analyze(
        reference: DoubleArray,
        recorded: DoubleArray,
        sampleRate: Int,
        analysisSeconds: Double = DEFAULT_ANALYSIS_SECONDS,
        bands: List<FrequencyBand> = OctaveBands.bands(BandResolution.FULL, 125.0, 8000.0),
    ): Result {
        require(sampleRate > 0) { "サンプリング周波数が不正: $sampleRate" }

        val maxDelaySeconds = recorded.size.toDouble() / sampleRate
        val ir = ImpulseResponse.deconvolve(reference, recorded)
        val arrival = ImpulseResponse.findArrival(ir, searchLength = recorded.size)

        // 遅延はまずデコンボリューションから読む。到来位置が正確なため。
        // ただし雑音に弱く、暗騒音が大きい現場では山が埋もれる。そのときだけ
        // 相互相関に落とす。相互相関は残響で数サンプル遅れ側に寄るので、
        // 静かな部屋でこちらを常用すると測定が甘くなる
        val byDeconvolution = ImpulseResponse.estimateDelay(ir, sampleRate, maxDelaySeconds)
        val delay: ImpulseResponse.DelayEstimate
        val delayMethod: DelayMethod
        if (byDeconvolution.isReliable) {
            delay = byDeconvolution
            delayMethod = DelayMethod.DECONVOLUTION
        } else {
            delay = ImpulseResponse.estimateDelay(
                ImpulseResponse.crossCorrelation(reference, recorded),
                sampleRate,
                maxDelaySeconds,
            )
            delayMethod = DelayMethod.CORRELATION
        }

        // 直接音の少し手前から切り出す。立ち上がりを削ると極性の判定が鈍る
        val preRoll = (PRE_ROLL_MS / 1000.0 * sampleRate).toInt()
        val offset = (arrival.index - preRoll).coerceAtLeast(0)
        val length = (analysisSeconds * sampleRate).toInt()
            .coerceAtMost(ir.size - offset)
        val trimmed = DoubleArray(length) { ir[offset + it] }

        val localArrival = arrival.index - offset
        val polarity = ImpulseResponse.estimatePolarity(trimmed, sampleRate)
        val reverb = ReverbTime.analyze(trimmed, sampleRate)
        val bandResults = ReverbTime.analyzeBands(trimmed, sampleRate, bands)

        var peak = 0.0
        for (v in recorded) {
            val magnitude = abs(v)
            if (magnitude > peak) peak = magnitude
        }

        return Result(
            delay = delay,
            delayMethod = delayMethod,
            polarity = polarity,
            reverb = reverb,
            bands = bandResults,
            impulse = ImpulseResponse.normalize(trimmed),
            impulseOffset = offset,
            sampleRate = sampleRate,
            peakAmplitude = peak,
            clarityC50Db = ReverbTime.clarityDb(trimmed, sampleRate, 50.0, localArrival),
            clarityC80Db = ReverbTime.clarityDb(trimmed, sampleRate, 80.0, localArrival),
            definitionPercent = ReverbTime.definitionPercent(trimmed, sampleRate, localArrival),
        )
    }

    /**
     * 表示用の dB 変換。
     *
     * 単発の反射を自動で拾って「壁まで何m」と出すことは意図的にしていない。
     * 拡散した残響の中から離散的な反射だけを機械的に切り分けるのは、
     * 部屋によって当たり外れが大きく、外したときに現場を誤らせる。
     * この包絡を出して読んでもらう方が確実で、目で見れば一目で分かる。
     */
    fun envelopeDb(ir: DoubleArray, floorDb: Double = -60.0): DoubleArray {
        var peak = 0.0
        for (v in ir) {
            val magnitude = abs(v)
            if (magnitude > peak) peak = magnitude
        }
        if (peak <= 0.0) return DoubleArray(ir.size) { floorDb }
        return DoubleArray(ir.size) {
            val magnitude = abs(ir[it])
            if (magnitude <= 0.0) floorDb
            else (20.0 * log10(magnitude / peak)).coerceAtLeast(floorDb)
        }
    }

    private const val DEFAULT_ANALYSIS_SECONDS = 3.0
    private const val PRE_ROLL_MS = 5.0

    /** これ以上でクリップとみなす。Float 録音なので 1.0 が上限 */
    const val CLIP_THRESHOLD = 0.99
}
