package com.patoolbox.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ScheduleTimelineTest {

    private val nineAm = 9 * HOUR

    private fun item(title: String, minutes: Int, fixedStart: Long? = null) = ScheduleItem(
        jobId = 1,
        title = title,
        durationMinutes = minutes,
        startAtEpochMs = fixedStart,
    )

    @Test
    fun `長さだけ入れれば時刻が積み上がる`() {
        val entries = ScheduleTimeline.build(
            anchorEpochMs = nineAm,
            items = listOf(
                item("搬入", 60),
                item("仕込み", 90),
                item("サウンドチェック", 30),
            ),
        )

        assertThat(entries[0].startAtEpochMs).isEqualTo(nineAm)
        assertThat(entries[1].startAtEpochMs).isEqualTo(nineAm + 60 * MINUTE)
        assertThat(entries[2].startAtEpochMs).isEqualTo(nineAm + 150 * MINUTE)
        assertThat(entries[2].endAtEpochMs).isEqualTo(nineAm + 180 * MINUTE)
    }

    @Test
    fun `1つ伸ばすと以降が全部後ろにずれる`() {
        val before = ScheduleTimeline.build(
            nineAm,
            listOf(item("仕込み", 60), item("リハ", 60), item("本番", 90)),
        )
        val after = ScheduleTimeline.build(
            nineAm,
            listOf(item("仕込み", 90), item("リハ", 60), item("本番", 90)),
        )

        // 仕込みを30分伸ばしたら、以降も30分ずつ押す
        assertThat(after[1].startAtEpochMs - before[1].startAtEpochMs).isEqualTo(30 * MINUTE)
        assertThat(after[2].startAtEpochMs - before[2].startAtEpochMs).isEqualTo(30 * MINUTE)
    }

    @Test
    fun `固定時刻の項目はそこで積み直す`() {
        val sevenPm = 19 * HOUR
        val entries = ScheduleTimeline.build(
            nineAm,
            listOf(
                item("仕込み", 60),
                item("本番", 90, fixedStart = sevenPm),
                item("撤収", 60),
            ),
        )

        assertThat(entries[1].startAtEpochMs).isEqualTo(sevenPm)
        assertThat(entries[1].isAnchor).isTrue()
        // 撤収は本番の終わりから積む
        assertThat(entries[2].startAtEpochMs).isEqualTo(sevenPm + 90 * MINUTE)
    }

    @Test
    fun `固定時刻に食い込んでいたら検出する`() {
        val tenAm = 10 * HOUR
        val entries = ScheduleTimeline.build(
            nineAm,
            listOf(
                // 9:00から3時間 = 12:00 までかかるので 10:00 固定に間に合わない
                item("リハ", 180),
                item("本番", 60, fixedStart = tenAm),
            ),
        )

        val overrunning = ScheduleTimeline.overrunningAnchors(entries)

        assertThat(overrunning).hasSize(1)
        assertThat(overrunning.first().item.title).isEqualTo("本番")
    }

    @Test
    fun `間に合っていれば警告は出ない`() {
        val onePm = 13 * HOUR
        val entries = ScheduleTimeline.build(
            nineAm,
            listOf(item("リハ", 120), item("本番", 60, fixedStart = onePm)),
        )

        assertThat(ScheduleTimeline.overrunningAnchors(entries)).isEmpty()
    }

    @Test
    fun `合計時間が出る`() {
        val items = listOf(item("A", 60), item("B", 30), item("C", 15))

        assertThat(ScheduleTimeline.totalMinutes(items)).isEqualTo(105)
    }

    @Test
    fun `空の進行表でも落ちない`() {
        assertThat(ScheduleTimeline.build(nineAm, emptyList())).isEmpty()
        assertThat(ScheduleTimeline.totalMinutes(emptyList())).isEqualTo(0)
        assertThat(ScheduleTimeline.overrunningAnchors(emptyList())).isEmpty()
    }

    @Test
    fun `終了時刻が長さと一致する`() {
        val entry = ScheduleTimeline.build(nineAm, listOf(item("本番", 45))).first()

        assertThat(entry.endAtEpochMs - entry.startAtEpochMs).isEqualTo(45 * MINUTE)
        assertThat(entry.item.endAtEpochMs).isNull() // 固定時刻がないので単体では出せない
    }

    private companion object {
        const val MINUTE = 60_000L
        const val HOUR = 60 * MINUTE
    }
}
