package com.patoolbox.core.billing

import com.patoolbox.core.data.BuildInfo
import com.patoolbox.core.data.UserPreferencesRepository
import com.patoolbox.core.model.ProSource
import com.patoolbox.core.model.ProStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 5 まで使う暫定実装。
 * デバッグビルドでは設定画面のトグルで Pro を強制 ON にでき、
 * Free/Pro 両方の画面を実機で確認できる。リリースビルドでは常に Free。
 */
@Singleton
class LocalProGate @Inject constructor(
    userPreferencesRepository: UserPreferencesRepository,
    private val buildInfo: BuildInfo,
) : ProGate {

    override val proStatus: Flow<ProStatus> =
        userPreferencesRepository.preferences.map { prefs ->
            if (buildInfo.isDebuggable && prefs.debugProOverride) {
                ProStatus(isPro = true, source = ProSource.DEBUG_OVERRIDE)
            } else {
                ProStatus.Free
            }
        }
}
