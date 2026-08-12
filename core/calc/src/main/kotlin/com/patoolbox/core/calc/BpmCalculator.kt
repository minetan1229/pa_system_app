package com.patoolbox.core.calc

/**
 * 音符の長さ。[quarterNoteFactor] は4分音符を1としたときの倍率。
 */
enum class NoteDivision(val label: String, val quarterNoteFactor: Double) {
    WHOLE("1/1", 4.0),
    HALF("1/2", 2.0),
    QUARTER("1/4", 1.0),
    EIGHTH("1/8", 0.5),
    SIXTEENTH("1/16", 0.25),
    THIRTY_SECOND("1/32", 0.125),

    // 3連符: 3つで通常2つ分の長さになる
    QUARTER_TRIPLET("1/4 3連", 2.0 / 3.0),
    EIGHTH_TRIPLET("1/8 3連", 1.0 / 3.0),
    SIXTEENTH_TRIPLET("1/16 3連", 1.0 / 6.0),

    // 付点: 1.5倍
    DOTTED_HALF("1/2 付点", 3.0),
    DOTTED_QUARTER("1/4 付点", 1.5),
    DOTTED_EIGHTH("1/8 付点", 0.75),
    ;

    val isTriplet: Boolean get() = label.contains("3連")
    val isDotted: Boolean get() = label.contains("付点")
}

/**
 * BPM とディレイ/リバーブのタイム換算。
 *
 * ディレイを曲のテンポに合わせるのと、リバーブのプリディレイを
 * 「付点8分」に置くような使い方をする。
 */
object BpmCalculator {

    const val MIN_BPM = 20.0
    const val MAX_BPM = 300.0

    private const val MILLIS_PER_MINUTE = 60_000.0

    /** BPM と音符 → 時間（ms）。 */
    fun millisecondsFor(bpm: Double, division: NoteDivision): Double {
        require(bpm > 0.0) { "BPM は正の値でなければならない" }
        return MILLIS_PER_MINUTE / bpm * division.quarterNoteFactor
    }

    /** 時間（ms）と音符 → BPM。既存のディレイ設定から曲のテンポを逆算するとき。 */
    fun bpmFor(milliseconds: Double, division: NoteDivision): Double {
        require(milliseconds > 0.0) { "時間は正の値でなければならない" }
        return MILLIS_PER_MINUTE / milliseconds * division.quarterNoteFactor
    }

    /** その音符の周期に相当する周波数（Hz）。LFO やトレモロの設定に使う。 */
    fun hertzFor(bpm: Double, division: NoteDivision): Double =
        1000.0 / millisecondsFor(bpm, division)

    /** すべての音符の長さを一覧で返す（表示用）。 */
    fun allDivisions(bpm: Double): Map<NoteDivision, Double> =
        NoteDivision.entries.associateWith { millisecondsFor(bpm, it) }
}
