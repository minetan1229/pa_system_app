package com.patoolbox.core.dsp

/**
 * 対数カラムの上で見つけた山。
 *
 * ハウリングの芽や電源ハムを「何Hzの何dB」と名指しするための最小単位。
 * 音名は付けない（付けるかどうかは表示側の都合なので [NoteNames] を呼んでもらう）。
 */
data class SpectrumPeak(
    val frequencyHz: Double,
    val levelDb: Double,
)

/**
 * 1フレームぶんの解析結果。
 *
 * 毎フレーム作り直す。使い回して中身だけ差し替えると、Compose 側が
 * 「同じ物なので描き直さなくてよい」と判断して画面が止まる。
 * 8k点・256カラムで 1回あたり約2KB、85ms に1回なので置き換えの方が安い。
 */
class SpectrumSnapshot(
    /** カラムごとのレベル（dB）。校正オフセット適用済み */
    val columnsDb: FloatArray,
    /** ピーク保持。保持していないときは空 */
    val peakHoldDb: FloatArray,
    /** 一番出ている成分の周波数。カラムではなく生のビンから読むので 1Hz 単位で当たる */
    val peakFrequencyHz: Double,
    val peakLevelDb: Double,
    /**
     * 全帯域の平均レベル（ブロードバンド）。
     *
     * 生のビンパワーの合計（Parseval で信号の平均二乗値と一致）を dB にしたもので、
     * 表示カラムの重なりに影響されない。大表示はこちらを主役にする——
     * 1本だけ飛び出た山を大きく見せると、実際にはさほど出ていない部屋でも
     * 「かなり出ている」と誤読されるため。
     */
    val overallLevelDb: Double,
    /** 上位の山。ハウリング対策で「どこを削るか」を決めるのに使う */
    val topPeaks: List<SpectrumPeak>,
)

/**
 * 波形フレーム → 対数周波数カラム（dB）まで。
 *
 * FFT アナライザと本番タイマーのモニタで同じものを使う。
 * 取り込み（[AudioCaptureEngine]）と表示の間に挟まる数値処理をここに集めてあるので、
 * Android を持ち込まずに JVM テストで理論値と突き合わせられる。
 *
 * 状態（平均のならし・ピーク保持）を持つので、**1インスタンスを1系統で使うこと**。
 */
