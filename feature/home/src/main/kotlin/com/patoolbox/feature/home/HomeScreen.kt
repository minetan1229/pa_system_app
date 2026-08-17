package com.patoolbox.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.component.PaSectionHeader
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.ProSource
import com.patoolbox.core.model.ToolCategory
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.reference.HelpTopics
import com.patoolbox.core.ui.accentColor
import com.patoolbox.core.ui.component.ToolCard
import com.patoolbox.core.ui.descriptionRes
import com.patoolbox.core.ui.titleRes

@Composable
fun HomeScreen(
    onToolClick: (ToolId) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onToggleFavorite = viewModel::onToggleFavorite,
        onToolClick = onToolClick,
        onSettingsClick = onSettingsClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    onQueryChange: (String) -> Unit,
    onToggleFavorite: (ToolId) -> Unit,
    onToolClick: (ToolId) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current
    val resources = LocalResources.current

    // 検索用インデックスは1回だけ作る。
    // LocalContext ではなく LocalResources を使うのは、設定（ロケールなど）が変わったときに
    // 再計算されるようにするため。LocalContext.getString() は無効化されず古い値が残る。
    //
    // 解説の本文も索引に混ぜている。現場で打つのは「Dante」「ハウリング」「70V」のような
    // 症状や単語で、ツール名や短い説明文には出てこないことが多い。
    val searchIndex = remember(resources) {
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

    val query = uiState.query.trim().lowercase()
    val matched = remember(query, searchIndex) {
        if (query.isEmpty()) {
            ToolId.entries.toList()
        } else {
            ToolId.entries.filter { searchIndex.getValue(it).contains(query) }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.home_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                actions = {
                    TextButton(onClick = onSettingsClick) {
                        Text(stringResource(R.string.home_settings))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        LazyVerticalGrid(
            // タブレット横持ちでは3〜4列に増える
            columns = GridCells.Adaptive(minSize = 168.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = dimens.gutter,
                end = dimens.gutter,
                bottom = dimens.spaceXl,
            ),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceMd),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceMd),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceMd)) {
                    SearchField(query = uiState.query, onQueryChange = onQueryChange)
                    ProStatusLine(uiState)
                }
            }

            if (matched.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = stringResource(R.string.home_no_results, uiState.query),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = dimens.spaceLg),
                    )
                }
                return@LazyVerticalGrid
            }

            val favorites = uiState.favoriteTools.filter { it in matched }
            if (favorites.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader(
                        title = stringResource(R.string.home_section_favorites),
                        count = favorites.size,
                        accent = MaterialTheme.colorScheme.primary,
                    )
                }
                items(favorites, key = { "fav_${it.name}" }) { tool ->
                    ToolCard(
                        tool = tool,
                        isFavorite = true,
                        onClick = { onToolClick(tool) },
                        onToggleFavorite = { onToggleFavorite(tool) },
                    )
                }
            }

            ToolCategory.entries.forEach { category ->
                val tools = matched.filter { it.category == category }
                if (tools.isEmpty()) return@forEach

                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader(
                        title = stringResource(category.titleRes),
                        count = tools.size,
                        accent = category.accentColor(),
                    )
                }
                items(tools, key = { it.name }) { tool ->
                    ToolCard(
                        tool = tool,
                        isFavorite = tool in uiState.favoriteTools,
                        onClick = { onToolClick(tool) },
                        onToggleFavorite = { onToggleFavorite(tool) },
                    )
                }
            }
        }
    }
}

/**
 * 検索。36枚のカードを縦に探すより打った方が速いので、常に一番上に置いている。
 * 枠線を消して面で見せるのは、押せる場所だと分かればよく、
 * 入力欄そのものを主役にしたくないため。
 */
@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val dimens = LocalPaDimens.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.home_search_hint)) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        shape = RoundedCornerShape(dimens.cornerLarge),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current
    Row(
        modifier = modifier.padding(top = dimens.space, bottom = dimens.spaceXs),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // カテゴリの色を見出しにも出す。カードのバッジと同じ色なので、
        // どこからどこまでが同じ仲間かがスクロール中でも分かる
        Box(
            modifier = Modifier
                .size(dimens.spaceMd)
                .background(accent, CircleShape),
        )
        PaSectionHeader(
            title = title,
            subtitle = stringResource(R.string.home_tool_count, count),
        )
    }
}

@Composable
private fun ProStatusLine(
    uiState: HomeUiState,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current
    val text = if (uiState.proStatus.isPro) {
        stringResource(R.string.home_pro_active, uiState.proStatus.source.label())
    } else {
        stringResource(R.string.home_pro_inactive)
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = if (uiState.proStatus.isPro) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = modifier
            .background(
                color = if (uiState.proStatus.isPro) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                shape = RoundedCornerShape(dimens.cornerSmall),
            )
            .padding(horizontal = dimens.spaceMd, vertical = dimens.spaceSm),
    )
}

private fun ProSource.label(): String = when (this) {
    ProSource.NONE -> "未購入"
    ProSource.SUBSCRIPTION -> "サブスク"
    ProSource.LIFETIME -> "買い切り"
    ProSource.OFFLINE_GRACE -> "オフライン猶予中"
    ProSource.DEBUG_OVERRIDE -> "デバッグ強制ON"
    ProSource.PRE_RELEASE -> "先行版（全機能開放中）"
}
