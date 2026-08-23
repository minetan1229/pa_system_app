package com.patoolbox.core.data

import com.patoolbox.core.model.ConsoleType
import com.patoolbox.core.model.ExperienceLevel
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

    /**
     * 慣れの度合い。ホームの並べ方と説明の量が変わる。
     * 呼ぶと [UserPreferences.hasChosenExperienceLevel] も true になる
     * （設定画面から呼んでも、初回案内から呼んでも同じ扱いでよい）。
     */
    suspend fun setExperienceLevel(level: ExperienceLevel)

    /** よく使う卓の種類。ホームに最初に出す道具が変わる。 */
    suspend fun setConsoleType(console: ConsoleType)
}
