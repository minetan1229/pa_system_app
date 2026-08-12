package com.patoolbox.core.dsp

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * クリック（メトロノーム）。
 *
 * 減衰するサイン波を拍ごとに鳴らす。小節の頭だけ高い音にして、
 * イヤモニで聞いたときに拍が分かるようにしている。
 *
 * 拍の位置は [currentBeat] / [beatCounter] で外から読める。UI の点滅を
 * 音と合わせるため、時刻から計算するのではなく実際に鳴った拍を公開している。
 */
class MetronomeSource(
    override val sampleRate: Int,
    @Volatile var bpm: Double = 120.0,
    @Volatile var beatsPerBar: Int = 4,
    @Volatile var levelDbFs: Double = -12.0,
    @Volatile var accentFirstBeat: Boolean = true,
) : SignalSource {

    /** いま鳴っている拍（0 始まり）。 */
    @Volatile
    var currentBeat: Int = 0
        private set

    /** 累積の拍数。値が変わったら UI を点滅させる。 */
    @Volatile
    var beatCounter: Long = 0
        private set

    private var samplesUntilNextBeat = 0
    private var clickSample = Int.MAX_VALUE
    private var clickFrequency = NORMAL_HZ
    private var nextBeat = 0

    override fun fill(buffer: FloatArray, length: Int) {
        val amplitude = sineAmplitudeFor(levelDbFs)
        val bars = beatsPerBar.coerceAtLeast(1)
        val beatPeriod = (SECONDS_PER_MINUTE / bpm * sampleRate).toInt().coerceAtLeast(1)
        val clickLength = (CLICK_SECONDS * sampleRate).toInt()

        for (i in 0 until length) {
            if (samplesUntilNextBeat <= 0) {
                val beat = nextBeat % bars
                clickFrequency = if (accentFirstBeat && beat == 0) ACCENT_HZ else NORMAL_HZ
                clickSample = 0
                samplesUntilNextBeat = beatPeriod
                currentBeat = beat
                beatCounter++
                nextBeat = beat + 1
            }

            buffer[i] = if (clickSample < clickLength) {
                val t = clickSample.toDouble() / sampleRate
                val envelope = exp(-t / DECAY_SECONDS)
                (amplitude * envelope * sin(2.0 * PI * clickFrequency * t)).toFloat()
            } else {
                0f
            }

            clickSample++
            samplesUntilNextBeat--
        }
    }

    override fun reset() {
        samplesUntilNextBeat = 0
        clickSample = Int.MAX_VALUE
        nextBeat = 0
        currentBeat = 0
        beatCounter = 0
    }

    companion object {
        const val MIN_BPM = 30.0
        const val MAX_BPM = 300.0

        private const val SECONDS_PER_MINUTE = 60.0
        private const val ACCENT_HZ = 1600.0
        private const val NORMAL_HZ = 1000.0
        private const val CLICK_SECONDS = 0.04
        private const val DECAY_SECONDS = 0.012
    }
}
