package com.patoolbox.core.dsp

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SpectrogramBufferTest {

    private fun row(value: Double, columns: Int = 4) = FloatArray(columns) { value.toFloat() }

    @Test
    fun `入れた順に古い順で取り出せる`() {
        val buffer = SpectrogramBuffer(columns = 4, historySize = 8)

        buffer.push(row(-10.0))
        buffer.push(row(-20.0))
        buffer.push(row(-30.0))

        assertThat(buffer.size).isEqualTo(3)
        assertThat(buffer.rowAt(0)[0]).isWithin(1e-5f).of(-10f)
        assertThat(buffer.rowAt(2)[0]).isWithin(1e-5f).of(-30f)
    }

    @Test
    fun `履歴を超えると古いものから捨てる`() {
        val buffer = SpectrogramBuffer(columns = 4, historySize = 3)

        for (i in 1..5) {
            buffer.push(row(-i * 10.0))
        }

        assertThat(buffer.size).isEqualTo(3)
        // -10 と -20 は押し出され、-30 が最古になる
        assertThat(buffer.rowAt(0)[0]).isWithin(1e-5f).of(-30f)
        assertThat(buffer.rowAt(2)[0]).isWithin(1e-5f).of(-50f)
    }

    @Test
    fun `新しい順でも読める`() {
        val buffer = SpectrogramBuffer(columns = 2, historySize = 4)
        for (i in 1..6) {
            buffer.push(row(-i * 10.0, columns = 2))
        }

        val order = mutableListOf<Float>()
        buffer.forEachNewestFirst { _, r -> order += r[0] }

        assertThat(order).containsExactly(-60f, -50f, -40f, -30f).inOrder()
    }

    @Test
    fun `行の配列は使い回される`() {
        // 毎フレーム配列を作ると GC を踏んで録音を取りこぼす。
        // リングの一周で同じインスタンスが返ることを固定しておく
        val buffer = SpectrogramBuffer(columns = 2, historySize = 2)
        buffer.push(row(-10.0, columns = 2))
        val first = buffer.rowAt(0)

        buffer.push(row(-20.0, columns = 2))
        buffer.push(row(-30.0, columns = 2))

        assertThat(buffer.rowAt(1)).isSameInstanceAs(first)
    }

    @Test
    fun `消すと空になる`() {
        val buffer = SpectrogramBuffer(columns = 2, historySize = 4)
        buffer.push(row(-10.0, columns = 2))

        buffer.clear()

        assertThat(buffer.size).isEqualTo(0)
    }

    @Test
    fun `カラム数が合わない行は拒否する`() {
        val buffer = SpectrogramBuffer(columns = 8, historySize = 2)

        val result = runCatching { buffer.push(row(-10.0, columns = 4)) }

        assertThat(result.isFailure).isTrue()
    }
}
