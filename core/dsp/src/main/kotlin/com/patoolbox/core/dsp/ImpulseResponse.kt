package com.patoolbox.core.dsp

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * インパルス応答（IR）の推定と、そこから読み取る遅延・極性。
 *
 * ディレイ実測・極性チェック・残響測定はすべて「まず IR を出す」ところが共通なので、
 * ここに1つだけ実装を置いて3つのツールで共有する。
 *
 * 測定信号はログスイープ（[LogSweepSource]）を想定しているが、
 * 実装は逆フィルタ方式ではなく **正則化つきの周波数領域デコンボリューション** にした。
 * Farina の時間反転フィルタは対数スイープ専用だが、こちらは参照信号が何であっても
 * （ピンクノイズでも、実際に出た音を録り直したものでも）同じ式で扱える。
 * 現場ではスイープを鳴らせない場面があり、そのとき手が無くなるのを避けたい。
 */
object ImpulseResponse {

    /**
     * FFT の上限。5秒スイープ＋5秒録音（48kHz）で 2^20 に収まる。
     * これを超える長さを渡されたら、黙って落ちるのではなく理由を出して止める。
     */
    private const val MAX_FFT_SIZE = 1 shl 21

    /** 参照信号の平均パワーに対する正則化項の比。 */
    private const val DEFAULT_REGULARIZATION = 1e-6

    /** 遅延の信頼度を測るとき、ピーク本体として除外する範囲。 */
    private const val PEAK_GUARD_MS = 1.0

    /**
     * 参照信号 [reference] と録音 [recorded] から IR を求める。
     *
     * IR = IFFT( REC * conj(REF) / (|REF|^2 + eps) )
     *
     * eps を入れているのは、スイープの帯域外（20Hz 以下、20kHz 以上）で |REF| が
     * ほぼ0になり、素直に割ると録音のノイズだけが莫大に増幅されるため。
     * eps は参照信号の平均パワーに比例させてあるので、入力レベルを変えても効き方は変わらない。
     *
     * @return 長さ 2のべき乗の IR。先頭が遅延0に対応する。
     *   末尾は循環畳み込みの折り返しが乗るので、必要な長さだけ切り出して使うこと。
     */
    fun deconvolve(
        reference: DoubleArray,
        recorded: DoubleArray,
        regularization: Double = DEFAULT_REGULARIZATION,
    ): DoubleArray {
        require(reference.isNotEmpty() && recorded.isNotEmpty()) { "空の信号は解析できない" }

        // 線形デコンボリューションにするため、両者の和より長い FFT を取る。
        // ここをケチると応答の後ろが先頭に巻き込まれ、遅延のピークが偽物になる。
        val n = nextPowerOfTwo(reference.size + recorded.size)
        require(n <= MAX_FFT_SIZE) {
            "信号が長すぎる（FFT $n 点）。スイープと録音を短くすること"
        }

        val fft = Fft(n)
        val refRe = DoubleArray(n)
        val refIm = DoubleArray(n)
        val recRe = DoubleArray(n)
        val recIm = DoubleArray(n)
        reference.copyInto(refRe)
        recorded.copyInto(recRe)

        fft.transform(refRe, refIm)
        fft.transform(recRe, recIm)

        var totalPower = 0.0
        for (i in 0 until n) {
            totalPower += refRe[i] * refRe[i] + refIm[i] * refIm[i]
        }
        val eps = regularization * (totalPower / n)

        for (i in 0 until n) {
            val ar = refRe[i]
            val ai = refIm[i]
            val br = recRe[i]
            val bi = recIm[i]
            val denominator = ar * ar + ai * ai + eps
            // REC * conj(REF) / (|REF|^2 + eps)
            recRe[i] = (br * ar + bi * ai) / denominator
            recIm[i] = (bi * ar - br * ai) / denominator
        }

        fft.transform(recRe, recIm, inverse = true)
        return recRe
    }

