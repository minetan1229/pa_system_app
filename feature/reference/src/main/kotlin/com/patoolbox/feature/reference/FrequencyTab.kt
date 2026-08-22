package com.patoolbox.feature.reference

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patoolbox.core.designsystem.component.PaFilterChip
import com.patoolbox.core.designsystem.component.PaKeyValueRow
import com.patoolbox.core.designsystem.component.PaPanel
import com.patoolbox.core.designsystem.component.PaPill
import com.patoolbox.core.designsystem.component.PaTone
import com.patoolbox.core.designsystem.component.content
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.designsystem.theme.LocalPaThemeMode
import com.patoolbox.core.model.ThemeMode
import com.patoolbox.core.reference.BandTip
import com.patoolbox.core.reference.FrequencyChart
import com.patoolbox.core.reference.InstrumentBands
import com.patoolbox.core.reference.InstrumentGroup
import com.patoolbox.core.ui.component.BandRuler
import com.patoolbox.core.ui.component.ChartLegend
import com.patoolbox.core.ui.component.bandActionLegend
import com.patoolbox.core.ui.component.formatHz
import com.patoolbox.core.ui.component.tone

/**
 * 帯域の引き方は2通りある。
 *
 * 　楽器別＝「キックをどう作るか」。仕込みの前に読む
 * 　帯域別＝「いま出ている 250Hz が何なのか」。卓やアナライザの前で引く
 *
 * 同じ知識でも入口が逆なので、1つの画面に混ぜず切り替えにしている。
 */
private enum class FrequencyMode(val titleRes: Int) {
    INSTRUMENT(R.string.reference_band_mode_instrument),
    DICTIONARY(R.string.reference_band_mode_dictionary),
}

/**
 * 帯域チャート。
 *
 * 楽器が20を超えたので、検索と分類の絞り込みを先に置いている。
 * 1枚ごとに図（[BandRuler]）を出すのは、数字の一覧だけでは
 * 「その帯域が可聴域のどこにあるか」が体に入らないため。
 */
@Composable
internal fun FrequencyTab(gutter: Dp) {
    val dimens = LocalPaDimens.current
    var query by rememberSaveable { mutableStateOf("") }
    var group by rememberSaveable { mutableStateOf<InstrumentGroup?>(null) }
    var mode by rememberSaveable { mutableStateOf(FrequencyMode.INSTRUMENT) }

    val results = remember(query, group) {
        FrequencyChart.search(query).filter { group == null || it.group == group }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = gutter, vertical = dimens.spaceSm),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
        ) {
            FrequencyMode.entries.forEach { entry ->
                PaFilterChip(
                    text = stringResource(entry.titleRes),
                    selected = mode == entry,
                    onClick = { mode = entry },
                )
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = {
                Text(
                    stringResource(
                        if (mode == FrequencyMode.INSTRUMENT) {
                            R.string.reference_band_search
                        } else {
                            R.string.reference_dict_search
                        },
                    ),
                )
            },
            placeholder = {
                Text(
                    stringResource(
                        if (mode == FrequencyMode.INSTRUMENT) {
                            R.string.reference_band_search_hint
                        } else {
                            R.string.reference_dict_search_hint
                        },
                    ),
                )
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = gutter, vertical = dimens.spaceSm),
        )

        if (mode == FrequencyMode.DICTIONARY) {
            BandDictionaryList(gutter = gutter, query = query)
            return@Column
        }

        GroupFilterRow(
            selected = group,
            onSelect = { group = it },
            gutter = gutter,
        )

        Text(
            text = stringResource(R.string.reference_band_count, results.size),
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
            items(results, key = { it.instrument }) { instrument ->
                InstrumentPanel(instrument)
            }
        }
    }
}

@Composable
private fun GroupFilterRow(
    selected: InstrumentGroup?,
    onSelect: (InstrumentGroup?) -> Unit,
    gutter: Dp,
) {
    val dimens = LocalPaDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = gutter),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
    ) {
        PaFilterChip(
            text = stringResource(R.string.reference_band_group_all),
            selected = selected == null,
            onClick = { onSelect(null) },
        )
        InstrumentGroup.entries.forEach { entry ->
            PaFilterChip(
                text = entry.label,
                selected = selected == entry,
                onClick = { onSelect(if (selected == entry) null else entry) },
                accent = entry.rail(),
            )
        }
    }
}

/**
 * 楽器1枚。
 *
 * 上半分（帯域と図）は常に開いておき、下半分（マイキング・ぶつかる相手）は畳む。
 * 卓の前で開くときは帯域だけ見たいことが多いが、
 * 仕込みの前には作り方まで読みたい——用途が2つあるので1枚に両方入れて折り畳む。
 */