class SpectrumPipeline(
    val sampleRate: Int,
    val fftSize: Int = SpectrumAnalyzer.DEFAULT_FFT_SIZE,
    val columns: Int = LogSpectrumMapper.DEFAULT_COLUMNS,
    val windowFunction: WindowFunction = WindowFunction.HANN,
) {
    private val analyzer = SpectrumAnalyzer(sampleRate, fftSize, windowFunction)
    private val mapper = LogSpectrumMapper(
        binCount = analyzer.binCount,
        binWidthHz = analyzer.binWidthHz,
        columns = columns,
    )

    /**
     * 50% オーバーラップ。点数を上げても更新が飛び飛びに見えないようにする。
     * オーディオスレッドから直接叩くので公開している（[FrameAccumulator.add] が inline）。
     */
    val accumulator = FrameAccumulator(fftSize, hopSize = fftSize / 2)

    private val scratch = DoubleArray(columns)
    private val smoothedPowers = DoubleArray(columns)
    private val peakPowers = DoubleArray(columns)
    /** 各カラムのピークを更新した時刻（[elapsedSeconds] 基準）。保持時間の期限切れ判定に使う */
    private val peakSetAtSeconds = DoubleArray(columns)
    /** analyze() を呼ぶたびに hopSeconds ずつ進める内部時計。
     * System 時刻を使わないのは、JVM テストで結果を決定的にするため */
    private var elapsedSeconds = 0.0
    private var hasFrame = false

    /** 各カラムの中心周波数。目盛りとカーソルの逆引きに使う */
    val frequencies: DoubleArray get() = mapper.frequencies

    val binWidthHz: Double get() = analyzer.binWidthHz

    /** 更新の間隔（秒）。スペクトログラムの時間目盛りに使う */
    val hopSeconds: Double get() = accumulator.hopSize.toDouble() / sampleRate

    /**
     * フレーム1つを解析する。
     *
     * @param averagingCoefficient 指数移動平均の係数（0<a<=1）。小さいほど落ち着く
     * @param offsetDb 校正オフセット。未校正なら 0 を渡す（表示は dBFS になる）
     * @param peakHold ピーク保持を返すかどうか
     * @param peakHoldSeconds ピークを保持する秒数。この秒数のあいだ更新が無いカラムは
     *   現在値まで下ろす。既定は無期限（値を渡さなければ今までどおり上がりっぱなし）
     */
    fun analyze(
        frame: FloatArray,
        smoothing: OctaveSmoothing = OctaveSmoothing.SIXTH,
        averagingCoefficient: Double = DEFAULT_AVERAGING,
        offsetDb: Double = 0.0,
        peakHold: Boolean = false,
        peakHoldSeconds: Double = Double.POSITIVE_INFINITY,
    ): SpectrumSnapshot {
        val spectrum = analyzer.powerSpectrum(frame)

        // ピーク周波数は表示カラムではなく生のビンから読む。
        // カラムに畳んだ後だと分解能がカラム幅（1オクターブの1/25程度）まで落ちる
        val peakBin = analyzer.peakBin(spectrum)
        val peakHz = analyzer.interpolatedPeakHz(spectrum, peakBin)
        val peakPower = analyzer.toneMeanSquareAround(spectrum, peakBin)
        // 全ビンの合計 = 信号の平均二乗値（SpectrumAnalyzer の正規化による）。
        // カラムに畳む前の生スペクトラムから取るので、対数カラムの重なりで水増しされない
        val overallPower = spectrum.sum()

        mapper.map(spectrum, smoothing, scratch)

        elapsedSeconds += hopSeconds
        for (i in scratch.indices) {
            smoothedPowers[i] = if (hasFrame) {
                smoothedPowers[i] + averagingCoefficient * (scratch[i] - smoothedPowers[i])
            } else {
                scratch[i]
            }
            when {
                smoothedPowers[i] > peakPowers[i] -> {
                    peakPowers[i] = smoothedPowers[i]
                    peakSetAtSeconds[i] = elapsedSeconds
                }
                // 保持時間を過ぎたら現在値まで下ろす。ここで下ろした値も
                // 次のフレームからまた「新しいピーク」として保持され直す
                elapsedSeconds - peakSetAtSeconds[i] >= peakHoldSeconds -> {
                    peakPowers[i] = smoothedPowers[i]
                    peakSetAtSeconds[i] = elapsedSeconds
                }
            }
        }
        hasFrame = true

        val columnsDb = FloatArray(columns) {
            (powerToDb(smoothedPowers[it]) + offsetDb).toFloat()
        }

        return SpectrumSnapshot(
            columnsDb = columnsDb,
            peakHoldDb = if (peakHold) {
                FloatArray(columns) { (powerToDb(peakPowers[it]) + offsetDb).toFloat() }
            } else {
                FloatArray(0)
            },
            peakFrequencyHz = peakHz,
            peakLevelDb = powerToDb(peakPower) + offsetDb,
            overallLevelDb = powerToDb(overallPower) + offsetDb,
            topPeaks = findTopPeaks(columnsDb),
        )
    }

    /** 表示カラムの周波数 → カラム番号。カーソルの逆引き用 */
    fun columnOf(frequencyHz: Double): Int = mapper.columnOf(frequencyHz)

    fun clearPeakHold() {
        peakPowers.fill(0.0)
        peakSetAtSeconds.fill(elapsedSeconds)
    }

    /** FFT 点数を変えたときなど、溜まっているものを全部捨てる */
    fun reset() {
        accumulator.reset()
        smoothedPowers.fill(0.0)
        peakPowers.fill(0.0)
        peakSetAtSeconds.fill(0.0)
        elapsedSeconds = 0.0
        hasFrame = false
    }

    /**
     * 上位の山を拾う。
     *
     * 単純に大きい順へ並べると、1つの山の肩が2位3位を占めて何も分からなくなる。
     * そこで「近傍で最大」かつ「肩から [PEAK_PROMINENCE_DB] 以上立っている」ものだけを
     * 候補にし、さらに 1/3 オクターブ以上離して選ぶ。
     * ハウリング対策で削る帯域は 1/3 オクターブ程度の幅で考えるので、
     * それより近い2本を別々に出しても打つ手は同じになる。
     */
    private fun findTopPeaks(columnsDb: FloatArray): List<SpectrumPeak> {
        val neighborhood = (mapper.columnsPerOctave / PEAK_NEIGHBORHOOD_OCTAVE_DIVISOR)
            .toInt()
            .coerceAtLeast(1)
        val separation = (mapper.columnsPerOctave / PEAK_SEPARATION_OCTAVE_DIVISOR)
            .toInt()
            .coerceAtLeast(1)

        val candidates = ArrayList<Int>()
        for (i in columnsDb.indices) {
            val level = columnsDb[i]
            if (!level.isFinite()) continue

            var isLocalMax = true
            var shoulder = Float.MAX_VALUE
            val from = (i - neighborhood).coerceAtLeast(0)
            val to = (i + neighborhood).coerceAtMost(columnsDb.size - 1)
            for (j in from..to) {
                if (j == i) continue
                if (columnsDb[j] > level) {
                    isLocalMax = false
                    break
                }
                if (columnsDb[j] < shoulder) shoulder = columnsDb[j]
            }
            if (!isLocalMax) continue
            if (shoulder != Float.MAX_VALUE && level - shoulder < PEAK_PROMINENCE_DB) continue
            candidates.add(i)
        }

        candidates.sortByDescending { columnsDb[it] }

        val chosen = ArrayList<Int>(MAX_TOP_PEAKS)
        for (index in candidates) {
            if (chosen.size == MAX_TOP_PEAKS) break
            if (chosen.any { kotlin.math.abs(it - index) < separation }) continue
            chosen.add(index)
        }

        return chosen.map {
            SpectrumPeak(
                frequencyHz = mapper.frequencies[it],
                levelDb = columnsDb[it].toDouble(),
            )
        }
    }

    companion object {
        const val DEFAULT_AVERAGING = 0.25

        /** 表に出す山の本数。現場で一度に打てる手はせいぜい3つ */
        const val MAX_TOP_PEAKS = 3

        /** 「近傍で最大」の近傍幅。1/6 オクターブ片側 */
        private const val PEAK_NEIGHBORHOOD_OCTAVE_DIVISOR = 6.0

        /** 選んだ山どうしの最低間隔。1/3 オクターブ */
        private const val PEAK_SEPARATION_OCTAVE_DIVISOR = 3.0

        /** これ以下しか立っていない凸凹は山として扱わない */
        private const val PEAK_PROMINENCE_DB = 3.0f
    }
}
