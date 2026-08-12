package com.patoolbox.core.dsp

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

class FftTest {

    @Test
    fun `2のべき乗以外は拒否する`() {
        runCatching { Fft(1000) }.also { assertThat(it.isFailure).isTrue() }
    }

    @Test
    fun `インパルスのスペクトラムはフラット`() {
        val size = 256
        val fft = Fft(size)
        val re = DoubleArray(size).also { it[0] = 1.0 }
        val im = DoubleArray(size)

        fft.transform(re, im)

        for (bin in 0 until size) {
            assertThat(hypot(re[bin], im[bin])).isWithin(1e-9).of(1.0)
        }
    }

    @Test
    fun `ビン中心のサインは該当ビンにだけ現れる`() {
        val size = 1024
        val bin = 64
        val fft = Fft(size)
        val re = DoubleArray(size) {
            kotlin.math.sin(2.0 * Math.PI * bin * it / size)
        }
        val im = DoubleArray(size)

        fft.transform(re, im)

        val magnitude = DoubleArray(size / 2 + 1) { hypot(re[it], im[it]) }
        val peak = magnitude.indices.maxBy { magnitude[it] }

        assertThat(peak).isEqualTo(bin)
        // 隣接ビンには漏れない（窓なし・ビン中心ちょうどなので理論上ゼロ）
        assertThat(magnitude[bin - 1]).isLessThan(magnitude[bin] * 1e-6)
        assertThat(magnitude[bin + 1]).isLessThan(magnitude[bin] * 1e-6)
    }

    @Test
    fun `順変換と逆変換で元に戻る`() {
        val size = 512
        val fft = Fft(size)
        val original = DoubleArray(size) { kotlin.math.sin(it * 0.1) + 0.3 * kotlin.math.cos(it * 0.7) }
        val re = original.copyOf()
        val im = DoubleArray(size)

        fft.transform(re, im)
        fft.transform(re, im, inverse = true)

        for (i in 0 until size) {
            assertThat(abs(re[i] - original[i])).isLessThan(1e-9)
        }
    }

    @Test
    fun `パーセバルの等式が成り立つ`() {
        val size = 1024
        val fft = Fft(size)
        val signal = DoubleArray(size) { kotlin.math.sin(it * 0.05) * 0.7 }
        val re = signal.copyOf()
        val im = DoubleArray(size)

        val timeEnergy = signal.sumOf { it * it }

        fft.transform(re, im)
        var freqEnergy = 0.0
        for (i in 0 until size) {
            freqEnergy += re[i] * re[i] + im[i] * im[i]
        }
        freqEnergy /= size

        assertThat(freqEnergy).isWithin(timeEnergy * 1e-9).of(timeEnergy)
    }
}
