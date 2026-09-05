package com.patoolbox.feature.home

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.model.PlannedShow
import com.patoolbox.core.ui.DateTimeText
import kotlinx.coroutines.delay
import com.patoolbox.core.designsystem.component.PaAppMark
import com.patoolbox.core.designsystem.component.PaCard
import com.patoolbox.core.designsystem.component.PaIllustration
import com.patoolbox.core.designsystem.component.PaNotice
import com.patoolbox.core.designsystem.component.PaPill
import com.patoolbox.core.designsystem.component.PaScene
import com.patoolbox.core.designsystem.component.PaTone
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.CalibrationConfidence
import com.patoolbox.core.model.ConsoleType
import com.patoolbox.core.model.ExperienceLevel
import com.patoolbox.core.model.ToolCategory
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.ui.accentColor
import com.patoolbox.core.ui.component.OpenAccessNotice
import com.patoolbox.core.ui.component.ToolCard
import com.patoolbox.core.ui.descriptionResOrNull
import com.patoolbox.core.ui.labelRes
import com.patoolbox.core.ui.titleRes

/**
 * ホーム。
 *
 * 上から
 *
 * 0. 今日の進行表（あるときだけ。始まっていれば「スタートしますか？」）
 * 1. おすすめ／★を付けた道具
 * 2. 最初に一度だけやること（マイク校正）と、本番前に開く近道
 * 3. すべての道具
 * 4. まとめ——分類の入口
 *
 * の順に置く。**この並びはどの段でも同じ**にしてある（[landingSections]）。
 * 段によって置き場所が変わる方が、初心者にも上級者にも覚えにくかった。
 * [ExperienceLevel] で変えるのは説明の量とカードの密度だけで、
 * **機能は消さない**——どの段でも全部の道具に手が届く。
 *
 * 検索はこの画面に置いたまま結果をその場に出す（現場では「探す」より
 * 「打つ」方が速いので、1画面ぶんの移動も惜しい）。分類で絞った一覧は
 * [ToolListScreen] に分けてある。
 */
