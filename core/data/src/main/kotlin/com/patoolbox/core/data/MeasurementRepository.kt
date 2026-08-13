package com.patoolbox.core.data

import com.patoolbox.core.database.dao.MeasurementDao
import com.patoolbox.core.database.entity.MeasurementEntity
import com.patoolbox.core.database.entity.MeasurementSampleEntity
import com.patoolbox.core.model.CalibrationMethod
import com.patoolbox.core.model.Measurement
import com.patoolbox.core.model.MeasurementSample
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** 保存された測定。SPLロガーが書き、書き出し画面が読む。 */
interface MeasurementRepository {

    fun observeAll(): Flow<List<Measurement>>

    suspend fun find(id: Long): Measurement?

    suspend fun samples(id: Long): List<MeasurementSample>

    suspend fun count(): Int

    /** @return 保存した測定のID */
    suspend fun save(measurement: Measurement, samples: List<MeasurementSample>): Long

    suspend fun delete(id: Long)
}

@Singleton
class RoomMeasurementRepository @Inject constructor(
    private val dao: MeasurementDao,
) : MeasurementRepository {

    override fun observeAll(): Flow<List<Measurement>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    override suspend fun find(id: Long): Measurement? = dao.findById(id)?.toModel()

    override suspend fun samples(id: Long): List<MeasurementSample> =
        dao.samplesOf(id).map { it.toModel() }

    override suspend fun count(): Int = dao.count()

    override suspend fun save(
        measurement: Measurement,
        samples: List<MeasurementSample>,
    ): Long = dao.save(
        measurement = measurement.toEntity(),
        samples = samples.map { it.toEntity(measurement.id) },
    )

    override suspend fun delete(id: Long) = dao.deleteById(id)
}

private fun MeasurementEntity.toModel() = Measurement(
    id = id,
    jobId = jobId,
    title = title,
    startedAtEpochMs = startedAtEpochMs,
    endedAtEpochMs = endedAtEpochMs,
    frequencyWeighting = frequencyWeighting,
    timeWeighting = timeWeighting,
    calibrationOffsetDb = calibrationOffsetDb,
    calibrationMethod = CalibrationMethod.entries
        .firstOrNull { it.name == calibrationMethod } ?: CalibrationMethod.NONE,
    leqDb = leqDb,
    maxDb = maxDb,
    minDb = minDb,
    peakDb = peakDb,
    l10Db = l10Db,
    l50Db = l50Db,
    l90Db = l90Db,
    clipped = clipped,
)

private fun Measurement.toEntity() = MeasurementEntity(
    id = id,
    jobId = jobId,
    title = title,
    startedAtEpochMs = startedAtEpochMs,
    endedAtEpochMs = endedAtEpochMs,
    frequencyWeighting = frequencyWeighting,
    timeWeighting = timeWeighting,
    calibrationOffsetDb = calibrationOffsetDb,
    calibrationMethod = calibrationMethod.name,
    leqDb = leqDb,
    maxDb = maxDb,
    minDb = minDb,
    peakDb = peakDb,
    l10Db = l10Db,
    l50Db = l50Db,
    l90Db = l90Db,
    clipped = clipped,
)

private fun MeasurementSampleEntity.toModel() = MeasurementSample(
    offsetMs = offsetMs,
    instantDb = instantDb,
    leqDb = leqDb,
    marker = marker,
)

private fun MeasurementSample.toEntity(measurementId: Long) = MeasurementSampleEntity(
    measurementId = measurementId,
    offsetMs = offsetMs,
    instantDb = instantDb,
    leqDb = leqDb,
    marker = marker,
)
