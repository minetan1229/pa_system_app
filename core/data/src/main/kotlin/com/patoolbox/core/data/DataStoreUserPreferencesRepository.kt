package com.patoolbox.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.patoolbox.core.model.ConsoleType
import com.patoolbox.core.model.ExperienceLevel
import com.patoolbox.core.model.FieldProfile
import com.patoolbox.core.model.ShowModeSettings
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
            showMode = ShowModeSettings(
                silenceNotifications = prefs[Keys.SHOW_MODE_SILENCE]
                    ?: ShowModeSettings.Default.silenceNotifications,
                allowAlarms = prefs[Keys.SHOW_MODE_ALARMS]
                    ?: ShowModeSettings.Default.allowAlarms,
                keepScreenOn = prefs[Keys.SHOW_MODE_SCREEN_ON]
                    ?: ShowModeSettings.Default.keepScreenOn,
                allowOtherAppAudio = prefs[Keys.SHOW_MODE_OTHER_AUDIO]
                    ?: ShowModeSettings.Default.allowOtherAppAudio,
            ),
            profile = FieldProfile(
                // 知らない名前が入っていたら既定に戻す。enum を消したり
                // 名前を変えたりしたときに、古い端末で落とさないため
                level = prefs[Keys.EXPERIENCE_LEVEL]
                    ?.let { stored -> ExperienceLevel.entries.firstOrNull { it.name == stored } }
                    ?: FieldProfile.Default.level,
                console = prefs[Keys.CONSOLE_TYPE]
                    ?.let { stored -> ConsoleType.entries.firstOrNull { it.name == stored } }
                    ?: FieldProfile.Default.console,
            ),
            hasChosenExperienceLevel = prefs[Keys.HAS_CHOSEN_EXPERIENCE_LEVEL] ?: false,
        )
    }

    override suspend fun setShowMode(settings: ShowModeSettings) {
        dataStore.edit { prefs ->
            prefs[Keys.SHOW_MODE_SILENCE] = settings.silenceNotifications
            prefs[Keys.SHOW_MODE_ALARMS] = settings.allowAlarms
            prefs[Keys.SHOW_MODE_SCREEN_ON] = settings.keepScreenOn
            prefs[Keys.SHOW_MODE_OTHER_AUDIO] = settings.allowOtherAppAudio
        }
    }

    override suspend fun setExperienceLevel(level: ExperienceLevel) {
        dataStore.edit { prefs ->
            prefs[Keys.EXPERIENCE_LEVEL] = level.name
            prefs[Keys.HAS_CHOSEN_EXPERIENCE_LEVEL] = true
        }
    }

    override suspend fun setConsoleType(console: ConsoleType) {
        dataStore.edit { it[Keys.CONSOLE_TYPE] = console.name }
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
        val SHOW_MODE_SILENCE = booleanPreferencesKey("show_mode_silence_notifications")
        val SHOW_MODE_ALARMS = booleanPreferencesKey("show_mode_allow_alarms")
        val SHOW_MODE_SCREEN_ON = booleanPreferencesKey("show_mode_keep_screen_on")
        val SHOW_MODE_OTHER_AUDIO = booleanPreferencesKey("show_mode_allow_other_audio")
        val EXPERIENCE_LEVEL = stringPreferencesKey("experience_level")
        val HAS_CHOSEN_EXPERIENCE_LEVEL = booleanPreferencesKey("has_chosen_experience_level")
        val CONSOLE_TYPE = stringPreferencesKey("console_type")
    }
}