@Composable
fun HomeScreen(
    onToolClick: (ToolId) -> Unit,
    onCategoryClick: (ToolCategory?) -> Unit,
    onCalibrationClick: () -> Unit,
    onCalibrationGuideClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    onStartShow: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onToggleFavorite = viewModel::onToggleFavorite,
        onLevelChange = viewModel::onLevelChange,
        onConsoleTypeChange = viewModel::onConsoleTypeChange,
        onToolClick = onToolClick,
        onCategoryClick = onCategoryClick,
        onCalibrationClick = onCalibrationClick,
        onCalibrationGuideClick = onCalibrationGuideClick,
        onSettingsClick = onSettingsClick,
        onStartShow = onStartShow,
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
    onConsoleTypeChange: (ConsoleType) -> Unit,
    onToolClick: (ToolId) -> Unit,
    onCategoryClick: (ToolCategory?) -> Unit,
    onCalibrationClick: () -> Unit,
    onCalibrationGuideClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    onStartShow: () -> Unit = {},
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
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // 印を題の左に置く。文字だけの上帯はどの画面でも同じ顔になり、
                            // 38画面を行き来していると自分がどこにいるのか手掛かりが無くなる
                            PaAppMark(modifier = Modifier.size(dimens.space + dimens.spaceSm))
                            Text(
                                text = stringResource(R.string.home_title),
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }
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
        // 慣れの度合いをまだ選んでいなければ、検索も分類もヒーローも出さず
        // 大画面のオンボーディングだけに専念させる。埋め込みカードだと
        // 一覧の中に紛れて見落とされるうえ、他の操作ができてしまい迷う
        if (!uiState.hasChosenExperienceLevel) {
            OnboardingFlow(
                onComplete = { level, console ->
                    onLevelChange(level)
                    onConsoleTypeChange(console)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // スクロールに追従させず、常に同じ場所に固定する。分類を掘って下まで
            // スクロールしたあとも、一番上まで戻らずにその場で検索し直せるようにするため
            // （検索欄が一覧の中に埋もれると、道具の在り処が分からず「迷路」に感じる原因になる）
            SearchField(
                query = uiState.query,
                onQueryChange = onQueryChange,
                modifier = Modifier.padding(
                    horizontal = dimens.gutter,
                    vertical = dimens.spaceSm,
                ),
            )

            LazyVerticalGrid(
                // タブレット横持ちでは3〜4列に増える
                columns = GridCells.Adaptive(minSize = 168.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
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
                        // 今日の進行表があれば何よりも先に出す。始まっているなら
                        // 「スタートしますか？」まで、この1枚で片が付くようにする
                        if (query.isEmpty()) {
                            val now by produceState(initialValue = System.currentTimeMillis()) {
                                while (true) {
                                    value = System.currentTimeMillis()
                                    delay(NOW_TICK_MS)
                                }
                            }
                            uiState.showForNow(now)?.let { show ->
                                TodayShowCard(
                                    show = show,
                                    nowEpochMs = now,
                                    onStart = onStartShow,
                                    onOpen = { onToolClick(ToolId.SHOW_RUNNER) },
                                )
                            }
                        }
                        // 上級者では見出しの絵を出さない。情報を持たない絵に
                        // 一画面の1/3を使われるより、道具が1行でも多く見える方がいい
                        if (query.isEmpty() && uiState.level != ExperienceLevel.ADVANCED) {
                            Hero()
                        }
                        if (query.isEmpty()) {
                            // オンボーディングは完了済み（ここに来る時点で hasChosenExperienceLevel）。
                            // 現在値だけの小さい行にして、変えたくなったら設定画面へ飛べるようにする
                            LevelIndicator(level = uiState.level, onOpenSettings = onSettingsClick)
                            // 段の札のすぐ下に置く。「初心者にすると札が増える」ことと
                            // 「札は増えるが開けなくなるものは無い」ことを続けて読ませたい
                            if (uiState.level != ExperienceLevel.ADVANCED) {
                                OpenAccessNotice(proUnlocked = uiState.proStatus.isPro)
                            }
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
}

/**
 * 検索していないときに出す中身。
 *
 * 並びはどの段でも同じにしてある。以前は初心者に「分類から掘る入口」だけを見せ、
 * 上級者にだけ全部を並べていたが、**同じアプリなのに人によって置き場所が違う**のは
 * 結局どちらの段にとっても覚えにくい。いま置いているのは上から
 *
 * 1. おすすめ（★を付けていればその道具）——最初の1タップをここで終わらせる
 * 2. はじめに（マイク校正）と、本番前に開く近道
 * 3. すべての道具（分類の見出し付き）
 * 4. まとめ——分類の入口。**全部を見せたあと**に置く
 *
 * 4 を最後にしたのが今回の要点。分類は「探し方の目次」であって、開いたときに
 * 最初に読みたいものではない。上に置くと、目当ての道具に届くまでに必ず
 * 一段深く潜ることになり、初心者ほど迷子になっていた。
 *
 * 変わるのは並べ方と説明の量だけで、**どの段でも全部の道具に手が届く**。
 */
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

    // ① おすすめ（一番上）。
    // 初心者・中級者には「まず開く4つ」を出す。上級者に自動のおすすめを出さないのは、
    // その段の人は★で自分の4つを決めているため——勝手に並ぶ方が邪魔になる
    val hasFavorites = uiState.favoriteTools.isNotEmpty()
    val quickTools = when {
        hasFavorites -> uiState.favoriteTools
        compact -> emptyList()
        else -> starterTools(uiState.profile)
    }
    if (quickTools.isNotEmpty()) {
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
    }

    // ② はじめに（校正）と近道
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
    // 用語集と本番万能コントローラーは、分類を掘ったり検索したりせずここから一発で開ける。
    // 前者は現場で言葉が飛んできた瞬間に、後者は本番が始まる直前に開きたい道具
    fullSpan {
        QuickLinksPanel(
            onOpenGlossary = { onToolClick(ToolId.GLOSSARY) },
            onOpenShowRunner = { onToolClick(ToolId.SHOW_RUNNER) },
        )
    }

    // ③ すべての道具
    toolSections(
        tools = ToolId.entries.toList(),
        uiState = uiState,
        keyPrefix = "all",
        onToolClick = onToolClick,
        onToggleFavorite = onToggleFavorite,
    )

    // ④ まとめ（分類の入口）。一番下に置く
    fullSpan {
        SectionHeader(
            title = stringResource(R.string.home_section_categories),
            subtitle = stringResource(R.string.home_section_categories_subtitle),
        )
    }
    if (level == ExperienceLevel.BEGINNER) {
        fullSpan {
            PaNotice(
                title = stringResource(R.string.home_beginner_guide_title),
                body = stringResource(R.string.home_beginner_guide_body),
                tone = PaTone.INFO,
            )
        }
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
 * 今日の進行表。
 *
 * 日付の入った進行表（案件管理で作るもの）のうち、今日のものだけをホームに出す。
 * すでに予定の時刻を過ぎているなら「始まっています。スタートしますか？」まで言う——
 * 本番中に道具を探させないための1枚なので、押す先は1つに絞ってある。
 */
@Composable
private fun TodayShowCard(
    show: PlannedShow,
    nowEpochMs: Long,
    onStart: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current
    val started = show.hasStarted(nowEpochMs)
    val current = show.entryAt(nowEpochMs)

    PaCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = if (started) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        borderColor = if (started) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        contentPadding = dimens.space,
        verticalArrangement = Arrangement.spacedBy(dimens.spaceSm),
    ) {
        Text(
            text = stringResource(
                if (started) R.string.home_today_started_title else R.string.home_today_title,
            ),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(
                R.string.home_today_name,
                show.job.name,
                DateTimeText.formatTime(show.startAtEpochMs),
                show.totalMinutes,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        current?.let { entry ->
            Text(
                text = stringResource(
                    R.string.home_today_now,
                    entry.item.title,
                    DateTimeText.formatTime(entry.endAtEpochMs),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (started) {
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(dimens.minTouch * 1.3f),
            ) {
                Text(stringResource(R.string.home_today_start))
            }
        }
        OutlinedButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.home_today_open))
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
        fullSpan { EmptyResult(query = uiState.query) }
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
 * 初回オンボーディング。
 *
 * 「慣れの度合い」→「卓の種類」の2問だけを、大画面で1問ずつ尋ねる。
 * 埋め込みカードにしないのは、一覧の中に紛れて見落とされるのと、
 * 選び終わるまで他の操作ができてしまうのを防ぐため——
 * 選び終わるまでは検索も分類も出さず、この画面に専念させる。
 *
 * 両方選び終えてはじめて [onComplete] を呼ぶ。呼ぶと
 * [HomeUiState.hasChosenExperienceLevel] が true になり、この画面自体が消えて
 * 通常のホームに置き換わる（呼び出し側の条件分岐で制御している）。
 * 以降、慣れの度合いと卓の種類を変えられるのは設定画面からだけにする——
 * ホームに常時の切り替えを置くと、選び終えた人には毎回場所を取るだけになる。
 */
@Composable
private fun OnboardingFlow(
    onComplete: (ExperienceLevel, ConsoleType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedLevel by rememberSaveable { mutableStateOf<ExperienceLevel?>(null) }
    val level = selectedLevel

    if (level == null) {
        LevelOnboardingScreen(
            onSelect = { selectedLevel = it },
            modifier = modifier,
        )
    } else {
        ConsoleOnboardingScreen(
            onSelect = { console -> onComplete(level, console) },
            onBack = { selectedLevel = null },
            modifier = modifier,
        )
    }
}

@Composable
private fun LevelOnboardingScreen(
    onSelect: (ExperienceLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dimens.gutter, vertical = dimens.spaceXl),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceLg, Alignment.CenterVertically),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceXs)) {
            Text(
                text = stringResource(R.string.home_onboarding_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.home_onboarding_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceMd)) {
            ExperienceLevel.entries.forEach { entry ->
                OnboardingOption(
                    label = stringResource(entry.labelRes()),
                    note = stringResource(entry.onboardingNoteRes(), ToolId.entries.size),
                    onClick = { onSelect(entry) },
                )
            }
        }
    }
}

@Composable
private fun ConsoleOnboardingScreen(
    onSelect: (ConsoleType) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dimens.gutter, vertical = dimens.spaceXl),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceLg, Alignment.CenterVertically),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceXs)) {
            Text(
                text = stringResource(R.string.home_onboarding_console_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.home_onboarding_console_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceMd)) {
            ConsoleType.entries.forEach { entry ->
                OnboardingOption(
                    label = stringResource(entry.labelRes()),
                    note = entry.descriptionResOrNull()?.let { stringResource(it) },
                    onClick = { onSelect(entry) },
                )
            }
        }
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.home_onboarding_back))
        }
    }
}

@Composable
private fun OnboardingOption(
    label: String,
    note: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current
    PaCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = dimens.space,
        verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (note != null) {
            Text(
                text = note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 選んだあとの、慣れの度合いの現在値。
 *
 * 常時のチップ列（[LevelOnboarding] 相当のもの）に戻さないのは、
 * 選び終えた人には毎回同じ選択肢が場所を取るだけになるため。
 * とはいえ完全に隠すと「変えられることに気づけない」ので、
 * 現在値と設定への導線だけは毎回出しておく。
 */
@Composable
private fun LevelIndicator(
    level: ExperienceLevel,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.home_level_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PaPill(text = stringResource(level.labelRes()), tone = PaTone.NEUTRAL)
        TextButton(onClick = onOpenSettings) {
            Text(
                text = stringResource(R.string.home_level_change),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

private fun ExperienceLevel.labelRes(): Int = when (this) {
    ExperienceLevel.BEGINNER -> R.string.home_level_beginner
    ExperienceLevel.INTERMEDIATE -> R.string.home_level_intermediate
    ExperienceLevel.ADVANCED -> R.string.home_level_advanced
}

private fun ExperienceLevel.onboardingNoteRes(): Int = when (this) {
    ExperienceLevel.BEGINNER -> R.string.home_level_beginner_note
    ExperienceLevel.INTERMEDIATE -> R.string.home_level_intermediate_note
    ExperienceLevel.ADVANCED -> R.string.home_level_advanced_note
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
                .height(dimens.illustrationHero),
        )
        Text(
            // ここだけセリフ体（displaySmall）。読み物の題と、操作するUIの文字を
            // 書体で分けている。同じゴシックで大きさだけ変えると、
            // 下に続く測定値の見出しと同じ強さに見えてしまう
            text = stringResource(R.string.home_hero_title),
            style = MaterialTheme.typography.displaySmall,
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
                    modifier = Modifier.size(
                        width = dimens.illustrationRow * 1.6f,
                        height = dimens.illustrationRow,
                    ),
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

/**
 * 本番万能コントローラーへの近道。
 *
 * 本番開始直前に探す時間なしで開きたい道具なので、ホームから一発で開けるように
 * 他のカードより目立つ扱いにしてある。
 */
@Composable
private fun QuickLinksPanel(
    onOpenGlossary: () -> Unit,
    onOpenShowRunner: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current

    PaCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        borderColor = MaterialTheme.colorScheme.primary,
        contentPadding = dimens.space,
        verticalArrangement = Arrangement.spacedBy(dimens.spaceMd),
    ) {
        Text(
            text = stringResource(R.string.home_quicklinks_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = stringResource(R.string.home_quicklinks_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
        )
        // 本番万能コントローラーを最優先で大きく出す
        Button(
            onClick = onOpenShowRunner,
            modifier = Modifier.fillMaxWidth().height(dimens.minTouch * 1.3f),
        ) {
            Text(
                text = stringResource(R.string.home_quicklink_showrunner),
                style = MaterialTheme.typography.titleSmall,
            )
        }
        OutlinedButton(onClick = onOpenGlossary, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.home_quicklink_glossary))
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
        // ツールカードと同じ位置に同じ色の帯を出す。入口とその中身が
        // 同じ色で繋がっていないと、押した先が同じ分類だと確認できない
        rail = category.accentColor(),
        contentPadding = dimens.space,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimens.space),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PaIllustration(
                scene = category.scene(),
                modifier = Modifier.size(
                    width = dimens.illustrationRow * 1.6f,
                    height = dimens.illustrationRow,
                ),
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

/** 今日の進行表の「いま」を数え直す間隔。分単位で足りるので粗くていい */
private const val NOW_TICK_MS = 30_000L
