package com.patoolbox.core.dsp

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * テスト信号の生成元。
 *
 * レベルはすべて **dBFS = 20log10(RMS)** で扱う。この規約だとフルスケールの
 * サイン波（振幅1.0）が -3.01 dBFS になり、[SpectrumAnalyzer] の読みと一致する。
 * 解析側と生成側で規約が食い違うのが一番タチの悪いバグなので統一している。
 *
 * [fill] は割り当てを起こさない。出力スレッドから毎ブロック呼ぶため。
 */
interface SignalSource {
    val sampleRate: Int

    fun fill(buffer: FloatArray, length: Int = buffer.size)

    fun reset()
}

/** dBFS（RMS基準）→ サイン波の振幅。 */
fun sineAmplitudeFor(levelDbFs: Double): Double = sqrt(2.0) * dbToAmplitude(levelDbFs)

/** dBFS（RMS基準）→ RMS 値。ノイズ系の振幅計算に使う。 */
fun rmsFor(levelDbFs: Double): Double = dbToAmplitude(levelDbFs)

const val DEFAULT_LEVEL_DB_FS = -20.0

class SineSource(
    override val sampleRate: Int,
    var frequencyHz: Double = 1000.0,
    var levelDbFs: Double = DEFAULT_LEVEL_DB_FS,
) : SignalSource {
    private var phase = 0.0

    override fun fill(buffer: FloatArray, length: Int) {
        val amplitude = sineAmplitudeFor(levelDbFs)
        val increment = 2.0 * PI * frequencyHz / sampleRate
        for (i in 0 until length) {
            buffer[i] = (amplitude * sin(phase)).toFloat()
            phase += increment
            if (phase >= 2.0 * PI) phase -= 2.0 * PI
        }
    }

    override fun reset() {
        phase = 0.0
    }
}

/** 相互変調（IMD）の確認用。2音を同レベルで出す。 */
class DualToneSource(
    override val sampleRate: Int,
    var lowHz: Double = 250.0,
    var highHz: Double = 8000.0,
    var levelDbFs: Double = DEFAULT_LEVEL_DB_FS,
    maxBlockSize: Int = DEFAULT_MAX_BLOCK,
) : SignalSource {
    private val low = SineSource(sampleRate)
    private val high = SineSource(sampleRate)
    private val scratch = FloatArray(maxBlockSize)

    override fun fill(buffer: FloatArray, length: Int) {
        require(length <= scratch.size) {
            "ブロックが maxBlockSize を超えている: $length > ${scratch.size}"
        }
        // 合計レベルを levelDbFs に合わせるため、各音は 3dB 下げる
        val each = levelDbFs - 3.01
        low.frequencyHz = lowHz
        low.levelDbFs = each
        high.frequencyHz = highHz
        high.levelDbFs = each

        low.fill(buffer, length)
        high.fill(scratch, length)
        for (i in 0 until length) {
            buffer[i] += scratch[i]
        }
    }

    override fun reset() {
        low.reset()
        high.reset()
    }

    private companion object {
        const val DEFAULT_MAX_BLOCK = 8192
    }
}

/** 矩形波。位相チェックや簡易的な高調波確認に使う。 */
class SquareSource(
    override val sampleRate: Int,
    var frequencyHz: Double = 1000.0,
    var levelDbFs: Double = DEFAULT_LEVEL_DB_FS,
) : SignalSource {
    private var phase = 0.0

    override fun fill(buffer: FloatArray, length: Int) {
        // 矩形波の RMS は振幅そのままなので、dBFS からの換算はサインと異なる
        val amplitude = rmsFor(levelDbFs)
        val increment = frequencyHz / sampleRate
        for (i in 0 until length) {
            buffer[i] = (if (phase < 0.5) amplitude else -amplitude).toFloat()
            phase += increment
            if (phase >= 1.0) phase -= 1.0
        }
    }

    override fun reset() {
        phase = 0.0
    }
}

