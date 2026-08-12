package com.patoolbox.feature.calc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.patoolbox.core.calc.BpmCalculator
import com.patoolbox.core.calc.NoteDivision

/**
 * BPM とディレイタイムの換算。
 * 一覧で全部出す。1つずつ音符を選ばせるより、卓の前で見比べるほうが早い。
 */
@Composable
internal fun BpmTab(
    modifier: Modifier = Modifier,
) {
    var bpmText by rememberSaveable { mutableStateOf("120") }
    var msText by rememberSaveable { mutableStateOf("") }

    val bpm = bpmText.toDoubleOrNullLenient()
        ?.coerceIn(BpmCalculator.MIN_BPM, BpmCalculator.MAX_BPM)

    CalcColumn(modifier = modifier) {
        NumberField(
            value = bpmText,
            onValueChange = { bpmText = it },
            label = stringResource(R.string.calc_bpm_label),
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            HeaderCell(stringResource(R.string.calc_bpm_note), weight = 1.4f)
            HeaderCell(stringResource(R.string.calc_bpm_ms), weight = 1f)
            HeaderCell(stringResource(R.string.calc_bpm_hz), weight = 1f)
        }

        NoteDivision.entries.forEach { division ->
            val ms = bpm?.let { BpmCalculator.millisecondsFor(it, division) }
            val hz = bpm?.let { BpmCalculator.hertzFor(it, division) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ValueCell(division.label, weight = 1.4f, emphasis = division.isBasic)
                ValueCell(ms.format(1), weight = 1f, emphasis = division.isBasic)
                ValueCell(hz.format(2), weight = 1f, emphasis = division.isBasic)
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        CalcSectionTitle(stringResource(R.string.calc_bpm_from_ms))
        NumberField(
            value = msText,
            onValueChange = { msText = it },
            label = stringResource(R.string.calc_bpm_ms),
        )
        CalcResult(
            text = stringResource(
                R.string.calc_bpm_from_ms_result,
                msText.toDoubleOrNullLenient()
                    ?.takeIf { it > 0.0 }
                    ?.let { BpmCalculator.bpmFor(it, NoteDivision.QUARTER) }
                    .format(1),
            ),
        )
    }
}

/** 4分・8分・16分は使用頻度が高いので強調する。 */
private val NoteDivision.isBasic: Boolean
    get() = this == NoteDivision.QUARTER ||
        this == NoteDivision.EIGHTH ||
        this == NoteDivision.SIXTEENTH

@Composable
private fun RowScope.HeaderCell(text: String, weight: Float) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.End,
        modifier = Modifier
            .weight(weight)
            .padding(horizontal = 4.dp),
    )
}

@Composable
private fun RowScope.ValueCell(text: String, weight: Float, emphasis: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = if (emphasis) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        textAlign = TextAlign.End,
        modifier = Modifier
            .weight(weight)
            .padding(horizontal = 4.dp, vertical = 2.dp),
    )
}
