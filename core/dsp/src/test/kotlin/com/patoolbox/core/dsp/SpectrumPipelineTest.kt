package com.patoolbox.core.dsp

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SpectrumPipelineTest {

    private fun pipeline(
        fftSize: Int = 8192,
        window: WindowFunction = WindowFunction.HANN,
    ) = SpectrumPipeline(TEST_SAMPLE_RATE, fftSize = fftSize, windowFunction = window)

    /** ならしを掛けると平均に寄るので、山の位置を見るテストでは生のまま扱う */
    private fun SpectrumPipeline.analyzeOnce(
        frame: FloatArray,
        smoothing: OctaveSmoothing = OctaveSmoothing.NONE,
        offsetDb: Double = 0.0,
        peakHold: Boolean = false,
    ) = analyze(
        frame = frame,
        smoothing = smoothing,
        averagingCoefficient = 1.0,
        offsetDb = offsetDb,
        peakHold = peakHold,
    )

    @Test
    fun `純音のピーク周波数をビン幅より細かく当てる`() {
        // 1234Hz はビン中心（5.86Hz 刻み）に乗らない。放物線補間が効いていないと
        // 最寄りのビン中心 1230.5Hz に張り付く
        val snapshot = pipeline().analyzeOnce(sineAtLevel(1234.0, -20.0, 8192))

        assertThat(snapshot.peakFrequencyHz).isWithin(1.0).of(1234.0)
    }

    @Test
    fun `純音のレベルを窓の種類によらず読める`() {
        for (window in listOf(
            WindowFunction.HANN,
            WindowFunction.BLACKMAN_HARRIS,
            WindowFunction.FLAT_TOP,
        )) {
            val snapshot = pipeline(window = window)
                .analyzeOnce(sineAtLevel(1000.0, -20.0, 8192))

            assertThat(snapshot.peakLevelDb).isWithin(0.5).of(-20.0)
        }
    }

    @Test
    fun `校正オフセットはカラムにもピークにも同じだけ乗る`() {
        val frame = sineAtLevel(1000.0, -20.0, 8192)
        val plain = pipeline().analyzeOnce(frame)
        val offset = pipeline().analyzeOnce(frame, offsetDb = 94.0)

        assertThat(offset.peakLevelDb).isWithin(1e-6).of(plain.peakLevelDb + 94.0)
        assertThat(offset.columnsDb.max()).isWithin(1e-3f).of(plain.columnsDb.max() + 94.0f)
    }

    @Test
    fun `倍音を持つ信号では基音と倍音が別々の山として並ぶ`() {
        // のこぎり波の 200Hz。1/3オクターブ以上離れているので 400Hz・600Hz は別の山になる
        val snapshot = pipeline().analyzeOnce(harmonicTone(200.0, harmonics = 5, lengthSamples = 8192))

        assertThat(snapshot.topPeaks).hasSize(SpectrumPipeline.MAX_TOP_PEAKS)
        // 一番強いのは基音
        assertThat(snapshot.topPeaks.first().frequencyHz).isWithin(10.0).of(200.0)
        // 同じ山の肩で埋まっていない（全部が 200Hz 付近ではない）
        assertThat(snapshot.topPeaks.map { it.frequencyHz }.max()).isGreaterThan(300.0)
    }

    @Test
    fun `山どうしは1_3オクターブ以上離れている`() {
        val snapshot = pipeline().analyzeOnce(harmonicTone(200.0, harmonics = 5, lengthSamples = 8192))
        val sorted = snapshot.topPeaks.map { it.frequencyHz }.sorted()

        for (i in 1 until sorted.size) {
            // 1/3 オクターブ = 比 1.26。カラムの丸めぶんの余裕を見て 1.2 で判定
            assertThat(sorted[i] / sorted[i - 1]).isGreaterThan(1.2)
        }
    }

    @Test
    fun `無音では山を拾わない`() {
        val snapshot = pipeline().analyzeOnce(FloatArray(8192))

        assertThat(snapshot.topPeaks).isEmpty()
    }

    @Test
    fun `ピーク保持は小さくなっても下がらない`() {
        val pipeline = pipeline()
        pipeline.analyzeOnce(sineAtLevel(1000.0, -20.0, 8192), peakHold = true)
        val quiet = pipeline.analyzeOnce(sineAtLevel(1000.0, -60.0, 8192), peakHold = true)

        assertThat(quiet.columnsDb.max()).isLessThan(-50f)
        assertThat(quiet.peakHoldDb.max()).isGreaterThan(-30f)
    }

    @Test
    fun `ピーク保持を消すと現在値まで下がる`() {
        val pipeline = pipeline()
        pipeline.analyzeOnce(sineAtLevel(1000.0, -20.0, 8192), peakHold = true)
        pipeline.clearPeakHold()
        val after = pipeline.analyzeOnce(sineAtLevel(1000.0, -60.0, 8192), peakHold = true)

        assertThat(after.peakHoldDb.max()).isLessThan(-50f)
    }

    @Test
    fun `ピーク保持を求めなければ空の配列を返す`() {
        val snapshot = pipeline().analyzeOnce(sineAtLevel(1000.0, -20.0, 8192))

        assertThat(snapshot.peakHoldDb).isEmpty()
    }

    @Test
    fun `平均を掛けると1フレーム目より落ち着いた値になる`() {
        val pipeline = pipeline()
        val loud = sineAtLevel(1000.0, -20.0, 8192)
        val quiet = sineAtLevel(1000.0, -60.0, 8192)

        pipeline.analyze(loud, averagingCoefficient = 0.25)
        val second = pipeline.analyze(quiet, averagingCoefficient = 0.25)

        // 1フレームで -60 まで落ちきらない（前の値を 75% 引きずる）
        assertThat(second.columnsDb.max()).isGreaterThan(-40f)
    }

    @Test
    fun `カーソルの逆引きはカラムの周波数と一致する`() {
        val pipeline = pipeline()
        val column = pipeline.columnOf(1000.0)

        assertThat(pipeline.frequencies[column]).isWithin(20.0).of(1000.0)
    }

    @Test
    fun `更新間隔は50パーセント重なりぶん`() {
        val pipeline = pipeline(fftSize = 8192)

        // 8192/2 = 4096 サンプル = 48kHz で 85.3ms
        assertThat(pipeline.hopSeconds).isWithin(1e-6).of(4096.0 / TEST_SAMPLE_RATE)
    }

    @Test
    fun `ブロックを溜めてフレームが揃うと解析できる`() {
        val pipeline = pipeline()
        val signal = sineAtLevel(1000.0, -20.0, 8192)
        val block = FloatArray(1024)
        var frames = 0

        var offset = 0
        while (offset < signal.size) {
            signal.copyInto(block, 0, offset, offset + block.size)
            pipeline.accumulator.add(block, block.size) { frames++ }
            offset += block.size
        }

        assertThat(frames).isEqualTo(1)
    }

    @Test
    fun `リセットすると平均もピークも捨てられる`() {
        val pipeline = pipeline()
        pipeline.analyzeOnce(sineAtLevel(1000.0, -20.0, 8192), peakHold = true)
        pipeline.reset()
        val after = pipeline.analyzeOnce(sineAtLevel(1000.0, -60.0, 8192), peakHold = true)

        assertThat(after.peakHoldDb.max()).isLessThan(-50f)
    }
}