@Composable
private fun InstrumentPanel(instrument: InstrumentBands) {
    val dimens = LocalPaDimens.current
    var expanded by rememberSaveable(instrument.instrument) { mutableStateOf(false) }

    PaPanel(
        title = instrument.instrument,
        subtitle = instrument.role,
        rail = instrument.group.rail(),
        trailing = { PaPill(text = instrument.group.label) },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
            PaPill(
                text = stringResource(
                    R.string.reference_fundamental,
                    formatHz(instrument.fundamentalFromHz),
                    formatHz(instrument.fundamentalToHz),
                ),
                tone = PaTone.NEUTRAL,
            )
            PaPill(
                text = instrument.highPassHz?.let {
                    stringResource(R.string.reference_band_highpass, it)
                } ?: stringResource(R.string.reference_band_highpass_none),
                tone = if (instrument.highPassHz == null) PaTone.NEUTRAL else PaTone.INFO,
            )
        }

        BandRuler(
            fundamentalFromHz = instrument.fundamentalFromHz,
            fundamentalToHz = instrument.fundamentalToHz,
            tips = instrument.tips,
            modifier = Modifier.padding(top = dimens.spaceXs),
        )
        ChartLegend(entries = bandActionLegend())

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        instrument.tips.forEach { tip -> BandRow(tip) }

        TextButton(onClick = { expanded = !expanded }) {
            Text(
                stringResource(
                    if (expanded) {
                        R.string.reference_band_detail_hide
                    } else {
                        R.string.reference_band_detail_show
                    },
                ),
            )
        }

        if (!expanded) return@PaPanel

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        PaKeyValueRow(
            label = stringResource(R.string.reference_band_mic),
            value = instrument.micTip,
        )
        PaKeyValueRow(
            label = stringResource(R.string.reference_band_dynamics),
            value = instrument.dynamicsTip,
        )
        BulletBlock(
            title = stringResource(R.string.reference_band_conflicts),
            lines = instrument.conflicts,
        )
        BulletBlock(
            title = stringResource(R.string.reference_band_pitfalls),
            lines = instrument.pitfalls,
        )
    }
}

/**
 * 帯域1つ。
 *
 * 「範囲・向き・効き方・最初の一手」を必ず縦に揃えて出す。
 * ワンアドバイスだけ地色を付けているのは、卓の前で読むのがこの1行だけになるため。
 */
@Composable
private fun BandRow(tip: BandTip) {
    val dimens = LocalPaDimens.current
    val tone = tip.action.tone()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(
                    R.string.reference_band_range,
                    formatHz(tip.fromHz),
                    formatHz(tip.toHz),
                ),
                style = MaterialTheme.typography.labelMedium,
                color = tone.content(),
                modifier = Modifier.width(BAND_RANGE_COLUMN_WIDTH),
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = tip.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    PaPill(text = tip.action.label, tone = tone)
                }
                Text(
                    text = tip.effect,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        AdviceLine(text = tip.advice, accent = tone.content())
    }
}

/** ワンアドバイスの1行。左端の色帯で、どの向きの操作かを図と同じ色で示す。 */
@Composable
private fun AdviceLine(text: String, accent: Color) {
    val dimens = LocalPaDimens.current
    val shape = RoundedCornerShape(dimens.cornerSmall)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(dimens.railWidth)
                .background(accent),
        )
        Column(
            modifier = Modifier.padding(
                horizontal = dimens.spaceSm,
                vertical = dimens.spaceSm,
            ),
        ) {
            Text(
                text = stringResource(R.string.reference_band_advice),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
internal fun BulletBlock(title: String, lines: List<String>) {
    if (lines.isEmpty()) return
    val dimens = LocalPaDimens.current
    Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceXs / 2)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        lines.forEach { line ->
            Text(
                text = "・$line",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * 楽器の分類の色。
 *
 * 20枚以上を縦に並べるので、左端の帯の色でグループの切り替わりが分かるようにしている。
 * 暗所モードでは赤以外の光を出せないため、明度だけで分ける。
 */
@Composable
internal fun InstrumentGroup.rail(): Color {
    if (LocalPaThemeMode.current == ThemeMode.NIGHT_RED) {
        return when (ordinal % 4) {
            0 -> Color(0xFFFF6B4A)
            1 -> Color(0xFFD9502F)
            2 -> Color(0xFFB33D22)
            else -> Color(0xFF8C2F1A)
        }
    }
    return when (this) {
        InstrumentGroup.DRUMS -> Color(0xFFC2410C)
        InstrumentGroup.BASS -> Color(0xFF1E4FA8)
        InstrumentGroup.GUITAR -> Color(0xFF0E7A62)
        InstrumentGroup.VOCAL -> Color(0xFFA0348F)
        InstrumentGroup.KEYS -> Color(0xFF6D3BC4)
        InstrumentGroup.WIND_STRINGS -> Color(0xFF9A7100)
        InstrumentGroup.PERCUSSION -> Color(0xFFB0392F)
        InstrumentGroup.PLAYBACK -> Color(0xFF55606B)
    }
}

/**
 * 「50 〜 80 Hz」の列幅。
 * 等幅フォントなので固定でも崩れず、縦に並べたときに数字の位置が揃う。
 */
private val BAND_RANGE_COLUMN_WIDTH = 108.dp
