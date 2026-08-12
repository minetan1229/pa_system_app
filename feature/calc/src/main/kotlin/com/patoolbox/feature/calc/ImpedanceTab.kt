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
import com.patoolbox.core.calc.ImpedanceCalculator
import com.patoolbox.core.calc.LineVoltage

internal enum class Wiring { PARALLEL, SERIES }

/**
 * インピーダンスとアンプ負荷。
 *
 * 並列に繋ぎすぎてアンプの最低負荷を割るのが現場でアンプを壊す典型例なので、
 * 「安全か」「あと何台繋げるか」を必ず出す。
 */
@Composable
internal fun ImpedanceTab(
    minTouch: Dp,
    modifier: Modifier = Modifier,
) {
    var speakerOhms by rememberSaveable { mutableStateOf("8") }
    var count by rememberSaveable { mutableStateOf("2") }
    var wiring by rememberSaveable { mutableStateOf(Wiring.PARALLEL) }
    var ampMinOhms by rememberSaveable { mutableStateOf("4") }
    var ratedWatts by rememberSaveable { mutableStateOf("500") }
    var ratedOhms by rememberSaveable { mutableStateOf("8") }
    var line by rememberSaveable { mutableStateOf(LineVoltage.V100) }
    var tapTotal by rememberSaveable { mutableStateOf("45") }
    var highZAmpWatts by rememberSaveable { mutableStateOf("120") }

    val ohms = speakerOhms.toDoubleOrNullLenient()?.takeIf { it > 0.0 }
    val speakerCount = count.toDoubleOrNullLenient()?.toInt()?.coerceAtLeast(1)
    val load = if (ohms != null && speakerCount != null) {
        when (wiring) {
            Wiring.PARALLEL -> ImpedanceCalculator.parallelIdentical(ohms, speakerCount)
            Wiring.SERIES -> ImpedanceCalculator.series(List(speakerCount) { ohms })
        }
    } else {
        null
    }
    val ampMin = ampMinOhms.toDoubleOrNullLenient()?.takeIf { it > 0.0 }

    CalcColumn(modifier = modifier) {
        CalcFieldRow {
            NumberField(
                value = speakerOhms,
                onValueChange = { speakerOhms = it },
                label = stringResource(R.string.calc_imp_speaker),
                modifier = Modifier.weight(1f),
            )
            NumberField(
                value = count,
                onValueChange = { count = it },
                label = stringResource(R.string.calc_imp_count),
                modifier = Modifier.weight(1f),
            )
        }
        CalcSelector(
            options = listOf(
                Wiring.PARALLEL to stringResource(R.string.calc_imp_parallel),
                Wiring.SERIES to stringResource(R.string.calc_imp_series),
            ),
            selected = wiring,
            onSelect = { wiring = it },
            minTouch = minTouch,
        )
        CalcResult(
            text = stringResource(R.string.calc_imp_result, load.format(2)),
            emphasis = true,
        )

        NumberField(
            value = ampMinOhms,
            onValueChange = { ampMinOhms = it },
            label = stringResource(R.string.calc_imp_amp_min),
        )
        if (load != null && ampMin != null) {
            val safe = ImpedanceCalculator.isSafeLoad(load, ampMin)
            Text(
                text = stringResource(
                    if (safe) R.string.calc_imp_safe else R.string.calc_imp_unsafe,
                ),
                style = MaterialTheme.typography.titleMedium,
                color = if (safe) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
            if (ohms != null && wiring == Wiring.PARALLEL) {
                CalcNote(
                    stringResource(
                        R.string.calc_imp_max_count,
                        ImpedanceCalculator.maxParallelSpeakers(ohms, ampMin),
                    ),
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        CalcSectionTitle(stringResource(R.string.calc_imp_amp_power))
        CalcFieldRow {
            NumberField(
                value = ratedWatts,
                onValueChange = { ratedWatts = it },
                label = stringResource(R.string.calc_imp_rated_watts),
                modifier = Modifier.weight(1f),
            )
            NumberField(
                value = ratedOhms,
                onValueChange = { ratedOhms = it },
                label = stringResource(R.string.calc_imp_rated_ohms),
                modifier = Modifier.weight(1f),
            )
        }
        val totalPower = ratedWatts.toDoubleOrNullLenient()?.takeIf { it >= 0.0 }?.let { watts ->
            ratedOhms.toDoubleOrNullLenient()?.takeIf { it > 0.0 }?.let { rated ->
                load?.let { ImpedanceCalculator.powerAtLoad(watts, rated, it) }
            }
        }
        CalcResult(
            text = stringResource(
                R.string.calc_imp_power_result,
                totalPower.format(0),
                totalPower?.let {
                    ImpedanceCalculator.powerPerSpeaker(it, speakerCount ?: 1)
                }.format(0),
            ),
        )
        CalcNote(stringResource(R.string.calc_imp_power_note))

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        CalcSectionTitle(stringResource(R.string.calc_imp_high_z))
        CalcSelector(
            options = LineVoltage.entries.map { it to it.label },
            selected = line,
            onSelect = { line = it },
            minTouch = minTouch,
        )
        CalcFieldRow {
            NumberField(
                value = tapTotal,
                onValueChange = { tapTotal = it },
                label = stringResource(R.string.calc_imp_tap),
                modifier = Modifier.weight(1f),
            )
            NumberField(
                value = highZAmpWatts,
                onValueChange = { highZAmpWatts = it },
                label = stringResource(R.string.calc_imp_high_z_amp),
                modifier = Modifier.weight(1f),
            )
        }
        val taps = tapTotal.toDoubleOrNullLenient()?.takeIf { it > 0.0 }
        val remaining = highZAmpWatts.toDoubleOrNullLenient()?.takeIf { it > 0.0 }?.let { amp ->
            taps?.let {
                ImpedanceCalculator.highImpedanceRemainingWatts(amp, listOf(it))
            }
        }
        CalcResult(
            text = if (remaining != null && remaining < 0.0) {
                stringResource(R.string.calc_imp_high_z_over)
            } else {
                stringResource(R.string.calc_imp_high_z_remaining, remaining.format(1))
            },
        )
        CalcNote(
            stringResource(
                R.string.calc_imp_high_z_load,
                taps?.let { ImpedanceCalculator.highImpedanceLoadOhms(it, line) }.format(1),
            ),
        )
    }
}
