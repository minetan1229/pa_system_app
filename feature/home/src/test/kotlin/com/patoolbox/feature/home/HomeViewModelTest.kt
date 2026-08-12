package com.patoolbox.feature.home

import com.google.common.truth.Truth.assertThat
import com.patoolbox.core.model.ProSource
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.model.UserPreferences
import com.patoolbox.core.testing.FakeProGate
import com.patoolbox.core.testing.FakeUserPreferencesRepository
import com.patoolbox.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val proGate = FakeProGate()

    /**
     * uiState は WhileSubscribed なので、購読者がいないと上流が動かない。
     * テストでは常にコレクタを1つ立ててから value を見る（awaitItem に頼らないので順序が安定する）。
     */
    private fun TestScope.subscribe(viewModel: HomeViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
    }

    @Test
    fun `初期状態は検索なし Free お気に入りなし`() = runTest {
        val viewModel = HomeViewModel(FakeUserPreferencesRepository(), proGate)
        subscribe(viewModel)

        val state = viewModel.uiState.value
        assertThat(state.query).isEmpty()
        assertThat(state.proStatus.isPro).isFalse()
        assertThat(state.favoriteTools).isEmpty()
    }

    @Test
    fun `検索文字列が状態に反映される`() = runTest {
        val viewModel = HomeViewModel(FakeUserPreferencesRepository(), proGate)
        subscribe(viewModel)

        viewModel.onQueryChange("SPL")

        assertThat(viewModel.uiState.value.query).isEqualTo("SPL")
    }

    @Test
    fun `お気に入りの追加と解除ができる`() = runTest {
        val viewModel = HomeViewModel(FakeUserPreferencesRepository(), proGate)
        subscribe(viewModel)

        viewModel.onToggleFavorite(ToolId.SPL_METER)
        assertThat(viewModel.uiState.value.favoriteTools).containsExactly(ToolId.SPL_METER)

        viewModel.onToggleFavorite(ToolId.SPL_METER)
        assertThat(viewModel.uiState.value.favoriteTools).isEmpty()
    }

    @Test
    fun `お気に入りは定義順に並ぶ`() = runTest {
        val repository = FakeUserPreferencesRepository(
            UserPreferences.Default.copy(
                favoriteToolIds = setOf(ToolId.GLOSSARY.name, ToolId.SPL_METER.name),
            ),
        )
        val viewModel = HomeViewModel(repository, proGate)
        subscribe(viewModel)

        assertThat(viewModel.uiState.value.favoriteTools)
            .containsExactly(ToolId.SPL_METER, ToolId.GLOSSARY)
            .inOrder()
    }

    @Test
    fun `保存済みの不正なIDは無視する`() = runTest {
        val repository = FakeUserPreferencesRepository(
            UserPreferences.Default.copy(favoriteToolIds = setOf("REMOVED_TOOL")),
        )
        val viewModel = HomeViewModel(repository, proGate)
        subscribe(viewModel)

        assertThat(viewModel.uiState.value.favoriteTools).isEmpty()
    }

    @Test
    fun `ProGate の状態が UI に伝わる`() = runTest {
        val viewModel = HomeViewModel(FakeUserPreferencesRepository(), proGate)
        subscribe(viewModel)

        assertThat(viewModel.uiState.value.proStatus.isPro).isFalse()

        proGate.setPro(true, ProSource.LIFETIME)

        val state = viewModel.uiState.value
        assertThat(state.proStatus.isPro).isTrue()
        assertThat(state.proStatus.source).isEqualTo(ProSource.LIFETIME)
    }
}
