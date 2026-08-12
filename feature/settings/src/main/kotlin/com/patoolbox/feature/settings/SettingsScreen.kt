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
import com.patoolbox.core.model.ThemeMode
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
            SectionTitle(stringResource(R.string.settings_section_display))
            ThemeMode.entries.forEach { mode ->
                ThemeModeRow(
                    mode = mode,
                    selected = uiState.themeMode == mode,
                    onSelect = { onThemeModeChange(mode) },
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

@Composable
private fun ThemeModeRow(
    mode: ThemeMode,
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
                text = stringResource(mode.labelRes()),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            mode.descriptionRes()?.let { descRes ->
                Text(
                    text = stringResource(descRes),
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
