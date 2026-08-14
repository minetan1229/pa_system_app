package com.patoolbox.core.dsp

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.roundToInt

/**
 * インパルス応答から残響時間を求める（Schroeder の逆方向積分）。
 *
 * 減衰カーブは
 *   E(t) = ∫[t..∞] h(τ)² dτ
 * で作る。h(t) をそのまま眺めるとノイズでギザギザして直線を引けないが、
 * 積分すると単調減少の滑らかな曲線になり、傾きを最小二乗で読める。
 *
 * ISO 3382 に沿って T20（-5〜-25dB）と T30（-5〜-35dB）を出す。
 * -5dB から始めるのは直接音の影響を外すため、下限を -25/-35 で止めるのは
 * 暗騒音に埋もれた部分を含めないため。どちらも 60dB 分に換算して表示する。
 */
object ReverbTime {

    /** 減衰が直線と見なせるかの目安。ISO 3382 の非直線性の考え方に近い。 */
    private const val RELIABLE_CORRELATION = 0.98

    /** 暗騒音の推定に使う末尾の割合。 */
    private const val NOISE_TAIL_RATIO = 0.1

    /** 打ち切り点の判定でノイズ床に対して確保する余裕（dB）。 */
    private const val TRUNCATION_MARGIN_DB = 5.0

    /** 打ち切り点探索のブロック長。 */
    private const val TRUNCATION_BLOCK_MS = 10.0

    data class DecayFit(
        /** 60dB 減衰に換算した残響時間（秒） */
        val rtSeconds: Double,
        val slopeDbPerSecond: Double,
        /** 減衰直線とのあてはまりの良さ。-1 に近いほど素直な減衰 */
        val correlation: Double,
    ) {
        val isReliable: Boolean get() = abs(correlation) >= RELIABLE_CORRELATION
    }

    data class Result(
        /** 初期減衰時間（0〜-10dB）。聴感上の響きの短さはこちらに近い */
        val edt: DecayFit?,
        val t20: DecayFit?,
        val t30: DecayFit?,
        /** 表示用に間引いた減衰カーブ（dB、先頭が 0dB） */
        val curveDb: DoubleArray,
        val curveStepSeconds: Double,
        /** IR のピークに対する暗騒音の高さ（dB、負の値） */
        val noiseFloorDb: Double,
        /** 積分を打ち切った時刻（秒） */
        val truncationSeconds: Double,
        /** 使える減衰の幅（dB）。35 未満なら T30 は出せない */
        val decayRangeDb: Double,
    ) {
        /** 表示に使う代表値。T30 → T20 → EDT の順に、出せたものを使う。 */
        val bestFit: DecayFit? get() = t30 ?: t20 ?: edt

        /** 代表値がどれか。画面に「T30」なのか「T20」なのかを出すため。 */
        val bestLabel: String?
            get() = when {
                t30 != null -> "T30"
                t20 != null -> "T20"
                edt != null -> "EDT"
                else -> null
            }
    }

