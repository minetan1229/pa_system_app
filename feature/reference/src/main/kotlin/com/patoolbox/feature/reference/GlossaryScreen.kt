package com.patoolbox.feature.reference

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.ui.component.PaToolScaffold

/**
 * PA用語辞典。
 *
 * 以前はリファレンス（コネクタ図鑑・切り分け・周波数など）のタブの1つだったが、
 * 「結線図やスイープの話」と「言葉の意味を引く」は開く場面がまったく違うので
 * 別画面に切り出した。ホームからも直接開ける独立した道具にしてある。
 */
@Composable
fun GlossaryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current
    PaToolScaffold(
        tool = ToolId.GLOSSARY,
        onBack = onBack,
        modifier = modifier,
        title = stringResource(R.string.reference_tab_glossary),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            GlossaryTab(gutter = dimens.gutter)
        }
    }
}