/**
 * 決定論的な白色ノイズ。
 * java.util.Random ではなく xorshift を使うのは、テストで再現性を確保しつつ
 * 割り当てとロックを避けるため。
 */
class WhiteNoiseSource(
    override val sampleRate: Int,
    var levelDbFs: Double = DEFAULT_LEVEL_DB_FS,
    private val seed: Long = 1L,
) : SignalSource {
    private var state = seed

    override fun fill(buffer: FloatArray, length: Int) {
        val amplitude = rmsFor(levelDbFs)
        for (i in 0 until length) {
            buffer[i] = (nextGaussianish() * amplitude).toFloat()
        }
    }

    override fun reset() {
        state = seed
    }

    /**
     * 一様乱数を12個足して正規分布に近づける（中心極限定理）。分散が1になるよう正規化済み。
     * 正規分布そのものは不要で、クレストファクタが素直で RMS が正確なことが重要。
     */
    private fun nextGaussianish(): Double {
        var sum = 0.0
        repeat(12) { sum += nextUniform() }
        return sum // 一様[-0.5,0.5]の分散は1/12。12個足すと分散1
    }

    private fun nextUniform(): Double {
        state = state xor (state shl 13)
        state = state xor (state ushr 7)
        state = state xor (state shl 17)
        return (state ushr 11).toDouble() / (1L shl 53).toDouble() - 0.5
    }
}

/**
 * ピンクノイズ（-3dB/oct）。RTA でスピーカーの特性を見る基本信号。
 *
 * Paul Kellet の 7 極フィルタ。44.1kHz を想定した係数なので 48kHz では
 * 傾きがわずかにずれるが、1/3オクターブで見て 1dB 以内に収まる（テストで確認）。
 */
class PinkNoiseSource(
    override val sampleRate: Int,
    var levelDbFs: Double = DEFAULT_LEVEL_DB_FS,
    private val seed: Long = 1L,
) : SignalSource {
    private val white = WhiteNoiseSource(sampleRate, levelDbFs = 0.0, seed = seed)
    private val state = DoubleArray(7)
    private val scratch = FloatArray(BLOCK)

    /** 単位分散の白色ノイズを入れたときの出力RMS。構築時に実測して正規化に使う */
    private val filterGain: Double = measureFilterGain(seed)

    override fun fill(buffer: FloatArray, length: Int) {
        val target = rmsFor(levelDbFs) / filterGain
        var offset = 0
        while (offset < length) {
            val chunk = minOf(BLOCK, length - offset)
            white.fill(scratch, chunk)
            for (i in 0 until chunk) {
                buffer[offset + i] = (pink(scratch[i].toDouble(), state) * target).toFloat()
            }
            offset += chunk
        }
    }

    override fun reset() {
        white.reset()
        state.fill(0.0)
    }

    private companion object {
        const val BLOCK = 1024

        fun pink(white: Double, s: DoubleArray): Double {
            s[0] = 0.99886 * s[0] + white * 0.0555179
            s[1] = 0.99332 * s[1] + white * 0.0750759
            s[2] = 0.96900 * s[2] + white * 0.1538520
            s[3] = 0.86650 * s[3] + white * 0.3104856
            s[4] = 0.55000 * s[4] + white * 0.5329522
            s[5] = -0.7616 * s[5] - white * 0.0168980
            val out = s[0] + s[1] + s[2] + s[3] + s[4] + s[5] + s[6] + white * 0.5362
            s[6] = white * 0.115926
            return out
        }

        /** フィルタは線形時不変なので、一度測れば固定のゲインとして使える。 */
        fun measureFilterGain(seed: Long): Double {
            val warmup = WhiteNoiseSource(48000, levelDbFs = 0.0, seed = seed + 1)
            val s = DoubleArray(7)
            val block = FloatArray(BLOCK)
            var sumSquares = 0.0
            var count = 0L
            repeat(64) { iteration ->
                warmup.fill(block)
                for (v in block) {
                    val out = pink(v.toDouble(), s)
                    // 最初の1ブロックはフィルタの立ち上がりなので統計に入れない
                    if (iteration > 0) {
                        sumSquares += out * out
                        count++
                    }
                }
            }
            return sqrt(sumSquares / count)
        }
    }
}

