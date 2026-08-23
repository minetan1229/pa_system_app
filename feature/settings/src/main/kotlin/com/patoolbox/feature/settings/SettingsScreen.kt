package com.patoolbox.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.ConsoleType
import com.patoolbox.core.model.ExperienceLevel
import com.patoolbox.core.model.ThemeMode
import com.patoolbox.core.reference.HelpTopics
import com.patoolbox.core.ui.component.HelpAction
import com.patoolbox.core.ui.R as CoreUiR

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreen(
        uiState = uiState,
        onThemeModeChange = viewModel::onThemeModeChange,
        onExperienceLevelChange = viewModel::onExperienceLevelChange,
        onConsoleTypeChange = viewModel::onConsoleTypeChange,
        onKeepScreenOnChange = viewModel::onKeepScreenOnChange,
        onDebugProOverrideChange = viewModel::onDebugProOverrideChange,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onExperienceLevelChange: (ExperienceLevel) -> Unit,
    onConsoleTypeChange: (ConsoleType) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onDebugProOverrideChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(CoreUiR.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.gutter),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // 解説は各画面の右上にもあるが、「そもそも解説があること」に気づかない人がいる。
            // 設定からも全部を検索して読めるようにしておく
            SectionTitle(stringResource(R.string.settings_section_help))
            HelpTopics.byId("index")?.let { index ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_help_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    HelpAction(topic = index)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionTitle(stringResource(R.string.settings_section_display))
            ThemeMode.entries.forEach { mode ->
                ChoiceRow(
                    title = stringResource(mode.labelRes()),
                    description = mode.descriptionRes()?.let { stringResource(it) },
                    selected = uiState.themeMode == mode,
                    onSelect = { onThemeModeChange(mode) },
                    minTouch = dimens.minTouch,
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 機能を消す設定ではないので、ここに置いても「制限」に見えないよう
            // 説明を先に出す。ホームの札からも同じ値を変えられる
            SectionTitle(stringResource(R.string.settings_section_profile))
            SubTitle(stringResource(R.string.settings_level))
            Text(
                text = stringResource(R.string.settings_level_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ExperienceLevel.entries.forEach { level ->
                ChoiceRow(
                    title = stringResource(level.labelRes()),
                    description = stringResource(level.descriptionRes()),
                    selected = uiState.profile.level == level,
                    onSelect = { onExperienceLevelChange(level) },
                    minTouch = dimens.minTouch,
                )
            }

            SubTitle(
                text = stringResource(R.string.settings_console),
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = stringResource(R.string.settings_console_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ConsoleType.entries.forEach { console ->
                ChoiceRow(
                    title = stringResource(console.labelRes()),
                    description = console.descriptionRes()?.let { stringResource(it) },
                    selected = uiState.profile.console == console,
                    onSelect = { onConsoleTypeChange(console) },
                    minTouch = dimens.minTouch,
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionTitle(stringResource(R.string.settings_section_measure))
            SwitchRow(
                title = stringResource(R.string.settings_keep_screen_on),
                description = stringResource(R.string.settings_keep_screen_on_desc),
                checked = uiState.keepScreenOnWhileMeasuring,
                onCheckedChange = onKeepScreenOnChange,
                minTouch = dimens.minTouch,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionTitle(stringResource(R.string.settings_section_pro))
            Text(
                text = stringResource(
                    R.string.settings_pro_status,
                    stringResource(
                        if (uiState.proStatus.isPro) {
                            R.string.settings_pro_status_pro
                        } else {
                            R.string.settings_pro_status_free
                        },
                    ),
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (uiState.isDebugBuild) {
                SwitchRow(
                    title = stringResource(R.string.settings_debug_pro),
                    description = stringResource(R.string.settings_debug_pro_desc),
                    checked = uiState.debugProOverride,
                    onCheckedChange = onDebugProOverrideChange,
                    minTouch = dimens.minTouch,
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 有料販売する前提なので、精度の限界と法的な使用範囲は設定画面から常に読めるようにしておく
            SectionTitle(stringResource(R.string.settings_section_legal))
            Text(
                text = stringResource(R.string.settings_disclaimer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text = stringResource(R.string.settings_licenses),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.settings_licenses_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = dimens.gutter),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

/**
 * 択一の1行。テーマ・慣れの度合い・卓の3か所で同じ形にしている。
 * 選択肢の見た目が節ごとに違うと、どれが択一でどれが複数選択なのかが読み取れなくなる。
 */
/** 節の中の小見出し。1つの節に択一の組が2つ並ぶときに、どこで切れるかを示す。 */
@Composable
private fun SubTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(bottom = 2.dp),
    )
}

@Composable
private fun ChoiceRow(
    title: String,
    description: String?,
    selected: Boolean,
    onSelect: () -> Unit,
    minTouch: Dp,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minTouch)
            .selectable(selected = selected, onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
        )
        Column(modifier = Modifier.padding(start = 4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    minTouch: Dp,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minTouch)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.settings_theme_system
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.DARK -> R.string.settings_theme_dark
    ThemeMode.NIGHT_RED -> R.string.settings_theme_night_red
    ThemeMode.OUTDOOR -> R.string.settings_theme_outdoor
}

private fun ThemeMode.descriptionRes(): Int? = when (this) {
    ThemeMode.NIGHT_RED -> R.string.settings_theme_night_red_desc
    ThemeMode.OUTDOOR -> R.string.settings_theme_outdoor_desc
    else -> null
}

private fun ExperienceLevel.labelRes(): Int = when (this) {
    ExperienceLevel.BEGINNER -> R.string.settings_level_beginner
    ExperienceLevel.INTERMEDIATE -> R.string.settings_level_intermediate
    ExperienceLevel.ADVANCED -> R.string.settings_level_advanced
}

private fun ExperienceLevel.descriptionRes(): Int = when (this) {
    ExperienceLevel.BEGINNER -> R.string.settings_level_beginner_desc
    ExperienceLevel.INTERMEDIATE -> R.string.settings_level_intermediate_desc
    ExperienceLevel.ADVANCED -> R.string.settings_level_advanced_desc
}

private fun ConsoleType.labelRes(): Int = when (this) {
    ConsoleType.UNSET -> R.string.settings_console_unset
    ConsoleType.ANALOG -> R.string.settings_console_analog
    ConsoleType.DIGITAL -> R.string.settings_console_digital
}

private fun ConsoleType.descriptionRes(): Int? = when (this) {
    // 指定なしが既定。説明を付けると「何かを選ばされている」ように見える
    ConsoleType.UNSET -> null
    ConsoleType.ANALOG -> R.string.settings_console_analog_desc
    ConsoleType.DIGITAL -> R.string.settings_console_digital_desc
}
