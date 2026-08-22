package com.patoolbox.core.dsp

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max

/**
 * ハウリング候補を時間方向に束ねる。
 *
 * [FeedbackDetector] が返すのは「いまこの瞬間に出ている成分」だけなので、
 * 消えたら画面から分からなくなる。ところが現場で潰したいのは
 * **鳴ったり消えたりしながら結局ずっと居座っている周波数**で、
 * 一瞬だけ立った成分（拍手・シンバル・ハウリングの芽）とは打つ手が違う。
 *
 * そこで検出を周波数で1本にまとめ、累計の鳴っていた時間・回数・最長連続を持たせる。
 * 「いま」ではなく「今日いちばん長く鳴っていたのはどれか」で並べ替えられるようにするのが目的。
 *
 * 時刻は呼び出し側から渡す（テストで時計を進められるようにするため）。
 * 単調増加であればよく、`SystemClock.elapsedRealtime()` を想定している。
 *
 * @param mergeRatio この比率以内の周波数は同じ発振とみなす。既定は約 1/12 オクターブ。
 *   [FeedbackDetector] の統合（1/6 オクターブ）より狭くしているのは、
 *   こちらは長時間の集計で、630Hz と 700Hz を混ぜると EQ に入れる値が濁るため
 * @param gapToleranceMs 途切れてもこの時間までは同じ一続きの発振として数える。
 *   歌の合間で一瞬止まるだけで「別の発振」に切り替わると回数が意味を失う
 * @param frameIntervalCapMs 1回の更新で足す時間の上限。取り込みが詰まって
 *   フレームが飛んだとき、その空白を「鳴っていた時間」に入れないための蓋
 */