    /**
     * @param startIndex 積分の開始位置。負なら直接音の到来位置を自動で探す
     * @param analysisLength IR のうち解析する長さ。デコンボリューション結果を
     *   そのまま渡すと後半に循環の折り返しが乗っているので、必ず切ること
     */
    fun analyze(
        ir: DoubleArray,
        sampleRate: Int,
        startIndex: Int = -1,
        analysisLength: Int = ir.size,
        curvePoints: Int = DEFAULT_CURVE_POINTS,
    ): Result {
        require(sampleRate > 0) { "サンプリング周波数が不正: $sampleRate" }
        val length = analysisLength.coerceIn(1, ir.size)
        val start = if (startIndex >= 0) {
            startIndex.coerceIn(0, length - 1)
        } else {
            ImpulseResponse.findArrival(ir, length).index
        }

        val squared = DoubleArray(length) { ir[it] * ir[it] }
        val noisePower = tailNoisePower(squared)
        val truncation = truncationIndex(squared, sampleRate, noisePower, start)

        // 打ち切った先に残っているはずのエネルギーを見積もって足し戻す。
        // 傾きの推定には打ち切り後の補正が要り、補正には傾きが要るので反復する。
        var curveDbFull = integrate(squared, start, truncation, tailEnergy = 0.0)
        var remaining = TAIL_ITERATIONS
        while (remaining-- > 0) {
            val slope = slopeOf(curveDbFull, sampleRate) ?: break
            val tailEnergy = estimateTailEnergy(squared, truncation, sampleRate, slope, noisePower)
            if (tailEnergy <= 0.0) break
            curveDbFull = integrate(squared, start, truncation, tailEnergy)
        }

        val peakSquare = squared.max()
        val noiseFloorDb = if (peakSquare <= 0.0 || noisePower <= 0.0) {
            Double.NEGATIVE_INFINITY
        } else {
            10.0 * log10(noisePower / peakSquare)
        }

        // 末尾補正が効いていればカーブは暗騒音のところで水平になる。
        // その水平になった高さが、この測定であてはめに使える幅の限界。
        val decayRangeDb = usableRangeDb(curveDbFull)

        return Result(
            edt = fit(curveDbFull, sampleRate, from = 0.0, to = -10.0),
            t20 = fit(curveDbFull, sampleRate, from = -5.0, to = -25.0),
            t30 = fit(curveDbFull, sampleRate, from = -5.0, to = -35.0),
            curveDb = decimate(curveDbFull, curvePoints),
            curveStepSeconds = curveDbFull.size.toDouble() /
                curvePoints.coerceAtLeast(1) / sampleRate,
            noiseFloorDb = noiseFloorDb,
            truncationSeconds = truncation.toDouble() / sampleRate,
            decayRangeDb = decayRangeDb,
        )
    }

    /**
     * オクターブバンドごとの残響時間。
     *
     * 帯域分割は時間反転して掛ける（[ButterworthBand.filterReversed]）。
     * フィルタ自身の尾が減衰に足されて、残響が長めに出るのを防ぐため。
     */
    fun analyzeBands(
        ir: DoubleArray,
        sampleRate: Int,
        bands: List<FrequencyBand> = OctaveBands.bands(BandResolution.FULL, 63.0, 8000.0),
        analysisLength: Int = ir.size,
    ): List<BandResult> = bands.map { band ->
        val cascade = ButterworthBand.bandPass(band, sampleRate)
        val filtered = ButterworthBand.filterReversed(
            ir.copyOf(analysisLength.coerceIn(1, ir.size)),
            cascade,
        )
        BandResult(band = band, result = analyze(filtered, sampleRate))
    }

    /**
     * 明瞭度 C50 / C80（ISO 3382）。
     *
     * 「[earlyMs] までに届いたエネルギー」と「それ以降のエネルギー」の比を dB で出す。
     * 直接音と初期反射は音を支えるが、遅れて届く音は前の音に被って濁らせる、という
     * 経験則を数値にしたもの。C50 は話し声の聞き取りやすさ、C80 は音楽の粒立ちに対応する。
     *
     * 目安として C50 が 0dB を下回る部屋は、拡声しても言葉が潰れる。
     * その場合に効くのは音量ではなく、スピーカーの指向を客席だけに当てることなので、
     * 数値が出ると打つ手が変わる。
     *
     * @param startIndex 直接音の到来位置。ここを時刻0として区切る
     */
    fun clarityDb(
        ir: DoubleArray,
        sampleRate: Int,
        earlyMs: Double,
        startIndex: Int = -1,
    ): Double? {
        val start = if (startIndex >= 0) {
            startIndex.coerceIn(0, ir.size - 1)
        } else {
            ImpulseResponse.findArrival(ir).index
        }
        val boundary = start + (earlyMs / 1000.0 * sampleRate).toInt()
        if (boundary >= ir.size - 1) return null

        var early = 0.0
        for (i in start until boundary) {
            early += ir[i] * ir[i]
        }
        var late = 0.0
        for (i in boundary until ir.size) {
            late += ir[i] * ir[i]
        }
        if (late <= 0.0 || early <= 0.0) return null
        return 10.0 * log10(early / late)
    }

