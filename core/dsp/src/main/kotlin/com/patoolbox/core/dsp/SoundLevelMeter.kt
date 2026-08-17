package com.patoolbox.core.dsp

import kotlin.math.abs
import kotlin.math.exp

/** 時間重み付け。IEC 61672-1 の時定数。 */
enum class TimeWeighting(val label: String, val timeConstantSeconds: Double) {
    /** Fast: 125ms。通常の測定 */
    FAST("F", 0.125),

    /** Slow: 1s。値が暴れるときの平均的な把握 */
    SLOW("S", 1.0),

    /** Impulse: 35ms。打撃音の立ち上がり */
    IMPULSE("I", 0.035),
}

/**
 * 騒音計の本体。
 *
 * 1サンプルごとに重み付けフィルタを通し、二乗を指数平均する。
 * dB への変換はブロックの最後に1回だけ行う（毎サンプル log10 を呼ばないため）。
 * 内部バッファを持たず入力も書き換えないので、呼び出し側の配列は無傷のまま使える。
 */
class SoundLevelMeter(
    val sampleRate: Int,
    val frequencyWeighting: FrequencyWeighting = FrequencyWeighting.A,
    val timeWeighting: TimeWeighting = TimeWeighting.FAST,
    var calibrationOffsetDb: Double = DEFAULT_CALIBRATION_OFFSET_DB,
) {
    private val filter = WeightingFilter.create(frequencyWeighting, sampleRate)
    private val alpha = 1.0 - exp(-1.0 / (timeWeighting.timeConstantSeconds * sampleRate))

    private var smoothedPower = 0.0
    private var maxPower = 0.0
    private var minPower = Double.MAX_VALUE
    private var peakPower = 0.0
    private var leqSum = 0.0
    private var leqSamples = 0L
    private var totalSamples = 0L
    private var clippedSamples = 0L

    /** 統計レベル（L10/L50/L90）用のヒストグラム。0.5dB 刻み */
    private val histogram = IntArray(HISTOGRAM_BINS)
    private val histogramInterval = (sampleRate * HISTOGRAM_INTERVAL_SECONDS).toInt()
    private var histogramSamples = 0L
    private var samplesUntilHistogram = 0

    /**
     * 表示を落ち着かせるための移動平均。
     *
     * [SLIDING_SLOT_SECONDS] ごとの二乗和を輪にためておき、
     * 欲しい窓の長さぶんだけ後ろから足して平均する。
     * こうしておくと窓を変えても測り直しにならない
     * （時間重み付けを変えるとフィルタの積分が飛ぶが、こちらは表示だけの話なので）。
     */
    private val slotSamples = (sampleRate * SLIDING_SLOT_SECONDS).toInt().coerceAtLeast(1)
    private val slotPowerSums = DoubleArray(SLIDING_SLOTS)
    private val slotCounts = IntArray(SLIDING_SLOTS)
    private var slotIndex = 0
    private var filledSlots = 0
    private var currentSlotSum = 0.0
    private var currentSlotCount = 0

    data class Reading(
        /** 時間重み付けされた現在値 */
        val instantDb: Double,
        /** 測定開始からの等価騒音レベル */
        val leqDb: Double,
        val maxDb: Double,
        val minDb: Double,
        /** 重み付け後のピーク値（時間重み付けなし） */
        val peakDb: Double,
        val elapsedSeconds: Double,
        /** 過大入力でクリップしたサンプルがあったか */
        val clipped: Boolean,
    )

    /**
     * ブロックを処理して現在の読みを返す。
     * @param length 有効サンプル数
     */
    fun process(buffer: FloatArray, length: Int = buffer.size): Reading {
        // 立ち上がり中は最小値の追跡を始めない（0 から始まるので必ず0dBが最小になってしまう）
        val settleSamples = (timeWeighting.timeConstantSeconds * sampleRate * SETTLE_TIME_CONSTANTS)

        for (i in 0 until length) {
            val raw = buffer[i].toDouble()
            if (abs(raw) >= CLIP_THRESHOLD) clippedSamples++

            val weighted = filter.process(raw)
            val power = weighted * weighted

            leqSum += power
            leqSamples++
            totalSamples++

            currentSlotSum += power
            if (++currentSlotCount >= slotSamples) {
                slotPowerSums[slotIndex] = currentSlotSum
                slotCounts[slotIndex] = currentSlotCount
                slotIndex = (slotIndex + 1) % SLIDING_SLOTS
                if (filledSlots < SLIDING_SLOTS) filledSlots++
                currentSlotSum = 0.0
                currentSlotCount = 0
            }

            smoothedPower += alpha * (power - smoothedPower)

            if (totalSamples.toDouble() > settleSamples) {
                // ピークも重み付けフィルタの立ち上がりが落ち着いてから追う。
                // 起動直後のフィルタのリンギングを「音のピーク」と誤って記録しないため
                // （測定開始から約 0.4 秒はピークを拾わない）
                if (power > peakPower) peakPower = power

                if (smoothedPower > maxPower) maxPower = smoothedPower
                if (smoothedPower < minPower) minPower = smoothedPower

                if (samplesUntilHistogram <= 0) {
                    addToHistogram(powerToDb(smoothedPower) + calibrationOffsetDb)
                    samplesUntilHistogram = histogramInterval
                }
                samplesUntilHistogram--
            }
        }

        return Reading(
            instantDb = powerToDb(smoothedPower) + calibrationOffsetDb,
            leqDb = if (leqSamples == 0L) {
                Double.NEGATIVE_INFINITY
            } else {
                powerToDb(leqSum / leqSamples) + calibrationOffsetDb
            },
            maxDb = powerToDb(maxPower) + calibrationOffsetDb,
            minDb = if (minPower == Double.MAX_VALUE) {
                Double.NEGATIVE_INFINITY
            } else {
                powerToDb(minPower) + calibrationOffsetDb
            },
            // ピークは振幅なので 20log10。power に二乗を入れてあるので powerToDb でよい
            peakDb = powerToDb(peakPower) + calibrationOffsetDb,
            elapsedSeconds = totalSamples.toDouble() / sampleRate,
            clipped = clippedSamples > 0,
        )
    }

    /**
     * 直近 [windowSeconds] の等価レベル（移動平均）。
     *
     * 時間重み付け（F/S/I）は規格で決まっていて動かせないが、
     * F のままだと大表示の数字が毎秒何度も跳ねて読めない。
     * これは**表示のためだけ**の平均で、Leq や Lmax には影響しない。
     *
     * ためた長さが窓に足りないときは、ある分だけで平均する
     * （測定開始直後に「値なし」を出すより、荒い値でも出した方が使える）。
     *
     * @param windowSeconds 0 以下なら [Reading.instantDb] と同じ扱いにはせず、直近1スロットを返す
     */
    fun slidingLeqDb(windowSeconds: Double): Double {
        val wanted = kotlin.math.ceil(windowSeconds / SLIDING_SLOT_SECONDS)
            .toInt()
            .coerceIn(1, SLIDING_SLOTS)

        var sum = currentSlotSum
        var count = currentSlotCount.toLong()

        var taken = 0
        while (taken < wanted && taken < filledSlots) {
            // slotIndex は次に書く位置なので、その1つ手前が最新
            val index = (slotIndex - 1 - taken + SLIDING_SLOTS * 2) % SLIDING_SLOTS
            sum += slotPowerSums[index]
            count += slotCounts[index]
            taken++
        }

        if (count == 0L) return Double.NEGATIVE_INFINITY
        return powerToDb(sum / count) + calibrationOffsetDb
    }

    /**
     * 統計レベル。L10 なら「測定時間の10%を超えていたレベル」。
     * @param percentile 0..100
     */
    fun percentileDb(percentile: Double): Double {
        if (histogramSamples == 0L) return Double.NEGATIVE_INFINITY
        // L10 は上位10%の境界なので、高い方から数える
        val target = histogramSamples * (percentile / 100.0)
        var counted = 0L
        for (bin in HISTOGRAM_BINS - 1 downTo 0) {
            counted += histogram[bin]
            if (counted >= target) return binToDb(bin)
        }
        return binToDb(0)
    }

    fun reset() {
        filter.reset()
        smoothedPower = 0.0
        maxPower = 0.0
        minPower = Double.MAX_VALUE
        peakPower = 0.0
        leqSum = 0.0
        leqSamples = 0L
        totalSamples = 0L
        clippedSamples = 0L
        histogram.fill(0)
        histogramSamples = 0L
        samplesUntilHistogram = 0
        slotPowerSums.fill(0.0)
        slotCounts.fill(0)
        slotIndex = 0
        filledSlots = 0
        currentSlotSum = 0.0
        currentSlotCount = 0
    }

    private fun addToHistogram(db: Double) {
        val bin = ((db - HISTOGRAM_MIN_DB) / HISTOGRAM_BIN_WIDTH_DB).toInt()
        if (bin in 0 until HISTOGRAM_BINS) {
            histogram[bin]++
            histogramSamples++
        }
    }

    private fun binToDb(bin: Int): Double =
        HISTOGRAM_MIN_DB + (bin + 0.5) * HISTOGRAM_BIN_WIDTH_DB

    companion object {
        /**
         * 未校正時の暫定オフセット（0 dBFS を何 dB SPL とみなすか）。
         * 端末ごとに10dB以上違うので、必ず校正して置き換える前提の仮値。
         */
        const val DEFAULT_CALIBRATION_OFFSET_DB = 120.0

        /** これ以上の絶対値はADCが飽和しているとみなす */
        const val CLIP_THRESHOLD = 0.999

        /** 指数平均が落ち着くまで待つ時定数の本数 */
        private const val SETTLE_TIME_CONSTANTS = 3.0

        // 未校正（dBFS のまま）でも統計が取れるよう、負の値まで範囲に入れている
        private const val HISTOGRAM_MIN_DB = -60.0
        private const val HISTOGRAM_BIN_WIDTH_DB = 0.5
        private const val HISTOGRAM_BINS = 480
        private const val HISTOGRAM_INTERVAL_SECONDS = 0.125

        /**
         * 移動平均の刻み。これより短い窓は作れない。
         * 50ms にしてあるのは、0.5秒を10個で割り切れて、
         * かつ画面の更新間隔（約43ms）とほぼ揃うため
         */
        const val SLIDING_SLOT_SECONDS = 0.05

        /** ためておく刻みの数。50ms × 40 で最長 2 秒 */
        const val SLIDING_SLOTS = 40

        /** [slidingLeqDb] に渡せる最長の窓（秒） */
        const val MAX_SLIDING_WINDOW_SECONDS = SLIDING_SLOT_SECONDS * SLIDING_SLOTS
    }
}
