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
 * 課金まわりのビルド時設定。値はアプリモジュールの BuildConfig から渡す。
 *
 * value class にしないのは、Dagger が boolean に消える型を扱えないため
 * （`invalid type parameter: boolean` で KSP が落ちる）。
 */
class BillingConfig(
    /**
     * 課金が入るまでの暫定的な全機能開放。
     * Play Billing がまだ無いので、false にすると Pro のツールを誰も開けない。
     */
    val preReleaseUnlock: Boolean,
)

/**
 * Phase 5 まで使う暫定実装。
 *
 * 判定の順は
 * 1. [BillingConfig.preReleaseUnlock] が true → 常に Pro（課金が実装されるまでの全機能開放）
 * 2. デバッグビルドで設定のトグルが ON → Pro（Free 画面の確認用に切り替えられる）
 * 3. それ以外 → Free
 *
 * 1 を消すのが Phase 5 の最初の作業になる。消し忘れると有料機能が無料で出続ける。
 */
@Singleton
class LocalProGate @Inject constructor(
    userPreferencesRepository: UserPreferencesRepository,
    private val buildInfo: BuildInfo,
    private val config: BillingConfig,
) : ProGate {

    override val proStatus: Flow<ProStatus> =
        userPreferencesRepository.preferences.map { prefs ->
            when {
                config.preReleaseUnlock ->
                    ProStatus(isPro = true, source = ProSource.PRE_RELEASE)

                buildInfo.isDebuggable && prefs.debugProOverride ->
                    ProStatus(isPro = true, source = ProSource.DEBUG_OVERRIDE)

                else -> ProStatus.Free
            }
        }
}
