package com.patoolbox.core.dsp

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LogSpectrumMapperTest {

    private val analyzer = SpectrumAnalyzer(TEST_SAMPLE_RATE, fftSize = 8192)

    private fun mapper(columns: Int = 256) = LogSpectrumMapper(
        binCount = analyzer.binCount,
        binWidthHz = analyzer.binWidthHz,
        columns = columns,
    )

    private fun spectrumOf(frequencyHz: Double, levelDbFs: Double = -20.0): DoubleArray =
        analyzer.powerSpectrum(sineAtLevel(frequencyHz, levelDbFs, 8192)).copyOf()

    @Test
    fun `カラムは対数間隔で並ぶ`() {
        val mapper = mapper()

        // 隣り合うカラムの比が一定であること（＝対数軸）
        val first = mapper.frequencies[1] / mapper.frequencies[0]
        val last = mapper.frequencies[255] / mapper.frequencies[254]
        assertThat(last).isWithin(1e-9).of(first)
        assertThat(mapper.frequencies.first()).isWithin(1e-9).of(20.0)
        assertThat(mapper.frequencies.last()).isWithin(1.0).of(20000.0)
    }

    @Test
    fun `純音は該当するカラムに出る`() {
        val mapper = mapper()
        val columns = mapper.map(spectrumOf(1000.0))

        val peak = columns.indices.maxBy { columns[it] }
        assertThat(mapper.frequencies[peak]).isWithin(20.0).of(1000.0)
    }

    @Test
    fun `高域の純音もカラムに潰されずに残る`() {
        // 10kHz 付近では1カラムが 46 ビンぶんに相当する。平均で畳むと
        // 単一のピークが 1/46 に薄まって見えなくなる。最大を取る理由がこれ
        val mapper = mapper()
        val level1k = mapper.map(spectrumOf(1000.0)).max()
        val level10k = mapper.map(spectrumOf(10000.0)).max()

        assertThat(amplitudeToDb(level10k) - amplitudeToDb(level1k)).isWithin(1.0).of(0.0)
    }

    @Test
    fun `ならすとピークは低くなり裾が広がる`() {
        val mapper = mapper()
        val spectrum = spectrumOf(1000.0)

        val raw = mapper.map(spectrum, OctaveSmoothing.NONE).copyOf()
        val smoothed = mapper.map(spectrum, OctaveSmoothing.THIRD).copyOf()

        val peakColumn = raw.indices.maxBy { raw[it] }
        assertThat(smoothed[peakColumn]).isLessThan(raw[peakColumn])

        // 1/3オクターブ離れた位置では、ならした方が高い（裾が持ち上がる）
        val offColumn = mapper.columnOf(1000.0 * 1.12)
        assertThat(smoothed[offColumn]).isGreaterThan(raw[offColumn])
    }

    @Test
    fun `ならしたホワイトノイズは平坦になる`() {
        // ビンあたりのパワーを見ているので、白色雑音は周波数に依らず一定のはず。
        // 帯域の合計を出しているなら高域ほど上がってしまう
        val noise = WhiteNoiseSource(TEST_SAMPLE_RATE, levelDbFs = -20.0)
        val mapper = mapper()

        val averaged = DoubleArray(mapper.columns)
        val scratch = DoubleArray(mapper.columns)
        repeat(20) {
            val spectrum = analyzer.powerSpectrum(noise.render(8192))
            mapper.map(spectrum, OctaveSmoothing.THIRD, scratch)
            for (i in averaged.indices) averaged[i] += scratch[i] / 20.0
        }

        // 100Hz〜10kHz を比較する。両端はビンの粗さと帯域端の影響が出る
        val from = mapper.columnOf(100.0)
        val to = mapper.columnOf(10000.0)
        val levels = (from..to).map { amplitudeToDb(averaged[it]) }
        assertThat(levels.max() - levels.min()).isLessThan(4.0)
    }

    @Test
    fun `ナイキストを超える指定は切り詰める`() {
        val mapper = LogSpectrumMapper(
            binCount = analyzer.binCount,
            binWidthHz = analyzer.binWidthHz,
            maxHz = 40000.0,
        )

        assertThat(mapper.maxHz).isAtMost(TEST_SAMPLE_RATE / 2.0)
    }

    @Test
    fun `周波数からカラムを逆引きできる`() {
        val mapper = mapper()

        for (hz in listOf(20.0, 100.0, 1000.0, 10000.0)) {
            val column = mapper.columnOf(hz)
            assertThat(mapper.frequencies[column]).isWithin(hz * 0.02).of(hz)
        }
    }

    @Test
    fun `範囲外の周波数は端に丸める`() {
        val mapper = mapper()

        assertThat(mapper.columnOf(1.0)).isEqualTo(0)
        assertThat(mapper.columnOf(100000.0)).isEqualTo(mapper.columns - 1)
    }
}
