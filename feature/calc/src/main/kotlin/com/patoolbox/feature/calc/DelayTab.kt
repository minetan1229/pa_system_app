package com.patoolbox.feature.calc

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.patoolbox.core.calc.DelayCalculator
import com.patoolbox.core.calc.SpeedOfSound

/**
 * ディレイ計算。
 *
 * 距離と時間はどちらからでも引ける双方向入力にしている。
 * 「巻尺で測った距離から ms を出す」場合と「卓に入っている ms から
 * 何m相当か確認する」場合が同じくらいあるため。
 */
@Composable
internal fun DelayTab(
    modifier: Modifier = Modifier,
) {
    var temperature by rememberSaveable { mutableStateOf("20") }
    var humidity by rememberSaveable { mutableStateOf("50") }
    var distance by rememberSaveable { mutableStateOf("30") }
    var time by rememberSaveable { mutableStateOf("") }
    var towerOffset by rememberSaveable { mutableStateOf("0") }

    val speed = SpeedOfSound.forConditions(
        celsius = temperature.toDoubleOrNullLenient() ?: 20.0,
        relativeHumidityPercent = humidity.toDoubleOrNullLenient() ?: 50.0,
    )

    // 初期表示のために距離から時間を出す。以降は編集した側が優先される
    val distanceValue = distance.toDoubleOrNullLenient()
    val timeValue = time.toDoubleOrNullLenient()

    val resolvedTime = timeValue ?: distanceValue?.let {
        DelayCalculator.millisecondsForDistance(it, speed)
    }
    val resolvedDistance = distanceValue ?: timeValue?.let {
        DelayCalculator.distanceForMilliseconds(it, speed)
    }

    CalcColumn(modifier = modifier) {
        CalcSectionTitle(stringResource(R.string.calc_delay_conditions))
        CalcFieldRow {
            NumberField(
                value = temperature,
                onValueChange = { temperature = it },
                label = stringResource(R.string.calc_delay_temperature),
                modifier = Modifier.weight(1f),
            )
            NumberField(
                value = humidity,
                onValueChange = { humidity = it },
                label = stringResource(R.string.calc_delay_humidity),
                modifier = Modifier.weight(1f),
            )
        }
        CalcNote(stringResource(R.string.calc_delay_speed, speed.format(1)))

        CalcSectionTitle(stringResource(R.string.calc_delay_distance))
        NumberField(
            value = distance,
            onValueChange = {
                distance = it
                time = ""
            },
            label = stringResource(R.string.calc_delay_distance),
        )
        NumberField(
            value = time,
            onValueChange = {
                time = it
                distance = ""
            },
            label = stringResource(R.string.calc_delay_time),
        )
        CalcNote(stringResource(R.string.calc_delay_hint))

        CalcResult(
            text = "${resolvedTime.format(2)} ms / ${resolvedDistance.format(2)} m",
            emphasis = true,
        )
        CalcNote(
            stringResource(
                R.string.calc_delay_feet,
                resolvedDistance?.let { DelayCalculator.metersToFeet(it) }.format(1),
            ),
        )
        CalcNote(
            stringResource(
                R.string.calc_delay_samples,
                resolvedTime?.let {
                    DelayCalculator.samplesForMilliseconds(it, SAMPLE_RATE)
                }.format(0),
            ),
        )
        CalcNote(
            stringResource(
                R.string.calc_delay_cancellation,
                resolvedTime?.let { DelayCalculator.firstCancellationHz(it) }.format(1),
            ),
        )

        CalcSectionTitle(stringResource(R.string.calc_delay_tower))
        NumberField(
            value = towerOffset,
            onValueChange = { towerOffset = it },
            label = stringResource(R.string.calc_delay_tower_offset),
        )
        CalcResult(
            text = stringResource(
                R.string.calc_delay_tower_result,
                resolvedDistance?.let {
                    DelayCalculator.towerDelayMs(
                        mainToTowerMeters = it,
                        speedOfSoundMPerSec = speed,
                        alignmentOffsetMs = towerOffset.toDoubleOrNullLenient() ?: 0.0,
                    )
                }.format(2),
            ),
        )
        CalcNote(stringResource(R.string.calc_delay_tower_note))
    }
}

private const val SAMPLE_RATE = 48000
