package com.patoolbox.core.calc

/**
 * 音速。ディレイタイムの計算はすべてこれに依存する。
 *
 * 屋外の現場では朝と本番で気温が10℃以上変わることがあり、そのぶん音速も変わる。
 * 30m 先のディレイタワーだと 10℃で約 2.5ms ずれるので、無視できない。
 */
object SpeedOfSound {

    /** 20℃・湿度50% のときの音速。既定値として使う */
    const val DEFAULT_M_PER_SEC = 344.0

    const val MIN_CELSIUS = -20.0
    const val MAX_CELSIUS = 50.0

    /**
     * 気温と相対湿度から音速（m/s）を求める。
     *
     * c = 331.4 + 0.6t + 0.0124h の近似式。
     * 常温・常圧の範囲では実測と 0.1% 程度で一致し、現場の計算には十分。
     * 厳密な値が必要な場合は Cramer の式を使うことになるが、
     * 気温の読み取り誤差のほうが大きいので実用上の意味がない。
     *
     * @param celsius 気温（℃）
     * @param relativeHumidityPercent 相対湿度（%）
     */
    fun forConditions(celsius: Double, relativeHumidityPercent: Double = 50.0): Double =
        331.4 + 0.6 * celsius + 0.0124 * relativeHumidityPercent.coerceIn(0.0, 100.0)

    /** 湿度を無視した乾燥空気の音速。 */
    fun forTemperature(celsius: Double): Double = forConditions(celsius, 0.0)
}
