package com.patoolbox.core.dsp

import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

data class NoteReading(
    /** "A", "C#" など */
    val name: String,
    val octave: Int,
    /** "A4" */
    val displayName: String,
    /** 目標音からのずれ（セント）。+ が高い */
    val cents: Double,
    val detectedHz: Double,
    val targetHz: Double,
)

object NoteNames {

    private val NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    /** A4 の既定値。現場では 442Hz などにずらすこともあるので可変にしている */
    const val DEFAULT_REFERENCE_A_HZ = 440.0

    /** これ以上ずれていたらチューニング範囲外として扱う目安 */
    const val IN_TUNE_CENTS = 5.0

    fun fromFrequency(
        frequencyHz: Double,
        referenceAHz: Double = DEFAULT_REFERENCE_A_HZ,
    ): NoteReading? {
        if (frequencyHz <= 0.0) return null

        val midi = 69.0 + 12.0 * log2(frequencyHz / referenceAHz)
        val nearest = midi.roundToInt()
        val target = referenceAHz * 2.0.pow((nearest - 69) / 12.0)
        val name = NAMES[Math.floorMod(nearest, 12)]
        val octave = Math.floorDiv(nearest, 12) - 1

        return NoteReading(
            name = name,
            octave = octave,
            displayName = "$name$octave",
            cents = (midi - nearest) * 100.0,
            detectedHz = frequencyHz,
            targetHz = target,
        )
    }

    private fun log2(x: Double): Double = ln(x) / ln(2.0)
}
