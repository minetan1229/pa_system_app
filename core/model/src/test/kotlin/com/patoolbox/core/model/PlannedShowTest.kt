package com.patoolbox.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlannedShowTest {

    private val nineAm = 1_700_000_000_000L

    private fun job(name: String = "テスト現場", loadIn: Long? = nineAm) = Job(
        id = 1,
        name = name,
        loadInAtEpochMs = loadIn,
    )

    private fun items() = listOf(
        ScheduleItem(id = 1, jobId = 1, title = "搬入", durationMinutes = 60),
        ScheduleItem(id = 2, jobId = 1, title = "リハ", durationMinutes = 30),
    )

    @Test
    fun `項目が無ければ進行表として成立しない`() {
        assertThat(PlannedShow.from(job(), emptyList())).isNull()
    }

    @Test
    fun `時刻が1つも入っていない案件は拾わない`() {
        assertThat(PlannedShow.from(job(loadIn = null), items())).isNull()
    }

    @Test
    fun `搬入時刻から積み上げて終了時刻を出す`() {
        val show = requireNotNull(PlannedShow.from(job(), items()))

        assertThat(show.startAtEpochMs).isEqualTo(nineAm)
        assertThat(show.endAtEpochMs).isEqualTo(nineAm + 90 * 60_000L)
        assertThat(show.totalMinutes).isEqualTo(90)
    }

    @Test
    fun `いまの時刻が当たっている項目を返す`() {
        val show = requireNotNull(PlannedShow.from(job(), items()))

        // 開始70分後 → 2本目（リハ）の最中
        val entry = show.entryAt(nineAm + 70 * 60_000L)

        assertThat(entry?.item?.title).isEqualTo("リハ")
        assertThat(show.isRunningAt(nineAm + 70 * 60_000L)).isTrue()
        assertThat(show.hasStarted(nineAm + 70 * 60_000L)).isTrue()
    }

    @Test
    fun `終わった進行表は当たらない`() {
        val show = requireNotNull(PlannedShow.from(job(), items()))
        val after = nineAm + 120 * 60_000L

        assertThat(show.entryAt(after)).isNull()
        assertThat(show.isRunningAt(after)).isFalse()
    }

    @Test
    fun `進行中のものを最優先で選び 終わったものは選ばない`() {
        val running = requireNotNull(PlannedShow.from(job("進行中"), items()))
        val later = requireNotNull(
            PlannedShow.from(
                job("あとで", loadIn = nineAm + 300 * 60_000L),
                items(),
            ),
        )
        val now = nineAm + 10 * 60_000L

        assertThat(PlannedShow.pick(listOf(running, later), now)?.job?.name).isEqualTo("進行中")

        // 進行中のものが終わっていれば、次に始まるものを出す
        val afterRunning = nineAm + 200 * 60_000L
        assertThat(PlannedShow.pick(listOf(running, later), afterRunning)?.job?.name)
            .isEqualTo("あとで")

        // 全部終わっていれば何も出さない（終わった現場を蒸し返さない）
        val afterAll = nineAm + 500 * 60_000L
        assertThat(PlannedShow.pick(listOf(running, later), afterAll)).isNull()
    }
}
