package com.patoolbox.core.data

import com.patoolbox.core.model.ShowModeSettings
import com.patoolbox.core.model.ThemeMode
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow

/**
 * 端末ローカルの設定。外部送信は一切しない（プライバシーポリシーの前提）。
 *
 * interface にしているのは、feature 側のテストで DataStore を用意せずに済ませるため。
 */
interface UserPreferencesRepository {

    val preferences: Flow<UserPreferences>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setKeepScreenOnWhileMeasuring(enabled: Boolean)

    /** デバッグビルド専用。リリースでは [com.patoolbox.core.billing] 側で無視される。 */
    suspend fun setDebugProOverride(enabled: Boolean)

    suspend fun toggleFavorite(tool: ToolId)

    /** 本番モードで何を止めるか。 */
    suspend fun setShowMode(settings: ShowModeSettings)
}