    /**
     * 明瞭度 D50（ISO 3382）。全体のうち 50ms までに届く割合（%）。
     * C50 と同じことを別の尺度で言っているが、こちらの方が直感で読める。
     */
    fun definitionPercent(ir: DoubleArray, sampleRate: Int, startIndex: Int = -1): Double? {
        val c50 = clarityDb(ir, sampleRate, earlyMs = 50.0, startIndex = startIndex) ?: return null
        val ratio = dbToPower(c50)
        return ratio / (1.0 + ratio) * 100.0
    }

    data class BandResult(
        val band: FrequencyBand,
        val result: Result,
    )

    /**
     * 逆方向に積算して dB の減衰カーブにする。E[i] は「i 以降に残っているエネルギー」。
     *
     * @param tailEnergy 打ち切り点より先に残っているエネルギーの見積もり
     */
    private fun integrate(
        squared: DoubleArray,
        start: Int,
        truncation: Int,
        tailEnergy: Double,
    ): DoubleArray {
        val curve = DoubleArray(truncation - start + 1)
        var accumulated = tailEnergy
        for (i in truncation downTo start) {
            accumulated += squared[i]
            curve[i - start] = accumulated
        }

        val total = curve.firstOrNull() ?: 0.0
        return DoubleArray(curve.size) {
            if (total <= 0.0 || curve[it] <= 0.0) {
                Double.NEGATIVE_INFINITY
            } else {
                10.0 * log10(curve[it] / total)
            }
        }
    }

    /** 補正に使う傾き。T20 の区間が取れなければ EDT の区間で代用する。 */
    private fun slopeOf(curveDb: DoubleArray, sampleRate: Int): Double? =
        (fit(curveDb, sampleRate, from = -5.0, to = -25.0)
            ?: fit(curveDb, sampleRate, from = 0.0, to = -10.0))
            ?.slopeDbPerSecond

    /**
     * 打ち切り点より先に残っているエネルギー（Lundeby の末尾補正）。
     *
     * 補正しないと、積分が打ち切り点で尽きてカーブが最後に急降下する。
     * その急降下を減衰と読んでしまうため、残響時間は実際より **短く** 出る。
     * 暗騒音が高い測定ほど短めに出るという、直感と逆の外れ方をするので厄介。
     *
     * 減衰が exp(-a t) なら打ち切り以降の総和は（打ち切り点のパワー）/ a で近似できる。
     * a は直線あてはめの傾きから逆算する。
     */
    private fun estimateTailEnergy(
        squared: DoubleArray,
        truncation: Int,
        sampleRate: Int,
        slopeDbPerSecond: Double,
        noisePower: Double,
    ): Double {
        if (slopeDbPerSecond >= 0.0) return 0.0
        val decayPerSecond = -slopeDbPerSecond * LN_10 / 10.0
        if (decayPerSecond <= 0.0) return 0.0

        val window = (TRUNCATION_BLOCK_MS / 1000.0 * sampleRate).toInt().coerceAtLeast(1)
        val from = (truncation - window + 1).coerceAtLeast(0)
        var sum = 0.0
        for (i in from..truncation) {
            sum += squared[i]
        }
        // 打ち切り点のパワーには暗騒音が乗っているので、その分を差し引く
        val power = (sum / (truncation - from + 1) - noisePower).coerceAtLeast(0.0)
        return power * sampleRate / decayPerSecond
    }

