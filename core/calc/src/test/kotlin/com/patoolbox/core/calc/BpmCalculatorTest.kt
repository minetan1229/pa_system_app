package com.patoolbox.core.calc

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BpmCalculatorTest {

    @Test
    fun `120BPMの4分音符は500ms`() {
        assertThat(BpmCalculator.millisecondsFor(120.0, NoteDivision.QUARTER))
            .isWithin(1e-9)
            .of(500.0)
    }

    @Test
    fun `60BPMの4分音符は1000ms`() {
        assertThat(BpmCalculator.millisecondsFor(60.0, NoteDivision.QUARTER))
            .isWithin(1e-9)
            .of(1000.0)
    }

    @Test
    fun `120BPMの各音符の長さ`() {
        val expected = mapOf(
            NoteDivision.WHOLE to 2000.0,
            NoteDivision.HALF to 1000.0,
            NoteDivision.QUARTER to 500.0,
            NoteDivision.EIGHTH to 250.0,
            NoteDivision.SIXTEENTH to 125.0,
            NoteDivision.THIRTY_SECOND to 62.5,
            NoteDivision.DOTTED_QUARTER to 750.0,
            NoteDivision.DOTTED_EIGHTH to 375.0,
        )
        expected.forEach { (division, ms) ->
            assertThat(BpmCalculator.millisecondsFor(120.0, division))
                .isWithin(1e-9)
                .of(ms)
        }
    }

    @Test
    fun `3連符は3つで通常2つ分になる`() {
        val eighth = BpmCalculator.millisecondsFor(120.0, NoteDivision.EIGHTH)
        val triplet = BpmCalculator.millisecondsFor(120.0, NoteDivision.EIGHTH_TRIPLET)

        // 8分3連を3つで4分音符1つ分
        assertThat(triplet * 3).isWithin(1e-9).of(eighth * 2)
        assertThat(triplet).isWithin(0.01).of(166.67)
    }

    @Test
    fun `付点は1_5倍になる`() {
        val eighth = BpmCalculator.millisecondsFor(140.0, NoteDivision.EIGHTH)
        val dotted = BpmCalculator.millisecondsFor(140.0, NoteDivision.DOTTED_EIGHTH)

        assertThat(dotted).isWithin(1e-9).of(eighth * 1.5)
    }

    @Test
    fun `msからBPMを逆算できる`() {
        assertThat(BpmCalculator.bpmFor(500.0, NoteDivision.QUARTER)).isWithin(1e-9).of(120.0)
        assertThat(BpmCalculator.bpmFor(375.0, NoteDivision.DOTTED_EIGHTH))
            .isWithin(1e-9)
            .of(120.0)
    }

    @Test
    fun `BPMとmsが往復で一致する`() {
        NoteDivision.entries.forEach { division ->
            val ms = BpmCalculator.millisecondsFor(137.0, division)
            assertThat(BpmCalculator.bpmFor(ms, division)).isWithin(1e-9).of(137.0)
        }
    }

    @Test
    fun `周波数に換算できる`() {
        // 120BPM の4分音符は 500ms = 2Hz
        assertThat(BpmCalculator.hertzFor(120.0, NoteDivision.QUARTER)).isWithin(1e-9).of(2.0)
    }

    @Test
    fun `全音符の一覧が返る`() {
        val all = BpmCalculator.allDivisions(120.0)

        assertThat(all).hasSize(NoteDivision.entries.size)
        assertThat(all[NoteDivision.QUARTER]).isWithin(1e-9).of(500.0)
    }

    @Test
    fun `3連と付点の判定が正しい`() {
        assertThat(NoteDivision.EIGHTH_TRIPLET.isTriplet).isTrue()
        assertThat(NoteDivision.EIGHTH_TRIPLET.isDotted).isFalse()
        assertThat(NoteDivision.DOTTED_EIGHTH.isDotted).isTrue()
        assertThat(NoteDivision.DOTTED_EIGHTH.isTriplet).isFalse()
        assertThat(NoteDivision.QUARTER.isTriplet).isFalse()
        assertThat(NoteDivision.QUARTER.isDotted).isFalse()
    }

    @Test
    fun `不正な入力は例外`() {
        runCatching { BpmCalculator.millisecondsFor(0.0, NoteDivision.QUARTER) }
            .also { assertThat(it.isFailure).isTrue() }
        runCatching { BpmCalculator.bpmFor(0.0, NoteDivision.QUARTER) }
            .also { assertThat(it.isFailure).isTrue() }
    }
}
