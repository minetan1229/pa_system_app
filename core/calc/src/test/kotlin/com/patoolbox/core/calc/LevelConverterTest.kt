package com.patoolbox.core.calc

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LevelConverterTest {

    @Test
    fun `0dBuは0_7746V`() {
        assertThat(LevelConverter.dbuToVolts(0.0)).isWithin(1e-6).of(0.774596669)
    }

    @Test
    fun `プロ機器の基準レベル`() {
        // +4 dBu = 1.228 Vrms
        assertThat(LevelConverter.dbuToVolts(4.0)).isWithin(0.001).of(1.228)
    }

    @Test
    fun `0dBVは1V`() {
        assertThat(LevelConverter.dbvToVolts(0.0)).isWithin(1e-9).of(1.0)
    }

    @Test
    fun `民生機器の基準レベル`() {
        // -10 dBV = 0.3162 Vrms
        assertThat(LevelConverter.dbvToVolts(-10.0)).isWithin(0.0001).of(0.3162)
    }

    @Test
    fun `dBuとdBVの差は約2_2dB`() {
        assertThat(LevelConverter.DBU_TO_DBV_OFFSET).isWithin(0.001).of(-2.2185)
    }

    @Test
    fun `プロとコンシューマのレベル差`() {
        // +4dBu と -10dBV の差は約 11.8dB。これがそのまま音量差になる
        val proDbv = LevelConverter.dbuToDbv(4.0)
        assertThat(proDbv).isWithin(0.01).of(1.78)

        val consumerDbu = LevelConverter.dbvToDbu(-10.0)
        assertThat(consumerDbu).isWithin(0.01).of(-7.78)
        assertThat(4.0 - consumerDbu).isWithin(0.01).of(11.78)
    }

    @Test
    fun `電圧とdBuが往復で一致する`() {
        val volts = 2.5
        assertThat(LevelConverter.dbuToVolts(LevelConverter.voltsToDbu(volts)))
            .isWithin(1e-9)
            .of(volts)
    }

    @Test
    fun `8オームで100Wは約28_3V`() {
        assertThat(LevelConverter.voltsFor(100.0, 8.0)).isWithin(0.01).of(28.28)
        assertThat(LevelConverter.wattsFor(28.284271, 8.0)).isWithin(0.01).of(100.0)
    }

    @Test
    fun `同じ電圧なら負荷が半分で電力は倍`() {
        val volts = 28.28
        val at8 = LevelConverter.wattsFor(volts, 8.0)
        val at4 = LevelConverter.wattsFor(volts, 4.0)

        assertThat(at4 / at8).isWithin(0.01).of(2.0)
    }

    @Test
    fun `電圧比2倍は6dB`() {
        assertThat(LevelConverter.voltageRatioToDb(2.0)).isWithin(0.01).of(6.02)
        assertThat(LevelConverter.dbToVoltageRatio(6.0206)).isWithin(0.001).of(2.0)
    }

    @Test
    fun `電力比2倍は3dB`() {
        assertThat(LevelConverter.powerRatioToDb(2.0)).isWithin(0.01).of(3.01)
        assertThat(LevelConverter.dbToPowerRatio(3.0103)).isWithin(0.001).of(2.0)
    }

    @Test
    fun `距離が2倍で6dB下がる`() {
        assertThat(LevelConverter.distanceAttenuationDb(1.0, 2.0)).isWithin(0.01).of(-6.02)
        assertThat(LevelConverter.distanceAttenuationDb(10.0, 20.0)).isWithin(0.01).of(-6.02)
        // 近づけば上がる
        assertThat(LevelConverter.distanceAttenuationDb(2.0, 1.0)).isWithin(0.01).of(6.02)
    }

    @Test
    fun `ヘッドルームが計算できる`() {
        // ピーク +10dBu、最大入力 +22dBu なら余裕は 12dB
        assertThat(LevelConverter.headroomDb(peakDb = 10.0, maxDb = 22.0)).isWithin(1e-9).of(12.0)
        // 超えていれば負
        assertThat(LevelConverter.headroomDb(peakDb = 26.0, maxDb = 22.0)).isWithin(1e-9).of(-4.0)
    }

    @Test
    fun `必要なパッド量が出る`() {
        assertThat(LevelConverter.requiredPadDb(inputDb = 30.0, maxInputDb = 22.0))
            .isWithin(1e-9)
            .of(8.0)
        // 余裕があるならパッドは不要（負の値にしない）
        assertThat(LevelConverter.requiredPadDb(inputDb = 10.0, maxInputDb = 22.0))
            .isWithin(1e-9)
            .of(0.0)
    }

    @Test
    fun `0V付近でも無限大にならない`() {
        assertThat(LevelConverter.voltsToDbu(0.0)).isFinite()
        assertThat(LevelConverter.voltageRatioToDb(0.0)).isFinite()
        assertThat(LevelConverter.powerRatioToDb(0.0)).isFinite()
    }

    @Test
    fun `不正な負荷は例外`() {
        runCatching { LevelConverter.wattsFor(1.0, 0.0) }
            .also { assertThat(it.isFailure).isTrue() }
        runCatching { LevelConverter.distanceAttenuationDb(0.0, 1.0) }
            .also { assertThat(it.isFailure).isTrue() }
    }
}
