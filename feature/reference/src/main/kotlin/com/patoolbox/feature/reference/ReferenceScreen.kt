package com.patoolbox.feature.reference

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.patoolbox.core.designsystem.component.PaUnderlineTabs
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.ui.component.PaToolScaffold
import com.patoolbox.core.ui.identityColor

/**
 * リファレンスのタブ。
 * 計算機と同じく、ホームの各ツールから該当タブを開いた状態で入る。
 */
enum class ReferenceTab(@param:StringRes val titleRes: Int, val tool: ToolId) {
    // 単語を引く場面が一番多いので先頭に置く。以前は別画面（GlossaryScreen）に
    // 切り出していたが、帯域チャートなどと同じ「リファレンス」の括りにまとめてほしい、
    // というフィードバックを受けてタブに戻した
    GLOSSARY(R.string.reference_tab_glossary, ToolId.GLOSSARY),
    CONNECTOR(R.string.reference_tab_connector, ToolId.CONNECTOR_REF),
    FREQUENCY(R.string.reference_tab_frequency, ToolId.FREQ_CHART),
    DEGRADATION(R.string.reference_tab_degradation, ToolId.SIGNAL_QUALITY),
    SIGNAL(R.string.reference_tab_signal, ToolId.TEST_SIGNALS),
    TROUBLESHOOT(R.string.reference_tab_troubleshoot, ToolId.TROUBLESHOOT),
}

fun ToolId.toReferenceTabOrNull(): ReferenceTab? = when (this) {
    ToolId.GLOSSARY -> ReferenceTab.GLOSSARY
    ToolId.CONNECTOR_REF -> ReferenceTab.CONNECTOR
    ToolId.FREQ_CHART -> ReferenceTab.FREQUENCY
    ToolId.SIGNAL_QUALITY -> ReferenceTab.DEGRADATION
    ToolId.TEST_SIGNALS -> ReferenceTab.SIGNAL
    ToolId.TROUBLESHOOT -> ReferenceTab.TROUBLESHOOT
    else -> null
}

@Composable
fun ReferenceScreen(
    initialTab: ReferenceTab,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }
    val titles = ReferenceTab.entries.map { stringResource(it.titleRes) }

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
            // 下線の色はいま開いているタブのツール色。
            // 上の識別帯と同じ色になるので、タブと画面の対応が目で追える
            PaUnderlineTabs(
                titles = titles,
                selectedIndex = selectedTab.ordinal,
                onSelect = { index -> selectedTab = ReferenceTab.entries[index] },
                accent = selectedTab.tool.identityColor(),
            )

            when (selectedTab) {
                ReferenceTab.GLOSSARY -> GlossaryTab(gutter = dimens.gutter)
                ReferenceTab.CONNECTOR -> ConnectorTab(gutter = dimens.gutter)
                ReferenceTab.FREQUENCY -> FrequencyTab(gutter = dimens.gutter)
                ReferenceTab.DEGRADATION -> DegradationTab(gutter = dimens.gutter)
                ReferenceTab.SIGNAL -> SignalTab(gutter = dimens.gutter)
                ReferenceTab.TROUBLESHOOT -> TroubleshootTab(
                    gutter = dimens.gutter,
                    minTouch = dimens.minTouch,
                )
            }
        }
    }
}
