package com.patoolbox.feature.calc

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patoolbox.core.calc.CoverageCalculator
import com.patoolbox.core.calc.PowerCalculator
import com.patoolbox.core.calc.WiringSystem

/**
 * 電源計算。
 *
 * ブレーカーが落ちるのは計算していないか力率を見ていないかのどちらかなので、
 * 「その回路で使ってよいか」を必ず判定して出す。
 */
@Composable
internal fun PowerTab(
    minTouch: Dp,
    modifier: Modifier = Modifier,
) {
    var watts by rememberSaveable { mutableStateOf("1500") }
    var volts by rememberSaveable { mutableStateOf("100") }
    var powerFactor by rememberSaveable { mutableStateOf("0.9") }
    var system by rememberSaveable { mutableStateOf(WiringSystem.SINGLE_PHASE_2WIRE) }
    var breaker by rememberSaveable { mutableStateOf("20") }
    var length by rememberSaveable { mutableStateOf("50") }
    var crossSection by rememberSaveable { mutableStateOf("3.5") }

    val voltsValue = volts.toDoubleOrNullLenient()?.takeIf { it > 0.0 }
    val pf = powerFactor.toDoubleOrNullLenient()?.takeIf { it > 0.0 && it <= 1.0 }
    val current = watts.toDoubleOrNullLenient()?.let { w ->
        voltsValue?.let { v -> PowerCalculator.currentAmps(w, v, system, pf ?: 1.0) }
    }
    val breakerValue = breaker.toDoubleOrNullLenient()?.takeIf { it > 0.0 }

    CalcColumn(modifier = modifier) {
        CalcSectionTitle(stringResource(R.string.calc_power_load))
        CalcFieldRow {
            NumberField(watts, { watts = it }, stringResource(R.string.calc_power_watts), Modifier.weight(1f))
            NumberField(volts, { volts = it }, stringResource(R.string.calc_power_volts), Modifier.weight(1f))
        }
        NumberField(powerFactor, { powerFactor = it }, stringResource(R.string.calc_power_pf))
        CalcSelector(
            options = WiringSystem.entries.map { it to it.label },
            selected = system,
            onSelect = { system = it },
            minTouch = minTouch,
        )
        CalcResult(
            text = stringResource(R.string.calc_power_current, current.format(2)),
            emphasis = true,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        CalcSectionTitle(stringResource(R.string.calc_power_breaker))
        NumberField(breaker, { breaker = it }, stringResource(R.string.calc_power_breaker_amps))
        if (current != null && breakerValue != null) {
            val safe = PowerCalculator.isWithinBreaker(current, breakerValue)
            Text(
                text = stringResource(
                    if (safe) R.string.calc_power_ok else R.string.calc_power_over,
                    PowerCalculator.continuousCapacityAmps(breakerValue).format(1),
                ),
                style = MaterialTheme.typography.titleMedium,
                color = if (safe) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
            CalcNote(
                stringResource(
                    R.string.calc_power_usable,
                    PowerCalculator.usableWatts(
                        breakerValue,
                        voltsValue ?: 100.0,
                        system,
                        pf ?: 1.0,
                    ).format(0),
                ),
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        CalcSectionTitle(stringResource(R.string.calc_power_drop))
        CalcFieldRow {
            NumberField(length, { length = it }, stringResource(R.string.calc_power_length), Modifier.weight(1f))
            NumberField(crossSection, { crossSection = it }, stringResource(R.string.calc_power_section), Modifier.weight(1f))
        }
        val dropPercent = current?.let { amps ->
            crossSection.toDoubleOrNullLenient()?.takeIf { it > 0.0 }?.let { area ->
                length.toDoubleOrNullLenient()?.let { len ->
                    PowerCalculator.voltageDropPercent(len, amps, area, voltsValue ?: 100.0, system)
                }
            }
        }
        CalcResult(
            text = stringResource(
                R.string.calc_power_drop_result,
                current?.let { amps ->
                    crossSection.toDoubleOrNullLenient()?.takeIf { it > 0.0 }?.let { area ->
                        length.toDoubleOrNullLenient()?.let { len ->
                            PowerCalculator.voltageDropVolts(len, amps, area, system)
                        }
                    }
                }.format(2),
                dropPercent.format(2),
            ),
        )
        if (dropPercent != null) {
            Text(
                text = stringResource(
                    if (PowerCalculator.isVoltageDropAcceptable(dropPercent)) {
                        R.string.calc_power_drop_ok
                    } else {
                        R.string.calc_power_drop_over
                    },
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = if (PowerCalculator.isVoltageDropAcceptable(dropPercent)) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        }
        CalcNote(stringResource(R.string.calc_power_note))
    }
}

/** 距離減衰とカバー範囲。台数と吊り位置を決めるための道具。 */
@Composable
internal fun CoverageTab(
    modifier: Modifier = Modifier,
) {
    var splAtOneMeter by rememberSaveable { mutableStateOf("130") }
    var distance by rememberSaveable { mutableStateOf("30") }
    var nearDistance by rememberSaveable { mutableStateOf("5") }
    var dispersion by rememberSaveable { mutableStateOf("90") }
    var count by rememberSaveable { mutableStateOf("2") }

    val onemeter = splAtOneMeter.toDoubleOrNullLenient()
    val far = distance.toDoubleOrNullLenient()?.takeIf { it > 0.0 }
    val near = nearDistance.toDoubleOrNullLenient()?.takeIf { it > 0.0 }
    val angle = dispersion.toDoubleOrNullLenient()?.takeIf { it > 0.0 && it < 180.0 }

    CalcColumn(modifier = modifier) {
        CalcSectionTitle(stringResource(R.string.calc_coverage_level))
        CalcFieldRow {
            NumberField(splAtOneMeter, { splAtOneMeter = it }, stringResource(R.string.calc_coverage_spl1m), Modifier.weight(1f))
            NumberField(distance, { distance = it }, stringResource(R.string.calc_coverage_distance), Modifier.weight(1f))
        }
        CalcResult(
            text = stringResource(
                R.string.calc_coverage_spl_result,
                (onemeter to far).let { (s, d) ->
                    if (s != null && d != null) CoverageCalculator.splFromOneMeter(s, d) else null
                }.format(1),
            ),
            emphasis = true,
        )

        NumberField(nearDistance, { nearDistance = it }, stringResource(R.string.calc_coverage_near))
        val difference = if (near != null && far != null) {
            CoverageCalculator.frontToBackDifferenceDb(near, far)
        } else {
            null
        }
        CalcResult(
            text = stringResource(R.string.calc_coverage_difference, difference.format(1)),
        )
        if (difference != null && difference > 10.0) {
            CalcNote(stringResource(R.string.calc_coverage_delay_hint))
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        CalcSectionTitle(stringResource(R.string.calc_coverage_width))
        NumberField(dispersion, { dispersion = it }, stringResource(R.string.calc_coverage_dispersion))
        CalcResult(
            text = stringResource(
                R.string.calc_coverage_width_result,
                (angle to far).let { (a, d) ->
                    if (a != null && d != null) CoverageCalculator.coverageWidthMeters(a, d) else null
                }.format(1),
            ),
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        CalcSectionTitle(stringResource(R.string.calc_coverage_multiple))
        NumberField(count, { count = it }, stringResource(R.string.calc_coverage_count))
        val gain = count.toDoubleOrNullLenient()?.toInt()?.takeIf { it >= 1 }?.let {
            CoverageCalculator.gainFromMultipleSources(it)
        }
        CalcResult(text = stringResource(R.string.calc_coverage_gain, gain.format(1)))
        CalcNote(stringResource(R.string.calc_coverage_note))
    }
}
