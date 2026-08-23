package com.patoolbox.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.patoolbox.core.designsystem.component.PaIllustration
import com.patoolbox.core.designsystem.component.PaScene
import com.patoolbox.core.designsystem.component.PaSectionHeader
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.ProSource
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.reference.HelpTopics
import com.patoolbox.core.ui.descriptionRes
import com.patoolbox.core.ui.titleRes
import com.patoolbox.core.ui.R as CoreUiR

/**
 * ホームと道具一覧で共通の部品。
 * 2画面から同じものを出すので、片方だけ直して食い違うことがないようここに集めている。
 */

/** グリッドの1行を丸ごと使う項目。見出しや帯を挟むのに使う。 */
internal fun LazyGridScope.fullSpan(
    key: Any? = null,
    content: @Composable () -> Unit,
) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
}

/**
 * 検索用の索引。
 *
 * LocalContext ではなく LocalResources を使うのは、設定（ロケールなど）が変わったときに
 * 再計算されるようにするため。LocalContext.getString() は無効化されず古い値が残る。
 *
 * 解説の本文も索引に混ぜている。現場で打つのは「Dante」「ハウリング」「70V」のような
 * 症状や単語で、ツール名や短い説明文には出てこないことが多い。
 */
@Composable
internal fun rememberToolSearchIndex(): Map<ToolId, String> {
    val resources = LocalResources.current
    return remember(resources) {
        ToolId.entries.associateWith { tool ->
            buildString {
                append(resources.getString(tool.titleRes))
                append(' ')
                append(resources.getString(tool.descriptionRes))
                append(' ')
                append(tool.badge)
                append(' ')
                append(tool.name)
                append(' ')
                append(HelpTopics.forTool(tool)?.searchText.orEmpty())
            }.lowercase()
        }
    }
}

/** [query]（小文字・trim 済み）に一致する道具。空なら全部。 */
@Composable
internal fun rememberMatchedTools(
    query: String,
    searchIndex: Map<ToolId, String>,
): List<ToolId> = remember(query, searchIndex) {
    if (query.isEmpty()) {
        ToolId.entries.toList()
    } else {
        ToolId.entries.filter { searchIndex.getValue(it).contains(query) }
    }
}

/**
 * 検索欄。
 *
 * 38枚のカードを縦に探すより打った方が速いので、常に一番上に置いている。
 * 角丸を浅くして面で見せるのは、押せる場所だと分かればよく、
 * 入力欄そのものを主役にしたくないため。
 */
@Composable
internal fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.home_search_hint)) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        shape = RoundedCornerShape(dimens.cornerSmall),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/** 節の見出し。[accent] を渡すと左に色の丸が付く。 */
@Composable
internal fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    accent: Color? = null,
) {
    val dimens = LocalPaDimens.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = dimens.space, bottom = dimens.spaceXs),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (accent != null) {
            // カテゴリの色を見出しにも出す。カードのバッジと同じ色なので、
            // どこからどこまでが同じ仲間かがスクロール中でも分かる
            Box(
                modifier = Modifier
                    .size(dimens.spaceMd)
                    .background(accent, CircleShape),
            )
        }
        PaSectionHeader(title = title, subtitle = subtitle)
    }
}

/**
 * 検索して何も出なかったときの表示。
 *
 * 文字だけだと「読み込み中なのか、無いのか」が分かりにくい。
 * 絵を1枚置くと、止まっているのではなく空振りだと一目で分かる。
 * ホームと道具一覧の両方から出すのでここに置いてある。
 */
@Composable
internal fun EmptyResult(
    query: String,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = dimens.spaceXl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimens.spaceMd),
    ) {
        PaIllustration(
            scene = PaScene.SEARCH_EMPTY,
            contentDescription = stringResource(CoreUiR.string.search_empty_image),
            modifier = Modifier.size(width = 128.dp, height = 80.dp),
        )
        Text(
            text = stringResource(R.string.home_no_results, query),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal fun ProSource.label(): String = when (this) {
    ProSource.NONE -> "未購入"
    ProSource.SUBSCRIPTION -> "サブスク"
    ProSource.LIFETIME -> "買い切り"
    ProSource.OFFLINE_GRACE -> "オフライン猶予中"
    ProSource.DEBUG_OVERRIDE -> "デバッグ強制ON"
    ProSource.PRE_RELEASE -> "先行版（全機能開放中）"
}