class FeedbackTracker(
    val mergeRatio: Double = 1.06,
    val gapToleranceMs: Long = 1_500,
    val frameIntervalCapMs: Long = 500,
) {
    /**
     * 1つの周波数の履歴。
     *
     * @param frequencyHz 代表の周波数。突出量で重みを付けた平均（強く出たときの値を信じる）
     * @param totalRingingMs 累計で鳴っていた時間。並べ替えの主キー
     * @param longestRunMs 最長の連続時間。累計が同じでも、
     *   1回で30秒鳴ったものと1秒が30回は別の問題（前者は共振、後者は演者の動き）
     * @param episodes 何回に分けて鳴ったか
     */
    data class Track(
        val frequencyHz: Double,
        val lowestHz: Double,
        val highestHz: Double,
        val firstSeenMs: Long,
        val lastSeenMs: Long,
        val totalRingingMs: Long,
        val longestRunMs: Long,
        val currentRunMs: Long,
        val episodes: Int,
        val peakProminenceDb: Double,
        val lastProminenceDb: Double,
        val peakLevelDb: Double,
        val isActive: Boolean,
    ) {
        /** カットすべき 1/3 オクターブ帯域 */
        val bandLabel: String
            get() = THIRD_OCTAVE_BANDS
                .minByOrNull { abs(it.centerHz - frequencyHz) }
                ?.label
                .orEmpty()

        /** 音名。EQ の周波数を耳で確かめるときの手がかり */
        val noteName: String
            get() = NoteNames.fromFrequency(frequencyHz)?.displayName.orEmpty()

        /** ひと続きで長く鳴っていたか。定在波・機材の共振の疑いが濃い */
        val isSustained: Boolean
            get() = longestRunMs >= SUSTAINED_MS

        /** 周波数が動いているか。動くものはハウリングではなく楽器の可能性がある */
        val isDrifting: Boolean
            get() = lowestHz > 0.0 && highestHz / lowestHz > DRIFT_RATIO

        private companion object {
            val THIRD_OCTAVE_BANDS = OctaveBands.bands(BandResolution.THIRD)

            /** これ以上ひと続きで鳴っていたら「居座っている」扱いにする */
            const val SUSTAINED_MS = 3_000L

            /** 代表周波数がこの比率以上動いていたら「動いている」扱い（約 1/12 オクターブ） */
            const val DRIFT_RATIO = 1.06
        }
    }

    /** 一覧の並べ替え方。 */
    enum class Sort(val label: String) {
        /** 累計で長く鳴っているもの順。EQ で潰す相手を決めるための既定 */
        TOTAL_TIME("長い順"),

        /** 直近に鳴ったもの順。いま何が起きているかを追うとき */
        RECENT("いま順"),

        /** 突出量の大きい順。1発の派手さで並べる */
        PROMINENCE("突出順"),

        /** 周波数の低い順。卓の EQ に順番に入れるとき */
        FREQUENCY("周波数順"),
    }

    private class MutableTrack(
        var frequencyHz: Double,
        var lowestHz: Double,
        var highestHz: Double,
        val firstSeenMs: Long,
        var lastSeenMs: Long,
        var runStartMs: Long,
        var peakProminenceDb: Double,
        var lastProminenceDb: Double,
        var peakLevelDb: Double,
        /** 重み付き平均のための分子と分母。突出量を重みにする */
        var weightedSum: Double,
        var weightSum: Double,
        var totalRingingMs: Long = 0,
        var longestRunMs: Long = 0,
        var episodes: Int = 1,
    )

    private val tracks = mutableListOf<MutableTrack>()

    /** 記録を始めてからの経過時間の基準。null なら1度も更新していない */
    private var startedAtMs: Long? = null

    /**
     * 1フレームぶんの検出を取り込む。
     *
     * **候補が空のフレームも渡すこと。** 渡さないと鳴り止んだことが分からず、
     * 連続時間が伸び続ける。
     */
    fun update(candidates: List<FeedbackDetector.Candidate>, nowMs: Long) {
        if (startedAtMs == null) startedAtMs = nowMs

        for (candidate in candidates) {
            val existing = find(candidate.frequencyHz)
            if (existing == null) {
                tracks += MutableTrack(
                    frequencyHz = candidate.frequencyHz,
                    lowestHz = candidate.frequencyHz,
                    highestHz = candidate.frequencyHz,
                    firstSeenMs = nowMs,
                    lastSeenMs = nowMs,
                    runStartMs = nowMs,
                    peakProminenceDb = candidate.prominenceDb,
                    lastProminenceDb = candidate.prominenceDb,
                    peakLevelDb = candidate.levelDb,
                    weightedSum = candidate.frequencyHz * weightOf(candidate.prominenceDb),
                    weightSum = weightOf(candidate.prominenceDb),
                )
                continue
            }

            val gap = nowMs - existing.lastSeenMs
            if (gap > gapToleranceMs) {
                // 一度切れている。連続時間を確定して、次の回として数え直す
                existing.longestRunMs = max(
                    existing.longestRunMs,
                    existing.lastSeenMs - existing.runStartMs,
                )
                existing.runStartMs = nowMs
                existing.episodes++
            } else {
                existing.totalRingingMs += gap.coerceAtMost(frameIntervalCapMs)
            }

            existing.lastSeenMs = nowMs
            existing.lastProminenceDb = candidate.prominenceDb
            existing.peakProminenceDb = max(existing.peakProminenceDb, candidate.prominenceDb)
            existing.peakLevelDb = max(existing.peakLevelDb, candidate.levelDb)
            existing.lowestHz = minOf(existing.lowestHz, candidate.frequencyHz)
            existing.highestHz = maxOf(existing.highestHz, candidate.frequencyHz)

            val weight = weightOf(candidate.prominenceDb)
            existing.weightedSum += candidate.frequencyHz * weight
            existing.weightSum += weight
            existing.frequencyHz = existing.weightedSum / existing.weightSum
        }

        // 鳴り止んだものの連続時間をここで確定させる。
        // 確定しておかないと、次に鳴り出すまで最長連続が更新されない
        for (track in tracks) {
            if (nowMs - track.lastSeenMs <= gapToleranceMs) continue
            track.longestRunMs = max(track.longestRunMs, track.lastSeenMs - track.runStartMs)
        }
    }

    /**
     * いまの一覧を返す。
     *
     * @param nowMs 現在時刻。これで「まだ鳴っているか」を判定する
     * @param minTotalMs これ未満しか鳴っていないものは出さない（いま鳴っているものは残す）。
     *   拍手や単発の衝撃音で一瞬だけ立った成分を一覧から落とすため
     */
    fun snapshot(
        nowMs: Long,
        sort: Sort = Sort.TOTAL_TIME,
        minTotalMs: Long = 0,
    ): List<Track> {
        val result = tracks
            .map { it.toTrack(nowMs) }
            .filter { it.totalRingingMs >= minTotalMs || it.isActive }

        return when (sort) {
            // 同じ累計なら最長連続が長い方を上に置く。潰す価値が高いのはそちら
            Sort.TOTAL_TIME -> result.sortedWith(
                compareByDescending<Track> { it.totalRingingMs }
                    .thenByDescending { it.longestRunMs },
            )

            Sort.RECENT -> result.sortedWith(
                compareByDescending<Track> { it.lastSeenMs }
                    .thenByDescending { it.totalRingingMs },
            )

            Sort.PROMINENCE -> result.sortedByDescending { it.peakProminenceDb }
            Sort.FREQUENCY -> result.sortedBy { it.frequencyHz }
        }
    }

    /** いま鳴っているものだけ。図に印を付けるのに使う */
    fun active(nowMs: Long): List<Track> =
        snapshot(nowMs, Sort.PROMINENCE).filter { it.isActive }

    /** 記録開始からの経過時間。「10分のうち4分鳴っていた」と読ませるための分母 */
    fun elapsedMs(nowMs: Long): Long = startedAtMs?.let { nowMs - it } ?: 0L

    val trackCount: Int get() = tracks.size

    fun reset() {
        tracks.clear()
        startedAtMs = null
    }

    private fun find(frequencyHz: Double): MutableTrack? = tracks
        .filter { matches(it.frequencyHz, frequencyHz) }
        // 複数に当たるときは一番近いものに寄せる。
        // 周波数がゆっくり動くと、隣り合う2本の許容範囲が重なることがある
        .minByOrNull { abs(ln(it.frequencyHz / frequencyHz)) }

    private fun matches(trackHz: Double, candidateHz: Double): Boolean {
        val ratio = if (candidateHz > trackHz) candidateHz / trackHz else trackHz / candidateHz
        return ratio <= mergeRatio
    }

    private fun MutableTrack.toTrack(nowMs: Long): Track {
        val active = nowMs - lastSeenMs <= gapToleranceMs
        val currentRun = if (active) nowMs - runStartMs else 0L
        return Track(
            frequencyHz = frequencyHz,
            lowestHz = lowestHz,
            highestHz = highestHz,
            firstSeenMs = firstSeenMs,
            lastSeenMs = lastSeenMs,
            totalRingingMs = totalRingingMs,
            longestRunMs = max(longestRunMs, currentRun),
            currentRunMs = currentRun,
            episodes = episodes,
            peakProminenceDb = peakProminenceDb,
            lastProminenceDb = lastProminenceDb,
            peakLevelDb = peakLevelDb,
            isActive = active,
        )
    }

    /** 重みは突出量。0dB 以下でも 1 を残して、平均が壊れないようにする */
    private fun weightOf(prominenceDb: Double): Double = prominenceDb.coerceAtLeast(1.0)
}
