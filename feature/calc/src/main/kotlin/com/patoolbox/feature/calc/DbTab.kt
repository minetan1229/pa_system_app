package com.patoolbox.feature.calc

import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patoolbox.core.calc.LevelConverter

/** 入力に使う単位。 */
internal enum class LevelUnit { DBU, DBV, VOLTS }

/**
 * dB 換算とゲインステージ。
 *
 * dBu と dBV を混同したままゲインを組むのが現場でよくある事故なので、
 * 1つ入れたら残り全部を同時に出す形にしている。
 */
@Composable
internal fun DbTab(
    minTouch: Dp,
    modifier: Modifier = Modifier,
) {
    var levelText by rememberSaveable { mutableStateOf("4") }
    var unit by rememberSaveable { mutableStateOf(LevelUnit.DBU) }
    var watts by rememberSaveable { mutableStateOf("100") }
    var impedance by rememberSaveable { mutableStateOf("8") }
    var fromMeters by rememberSaveable { mutableStateOf("1") }
    var toMeters by rememberSaveable { mutableStateOf("2") }

    val input = levelText.toDoubleOrNullLenient()

    // どの単位で入れても電圧に正規化してから全部出す
    val volts = input?.let {
        when (unit) {
            LevelUnit.DBU -> LevelConverter.dbuToVolts(it)
            LevelUnit.DBV -> LevelConverter.dbvToVolts(it)
            LevelUnit.VOLTS -> it
        }
    }

    CalcColumn(modifier = modifier) {
        CalcSectionTitle(stringResource(R.string.calc_db_voltage))
        NumberField(
            value = levelText,
            onValueChange = { levelText = it },
            label = stringResource(R.string.calc_db_value),
        )
        CalcSelector(
            options = listOf(
                LevelUnit.DBU to stringResource(R.string.calc_db_unit_dbu),
                LevelUnit.DBV to stringResource(R.string.calc_db_unit_dbv),
                LevelUnit.VOLTS to stringResource(R.string.calc_db_unit_volts),
            ),
            selected = unit,
            onSelect = { unit = it },
            minTouch = minTouch,
        )

        CalcResult(
            text = buildString {
                append("${volts?.let { LevelConverter.voltsToDbu(it) }.format(2)} dBu\n")
                append("${volts?.let { LevelConverter.voltsToDbv(it) }.format(2)} dBV\n")
                append("${volts.format(4)} V")
            },
            emphasis = true,
        )
        CalcNote(stringResource(R.string.calc_db_reference))

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        CalcSectionTitle(stringResource(R.string.calc_db_power))
        CalcFieldRow {
            NumberField(
                value = watts,
                onValueChange = { watts = it },
                label = stringResource(R.string.calc_db_watts),
                modifier = Modifier.weight(1f),
            )
            NumberField(
                value = impedance,
                onValueChange = { impedance = it },
                label = stringResource(R.string.calc_db_impedance),
                modifier = Modifier.weight(1f),
            )
        }

        val ohms = impedance.toDoubleOrNullLenient()?.takeIf { it > 0.0 }
        val neededVolts = watts.toDoubleOrNullLenient()
            ?.takeIf { it >= 0.0 }
            ?.let { w -> ohms?.let { LevelConverter.voltsFor(w, it) } }
        CalcResult(
            text = stringResource(
                R.string.calc_db_volts_needed,
                neededVolts.format(2),
                neededVolts?.let { LevelConverter.voltsToDbu(it) }.format(1),
            ),
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        CalcSectionTitle(stringResource(R.string.calc_db_distance))
        CalcFieldRow {
            NumberField(
                value = fromMeters,
                onValueChange = { fromMeters = it },
                label = stringResource(R.string.calc_db_from),
                modifier = Modifier.weight(1f),
            )
            NumberField(
                value = toMeters,
                onValueChange = { toMeters = it },
                label = stringResource(R.string.calc_db_to),
                modifier = Modifier.weight(1f),
            )
        }
        val attenuation = fromMeters.toDoubleOrNullLenient()?.takeIf { it > 0.0 }?.let { from ->
            toMeters.toDoubleOrNullLenient()?.takeIf { it > 0.0 }?.let { to ->
                LevelConverter.distanceAttenuationDb(from, to)
            }
        }
        CalcResult(
            text = stringResource(R.string.calc_db_distance_result, attenuation.format(2)),
        )
    }
}
