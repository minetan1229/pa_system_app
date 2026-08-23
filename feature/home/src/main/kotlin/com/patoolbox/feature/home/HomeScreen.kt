package com.patoolbox.feature.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.component.PaCard
import com.patoolbox.core.designsystem.component.PaFilterChip
import com.patoolbox.core.designsystem.component.PaIllustration
import com.patoolbox.core.designsystem.component.PaNotice
import com.patoolbox.core.designsystem.component.PaPill
import com.patoolbox.core.designsystem.component.PaScene
import com.patoolbox.core.designsystem.component.PaTone
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.CalibrationConfidence
import com.patoolbox.core.model.ExperienceLevel
import com.patoolbox.core.model.ToolCategory
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.ui.accentColor
import com.patoolbox.core.ui.component.ToolCard
import com.patoolbox.core.ui.titleRes

/**
 * ホーム。
 *
 * **38枚のカードを最初に見せない。** 一覧は「探すための画面」であって
 * 「開いたときに見たい画面」ではない。既定（中級者）でここに置くのは
 *
 * 1. いま何のアプリを開いたのか（見出しの絵）
 * 2. 最初に一度だけやること（マイク校正）
 * 3. すぐ開きたい4つ（★を付けていればその4つ）
 * 4. 分類から掘る入口
 *
 * の4つだけにして、全部の道具は [ToolListScreen] へ送る。
 * 検索だけは例外で、この画面に置いたまま結果をその場に出す
 * （現場では「探す」より「打つ」方が速いので、1画面ぶんの移動も惜しい）。
 *
 * ただしこの並びが合うのは中級者だけなので、[ExperienceLevel] で作りを3通りに変える。
 * **機能は消さない**——変わるのは並べ方と説明の量だけで、
 * どの段でも全部の道具に手が届く。
 */
@Composable
fun HomeScreen(
    onToolClick: (ToolId) -> Unit,
    onCategoryClick: (ToolCategory?) -> Unit,
    onCalibrationClick: () -> Unit,
    onCalibrationGuideClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onToggleFavorite = viewModel::onToggleFavorite,
        onLevelChange = viewModel::onLevelChange,
        onToolClick = onToolClick,
        onCategoryClick = onCategoryClick,
        onCalibrationClick = onCalibrationClick,
        onCalibrationGuideClick = onCalibrationGuideClick,
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
    onLevelChange: (ExperienceLevel) -> Unit,
    onToolClick: (ToolId) -> Unit,
    onCategoryClick: (ToolCategory?) -> Unit,
    onCalibrationClick: () -> Unit,
    onCalibrationGuideClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current
    val searchIndex = rememberToolSearchIndex()
    val query = uiState.query.trim().lowercase()
    val matched = rememberMatchedTools(query, searchIndex)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            // 上帯は面の色にして下端に線を引く。背景と同色にすると
            // スクロールしたときにカードが帯の下に潜って見えず、境界が消える
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.home_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    actions = {
                        TextButton(onClick = onSettingsClick) {
                            Text(stringResource(R.string.home_settings))
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
            fullSpan {
                Column(
                    verticalArrangement = Arrangement.spacedBy(dimens.spaceMd),
                    modifier = Modifier.padding(top = dimens.spaceMd),
                ) {
                    // 上級者では見出しの絵を出さない。情報を持たない絵に
                    // 一画面の1/3を使われるより、道具が1行でも多く見える方がいい
                    if (query.isEmpty() && uiState.level != ExperienceLevel.ADVANCED) {
                        Hero()
                    }
                    SearchField(query = uiState.query, onQueryChange = onQueryChange)
                    if (query.isEmpty()) {
                        LevelSelector(level = uiState.level, onLevelChange = onLevelChange)
                    }
                }
            }

            if (query.isEmpty()) {
                landingSections(
                    uiState = uiState,
                    onToolClick = onToolClick,
                    onToggleFavorite = onToggleFavorite,
                    onCategoryClick = onCategoryClick,
                    onCalibrationClick = onCalibrationClick,
                    onCalibrationGuideClick = onCalibrationGuideClick,
                )
            } else {
                toolSections(
                    tools = matched,
                    uiState = uiState,
                    keyPrefix = "result",
                    onToolClick = onToolClick,
                    onToggleFavorite = onToggleFavorite,
                )
            }
        }
    }
}

