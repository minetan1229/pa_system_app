package com.patoolbox.core.dsp

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FrameAccumulatorTest {

    @Test
    fun `フレームが揃うたびに呼ばれる`() {
        val accumulator = FrameAccumulator(frameSize = 8, hopSize = 8)
        val block = FloatArray(4) { 1f }
        var frames = 0

        accumulator.add(block, block.size) { frames++ }
        assertThat(frames).isEqualTo(0)

        accumulator.add(block, block.size) { frames++ }
        assertThat(frames).isEqualTo(1)
    }

    @Test
    fun `ブロックがフレームより大きくても取りこぼさない`() {
        val accumulator = FrameAccumulator(frameSize = 4, hopSize = 4)
        val block = FloatArray(16) { it.toFloat() }
        var frames = 0

        accumulator.add(block, block.size) { frames++ }

        assertThat(frames).isEqualTo(4)
    }

    @Test
    fun `フレームの内容が入力どおり`() {
        val accumulator = FrameAccumulator(frameSize = 4, hopSize = 4)
        val block = floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f)
        val captured = mutableListOf<List<Float>>()

        accumulator.add(block, block.size) { captured += it.toList() }

        assertThat(captured).containsExactly(
            listOf(1f, 2f, 3f, 4f),
            listOf(5f, 6f, 7f, 8f),
        ).inOrder()
    }

    @Test
    fun `オーバーラップすると前半が繰り越される`() {
        val accumulator = FrameAccumulator(frameSize = 4, hopSize = 2)
        val block = floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f)
        val captured = mutableListOf<List<Float>>()

        accumulator.add(block, block.size) { captured += it.toList() }

        // 50%オーバーラップ: [1,2,3,4] → [3,4,5,6]
        assertThat(captured).containsExactly(
            listOf(1f, 2f, 3f, 4f),
            listOf(3f, 4f, 5f, 6f),
        ).inOrder()
    }

    @Test
    fun `resetで溜まりが消える`() {
        val accumulator = FrameAccumulator(frameSize = 4, hopSize = 4)
        accumulator.add(FloatArray(3), 3) { }
        assertThat(accumulator.pending).isEqualTo(3)

        accumulator.reset()

        assertThat(accumulator.pending).isEqualTo(0)
    }

    @Test
    fun `不正なサイズは拒否する`() {
        runCatching { FrameAccumulator(0) }.also { assertThat(it.isFailure).isTrue() }
        runCatching { FrameAccumulator(8, hopSize = 0) }.also {
            assertThat(it.isFailure).isTrue()
        }
        runCatching { FrameAccumulator(8, hopSize = 9) }.also {
            assertThat(it.isFailure).isTrue()
        }
    }
}
