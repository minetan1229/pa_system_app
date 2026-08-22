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
import com.patoolbox.core.designsystem.component.PaSectionHeader
import com.patoolbox.core.designsystem.component.PaTone
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.designsystem.theme.LocalPaThemeMode
import com.patoolbox.core.model.ThemeMode
import com.patoolbox.core.reference.TestSignalEntry
import com.patoolbox.core.reference.TestSignalGuide
import com.patoolbox.core.reference.TestSignalKind
import com.patoolbox.core.ui.component.NoiseSlopeChart

/**
 * テスト信号ガイド。
 *
 * 「ピンクノイズを流す」まではできても、なぜピンクなのかを説明できないまま
 * ホワイトノイズで RTA を見て「高域が出すぎ」と誤診する事故が起きる。
 * そこで**傾きを図にして**、FFT 表示と 1/3 オクターブ表示で
 * 同じ信号が違う形に見えることを最初に出している。
 */
@Composable
internal fun SignalTab(gutter: Dp) {
    val dimens = LocalPaDimens.current
    var query by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf<TestSignalKind?>(null) }

    val results = remember(query, kind) {
        TestSignalGuide.search(query).filter { kind == null || it.kind == kind }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.reference_signal_search)) },
            placeholder = { Text(stringResource(R.string.reference_signal_search_hint)) },
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
                text = stringResource(R.string.reference_signal_kind_all),
                selected = kind == null,
                onClick = { kind = null },
            )
            TestSignalKind.entries.forEach { entry ->
                PaFilterChip(
                    text = entry.label,
                    selected = kind == entry,
                    onClick = { kind = if (kind == entry) null else entry },
                    accent = entry.rail(),
                )
            }
        }

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
            item(key = "intro") {
                PaNotice(
                    title = stringResource(R.string.reference_signal_intro_title),
                    body = stringResource(R.string.reference_signal_intro_body),
                    tone = PaTone.WARNING,
                    modifier = Modifier.padding(top = dimens.spaceSm),
                )
            }

            if (query.isBlank() && kind == null) {
                TestSignalKind.entries.forEach { entry ->
                    item(key = "kind_${entry.name}") {
                        PaSectionHeader(
                            title = entry.label,
                            subtitle = entry.description,
                            modifier = Modifier.padding(top = dimens.spaceSm),
                        )
                    }
                    items(TestSignalGuide.byKind(entry), key = { it.name }) { signal ->
                        SignalPanel(signal)
                    }
                }
                return@LazyColumn
            }

            items(results, key = { it.name }) { signal ->
                SignalPanel(signal)
            }
        }
    }
}

/**
 * 信号1枚。
 *
 * 図（傾き）→ どう聞こえるか → 何に使うか、の順に置く。
 * 現場では耳で区別できることが先に立つので、仕組みの説明は畳んでおく。
 */
@Composable
private fun SignalPanel(signal: TestSignalEntry) {
    val dimens = LocalPaDimens.current
    var expanded by rememberSaveable(signal.name) { mutableStateOf(false) }

    PaPanel(
        title = signal.name,
        subtitle = signal.english,
        rail = signal.kind.rail(),
        trailing = {
            PaPill(
                text = signal.slopeDbPerOctave?.let { "%+.0f dB/oct".format(it) }
                    ?: signal.kind.label,
                tone = PaTone.NEUTRAL,
            )
        },
    ) {
        signal.slopeDbPerOctave?.let { slope ->
            NoiseSlopeChart(slopeDbPerOctave = slope)
        }

        PaKeyValueRow(
            label = stringResource(R.string.reference_signal_sounds),
            value = signal.soundsLike,
        )

        BulletBlock(
            title = stringResource(R.string.reference_signal_use),
            lines = signal.useFor,
        )

        if (signal.cautions.isNotEmpty()) {
            PaNotice(
                title = stringResource(R.string.reference_caution),
                body = signal.cautions.joinToString("\n") { "・$it" },
                tone = PaTone.DANGER,
            )
        }

        signal.inThisApp?.let { where ->
            PaKeyValueRow(
                label = stringResource(R.string.reference_signal_in_app),
                value = where,
            )
        }

        TextButton(onClick = { expanded = !expanded }) {
            Text(
                stringResource(
                    if (expanded) {
                        R.string.reference_signal_detail_hide
                    } else {
                        R.string.reference_signal_detail_show
                    },
                ),
            )
        }

        if (!expanded) return@PaPanel

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        PaKeyValueRow(
            label = stringResource(R.string.reference_signal_what),
            value = signal.whatItIs,
        )
        PaKeyValueRow(
            label = stringResource(R.string.reference_signal_level),
            value = signal.levelTip,
        )
    }
}

/**
 * 信号の種類の色。
 * 暗所モードでは赤以外の光を出せないため、明度だけで分ける。
 */
@Composable
private fun TestSignalKind.rail(): Color {
    if (LocalPaThemeMode.current == ThemeMode.NIGHT_RED) {
        return when (ordinal % 4) {
            0 -> Color(0xFFFF6B4A)
            1 -> Color(0xFFD9502F)
            2 -> Color(0xFFB33D22)
            else -> Color(0xFF8C2F1A)
        }
    }
    return when (this) {
        TestSignalKind.NOISE -> Color(0xFF8E6BE8)
        TestSignalKind.TONE -> Color(0xFFE8A33D)
        TestSignalKind.SWEEP -> Color(0xFFE07B39)
        TestSignalKind.SPECIAL -> Color(0xFF0E7A62)
    }
}
