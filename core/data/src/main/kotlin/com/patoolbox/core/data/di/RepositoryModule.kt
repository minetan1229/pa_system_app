package com.patoolbox.core.data.di

import com.patoolbox.core.data.CalibrationRepository
import com.patoolbox.core.data.JobRepository
import com.patoolbox.core.data.PatchSheetRepository
import com.patoolbox.core.data.RoomJobRepository
import com.patoolbox.core.data.RoomPatchSheetRepository
import com.patoolbox.core.data.RoomScheduleRepository
import com.patoolbox.core.data.ScheduleRepository
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

    @Binds
    @Singleton
    abstract fun bindJobRepository(impl: RoomJobRepository): JobRepository

    @Binds
    @Singleton
    abstract fun bindPatchSheetRepository(
        impl: RoomPatchSheetRepository,
    ): PatchSheetRepository

    @Binds
    @Singleton
    abstract fun bindScheduleRepository(impl: RoomScheduleRepository): ScheduleRepository
}