    /**
     * 相互相関。**遅延だけを知りたい場合はこちらを使う。**
     *
     * 周波数特性は平坦にならない（スイープの自己相関が残る）ので IR としては使えないが、
     * 雑音への強さが桁違いに違う。相互相関は整合フィルタそのもので、スイープの長さぶんの
     * 利得がまるごと乗る。デコンボリューションはスペクトルを平坦にする過程で、
     * スイープのエネルギーが薄い帯域の雑音を持ち上げてしまう。
     *
     * 客入り中のホールで暗騒音がスイープを上回るような条件だと、この差が
     * 「読める／読めない」の差になる（テストで固定してある）。
     */
    fun crossCorrelation(reference: DoubleArray, recorded: DoubleArray): DoubleArray {
        require(reference.isNotEmpty() && recorded.isNotEmpty()) { "空の信号は解析できない" }

        val n = nextPowerOfTwo(reference.size + recorded.size)
        require(n <= MAX_FFT_SIZE) { "信号が長すぎる（FFT $n 点）" }

        val fft = Fft(n)
        val refRe = DoubleArray(n)
        val refIm = DoubleArray(n)
        val recRe = DoubleArray(n)
        val recIm = DoubleArray(n)
        reference.copyInto(refRe)
        recorded.copyInto(recRe)

        fft.transform(refRe, refIm)
        fft.transform(recRe, recIm)

        for (i in 0 until n) {
            val ar = refRe[i]
            val ai = refIm[i]
            val br = recRe[i]
            val bi = recIm[i]
            recRe[i] = br * ar + bi * ai
            recIm[i] = bi * ar - br * ai
        }

        fft.transform(recRe, recIm, inverse = true)
        return recRe
    }

    data class Arrival(
        /** 絶対値が最大になったサンプル位置（整数） */
        val index: Int,
        /** 放物線補間したサンプル位置。遅延の計算にはこちらを使う */
        val position: Double,
        /** その位置の値。符号が極性を表す */
        val value: Double,
    )

    /**
     * 直接音の到来位置を探す。
     *
     * @param searchLength 先頭からこの長さだけを探索する。長い IR の後半にある
     *   反射や折り返しをピークと誤認しないように、既定では全体を見ない運用を想定。
     */
    fun findArrival(ir: DoubleArray, searchLength: Int = ir.size): Arrival {
        require(ir.isNotEmpty()) { "空の IR" }
        val limit = searchLength.coerceIn(1, ir.size)

        var peakIndex = 0
        var peakMagnitude = -1.0
        for (i in 0 until limit) {
            val magnitude = abs(ir[i])
            if (magnitude > peakMagnitude) {
                peakMagnitude = magnitude
                peakIndex = i
            }
        }

        // 補間は絶対値の山に対して行う。符号付きのままだと谷が山に化ける
        val magnitudes = DoubleArray(3)
        val position = if (peakIndex in 1 until ir.size - 1) {
            magnitudes[0] = abs(ir[peakIndex - 1])
            magnitudes[1] = abs(ir[peakIndex])
            magnitudes[2] = abs(ir[peakIndex + 1])
            peakIndex - 1 + parabolicPeak(magnitudes, 1)
        } else {
            peakIndex.toDouble()
        }

        return Arrival(index = peakIndex, position = position, value = ir[peakIndex])
    }

    data class DelayEstimate(
        val delaySamples: Double,
        val delayMs: Double,
        /**
         * ピークが「まぐれで立つ高さ」より何 dB 高いか。
         *
         * 単純な S/N ではない。信号が全く返ってこず雑音だけを解析しても、
         * サンプル数が多ければ最大値は平均の 4〜5 倍まで普通に届く
         * （N 個の正規乱数の最大値は概ね √(2 ln N) 倍）。その分をあらかじめ
         * 差し引いてあるので、0dB 付近なら「何も測れていない」と読める。
         */
        val confidenceDb: Double,
        val arrival: Arrival,
    ) {
        val isReliable: Boolean get() = confidenceDb >= RELIABLE_CONFIDENCE_DB

        companion object {
            const val RELIABLE_CONFIDENCE_DB = 10.0
        }
    }

