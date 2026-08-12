package com.patoolbox.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.ProSource
import com.patoolbox.core.model.ToolCategory
import com.patoolbox.core.model.ToolId
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
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    TextButton(onClick = onSettingsClick) {
                        Text(stringResource(R.string.home_settings))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChange,
                singleLine = true,
                label = { Text(stringResource(R.string.home_search_hint)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.gutter, vertical = dimens.gutterSmall),
            )

            ProStatusLine(uiState)

            if (matched.isEmpty()) {
                Text(
                    text = stringResource(R.string.home_no_results, uiState.query),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(dimens.gutter),
                )
                return@Column
            }

            LazyVerticalGrid(
                // タブレット横持ちでは3〜4列に増える
                columns = GridCells.Adaptive(minSize = 168.dp),
                contentPadding = PaddingValues(
                    start = dimens.gutter,
                    end = dimens.gutter,
                    bottom = dimens.gutter,
                ),
                horizontalArrangement = Arrangement.spacedBy(dimens.gutterSmall),
                verticalArrangement = Arrangement.spacedBy(dimens.gutterSmall),
                modifier = Modifier.fillMaxSize(),
            ) {
                val favorites = uiState.favoriteTools.filter { it in matched }
                if (favorites.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SectionHeader(stringResource(R.string.home_section_favorites), favorites.size)
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
                        SectionHeader(stringResource(category.titleRes), tools.size)
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
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(top = 12.dp, bottom = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.home_tool_count, count),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        style = MaterialTheme.typography.bodyMedium,
        color = if (uiState.proStatus.isPro) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = modifier.padding(horizontal = dimens.gutter, vertical = 2.dp),
    )
}

private fun ProSource.label(): String = when (this) {
    ProSource.NONE -> "未購入"
    ProSource.SUBSCRIPTION -> "サブスク"
    ProSource.LIFETIME -> "買い切り"
    ProSource.OFFLINE_GRACE -> "オフライン猶予中"
    ProSource.DEBUG_OVERRIDE -> "デバッグ強制ON"
}
