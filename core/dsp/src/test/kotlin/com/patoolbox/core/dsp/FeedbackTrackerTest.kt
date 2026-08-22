package com.patoolbox.core.dsp

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * 時間の集計が本題なので、検出器は通さず候補を直接組んで流し込む。
 * こうしないと「20分の本番で3分鳴っていた」のような場面をテストにできない。
 */
class FeedbackTrackerTest {

    private fun candidate(hz: Double, prominenceDb: Double = 20.0) =
        FeedbackDetector.Candidate(
            frequencyHz = hz,
            levelDb = -20.0,
            prominenceDb = prominenceDb,
            sustainedFrames = 4,
        )

    /** [stepMs] ごとに [frames] 回、同じ候補を流す。戻り値は最後の時刻 */
    private fun FeedbackTracker.ring(
        hz: Double,
        frames: Int,
        fromMs: Long,
        stepMs: Long = 100,
        prominenceDb: Double = 20.0,
    ): Long {
        var now = fromMs
        repeat(frames) {
            update(listOf(candidate(hz, prominenceDb)), now)
            now += stepMs
        }
        return now - stepMs
    }

    @Test
    fun `鳴り続けた時間が累計される`() {
        val tracker = FeedbackTracker()
        // 100ms 間隔で11回 = 1000ms 鳴っている
        val last = tracker.ring(hz = 1000.0, frames = 11, fromMs = 0)

        val track = tracker.snapshot(last).single()

        assertThat(track.totalRingingMs).isEqualTo(1000)
        assertThat(track.episodes).isEqualTo(1)
        assertThat(track.isActive).isTrue()
    }

    @Test
    fun `一瞬だけの成分より長く鳴ったものが上に来る`() {
        val tracker = FeedbackTracker()
        // 8kHz は1フレームだけ（シンバルや拍手の想定）
        tracker.update(listOf(candidate(8000.0, prominenceDb = 30.0)), 0)
        // 1kHz は2秒鳴り続ける
        val last = tracker.ring(hz = 1000.0, frames = 21, fromMs = 0)

        val ordered = tracker.snapshot(last, FeedbackTracker.Sort.TOTAL_TIME)

        assertThat(ordered.first().frequencyHz).isWithin(5.0).of(1000.0)
        assertThat(ordered.first().totalRingingMs).isEqualTo(2000)
        // 突出量では 8kHz が勝つ。並べ替えで結論が変わることを押さえておく
        assertThat(tracker.snapshot(last, FeedbackTracker.Sort.PROMINENCE).first().frequencyHz)
            .isWithin(50.0).of(8000.0)
    }

    @Test
    fun `途切れて鳴り直すと回数が増える`() {
        val tracker = FeedbackTracker(gapToleranceMs = 1_000)
        tracker.ring(hz = 500.0, frames = 6, fromMs = 0) // 0〜500ms
        // 5秒空けてから鳴り直す
        val last = tracker.ring(hz = 500.0, frames = 6, fromMs = 5_500)

        val track = tracker.snapshot(last).single()

        assertThat(track.episodes).isEqualTo(2)
        // 空白は累計に入れない
        assertThat(track.totalRingingMs).isEqualTo(1000)
        assertThat(track.longestRunMs).isEqualTo(500)
    }

    @Test
    fun `短い途切れは同じ一続きとして扱う`() {
        val tracker = FeedbackTracker(gapToleranceMs = 1_500)
        tracker.ring(hz = 500.0, frames = 6, fromMs = 0)
        val last = tracker.ring(hz = 500.0, frames = 6, fromMs = 1_000)

        val track = tracker.snapshot(last).single()

        assertThat(track.episodes).isEqualTo(1)
        assertThat(track.longestRunMs).isEqualTo(1500)
        assertThat(track.isSustained).isFalse()
    }

    @Test
    fun `鳴り止むと非アクティブになり最長連続が確定する`() {
        val tracker = FeedbackTracker(gapToleranceMs = 1_000)
        val last = tracker.ring(hz = 2000.0, frames = 41, fromMs = 0) // 4秒

        // 何も検出しないフレームを流す（呼び出し側の義務）
        tracker.update(emptyList(), last + 3_000)

        val track = tracker.snapshot(last + 3_000).single()

        assertThat(track.isActive).isFalse()
        assertThat(track.currentRunMs).isEqualTo(0)
        assertThat(track.longestRunMs).isEqualTo(4000)
        assertThat(track.isSustained).isTrue()
    }

