package com.patoolbox.core.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.tan

/**
 * オクターブバンド分析用のバターワース帯域フィルタ。
 *
 * 残響時間はバンドごとに大きく違う（低域だけ長く残る部屋はいくらでもある）ので、
 * 広帯域の値だけ出しても現場の判断材料にならない。そのための帯域分割。
 *
 * **これは IEC 61260 の検定フィルタではない。** 4次のハイパスと4次のローパスを
 * 直列にしたもので、肩の落ち方は 24dB/oct。クラス1が要求する阻止域減衰には届かない。
 * 隣の帯域が極端に大きいときは漏れ込む前提で読むこと。
 */
object ButterworthBand {

    /**
     * バターワース低域通過。次数は偶数のみ（2次セクションの直列で作るため）。
     *
     * 双一次変換の周波数の歪みを打ち消すため、アナログ側の遮断角周波数を
     * あらかじめ tan で伸ばしておく（プリワープ）。これを省くと 8k 帯の上端
     * 11.3kHz が 48kHz サンプリングで 1kHz 近くずれる。
     */
    fun lowPass(cutoffHz: Double, sampleRate: Int, order: Int = 4): List<Biquad> {
        requireEvenOrder(order)
        val w0 = prewarp(cutoffHz, sampleRate)
        return butterworthQs(order).map { q ->
            bilinearTransform(
                n2 = 0.0, n1 = 0.0, n0 = w0 * w0,
                d2 = 1.0, d1 = w0 / q, d0 = w0 * w0,
                sampleRate = sampleRate,
            )
        }
    }

    /** バターワース高域通過。 */
    fun highPass(cutoffHz: Double, sampleRate: Int, order: Int = 4): List<Biquad> {
        requireEvenOrder(order)
        val w0 = prewarp(cutoffHz, sampleRate)
        return butterworthQs(order).map { q ->
            bilinearTransform(
                n2 = 1.0, n1 = 0.0, n0 = 0.0,
                d2 = 1.0, d1 = w0 / q, d0 = w0 * w0,
                sampleRate = sampleRate,
            )
        }
    }

    /**
     * 帯域通過。上端がナイキストに近すぎる場合はローパスを省く
     * （20k 帯の上端 22.4kHz は 48kHz では 0.93×ナイキストで、そのまま作ると係数が暴れる）。
     */
    fun bandPass(
        lowerHz: Double,
        upperHz: Double,
        sampleRate: Int,
        order: Int = 4,
    ): BiquadCascade {
        require(lowerHz > 0 && upperHz > lowerHz) { "帯域が不正: $lowerHz..$upperHz" }
        val nyquist = sampleRate / 2.0
        val sections = buildList {
            addAll(highPass(lowerHz.coerceAtMost(nyquist * MAX_CUTOFF_RATIO), sampleRate, order))
            if (upperHz < nyquist * MAX_CUTOFF_RATIO) {
                addAll(lowPass(upperHz, sampleRate, order))
            }
        }
        return BiquadCascade(sections)
    }

    /** [FrequencyBand] からそのまま作る。 */
    fun bandPass(band: FrequencyBand, sampleRate: Int, order: Int = 4): BiquadCascade =
        bandPass(band.lowerHz, band.upperHz, sampleRate, order)

    /**
     * 時間を逆にして通す。
     *
     * 残響の解析ではこちらを使う。フィルタ自身にも立ち上がりの尾があり、順方向に
     * 掛けるとそれが応答の減衰に足されて残響時間が長めに出る。逆向きに通せば
     * フィルタの尾は音の到来より **前** に置かれるので、減衰部分が汚れない。
     */
    fun filterReversed(input: DoubleArray, cascade: BiquadCascade): DoubleArray {
        cascade.reset()
        val output = DoubleArray(input.size)
        for (i in input.indices.reversed()) {
            output[i] = cascade.process(input[i])
        }
        return output
    }

    /** 順方向に通す。信号の帯域分割など、因果性が要るとき。 */
    fun filterForward(input: DoubleArray, cascade: BiquadCascade): DoubleArray {
        cascade.reset()
        return DoubleArray(input.size) { cascade.process(input[it]) }
    }

    /** 次数 [order] のバターワースを2次セクションに分けたときの各段の Q。 */
    private fun butterworthQs(order: Int): List<Double> =
        (0 until order / 2).map { k ->
            1.0 / (2.0 * cos(PI * (2 * k + 1) / (2.0 * order)))
        }

    private fun prewarp(cutoffHz: Double, sampleRate: Int): Double {
        val limit = sampleRate * MAX_CUTOFF_RATIO / 2.0
        val fc = cutoffHz.coerceIn(MIN_CUTOFF_HZ, limit)
        return 2.0 * sampleRate * tan(PI * fc / sampleRate)
    }

    private fun requireEvenOrder(order: Int) {
        require(order >= 2 && order % 2 == 0) { "次数は2以上の偶数のみ: $order" }
    }

    /** ナイキストに対して許す遮断周波数の上限。 */
    private const val MAX_CUTOFF_RATIO = 0.9
    private const val MIN_CUTOFF_HZ = 1.0
}
