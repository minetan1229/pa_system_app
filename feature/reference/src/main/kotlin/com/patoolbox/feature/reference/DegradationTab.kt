package com.patoolbox.feature.reference

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.patoolbox.core.designsystem.component.PaFilterChip
import com.patoolbox.core.designsystem.component.PaKeyValueRow
import com.patoolbox.core.designsystem.component.PaNotice
import com.patoolbox.core.designsystem.component.PaPanel
import com.patoolbox.core.designsystem.component.PaPill
import com.patoolbox.core.designsystem.component.PaTone
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.designsystem.theme.LocalPaThemeMode
import com.patoolbox.core.model.ThemeMode
import com.patoolbox.core.reference.DegradationItem
import com.patoolbox.core.reference.DegradationSeverity
import com.patoolbox.core.reference.DegradationStage
import com.patoolbox.core.reference.SignalDegradation

/**
 * 音質劣化チェック。
 *
 * 並び順は信号の流れ（上流から下流）。人は音が悪いときスピーカーから疑うが、
 * 原因はほぼ上流にあるので、**上から読めば上流から確かめたことになる**
 * 並べ方にしている。分類で絞り込めるのは、
 * 「無線だけ疑いたい」のように当たりが付いている場合のため。
 */
@Composable
internal fun DegradationTab(gutter: Dp) {
    val dimens = LocalPaDimens.current
    var query by rememberSaveable { mutableStateOf("") }
    var stage by rememberSaveable { mutableStateOf<DegradationStage?>(null) }

    val results = remember(query, stage) {
        SignalDegradation.search(query).filter { stage == null || it.stage == stage }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.reference_degradation_search)) },
            placeholder = { Text(stringResource(R.string.reference_degradation_search_hint)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = gutter, vertical = dimens.spaceSm),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = gutter),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
        ) {
            PaFilterChip(
                text = stringResource(R.string.reference_degradation_stage_all),
                selected = stage == null,
                onClick = { stage = null },
            )
            DegradationStage.entries.forEach { entry ->
                PaFilterChip(
                    text = entry.label,
                    selected = stage == entry,
                    onClick = { stage = if (stage == entry) null else entry },
                    accent = entry.rail(),
                )
            }
        }

        Text(
            text = stringResource(R.string.reference_count, results.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = gutter, vertical = dimens.spaceXs),
        )

        if (results.isEmpty()) {
            Text(
                text = stringResource(R.string.reference_no_result),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(gutter),
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = gutter,
                end = gutter,
                bottom = dimens.spaceXl,
            ),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceMd),
        ) {
            // 選んだ分類の「何のかたまりか」を1行で出す。
            // 分類名だけでは、なぜその段で音が悪くなるのかが読み取れない
            stage?.let { selected ->
                item(key = "stage_note") {
                    PaNotice(
                        title = selected.label,
                        body = selected.description,
                        tone = PaTone.INFO,
                    )
                }
            }
            items(results, key = { it.title }) { item ->
                DegradationPanel(item)
            }
        }
    }
}

/**
 * 劣化1件。
 *
 * 見出しと症状だけを開いた状態にして、対処は畳む。
 * 探しているときは症状の一致だけを次々に読み飛ばしたいが、
 * 当たったあとは全部読みたい——という2つの読み方があるため。
 */
@Composable
private fun DegradationPanel(item: DegradationItem) {
    val dimens = LocalPaDimens.current
    var expanded by rememberSaveable(item.title) { mutableStateOf(false) }

    PaPanel(
        title = item.title,
        subtitle = item.stage.label,
        rail = item.stage.rail(),
        trailing = {
            PaPill(text = item.severity.label, tone = item.severity.tone())
        },
    ) {
        PaKeyValueRow(
            label = stringResource(R.string.reference_degradation_symptom),
            value = item.symptom,
        )

        // 不可逆なものは、対処を開く前に伝える。
        // 「後で直せる」と思って時間を使うのがいちばんの損失になる
        if (item.severity == DegradationSeverity.FATAL) {
            PaNotice(
                title = item.severity.note,
                tone = PaTone.DANGER,
            )
        }

        TextButton(onClick = { expanded = !expanded }) {
            Text(
                stringResource(
                    if (expanded) {
                        R.string.reference_degradation_hide
                    } else {
                        R.string.reference_degradation_show
                    },
                ),
            )
        }

        if (!expanded) return@PaPanel

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        PaKeyValueRow(
            label = stringResource(R.string.reference_degradation_mechanism),
            value = item.mechanism,
        )
        PaKeyValueRow(
            label = stringResource(R.string.reference_degradation_amount),
            value = item.amount,
            valueColor = MaterialTheme.colorScheme.onSurface,
        )
        BulletBlock(
            title = stringResource(R.string.reference_degradation_fixes),
            lines = item.fixes,
        )
        item.prevention?.let { prevention ->
            PaNotice(
                title = stringResource(R.string.reference_degradation_prevention),
                body = prevention,
                tone = PaTone.SUCCESS,
            )
        }
    }
}

@Composable
private fun DegradationSeverity.tone(): PaTone = when (this) {
    DegradationSeverity.FATAL -> PaTone.DANGER
    DegradationSeverity.HEAVY -> PaTone.WARNING
    DegradationSeverity.CREEPING -> PaTone.NEUTRAL
}

/**
 * 信号経路の段ごとの色。
 *
 * 上流を暖色、下流を寒色にして、縦に並べたときに
 * 「いま上流を見ているのか下流を見ているのか」が色で分かるようにしている。
 */
@Composable
private fun DegradationStage.rail(): Color {
    if (LocalPaThemeMode.current == ThemeMode.NIGHT_RED) {
        return when (ordinal % 4) {
            0 -> Color(0xFFFF6B4A)
            1 -> Color(0xFFD9502F)
            2 -> Color(0xFFB33D22)
            else -> Color(0xFF8C2F1A)
        }
    }
    return when (this) {
        DegradationStage.SOURCE -> Color(0xFFC2410C)
        DegradationStage.CABLE -> Color(0xFF9A7100)
        DegradationStage.GAIN -> Color(0xFFB0392F)
        DegradationStage.PROCESSING -> Color(0xFF6D3BC4)
        DegradationStage.TRANSPORT -> Color(0xFF1E4FA8)
        DegradationStage.OUTPUT -> Color(0xFF0E7A62)
        DegradationStage.VENUE -> Color(0xFF127C8C)
        DegradationStage.RECORD -> Color(0xFF55606B)
    }
}
