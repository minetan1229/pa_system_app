package com.patoolbox.feature.calc

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.ui.component.PaToolScaffold

/**
 * 計算機のタブ。
 * ホームの各ツール（ディレイ計算 / BPM / dB換算 / インピーダンス）は
 * すべてこの画面の該当タブを開いた状態で入る。
 */
enum class CalcTab(@param:StringRes val titleRes: Int, val tool: ToolId) {
    DELAY(R.string.calc_tab_delay, ToolId.DELAY_CALC),
    BPM(R.string.calc_tab_bpm, ToolId.BPM_CALC),
    DB(R.string.calc_tab_db, ToolId.DB_CALC),
    IMPEDANCE(R.string.calc_tab_impedance, ToolId.IMPEDANCE_CALC),
    POWER(R.string.calc_tab_power, ToolId.POWER_CALC),
    COVERAGE(R.string.calc_tab_coverage, ToolId.COVERAGE_CALC),
    ;

    /** 電源とカバレッジは Pro 専用。 */
    val requiresPro: Boolean get() = this == POWER || this == COVERAGE
}

/** ToolId → 開くタブ。計算機系以外は null。 */
fun ToolId.toCalcTabOrNull(): CalcTab? = when (this) {
    ToolId.DELAY_CALC -> CalcTab.DELAY
    ToolId.BPM_CALC -> CalcTab.BPM
    ToolId.DB_CALC -> CalcTab.DB
    ToolId.IMPEDANCE_CALC -> CalcTab.IMPEDANCE
    ToolId.POWER_CALC -> CalcTab.POWER
    ToolId.COVERAGE_CALC -> CalcTab.COVERAGE
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalcScreen(
    initialTab: CalcTab,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalcViewModel = hiltViewModel(),
) {
    val dimens = LocalPaDimens.current
    val proStatus by viewModel.proStatus.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }

    // タブごとに別のツール扱いにする。解説と識別色がタブに追従するので、
    // 「いま dB換算 を見ているのか電源計算を見ているのか」が上の帯で分かる
    PaToolScaffold(
        tool = selectedTab.tool,
        onBack = onBack,
        modifier = modifier,
        title = stringResource(selectedTab.titleRes),
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // タブが6つになったので横スクロールにする
            PrimaryScrollableTabRow(selectedTabIndex = selectedTab.ordinal) {
                CalcTab.entries.forEach { tab ->
                    Tab(
                        selected = tab == selectedTab,
                        onClick = { selectedTab = tab },
                        text = { Text(stringResource(tab.titleRes)) },
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = dimens.gutter, vertical = dimens.gutterSmall),
            ) {
                if (selectedTab.requiresPro && !proStatus.isPro) {
                    // 隠さずに「Proで使える」と出す。何が入っているかは見えたほうがよい
                    CalcResult(text = stringResource(R.string.calc_pro_required), emphasis = true)
                } else {
                    when (selectedTab) {
                        CalcTab.DELAY -> DelayTab()
                        CalcTab.BPM -> BpmTab()
                        CalcTab.DB -> DbTab(minTouch = dimens.minTouch)
                        CalcTab.IMPEDANCE -> ImpedanceTab(minTouch = dimens.minTouch)
                        CalcTab.POWER -> PowerTab(minTouch = dimens.minTouch)
                        CalcTab.COVERAGE -> CoverageTab()
                    }
                }
            }
        }
    }
}
