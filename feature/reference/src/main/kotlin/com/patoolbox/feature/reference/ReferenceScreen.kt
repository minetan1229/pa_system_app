package com.patoolbox.feature.reference

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.ui.component.PaToolScaffold

/**
 * リファレンスのタブ。
 * 計算機と同じく、ホームの各ツールから該当タブを開いた状態で入る。
 */
enum class ReferenceTab(@param:StringRes val titleRes: Int, val tool: ToolId) {
    CONNECTOR(R.string.reference_tab_connector, ToolId.CONNECTOR_REF),
    FREQUENCY(R.string.reference_tab_frequency, ToolId.FREQ_CHART),
    TROUBLESHOOT(R.string.reference_tab_troubleshoot, ToolId.TROUBLESHOOT),
    GLOSSARY(R.string.reference_tab_glossary, ToolId.GLOSSARY),
}

fun ToolId.toReferenceTabOrNull(): ReferenceTab? = when (this) {
    ToolId.CONNECTOR_REF -> ReferenceTab.CONNECTOR
    ToolId.FREQ_CHART -> ReferenceTab.FREQUENCY
    ToolId.TROUBLESHOOT -> ReferenceTab.TROUBLESHOOT
    ToolId.GLOSSARY -> ReferenceTab.GLOSSARY
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceScreen(
    initialTab: ReferenceTab,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }

    // タブごとに別のツール扱いにする。解説も色も切り替わるので、
    // 「いま結線図を見ているのか用語辞典を見ているのか」が上の帯で分かる
    PaToolScaffold(
        tool = selectedTab.tool,
        onBack = onBack,
        modifier = modifier,
        title = stringResource(selectedTab.titleRes),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab.ordinal) {
                ReferenceTab.entries.forEach { tab ->
                    Tab(
                        selected = tab == selectedTab,
                        onClick = { selectedTab = tab },
                        text = { Text(stringResource(tab.titleRes)) },
                    )
                }
            }

            when (selectedTab) {
                ReferenceTab.CONNECTOR -> ConnectorTab(gutter = dimens.gutter)
                ReferenceTab.FREQUENCY -> FrequencyTab(gutter = dimens.gutter)
                ReferenceTab.TROUBLESHOOT -> TroubleshootTab(
                    gutter = dimens.gutter,
                    minTouch = dimens.minTouch,
                )
                ReferenceTab.GLOSSARY -> GlossaryTab(gutter = dimens.gutter)
            }
        }
    }
}