/** 検索していないときに出す中身。段によって並びが変わる。 */
private fun LazyGridScope.landingSections(
    uiState: HomeUiState,
    onToolClick: (ToolId) -> Unit,
    onToggleFavorite: (ToolId) -> Unit,
    onCategoryClick: (ToolCategory?) -> Unit,
    onCalibrationClick: () -> Unit,
    onCalibrationGuideClick: () -> Unit,
) {
    val level = uiState.level
    val compact = level == ExperienceLevel.ADVANCED

    fullSpan {
        SectionHeader(
            title = stringResource(R.string.home_section_start),
            subtitle = stringResource(R.string.home_start_subtitle),
        )
    }
    fullSpan {
        CalibrationPanel(
            summary = uiState.calibration,
            compact = compact,
            onOpenGuide = onCalibrationGuideClick,
            onOpenCalibration = onCalibrationClick,
        )
    }

    // 初心者にだけ「何から開くか」を置く。中級以上には邪魔になるので出さない
    if (level == ExperienceLevel.BEGINNER) {
        fullSpan {
            PaNotice(
                title = stringResource(R.string.home_beginner_guide_title),
                body = stringResource(R.string.home_beginner_guide_body),
                tone = PaTone.INFO,
            )
        }
    }

    val quickTools = uiState.favoriteTools.ifEmpty { starterTools(uiState.profile) }
    val hasFavorites = uiState.favoriteTools.isNotEmpty()
    fullSpan {
        SectionHeader(
            title = stringResource(
                if (hasFavorites) R.string.home_section_favorites else R.string.home_starters,
            ),
            subtitle = stringResource(
                if (hasFavorites) {
                    R.string.home_favorites_subtitle
                } else {
                    R.string.home_starters_subtitle
                },
            ),
        )
    }
    items(quickTools, key = { "quick_${it.name}" }) { tool ->
        ToolCard(
            tool = tool,
            isFavorite = tool in uiState.favoriteTools,
            onClick = { onToolClick(tool) },
            onToggleFavorite = { onToggleFavorite(tool) },
            compact = compact,
            showLevelBadge = level == ExperienceLevel.BEGINNER,
        )
    }

    if (compact) {
        // 上級者は分類の入口を挟まず、全部をそのまま並べる。
        // 何がどこにあるか分かっている人にとっては、入口が1枚増えるだけ遠くなる
        toolSections(
            tools = ToolId.entries.toList(),
            uiState = uiState,
            keyPrefix = "all",
            onToolClick = onToolClick,
            onToggleFavorite = onToggleFavorite,
        )
        fullSpan { ProStatusLine(uiState) }
        return
    }

    fullSpan {
        SectionHeader(
            title = stringResource(R.string.home_section_categories),
            subtitle = stringResource(R.string.home_section_categories_subtitle),
        )
    }
    ToolCategory.entries.forEach { category ->
        fullSpan(key = "category_${category.name}") {
            CategoryCard(category = category, onClick = { onCategoryClick(category) })
        }
    }

    fullSpan {
        Column(verticalArrangement = Arrangement.spacedBy(LocalPaDimens.current.spaceMd)) {
            OutlinedButton(
                onClick = { onCategoryClick(null) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.home_all_tools, ToolId.entries.size))
            }
            ProStatusLine(uiState)
        }
    }
}

/**
 * 道具を分類ごとに並べる。検索結果と、上級者ホームの「全部」で使い回している。
 *
 * 分類の見出しを残すのは、似た名前の道具が別分類にもあるため。
 */