    @Test
    fun `近い周波数は同じ発振としてまとめる`() {
        val tracker = FeedbackTracker(mergeRatio = 1.06)
        // 1000Hz と 1020Hz は 2% 差。窓の揺れの範囲なので同じものとして扱う
        tracker.update(listOf(candidate(1000.0)), 0)
        tracker.update(listOf(candidate(1020.0)), 100)

        val tracks = tracker.snapshot(100)

        assertThat(tracks).hasSize(1)
        assertThat(tracks.single().lowestHz).isWithin(0.1).of(1000.0)
        assertThat(tracks.single().highestHz).isWithin(0.1).of(1020.0)
    }

    @Test
    fun `離れた周波数は別の発振として数える`() {
        val tracker = FeedbackTracker(mergeRatio = 1.06)
        tracker.update(listOf(candidate(1000.0), candidate(1250.0)), 0)

        assertThat(tracker.snapshot(0)).hasSize(2)
    }

    @Test
    fun `代表の周波数は強く出たときの値に寄る`() {
        val tracker = FeedbackTracker()
        // 弱く 1000Hz、強く 1030Hz。EQ に入れるべきは強く出た側
        tracker.update(listOf(candidate(1000.0, prominenceDb = 2.0)), 0)
        tracker.update(listOf(candidate(1030.0, prominenceDb = 40.0)), 100)

        assertThat(tracker.snapshot(100).single().frequencyHz).isGreaterThan(1025.0)
    }

    @Test
    fun `周波数が動いたものは動いていると分かる`() {
        val tracker = FeedbackTracker(mergeRatio = 1.10)
        tracker.update(listOf(candidate(1000.0)), 0)
        tracker.update(listOf(candidate(1090.0)), 100)

        assertThat(tracker.snapshot(100).single().isDrifting).isTrue()
    }

    @Test
    fun `フレームが飛んだ空白は累計に入れない`() {
        // 取り込みが詰まって 5 秒飛んだ場合。gapTolerance の内側でも
        // 「その間ずっと鳴っていた」ことにはできない
        val tracker = FeedbackTracker(gapToleranceMs = 10_000, frameIntervalCapMs = 500)
        tracker.update(listOf(candidate(1000.0)), 0)
        tracker.update(listOf(candidate(1000.0)), 5_000)

        assertThat(tracker.snapshot(5_000).single().totalRingingMs).isEqualTo(500)
    }

    @Test
    fun `一瞬の成分は最低時間の指定で一覧から外れる`() {
        val tracker = FeedbackTracker(gapToleranceMs = 500)
        tracker.update(listOf(candidate(8000.0)), 0)
        tracker.ring(hz = 1000.0, frames = 21, fromMs = 0)
        tracker.update(emptyList(), 5_000)

        val filtered = tracker.snapshot(5_000, minTotalMs = 1_000)

        assertThat(filtered.map { it.frequencyHz }).hasSize(1)
        assertThat(filtered.single().frequencyHz).isWithin(5.0).of(1000.0)
    }

    @Test
    fun `いま鳴っているものだけを取り出せる`() {
        val tracker = FeedbackTracker(gapToleranceMs = 1_000)
        tracker.ring(hz = 400.0, frames = 11, fromMs = 0)
        val last = tracker.ring(hz = 900.0, frames = 11, fromMs = 5_000)

        val active = tracker.active(last)

        assertThat(active.map { it.frequencyHz.toInt() }).containsExactly(900)
        assertThat(tracker.snapshot(last)).hasSize(2)
    }

    @Test
    fun `経過時間は最初の更新からの差`() {
        val tracker = FeedbackTracker()
        assertThat(tracker.elapsedMs(1_000)).isEqualTo(0)

        tracker.update(emptyList(), 1_000)

        assertThat(tracker.elapsedMs(61_000)).isEqualTo(60_000)
    }

    @Test
    fun `帯域ラベルと音名が付く`() {
        val tracker = FeedbackTracker()
        tracker.update(listOf(candidate(1000.0)), 0)

        val track = tracker.snapshot(0).single()

        assertThat(track.bandLabel).isEqualTo("1k")
        assertThat(track.noteName).isNotEmpty()
    }

    @Test
    fun `リセットで全部消える`() {
        val tracker = FeedbackTracker()
        tracker.ring(hz = 1000.0, frames = 5, fromMs = 0)

        tracker.reset()

        assertThat(tracker.trackCount).isEqualTo(0)
        assertThat(tracker.snapshot(1_000)).isEmpty()
        assertThat(tracker.elapsedMs(1_000)).isEqualTo(0)
    }
}
