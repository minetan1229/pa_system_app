package com.patoolbox.feature.home

import com.patoolbox.core.model.ProStatus
import com.patoolbox.core.model.ToolId

/**
 * ホーム画面の状態。
 *
 * 検索の絞り込みは ViewModel ではなく Composable 側で行う。
 * ツール名・説明が string リソースなので、その方がロケール切り替えに自然に追従でき、
 * ViewModel を Context 非依存に保てる（＝Robolectric 無しで単体テストできる）。
 */
data class HomeUiState(
    val query: String = "",
    val proStatus: ProStatus = ProStatus.Free,
    val favoriteTools: List<ToolId> = emptyList(),
)