/**
 * 対数（指数）スイープ。Farina 法。
 * Phase 4 のインパルス応答測定でそのまま使えるよう、位相が解析的に決まる形にしている。
 */
class LogSweepSource(
    override val sampleRate: Int,
    var startHz: Double = 20.0,
    var endHz: Double = 20000.0,
    var durationSeconds: Double = 5.0,
    var levelDbFs: Double = DEFAULT_LEVEL_DB_FS,
    var loop: Boolean = false,
) : SignalSource {
    private var sampleIndex = 0L

    val isFinished: Boolean
        get() = !loop && sampleIndex >= (durationSeconds * sampleRate).toLong()

    override fun fill(buffer: FloatArray, length: Int) {
        val amplitude = sineAmplitudeFor(levelDbFs)
        val totalSamples = (durationSeconds * sampleRate).toLong()
        val ratio = ln(endHz / startHz)
        val k = 2.0 * PI * startHz * durationSeconds / ratio

        for (i in 0 until length) {
            if (sampleIndex >= totalSamples) {
                if (loop) sampleIndex = 0 else { buffer[i] = 0f; continue }
            }
            val t = sampleIndex.toDouble() / sampleRate
            val phase = k * (exp(t / durationSeconds * ratio) - 1.0)
            buffer[i] = (amplitude * sin(phase)).toFloat()
            sampleIndex++
        }
    }

    override fun reset() {
        sampleIndex = 0
    }
}

/** リニアスイープ。共振の探索など、低域を等間隔で見たいとき。 */
class LinearSweepSource(
    override val sampleRate: Int,
    var startHz: Double = 20.0,
    var endHz: Double = 20000.0,
    var durationSeconds: Double = 5.0,
    var levelDbFs: Double = DEFAULT_LEVEL_DB_FS,
    var loop: Boolean = false,
) : SignalSource {
    private var sampleIndex = 0L

    val isFinished: Boolean
        get() = !loop && sampleIndex >= (durationSeconds * sampleRate).toLong()

    override fun fill(buffer: FloatArray, length: Int) {
        val amplitude = sineAmplitudeFor(levelDbFs)
        val totalSamples = (durationSeconds * sampleRate).toLong()
        val rate = (endHz - startHz) / durationSeconds

        for (i in 0 until length) {
            if (sampleIndex >= totalSamples) {
                if (loop) sampleIndex = 0 else { buffer[i] = 0f; continue }
            }
            val t = sampleIndex.toDouble() / sampleRate
            val phase = 2.0 * PI * (startHz * t + 0.5 * rate * t * t)
            buffer[i] = (amplitude * sin(phase)).toFloat()
            sampleIndex++
        }
    }

    override fun reset() {
        sampleIndex = 0
    }
}

/**
 * 元の信号を断続させる。
 * ハウリングを起こさずに残響やディレイの聞き取りをするときに使う。
 */
class BurstSource(
    private val source: SignalSource,
    var onSeconds: Double = 0.5,
    var offSeconds: Double = 0.5,
) : SignalSource {
    override val sampleRate: Int get() = source.sampleRate

    private var sampleIndex = 0L

    override fun fill(buffer: FloatArray, length: Int) {
        source.fill(buffer, length)
        val onSamples = (onSeconds * sampleRate).toLong()
        val periodSamples = ((onSeconds + offSeconds) * sampleRate).toLong().coerceAtLeast(1)
        for (i in 0 until length) {
            if (sampleIndex % periodSamples >= onSamples) buffer[i] = 0f
            sampleIndex++
        }
    }

    override fun reset() {
        source.reset()
        sampleIndex = 0
    }
}
