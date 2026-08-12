package com.patoolbox.core.dsp

import kotlin.math.PI
import kotlin.math.sin

// テスト用の合成信号。
// マイクを介さずに DSP へ直接注入して理論値と突き合わせるために使う。

const val TEST_SAMPLE_RATE = 48000

/** 振幅指定のサイン波。 */
fun sine(
    frequencyHz: Double,
    amplitude: Double = 1.0,
    lengthSamples: Int,
    sampleRate: Int = TEST_SAMPLE_RATE,
    phase: Double = 0.0,
): FloatArray = FloatArray(lengthSamples) { i ->
    (amplitude * sin(2.0 * PI * frequencyHz * i / sampleRate + phase)).toFloat()
}

/** dBFS（RMS基準）指定のサイン波。 */
fun sineAtLevel(
    frequencyHz: Double,
    levelDbFs: Double,
    lengthSamples: Int,
    sampleRate: Int = TEST_SAMPLE_RATE,
): FloatArray = sine(frequencyHz, sineAmplitudeFor(levelDbFs), lengthSamples, sampleRate)

/** 倍音を持つ信号（のこぎり波の近似）。基本波検出の耐性テスト用。 */
fun harmonicTone(
    fundamentalHz: Double,
    harmonics: Int,
    lengthSamples: Int,
    sampleRate: Int = TEST_SAMPLE_RATE,
): FloatArray {
    val out = FloatArray(lengthSamples)
    for (h in 1..harmonics) {
        val amplitude = 1.0 / h
        for (i in 0 until lengthSamples) {
            out[i] += (amplitude * sin(2.0 * PI * fundamentalHz * h * i / sampleRate)).toFloat()
        }
    }
    return out
}

/** SignalSource からまとめて取り出す。 */
fun SignalSource.render(lengthSamples: Int, blockSize: Int = 1024): FloatArray {
    val out = FloatArray(lengthSamples)
    val block = FloatArray(blockSize)
    var offset = 0
    while (offset < lengthSamples) {
        val chunk = minOf(blockSize, lengthSamples - offset)
        fill(block, chunk)
        block.copyInto(out, offset, 0, chunk)
        offset += chunk
    }
    return out
}

/** 信号の dBFS（RMS基準）。 */
fun levelDbFs(buffer: FloatArray): Double = amplitudeToDb(rms(buffer))
