package com.patoolbox.feature.reference

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.patoolbox.core.designsystem.component.PaKeyValueRow
import com.patoolbox.core.designsystem.component.PaNotice
import com.patoolbox.core.designsystem.component.PaPanel
import com.patoolbox.core.designsystem.component.PaPill
import com.patoolbox.core.designsystem.component.PaTone
import com.patoolbox.core.designsystem.component.content
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.reference.BandDictionary
import com.patoolbox.core.reference.BandEntry
import com.patoolbox.core.reference.FeedbackRisk
import com.patoolbox.core.ui.component.BandRuler
import com.patoolbox.core.ui.component.formatHz

/**
 * 帯域辞書。
 *
 * 楽器別のチャートと引く向きが逆になっている。
 * あちらは「キックをどう作るか」、こちらは
 * **「アナライザに出ている 250Hz が何なのか」** を引くための表。
 *
 * ハウリング検出やアナライザが出すのは数字だけなので、
 * その数字から意味に戻れる場所が要る。同じ [BandDictionary] を
 * ハウリング検出の一覧も引いていて、両方で同じ説明が出るようにしてある。
 */
@Composable
internal fun BandDictionaryList(gutter: Dp, query: String) {
    val dimens = LocalPaDimens.current
    val results = BandDictionary.search(query)

    if (results.isEmpty()) {
        Text(
            text = stringResource(R.string.reference_no_result),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(gutter),
        )
        return
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
        item(key = "dict_intro") {
            PaNotice(
                title = stringResource(R.string.reference_dict_intro_title),
                body = stringResource(R.string.reference_dict_intro_body),
                tone = PaTone.INFO,
                modifier = Modifier.padding(top = dimens.spaceSm),
            )
        }
        items(results, key = { it.label }) { band -> BandPanel(band) }
    }
}

@Composable
private fun BandPanel(band: BandEntry) {
    val dimens = LocalPaDimens.current
    val tone = band.feedbackRisk.tone()

    PaPanel(
        title = stringResource(
            R.string.reference_dict_title,
            formatHz(band.fromHz),
            formatHz(band.toHz),
            band.label,
        ),
        subtitle = band.oneLiner,
        rail = tone.content(),
        trailing = { PaPill(text = band.feedbackRisk.label, tone = tone) },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
            PaPill(text = band.nickname)
            PaPill(
                text = stringResource(
                    R.string.reference_dict_center,
                    "%.0f".format(band.centerHz),
                ),
                tone = PaTone.NEUTRAL,
            )
        }

        // 可聴域のどこにあるかを図で出す。数字だけでは体に入らない
        BandRuler(
            fundamentalFromHz = band.fromHz,
            fundamentalToHz = band.toHz,
            tips = emptyList(),
            fundamentalLabel = band.label,
            modifier = Modifier.padding(top = dimens.spaceXs),
        )

        BulletBlock(
            title = stringResource(R.string.reference_dict_lives),
            lines = band.lives,
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        PaKeyValueRow(
            label = stringResource(R.string.reference_dict_boost),
            value = band.boost,
        )
        PaKeyValueRow(
            label = stringResource(R.string.reference_dict_cut),
            value = band.cut,
        )

        BulletBlock(
            title = stringResource(R.string.reference_dict_problems),
            lines = band.problems,
        )

        PaNotice(
            title = stringResource(R.string.reference_dict_feedback, band.feedbackRisk.label),
            body = band.feedbackNote,
            tone = tone,
        )

        band.checkTone?.let { hint ->
            PaKeyValueRow(
                label = stringResource(R.string.reference_dict_check),
                value = hint,
            )
        }
    }
}

/**
 * ハウリングのしやすさの色。
 * 「回りやすい」を危険色にしているのは、一覧を上から眺めたときに
 * 先に手を付ける帯域が目に入るようにするため。
 */
@Composable
private fun FeedbackRisk.tone(): PaTone = when (this) {
    FeedbackRisk.HIGH -> PaTone.DANGER
    FeedbackRisk.MEDIUM -> PaTone.WARNING
    FeedbackRisk.LOW -> PaTone.NEUTRAL
}
