package com.patoolbox.core.dsp

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

class PitchDetectorTest {

    private val detector = PitchDetector(TEST_SAMPLE_RATE)

    @Test
    fun `A4のサイン波を検出する`() {
        val pitch = detector.detect(sine(440.0, lengthSamples = 4096))

        assertThat(pitch).isNotNull()
        assertThat(pitch!!.frequencyHz).isWithin(0.5).of(440.0)
        assertThat(pitch.clarity).isGreaterThan(0.9)
    }

    @Test
    fun `楽器の音域全体で検出できる`() {
        // ベースの最低音からギターのハイポジションまで
        val frequencies = listOf(41.2, 82.41, 110.0, 220.0, 440.0, 880.0, 1318.5)
        frequencies.forEach { hz ->
            val pitch = detector.detect(sine(hz, lengthSamples = 4096))
            assertWithMessage("%s Hz", hz).that(pitch).isNotNull()
            assertWithMessage("%s Hz", hz)
                .that(pitch!!.frequencyHz)
                .isWithin(hz * 0.005)
                .of(hz)
        }
    }

    @Test
    fun `倍音が多くてもオクターブを間違えない`() {
        // 素の自己相関だと 110Hz に落ちやすいケース
        val pitch = detector.detect(harmonicTone(220.0, harmonics = 10, lengthSamples = 4096))

        assertThat(pitch).isNotNull()
        assertThat(pitch!!.frequencyHz).isWithin(2.0).of(220.0)
    }

    @Test
    fun `低い音でも倍音に引っ張られない`() {
        val pitch = detector.detect(harmonicTone(82.41, harmonics = 12, lengthSamples = 4096))

        assertThat(pitch).isNotNull()
        assertThat(pitch!!.frequencyHz).isWithin(1.0).of(82.41)
    }

    @Test
    fun `無音では検出しない`() {
        assertThat(detector.detect(FloatArray(4096))).isNull()
    }

    @Test
    fun `白色ノイズでは高い確度を返さない`() {
        val noise = WhiteNoiseSource(TEST_SAMPLE_RATE, levelDbFs = -20.0, seed = 3).render(4096)
        val pitch = detector.detect(noise)

        // 検出しても構わないが、周期性が高いと主張してはいけない
        if (pitch != null) {
            assertThat(pitch.clarity).isLessThan(0.9)
        }
    }

    @Test
    fun `直流オフセットがあっても検出できる`() {
        val signal = sine(440.0, amplitude = 0.5, lengthSamples = 4096)
        for (i in signal.indices) signal[i] += 0.3f

        val pitch = detector.detect(signal)

        assertThat(pitch).isNotNull()
        assertThat(pitch!!.frequencyHz).isWithin(0.5).of(440.0)
    }

    @Test
    fun `検出範囲外は検出しない`() {
        // 既定は 40Hz..2000Hz
        val pitch = detector.detect(sine(20.0, lengthSamples = 4096))

        if (pitch != null) {
            assertThat(pitch.frequencyHz).isAtLeast(40.0)
        }
    }

    @Test
    fun `小さい音でも検出できる`() {
        val quiet = sineAtLevel(440.0, levelDbFs = -50.0, lengthSamples = 4096)
        val pitch = detector.detect(quiet)

        assertThat(pitch).isNotNull()
        assertThat(pitch!!.frequencyHz).isWithin(0.5).of(440.0)
    }
}
