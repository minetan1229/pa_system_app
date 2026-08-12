package com.patoolbox.core.calc

/** ハイインピーダンス（定電圧）配線のライン電圧。 */
enum class LineVoltage(val volts: Double, val label: String) {
    V70(70.7, "70V"),
    V100(100.0, "100V"),
}

/**
 * スピーカーのインピーダンスとアンプ負荷。
 *
 * 並列に繋ぎすぎてアンプの最低負荷を割るのは、現場でアンプを壊す典型的な原因。
 * 数を入れたら残り何台繋げるかまで出せるようにしている。
 */
object ImpedanceCalculator {

    /** 直列接続の合成インピーダンス。 */
    fun series(impedances: List<Double>): Double = impedances.sum()

    /** 並列接続の合成インピーダンス。 */
    fun parallel(impedances: List<Double>): Double {
        val valid = impedances.filter { it > 0.0 }
        if (valid.isEmpty()) return 0.0
        return 1.0 / valid.sumOf { 1.0 / it }
    }

    /** 同じインピーダンスのスピーカーを [count] 台並列にしたときの合成値。 */
    fun parallelIdentical(impedanceOhms: Double, count: Int): Double {
        require(impedanceOhms > 0.0) { "インピーダンスは正の値でなければならない" }
        require(count >= 1) { "台数は1以上" }
        return impedanceOhms / count
    }

    /**
     * アンプの最低負荷を割らずに並列にできる台数。
     *
     * @param speakerOhms スピーカー1台のインピーダンス
     * @param amplifierMinimumOhms アンプが許容する最低負荷
     */
    fun maxParallelSpeakers(speakerOhms: Double, amplifierMinimumOhms: Double): Int {
        require(speakerOhms > 0.0 && amplifierMinimumOhms > 0.0)
        return (speakerOhms / amplifierMinimumOhms).toInt().coerceAtLeast(1)
    }

    /** 負荷がアンプの許容範囲に収まっているか。 */
    fun isSafeLoad(loadOhms: Double, amplifierMinimumOhms: Double): Boolean =
        loadOhms >= amplifierMinimumOhms

    /**
     * 負荷を変えたときのアンプ出力の目安（W）。
     *
     * アンプを定電圧源と見なした近似で、負荷が半分になれば出力は倍になる。
     * 実際は電源とサーマルの制限で理想どおりには出ないので、
     * 「これ以上は出ない上限」として読むこと。
     */
    fun powerAtLoad(ratedWatts: Double, ratedOhms: Double, actualOhms: Double): Double {
        require(ratedWatts >= 0.0 && ratedOhms > 0.0 && actualOhms > 0.0)
        return ratedWatts * (ratedOhms / actualOhms)
    }

    /** 1台あたりに配分される電力（W）。同一インピーダンスのスピーカーを並列にした場合。 */
    fun powerPerSpeaker(totalWatts: Double, speakerCount: Int): Double {
        require(speakerCount >= 1)
        return totalWatts / speakerCount
    }

    // --- ハイインピーダンス（70V / 100V ライン） ---

    /**
     * トランスのタップ設定から見た負荷インピーダンス（Ω）。
     * Z = V² / W。100Vライン・60Wタップなら約167Ω。
     */
    fun highImpedanceLoadOhms(tapWatts: Double, line: LineVoltage): Double {
        require(tapWatts > 0.0) { "タップは正の値でなければならない" }
        return line.volts * line.volts / tapWatts
    }

    /** タップの合計電力（W）。アンプの定格を超えていないかの確認に使う。 */
    fun highImpedanceTotalWatts(tapWatts: List<Double>): Double = tapWatts.sum()

    /**
     * ハイインピーダンス回線の余裕。
     * アンプ定格の [safetyFactor]（既定80%）までに収めるのが定石。
     */
    fun highImpedanceRemainingWatts(
        amplifierWatts: Double,
        tapWatts: List<Double>,
        safetyFactor: Double = DEFAULT_SAFETY_FACTOR,
    ): Double = amplifierWatts * safetyFactor - highImpedanceTotalWatts(tapWatts)

    const val DEFAULT_SAFETY_FACTOR = 0.8

    /** 現場でよく使うスピーカーのインピーダンス。 */
    val COMMON_SPEAKER_OHMS = listOf(2.0, 4.0, 6.0, 8.0, 16.0)
}
