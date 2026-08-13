package com.patoolbox.core.dsp

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * ハウリング（発振）している周波数の検出。
 *
 * 「周りより突出していて、かつ鳴り続けている」成分を探す。
 * 楽器の音も突出はするが、音程が変わったり止まったりするので、
 * 継続フレーム数を条件に入れることで区別している。
 */
class FeedbackDetector(
    val sampleRate: Int,
    val fftSize: Int = SpectrumAnalyzer.DEFAULT_FFT_SIZE,
    val minFrequencyHz: Double = 80.0,
    val maxFrequencyHz: Double = 10_000.0,
    /** 周囲より何dB突出していたら候補にするか */
    val prominenceThresholdDb: Double = 12.0,
    /** 何フレーム続いたら「鳴り続けている」とみなすか */
    val sustainFrames: Int = 4,
) {
    data class Candidate(
        val frequencyHz: Double,
        /** その成分のレベル（dBFS。校正オフセットは掛かっていない） */
        val levelDb: Double,
        /** 周囲の平均からどれだけ突出しているか */
        val prominenceDb: Double,
        /** 連続して検出されているフレーム数 */
        val sustainedFrames: Int,
    ) {
        /** カットすべき 1/3 オクターブ帯域 */
        val bandLabel: String
            get() = THIRD_OCTAVE_BANDS
                .minByOrNull { kotlin.math.abs(it.centerHz - frequencyHz) }
                ?.label
                .orEmpty()

        /** 音名。EQ の周波数を耳で確かめるときの手がかり */
        val noteName: String
            get() = NoteNames.fromFrequency(frequencyHz)?.displayName.orEmpty()

        private companion object {
            val THIRD_OCTAVE_BANDS = OctaveBands.bands(BandResolution.THIRD)
        }
    }

    private val analyzer = SpectrumAnalyzer(sampleRate, fftSize)
    private val sustainCounters = IntArray(analyzer.binCount)
    private val minBin = (minFrequencyHz / analyzer.binWidthHz).toInt().coerceAtLeast(1)
    private val maxBin = (maxFrequencyHz / analyzer.binWidthHz).toInt()
        .coerceAtMost(analyzer.binCount - 2)

    /**
     * 1フレーム解析し、現在ハウっていると判断した成分を突出量の大きい順に返す。
     */
    fun process(frame: FloatArray): List<Candidate> {
        val spectrum = analyzer.powerSpectrum(frame)
        val found = mutableListOf<Candidate>()

        for (bin in minBin..maxBin) {
            val prominenceDb = prominenceAt(spectrum, bin)
            val isPeak = spectrum[bin] >= spectrum[bin - 1] && spectrum[bin] >= spectrum[bin + 1]

            if (isPeak && prominenceDb >= prominenceThresholdDb) {
                sustainCounters[bin]++
            } else {
                // 一度外しただけで0に戻すと、わずかな揺れで検出が途切れる
                sustainCounters[bin] = max(0, sustainCounters[bin] - 1)
            }

            if (sustainCounters[bin] >= sustainFrames) {
                found += Candidate(
                    frequencyHz = analyzer.interpolatedPeakHz(spectrum, bin),
                    levelDb = powerToDb(analyzer.toneMeanSquareAround(spectrum, bin)),
                    prominenceDb = prominenceDb,
                    sustainedFrames = sustainCounters[bin],
                )
            }
        }

        return mergeNeighbours(found).sortedByDescending { it.prominenceDb }
    }

    fun reset() {
        sustainCounters.fill(0)
    }

    /**
     * そのビンが周囲と比べてどれだけ突出しているか（dB）。
     *
     * 比較する「周囲」は対数軸で取る。低域と高域で同じビン数を見ると、
     * 低域では広すぎ・高域では狭すぎになるため。
     */
    private fun prominenceAt(spectrum: DoubleArray, bin: Int): Double {
        val halfWidth = max(MIN_NEIGHBOUR_BINS, (bin * NEIGHBOUR_RATIO).roundToInt())
        val from = (bin - halfWidth).coerceAtLeast(1)
        val to = (bin + halfWidth).coerceAtMost(analyzer.binCount - 1)

        var sum = 0.0
        var count = 0
        for (i in from..to) {
            // ピーク本体は平均から除く（自分自身で薄めてしまわないように）
            if (kotlin.math.abs(i - bin) <= EXCLUDE_BINS) continue
            sum += spectrum[i]
            count++
        }
        if (count == 0) return 0.0

        return powerToDb(spectrum[bin]) - powerToDb(sum / count)
    }

    /**
     * 隣接ビンに分かれた同じ発振をまとめる。
     * 窓の広がりで1つの発振が2〜3ビンにまたがるため。
     */
    private fun mergeNeighbours(candidates: List<Candidate>): List<Candidate> {
        if (candidates.isEmpty()) return candidates

        val merged = mutableListOf<Candidate>()
        var best = candidates.first()

        for (candidate in candidates.drop(1)) {
            val ratio = candidate.frequencyHz / best.frequencyHz
            if (ratio <= MERGE_RATIO) {
                if (candidate.prominenceDb > best.prominenceDb) best = candidate
            } else {
                merged += best
                best = candidate
            }
        }
        merged += best

        return merged
    }

    private companion object {
        /** 周囲を見る幅（ビン数に対する比）。約 1/6 オクターブ相当 */
        const val NEIGHBOUR_RATIO = 0.12

        const val MIN_NEIGHBOUR_BINS = 4

        /** ピーク本体とみなして平均から除くビン数 */
        const val EXCLUDE_BINS = 2

        /** この比率以内の候補は同じ発振とみなす（約 1/6 オクターブ） */
        const val MERGE_RATIO = 1.12
    }
}
