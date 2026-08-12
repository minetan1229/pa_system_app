package com.patoolbox.core.testing

import com.patoolbox.core.data.UserPreferencesRepository
import com.patoolbox.core.model.ThemeMode
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** DataStore を使わないインメモリ実装。ViewModel のテストに使う。 */
class FakeUserPreferencesRepository(
    initial: UserPreferences = UserPreferences.Default,
) : UserPreferencesRepository {

    private val state = MutableStateFlow(initial)

    override val preferences: Flow<UserPreferences> = state

    val current: UserPreferences get() = state.value

    override suspend fun setThemeMode(mode: ThemeMode) {
        state.value = state.value.copy(themeMode = mode)
    }

    override suspend fun setKeepScreenOnWhileMeasuring(enabled: Boolean) {
        state.value = state.value.copy(keepScreenOnWhileMeasuring = enabled)
    }

    override suspend fun setDebugProOverride(enabled: Boolean) {
        state.value = state.value.copy(debugProOverride = enabled)
    }

    override suspend fun toggleFavorite(tool: ToolId) {
        val favorites = state.value.favoriteToolIds
        state.value = state.value.copy(
            favoriteToolIds = if (tool.name in favorites) {
                favorites - tool.name
            } else {
                favorites + tool.name
            },
        )
    }
}
