package com.patoolbox.core.billing.di

import com.patoolbox.core.billing.LocalProGate
import com.patoolbox.core.billing.ProGate
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BillingModule {

    /** Phase 5 で PlayBillingProGate に差し替える。 */
    @Binds
    @Singleton
    abstract fun bindProGate(impl: LocalProGate): ProGate
}
