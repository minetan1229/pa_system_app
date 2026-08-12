package com.patoolbox.core.dsp

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NoteNamesTest {

    @Test
    fun `440HzはA4でぴったり`() {
        val note = NoteNames.fromFrequency(440.0)!!

        assertThat(note.displayName).isEqualTo("A4")
        assertThat(note.cents).isWithin(0.01).of(0.0)
        assertThat(note.targetHz).isWithin(0.01).of(440.0)
    }

    @Test
    fun `主要な音名が正しい`() {
        val expected = mapOf(
            261.626 to "C4",
            329.628 to "E4",
            440.0 to "A4",
            466.164 to "A#4",
            880.0 to "A5",
            110.0 to "A2",
            82.407 to "E2",
        )
        expected.forEach { (hz, name) ->
            assertThat(NoteNames.fromFrequency(hz)!!.displayName).isEqualTo(name)
        }
    }

    @Test
    fun `高いとセントが正になる`() {
        val note = NoteNames.fromFrequency(445.0)!!

        assertThat(note.displayName).isEqualTo("A4")
        assertThat(note.cents).isGreaterThan(0.0)
        assertThat(note.cents).isWithin(1.0).of(19.6)
    }

    @Test
    fun `低いとセントが負になる`() {
        val note = NoteNames.fromFrequency(435.0)!!

        assertThat(note.displayName).isEqualTo("A4")
        assertThat(note.cents).isLessThan(0.0)
    }

    @Test
    fun `セントは前後50に収まる`() {
        var hz = 80.0
        while (hz < 2000.0) {
            val note = NoteNames.fromFrequency(hz)!!
            assertThat(note.cents).isAtLeast(-50.0)
            assertThat(note.cents).isAtMost(50.0)
            hz *= 1.01
        }
    }

    @Test
    fun `A基準を変えると目標周波数が動く`() {
        val note = NoteNames.fromFrequency(442.0, referenceAHz = 442.0)!!

        assertThat(note.displayName).isEqualTo("A4")
        assertThat(note.cents).isWithin(0.01).of(0.0)
        assertThat(note.targetHz).isWithin(0.01).of(442.0)
    }

    @Test
    fun `A基準442では440はわずかに低いA4`() {
        val note = NoteNames.fromFrequency(440.0, referenceAHz = 442.0)!!

        assertThat(note.displayName).isEqualTo("A4")
        assertThat(note.cents).isLessThan(0.0)
        assertThat(note.cents).isWithin(1.0).of(-7.85)
    }

    @Test
    fun `不正な周波数はnull`() {
        assertThat(NoteNames.fromFrequency(0.0)).isNull()
        assertThat(NoteNames.fromFrequency(-100.0)).isNull()
    }
}
