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

fun FloatArray.toDoubleArray(): DoubleArray = DoubleArray(size) { this[it].toDouble() }

/**
 * ログスイープを連続時刻で評価する。
 *
 * [LogSweepSource] と同じ式だが、サンプル位置を実数で指定できるようにしてある。
 * 小数サンプルの遅延を厳密に作れるので、放物線補間の精度を検証できる。
 */
fun logSweepAt(
    sampleOffset: Double,
    lengthSamples: Int,
    startHz: Double = 50.0,
    endHz: Double = 12000.0,
    sampleRate: Int = TEST_SAMPLE_RATE,
): DoubleArray {
    val duration = lengthSamples.toDouble() / sampleRate
    val ratio = kotlin.math.ln(endHz / startHz)
    val k = 2.0 * PI * startHz * duration / ratio
    return DoubleArray(lengthSamples) { i ->
        val position = i - sampleOffset
        if (position < 0.0 || position >= lengthSamples) {
            0.0
        } else {
            val t = position / sampleRate
            sin(k * (kotlin.math.exp(t / duration * ratio) - 1.0))
        }
    }
}

/**
 * 指数減衰する雑音。残響のある部屋の応答の代用。
 *
 * エネルギーは exp(-2at) で減るので、dB の傾きは -20a/ln(10) = -8.6859a dB/s、
 * つまり残響時間は 6.9078/a 秒になる。[rtSeconds] からこの a を逆算している。
 */
fun decayingNoise(
    rtSeconds: Double,
    lengthSamples: Int,
    sampleRate: Int = TEST_SAMPLE_RATE,
    seed: Long = 7L,
): DoubleArray {
    val a = 6.907755278982137 / rtSeconds
    var state = seed
    fun nextUniform(): Double {
        state = state xor (state shl 13)
        state = state xor (state ushr 7)
        state = state xor (state shl 17)
        return (state ushr 11).toDouble() / (1L shl 53).toDouble() - 0.5
    }
    return DoubleArray(lengthSamples) { i ->
        val t = i.toDouble() / sampleRate
        nextUniform() * 2.0 * kotlin.math.exp(-a * t)
    }
}

/** 一定レベルの雑音を足す。暗騒音の再現用。 */
fun DoubleArray.plusNoise(amplitude: Double, seed: Long = 11L): DoubleArray {
    var state = seed
    return DoubleArray(size) { i ->
        state = state xor (state shl 13)
        state = state xor (state ushr 7)
        state = state xor (state shl 17)
        val uniform = (state ushr 11).toDouble() / (1L shl 53).toDouble() - 0.5
        this[i] + uniform * 2.0 * amplitude
    }
}
