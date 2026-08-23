package com.patoolbox.feature.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.component.PaFilterChip
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.ExperienceLevel
import com.patoolbox.core.model.ToolCategory
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.ui.accentColor
import com.patoolbox.core.ui.component.ToolCard
import com.patoolbox.core.ui.titleRes
import com.patoolbox.core.ui.R as CoreUiR

/**
 * 道具の一覧。
 *
 * ホームから溢れた38個ぜんぶがここに入る。分類の札で絞れるようにしてあり、
 * ホームの分類カードから来たときはその札が最初から選ばれている。
 *
 * 検索欄をここにも置いているのは、分類を選んだあとで
 * 「やっぱり名前で探す」に切り替わることが多いため。
 *
 * @param initialCategory ホームの分類カードから来たときの絞り込み。null なら全部
 */
@Composable
fun ToolListScreen(
    initialCategory: ToolCategory?,
    onToolClick: (ToolId) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ToolListScreen(
        uiState = uiState,
        initialCategory = initialCategory,
        onQueryChange = viewModel::onQueryChange,
        onToggleFavorite = viewModel::onToggleFavorite,
        onToolClick = onToolClick,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ToolListScreen(
    uiState: HomeUiState,
    initialCategory: ToolCategory?,
    onQueryChange: (String) -> Unit,
    onToggleFavorite: (ToolId) -> Unit,
    onToolClick: (ToolId) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current
    val searchIndex = rememberToolSearchIndex()
    val query = uiState.query.trim().lowercase()
    val matched = rememberMatchedTools(query, searchIndex)

    // 絞り込みは画面の状態。ViewModel に持たせると、ホームに戻って
    // もう一度別の分類から入ったときに前の選択が残る。
    // enum のまま保存せず名前で持つのは、Bundle に確実に入る形にしておくため
    var selectedName by rememberSaveable { mutableStateOf(initialCategory?.name) }
    val selected = remember(selectedName) {
        selectedName?.let { name -> ToolCategory.entries.firstOrNull { it.name == name } }
    }
    val visible = remember(matched, selected) {
        matched.filter { selected == null || it.category == selected }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = selected?.let { stringResource(it.titleRes) }
                                ?: stringResource(R.string.tools_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    navigationIcon = {
                        TextButton(onClick = onBack) {
                            Text(stringResource(CoreUiR.string.back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
    ) { innerPadding ->
        LazyVerticalGrid(
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
            fullSpan {
                Column(
                    verticalArrangement = Arrangement.spacedBy(dimens.spaceMd),
                    modifier = Modifier.padding(top = dimens.spaceMd),
                ) {
                    SearchField(query = uiState.query, onQueryChange = onQueryChange)
                    CategoryFilters(selected = selected, onSelect = { selectedName = it?.name })
                }
            }

            if (visible.isEmpty()) {
                fullSpan {
                    Text(
                        text = stringResource(R.string.home_no_results, uiState.query),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = dimens.spaceLg),
                    )
                }
                return@LazyVerticalGrid
            }

            ToolCategory.entries.forEach { category ->
                val tools = visible.filter { it.category == category }
                if (tools.isEmpty()) return@forEach

                fullSpan(key = "header_${category.name}") {
                    SectionHeader(
                        title = stringResource(category.titleRes),
                        subtitle = stringResource(R.string.home_tool_count, tools.size),
                        accent = category.accentColor(),
                    )
                }
                items(tools, key = { it.name }) { tool ->
                    ToolCard(
                        tool = tool,
                        isFavorite = tool in uiState.favoriteTools,
                        onClick = { onToolClick(tool) },
                        onToggleFavorite = { onToggleFavorite(tool) },
                        // 一覧では段によらず38個すべてを出す。初心者のときだけ
                        // 前提知識の要るものに札を付けて、押す前に分かるようにする
                        compact = uiState.level == ExperienceLevel.ADVANCED,
                        showLevelBadge = uiState.level == ExperienceLevel.BEGINNER,
                    )
                }
            }
        }
    }
}

/** 分類の札。5個なので横スクロールに置く（折り返すと1行目と2行目で高さが揃わない）。 */
@Composable
private fun CategoryFilters(
    selected: ToolCategory?,
    onSelect: (ToolCategory?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
    ) {
        PaFilterChip(
            text = stringResource(R.string.tools_filter_all),
            selected = selected == null,
            onClick = { onSelect(null) },
        )
        ToolCategory.entries.forEach { category ->
            PaFilterChip(
                text = stringResource(category.titleRes),
                selected = selected == category,
                onClick = { onSelect(category) },
                accent = category.accentColor(),
            )
        }
    }
}
