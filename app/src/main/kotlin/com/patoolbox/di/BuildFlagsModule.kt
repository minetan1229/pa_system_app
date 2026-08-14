package com.patoolbox.di

import com.patoolbox.BuildConfig
import com.patoolbox.core.billing.BillingConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * ビルド時のフラグを下位モジュールに渡す。
 *
 * BuildConfig を生成するのはアプリモジュールだけにしている。
 * 各モジュールで生成させると、どのビルドの値を見ているのか分かりにくくなるため。
 */
@Module
@InstallIn(SingletonComponent::class)
object BuildFlagsModule {

    /**
     * Phase 5 で課金を実装したら、app/build.gradle.kts の PRE_RELEASE_UNLOCK を
     * false にする。この Provides 自体はそのまま残してよい。
     */
    @Provides
    @Singleton
    fun provideBillingConfig(): BillingConfig =
        BillingConfig(preReleaseUnlock = BuildConfig.PRE_RELEASE_UNLOCK)
}