    /**
     * IR から遅延を読む。
     *
     * ここで出るのは **端末の入出力レイテンシを含んだ** 値であることに注意。
     * 絶対値が要るなら別途ループバック校正で端末分を引く。一方、2地点の測定の
     * 差を取る使い方（メインとディレイスピーカーの時間差など）なら端末分は打ち消されるので、
     * 校正なしでそのまま使える。
     *
     * [maxDelaySeconds] を実際の遅延より短く切ると、範囲内の別の山を拾って
     * それらしい数字を返してしまう。既定値は余裕を持たせてあるが、狭めるときは
     * [DelayEstimate.confidenceDb] を必ず併せて見ること。
     */
    fun estimateDelay(
        ir: DoubleArray,
        sampleRate: Int,
        maxDelaySeconds: Double = 1.0,
    ): DelayEstimate {
        require(sampleRate > 0) { "サンプリング周波数が不正: $sampleRate" }
        val searchLength = (maxDelaySeconds * sampleRate).toInt().coerceIn(1, ir.size)
        val arrival = findArrival(ir, searchLength)

        val guard = (PEAK_GUARD_MS / 1000.0 * sampleRate).toInt().coerceAtLeast(1)
        var sumSquares = 0.0
        var count = 0
        for (i in 0 until searchLength) {
            if (abs(i - arrival.index) <= guard) continue
            sumSquares += ir[i] * ir[i]
            count++
        }
        val background = if (count > 0) sqrt(sumSquares / count) else 0.0
        // 雑音だけでもこの高さまでは山ができる。ここを基準にしないと
        // 「何も返ってきていない測定」まで信頼できると出てしまう
        val chanceLevel = background * sqrt(2.0 * ln(searchLength.toDouble().coerceAtLeast(2.0)))
        val confidenceDb = if (chanceLevel <= 0.0) {
            Double.POSITIVE_INFINITY
        } else {
            amplitudeToDb(abs(arrival.value)) - amplitudeToDb(chanceLevel)
        }

        return DelayEstimate(
            delaySamples = arrival.position,
            delayMs = arrival.position / sampleRate * 1000.0,
            confidenceDb = confidenceDb,
            arrival = arrival,
        )
    }

    enum class Polarity {
        /** 正極性。押し出しに対して正の圧力が出ている */
        NORMAL,

        /** 逆相 */
        INVERTED,

        /** 差が小さく判定できない */
        UNCERTAIN,
    }

    data class PolarityEstimate(
        val polarity: Polarity,
        /** 優勢な向きの山が、逆向きの山より何 dB 大きいか */
        val marginDb: Double,
        val positivePeak: Double,
        val negativePeak: Double,
        val arrivalIndex: Int,
    )

    /**
     * 極性を判定する。
     *
     * 到来点の前後だけを見て、正の山と負の山のどちらが大きいかで決める。
     * 窓を短く切るのは、部屋の反射が混ざると波形が対称に近づいて判定が鈍るため。
     *
     * スピーカーは最小位相系ではないので、逆相でも波形が単純に反転するとは限らない。
     * 差が小さいときに無理に断定せず [Polarity.UNCERTAIN] を返すのはそのためで、
     * 「分からない」と出す方が、外れた判定を出すより現場では役に立つ。
     */
    fun estimatePolarity(
        ir: DoubleArray,
        sampleRate: Int,
        windowMs: Double = 3.0,
        minMarginDb: Double = 2.0,
        searchLength: Int = ir.size,
    ): PolarityEstimate {
        require(ir.isNotEmpty()) { "空の IR" }
        val arrival = findArrival(ir, searchLength)

        val windowSamples = (windowMs / 1000.0 * sampleRate).toInt().coerceAtLeast(1)
        // 到来点より少し前から見る。立ち上がりの先頭が窓の外に落ちないように
        val from = (arrival.index - windowSamples / 4).coerceAtLeast(0)
        val to = (arrival.index + windowSamples).coerceAtMost(ir.size - 1)

        var positive = 0.0
        var negative = 0.0
        for (i in from..to) {
            val v = ir[i]
            if (v > positive) positive = v
            if (-v > negative) negative = -v
        }

        val marginDb = amplitudeToDb(maxOf(positive, negative)) -
            amplitudeToDb(minOf(positive, negative))
        val polarity = when {
            marginDb < minMarginDb -> Polarity.UNCERTAIN
            positive >= negative -> Polarity.NORMAL
            else -> Polarity.INVERTED
        }

        return PolarityEstimate(
            polarity = polarity,
            marginDb = marginDb,
            positivePeak = positive,
            negativePeak = negative,
            arrivalIndex = arrival.index,
        )
    }

    /** IR の絶対値の最大で正規化する。表示とバンド分析の前処理。 */
    fun normalize(ir: DoubleArray): DoubleArray {
        var peak = 0.0
        for (v in ir) {
            val magnitude = abs(v)
            if (magnitude > peak) peak = magnitude
        }
        if (peak <= 0.0) return ir.copyOf()
        return DoubleArray(ir.size) { ir[it] / peak }
    }
}
