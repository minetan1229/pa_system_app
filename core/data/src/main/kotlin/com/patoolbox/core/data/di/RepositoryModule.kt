package com.patoolbox.core.data.di

import com.patoolbox.core.data.CalibrationRepository
import com.patoolbox.core.data.DataStoreUserPreferencesRepository
import com.patoolbox.core.data.RoomCalibrationRepository
import com.patoolbox.core.data.UserPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(
        impl: DataStoreUserPreferencesRepository,
    ): UserPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindCalibrationRepository(
        impl: RoomCalibrationRepository,
    ): CalibrationRepository
}