private fun LazyGridScope.toolSections(
    tools: List<ToolId>,
    uiState: HomeUiState,
    keyPrefix: String,
    onToolClick: (ToolId) -> Unit,
    onToggleFavorite: (ToolId) -> Unit,
) {
    if (tools.isEmpty()) {
        fullSpan {
            Text(
                text = stringResource(R.string.home_no_results, uiState.query),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = LocalPaDimens.current.spaceLg),
            )
        }
        return
    }

    ToolCategory.entries.forEach { category ->
        val inCategory = tools.filter { it.category == category }
        if (inCategory.isEmpty()) return@forEach

        fullSpan(key = "${keyPrefix}_header_${category.name}") {
            SectionHeader(
                title = stringResource(category.titleRes),
                subtitle = stringResource(R.string.home_tool_count, inCategory.size),
                accent = category.accentColor(),
            )
        }
        items(inCategory, key = { "${keyPrefix}_${it.name}" }) { tool ->
            ToolCard(
                tool = tool,
                isFavorite = tool in uiState.favoriteTools,
                onClick = { onToolClick(tool) },
                onToggleFavorite = { onToggleFavorite(tool) },
                compact = uiState.level == ExperienceLevel.ADVANCED,
                showLevelBadge = uiState.level == ExperienceLevel.BEGINNER,
            )
        }
    }
}

/**
 * 慣れの度合いの札。
 *
 * 設定の奥に入れず、ホームに出している。初めて開いた人が
 * 「情報が多すぎる／少なすぎる」と思ったその場で変えられる位置にないと、
 * この設定は無いのと同じになるため。
 */
