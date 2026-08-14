package com.patoolbox.core.database.di

import android.content.Context
import androidx.room.Room
import com.patoolbox.core.database.PaDatabase
import com.patoolbox.core.database.dao.CalibrationProfileDao
import com.patoolbox.core.database.dao.JobDao
import com.patoolbox.core.database.dao.MeasurementDao
import com.patoolbox.core.database.dao.PatchSheetDao
import com.patoolbox.core.database.dao.ScheduleItemDao
import com.patoolbox.core.database.dao.RecordingDao
import com.patoolbox.core.database.dao.StagePlotDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providePaDatabase(
        @ApplicationContext context: Context,
    ): PaDatabase = Room.databaseBuilder(
        context = context,
        klass = PaDatabase::class.java,
        name = PaDatabase.NAME,
    ).build()

    @Provides
    fun provideJobDao(database: PaDatabase): JobDao = database.jobDao()

    @Provides
    fun provideCalibrationProfileDao(database: PaDatabase): CalibrationProfileDao =
        database.calibrationProfileDao()

    @Provides
    fun providePatchSheetDao(database: PaDatabase): PatchSheetDao = database.patchSheetDao()

    @Provides
    fun provideScheduleItemDao(database: PaDatabase): ScheduleItemDao =
        database.scheduleItemDao()

    @Provides
    fun provideMeasurementDao(database: PaDatabase): MeasurementDao = database.measurementDao()

    @Provides
    fun provideStagePlotDao(database: PaDatabase): StagePlotDao = database.stagePlotDao()

    @Provides
    fun provideRecordingDao(database: PaDatabase): RecordingDao = database.recordingDao()
}
