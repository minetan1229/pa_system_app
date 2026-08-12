package com.patoolbox.core.dsp

import kotlin.math.cos
import kotlin.math.sin

/**
 * 基数2の反復 Cooley-Tukey FFT。
 *
 * 回転因子とビット反転表はコンストラクタで作り置きし、[transform] は割り当てを起こさない。
 * 計測ループから毎フレーム呼ぶので、GC を踏ませないことが重要。
 */
class Fft(val size: Int) {

    init {
        require(size >= 2 && size and (size - 1) == 0) {
            "FFT サイズは2のべき乗である必要がある: $size"
        }
    }

    private val cosTable = DoubleArray(size / 2)
    private val sinTable = DoubleArray(size / 2)
    private val bitReversed = IntArray(size)

    init {
        for (i in 0 until size / 2) {
            val angle = -2.0 * Math.PI * i / size
            cosTable[i] = cos(angle)
            sinTable[i] = sin(angle)
        }
        val bits = Integer.numberOfTrailingZeros(size)
        for (i in 0 until size) {
            bitReversed[i] = Integer.reverse(i) ushr (32 - bits)
        }
    }

    /**
     * [re] / [im] をその場で変換する。
     * @param inverse true なら逆変換（1/N のスケーリング込み）
     */
    fun transform(re: DoubleArray, im: DoubleArray, inverse: Boolean = false) {
        require(re.size >= size && im.size >= size) { "バッファが FFT サイズより小さい" }

        for (i in 0 until size) {
            val j = bitReversed[i]
            if (j > i) {
                var tmp = re[i]; re[i] = re[j]; re[j] = tmp
                tmp = im[i]; im[i] = im[j]; im[j] = tmp
            }
        }

        var len = 2
        while (len <= size) {
            val half = len / 2
            val step = size / len
            var blockStart = 0
            while (blockStart < size) {
                var k = 0
                for (j in blockStart until blockStart + half) {
                    val c = cosTable[k]
                    val s = if (inverse) -sinTable[k] else sinTable[k]
                    val pair = j + half
                    val tre = re[pair] * c - im[pair] * s
                    val tim = re[pair] * s + im[pair] * c
                    re[pair] = re[j] - tre
                    im[pair] = im[j] - tim
                    re[j] += tre
                    im[j] += tim
                    k += step
                }
                blockStart += len
            }
            len = len shl 1
        }

        if (inverse) {
            val scale = 1.0 / size
            for (i in 0 until size) {
                re[i] *= scale
                im[i] *= scale
            }
        }
    }
}