    /**
     * 減衰カーブの [from] dB から [to] dB までを直線で近似し、60dB 換算にする。
     * 下限まで届いていなければ null（無理に外挿すると平気で2倍ずれる）。
     */
    private fun fit(
        curveDb: DoubleArray,
        sampleRate: Int,
        from: Double,
        to: Double,
    ): DecayFit? {
        val startIndex = firstIndexBelow(curveDb, from) ?: return null
        val endIndex = firstIndexBelow(curveDb, to) ?: return null
        if (endIndex - startIndex < MIN_FIT_POINTS) return null

        val count = endIndex - startIndex + 1
        val times = DoubleArray(count) { (startIndex + it).toDouble() / sampleRate }
        val levels = DoubleArray(count) { curveDb[startIndex + it] }
        val fit = linearFit(times, levels)
        if (fit.slope >= 0.0) return null

        return DecayFit(
            rtSeconds = -60.0 / fit.slope,
            slopeDbPerSecond = fit.slope,
            correlation = fit.correlation,
        )
    }

    /** カーブは単調減少なので、最初に [level] を下回る位置がそのまま交点。 */
    private fun firstIndexBelow(curveDb: DoubleArray, level: Double): Int? {
        for (i in curveDb.indices) {
            if (curveDb[i] <= level) return i
        }
        return null
    }

    /** 末尾から暗騒音のパワーを見積もる。 */
    private fun tailNoisePower(squared: DoubleArray): Double {
        val tailStart = (squared.size * (1.0 - NOISE_TAIL_RATIO)).toInt()
            .coerceIn(0, squared.size - 1)
        var sum = 0.0
        for (i in tailStart until squared.size) {
            sum += squared[i]
        }
        return sum / (squared.size - tailStart)
    }

    /**
     * 積分を打ち切る位置を探す（Lundeby 法の簡易版）。
     *
     * ノイズ床まで含めて積分すると、カーブの後半が水平になって傾きが緩み、
     * 残響時間が実際より長く出る。ブロック平均がノイズ床＋5dB を最後に上回った
     * ところで切る。反復による精密化はしていないが、切らない場合との差の方が
     * はるかに大きいので、まずここを押さえる。
     */
    private fun truncationIndex(
        squared: DoubleArray,
        sampleRate: Int,
        noisePower: Double,
        start: Int,
    ): Int {
        val last = squared.size - 1
        if (noisePower <= 0.0) return last

        val blockSize = (TRUNCATION_BLOCK_MS / 1000.0 * sampleRate).toInt().coerceAtLeast(1)
        val threshold = noisePower * dbToPower(TRUNCATION_MARGIN_DB)

        var blockStart = start
        var truncation = start
        while (blockStart <= last) {
            val blockEnd = (blockStart + blockSize - 1).coerceAtMost(last)
            var sum = 0.0
            for (i in blockStart..blockEnd) {
                sum += squared[i]
            }
            if (sum / (blockEnd - blockStart + 1) > threshold) truncation = blockEnd
            blockStart += blockSize
        }
        // 最低限の長さが無いと積分にならない
        return truncation.coerceAtLeast((start + blockSize).coerceAtMost(last))
    }

    /** -inf に落ちる直前までの落差。あてはめに使える幅の目安。 */
    private fun usableRangeDb(curveDb: DoubleArray): Double {
        var lowest = 0.0
        for (value in curveDb) {
            if (value.isFinite()) lowest = value else break
        }
        return -lowest
    }

    private fun decimate(values: DoubleArray, points: Int): DoubleArray {
        if (values.isEmpty()) return DoubleArray(0)
        val target = points.coerceIn(1, values.size)
        return DoubleArray(target) { i ->
            val index = (i.toDouble() * (values.size - 1) / (target - 1).coerceAtLeast(1))
                .roundToInt()
                .coerceIn(0, values.size - 1)
            values[index]
        }
    }

    private const val DEFAULT_CURVE_POINTS = 256

    /** これより短い区間で引いた直線は、傾きがノイズに支配される。 */
    private const val MIN_FIT_POINTS = 32

    /** 末尾補正の反復回数。3回で十分収束する（1回目でほぼ決まる）。 */
    private const val TAIL_ITERATIONS = 3

    private val LN_10 = kotlin.math.ln(10.0)
}
