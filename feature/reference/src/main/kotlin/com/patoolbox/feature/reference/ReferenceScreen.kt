package com.patoolbox.feature.reference

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.ui.R as CoreUiR

/**
 * リファレンスのタブ。
 * 計算機と同じく、ホームの各ツールから該当タブを開いた状態で入る。
 */
enum class ReferenceTab(@param:StringRes val titleRes: Int) {
    CONNECTOR(R.string.reference_tab_connector),
    FREQUENCY(R.string.reference_tab_frequency),
    TROUBLESHOOT(R.string.reference_tab_troubleshoot),
    GLOSSARY(R.string.reference_tab_glossary),
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(selectedTab.titleRes)) },
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