@Composable
private fun LevelSelector(
    level: ExperienceLevel,
    onLevelChange: (ExperienceLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
    ) {
        Text(
            text = stringResource(R.string.home_level_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
        ) {
            ExperienceLevel.entries.forEach { entry ->
                PaFilterChip(
                    text = stringResource(entry.labelRes()),
                    selected = entry == level,
                    onClick = { onLevelChange(entry) },
                )
            }
        }
        when (level) {
            ExperienceLevel.BEGINNER -> LevelNote(stringResource(R.string.home_level_beginner_note))
            ExperienceLevel.ADVANCED -> LevelNote(
                stringResource(R.string.home_level_advanced_note, ToolId.entries.size),
            )
            // 既定なので補足を出さない。何も変えていない状態の説明は要らない
            ExperienceLevel.INTERMEDIATE -> Unit
        }
    }
}

@Composable
private fun LevelNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun ExperienceLevel.labelRes(): Int = when (this) {
    ExperienceLevel.BEGINNER -> R.string.home_level_beginner
    ExperienceLevel.INTERMEDIATE -> R.string.home_level_intermediate
    ExperienceLevel.ADVANCED -> R.string.home_level_advanced
}

/**
 * 見出しの絵。
 *
 * 情報は持たせていない。「いま何のアプリを開いたか」を一目で言うためだけに置いている。
 * 数字や図を入れると、その下の本物の測定値と見分けが付かなくなる。
 */
@Composable
private fun Hero(modifier: Modifier = Modifier) {
    val dimens = LocalPaDimens.current
    PaCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentPadding = dimens.space,
        verticalArrangement = Arrangement.spacedBy(dimens.spaceMd),
    ) {
        PaIllustration(
            scene = PaScene.STAGE,
            contentDescription = stringResource(R.string.home_hero_image),
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp),
        )
        Text(
            text = stringResource(R.string.home_hero_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.home_hero_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * マイク校正の状態。
 *
 * 未校正でも使えるアプリなので、赤い警告にはしない。
 * ただし「絶対値は目安のまま」であることは毎回目に入る場所に置く——
 * 有料で売る以上、どの状態で測っているかを隠さないのが前提になっている。
 *
 * @param compact 絵と説明を落として見出しと操作だけにする。上級者の表示で使う
 */
@Composable
private fun CalibrationPanel(
    summary: CalibrationSummary,
    compact: Boolean,
    onOpenGuide: () -> Unit,
    onOpenCalibration: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current

    PaCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = dimens.space,
        verticalArrangement = Arrangement.spacedBy(dimens.spaceMd),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimens.space),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!compact) {
                PaIllustration(
                    scene = if (summary.isCalibrated) {
                        PaScene.CALIBRATION
                    } else {
                        PaScene.ONE_PHONE
                    },
                    modifier = Modifier.size(width = 96.dp, height = 60.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
            ) {
                Text(
                    text = stringResource(
                        if (summary.isCalibrated) {
                            R.string.home_calibration_done_title
                        } else {
                            R.string.home_calibration_todo_title
                        },
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!compact) {
                    Text(
                        text = if (summary.isCalibrated) {
                            stringResource(
                                R.string.home_calibration_done_body,
                                stringResource(summary.bestConfidence.homeLabelRes()),
                            )
                        } else {
                            stringResource(R.string.home_calibration_todo_body)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
            OutlinedButton(onClick = onOpenGuide, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.home_calibration_guide))
            }
            OutlinedButton(onClick = onOpenCalibration, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.home_calibration_open))
            }
        }
    }
}

private fun CalibrationConfidence.homeLabelRes(): Int = when (this) {
    // 未校正のときはこの文言を使わない（呼び出し側で分岐済み）が、
    // when を網羅させておかないと CalibrationConfidence が増えたときに気づけない
    CalibrationConfidence.UNCALIBRATED -> R.string.home_calibration_confidence_fair
    CalibrationConfidence.FAIR -> R.string.home_calibration_confidence_fair
    CalibrationConfidence.GOOD -> R.string.home_calibration_confidence_good
}

/** 分類の入口。絵は分類ごとに変えて、色だけでなく形でも見分けられるようにする。 */
@Composable
private fun CategoryCard(
    category: ToolCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current
    val count = ToolId.entries.count { it.category == category }

    PaCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = dimens.space,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimens.space),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PaIllustration(
                scene = category.scene(),
                modifier = Modifier.size(width = 88.dp, height = 55.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
            ) {
                Text(
                    text = stringResource(category.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(category.bodyRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PaPill(
                    text = stringResource(R.string.home_tool_count, count),
                    leadingDotColor = category.accentColor(),
                )
            }
        }
    }
}

private fun ToolCategory.scene(): PaScene = when (this) {
    ToolCategory.MEASURE -> PaScene.MEASURE
    ToolCategory.CALC -> PaScene.CALC
    ToolCategory.DOCUMENT -> PaScene.DOCUMENT
    ToolCategory.BUSINESS -> PaScene.BUSINESS
}

private fun ToolCategory.bodyRes(): Int = when (this) {
    ToolCategory.MEASURE -> R.string.home_category_measure_body
    ToolCategory.CALC -> R.string.home_category_calc_body
    ToolCategory.DOCUMENT -> R.string.home_category_document_body
    ToolCategory.BUSINESS -> R.string.home_category_business_body
}

/**
 * Pro の状態。
 *
 * ピルに統一しているのは、状態を示す札が画面ごとに違う形をしていると
 * 「これは状態なのか操作なのか」が読み取れなくなるため。
 */
@Composable
private fun ProStatusLine(
    uiState: HomeUiState,
    modifier: Modifier = Modifier,
) {
    val text = if (uiState.proStatus.isPro) {
        stringResource(R.string.home_pro_active, uiState.proStatus.source.label())
    } else {
        stringResource(R.string.home_pro_inactive)
    }

    PaPill(
        text = text,
        tone = if (uiState.proStatus.isPro) PaTone.BRAND else PaTone.NEUTRAL,
        modifier = modifier,
    )
}
