package com.patoolbox.core.data.di

import com.patoolbox.core.data.CalibrationRepository
import com.patoolbox.core.data.JobRepository
import com.patoolbox.core.data.PatchSheetRepository
import com.patoolbox.core.data.RoomJobRepository
import com.patoolbox.core.data.RoomPatchSheetRepository
import com.patoolbox.core.data.MeasurementRepository
import com.patoolbox.core.data.RoomMeasurementRepository
import com.patoolbox.core.data.BackupRepository
import com.patoolbox.core.data.GearRepository
import com.patoolbox.core.data.InvoiceRepository
import com.patoolbox.core.data.LocalRecordingRepository
import com.patoolbox.core.data.LocalSoundCueRepository
import com.patoolbox.core.data.RoomGearRepository
import com.patoolbox.core.data.RoomInvoiceRepository
import com.patoolbox.core.data.RoomSnapshotRepository
import com.patoolbox.core.data.RoomWorkLogRepository
import com.patoolbox.core.data.SnapshotRepository
import com.patoolbox.core.data.SqliteBackupRepository
import com.patoolbox.core.data.WorkLogRepository
import com.patoolbox.core.data.RecordingRepository
import com.patoolbox.core.data.SoundCueRepository
import com.patoolbox.core.data.RoomStagePlotRepository
import com.patoolbox.core.data.StagePlotRepository
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

    @Binds
    @Singleton
    abstract fun bindMeasurementRepository(
        impl: RoomMeasurementRepository,
    ): MeasurementRepository

    @Binds
    @Singleton
    abstract fun bindStagePlotRepository(
        impl: RoomStagePlotRepository,
    ): StagePlotRepository

    @Binds
    @Singleton
    abstract fun bindRecordingRepository(
        impl: LocalRecordingRepository,
    ): RecordingRepository

    @Binds
    @Singleton
    abstract fun bindSoundCueRepository(
        impl: LocalSoundCueRepository,
    ): SoundCueRepository

    @Binds
    @Singleton
    abstract fun bindGearRepository(impl: RoomGearRepository): GearRepository

    @Binds
    @Singleton
    abstract fun bindSnapshotRepository(impl: RoomSnapshotRepository): SnapshotRepository

    @Binds
    @Singleton
    abstract fun bindInvoiceRepository(impl: RoomInvoiceRepository): InvoiceRepository

    @Binds
    @Singleton
    abstract fun bindWorkLogRepository(impl: RoomWorkLogRepository): WorkLogRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: SqliteBackupRepository): BackupRepository
}
