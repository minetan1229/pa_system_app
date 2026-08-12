package com.patoolbox.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.patoolbox.core.model.ThemeMode
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreUserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    override val preferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            themeMode = prefs[Keys.THEME_MODE]
                ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
                ?: ThemeMode.SYSTEM,
            favoriteToolIds = prefs[Keys.FAVORITE_TOOLS].orEmpty(),
            keepScreenOnWhileMeasuring = prefs[Keys.KEEP_SCREEN_ON] ?: true,
            debugProOverride = prefs[Keys.DEBUG_PRO_OVERRIDE] ?: false,
        )
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    override suspend fun setKeepScreenOnWhileMeasuring(enabled: Boolean) {
        dataStore.edit { it[Keys.KEEP_SCREEN_ON] = enabled }
    }

    override suspend fun setDebugProOverride(enabled: Boolean) {
        dataStore.edit { it[Keys.DEBUG_PRO_OVERRIDE] = enabled }
    }

    override suspend fun toggleFavorite(tool: ToolId) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITE_TOOLS].orEmpty()
            prefs[Keys.FAVORITE_TOOLS] = if (tool.name in current) {
                current - tool.name
            } else {
                current + tool.name
            }
        }
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val FAVORITE_TOOLS = stringSetPreferencesKey("favorite_tools")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val DEBUG_PRO_OVERRIDE = booleanPreferencesKey("debug_pro_override")
    }
}
