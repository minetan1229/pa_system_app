package com.patoolbox.core.data

import com.patoolbox.core.database.dao.CalibrationProfileDao
import com.patoolbox.core.database.entity.CalibrationProfileEntity
import com.patoolbox.core.model.AudioInputType
import com.patoolbox.core.model.CalibrationMethod
import com.patoolbox.core.model.CalibrationProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomCalibrationRepository @Inject constructor(
    private val dao: CalibrationProfileDao,
) : CalibrationRepository {

    override fun observe(
        deviceKey: String,
        inputType: AudioInputType,
    ): Flow<CalibrationProfile> =
        dao.observeFor(deviceKey, inputType.name).map { entity ->
            entity?.toModel() ?: CalibrationProfile.uncalibrated(deviceKey, inputType)
        }

    override fun observeAll(): Flow<List<CalibrationProfile>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    override suspend fun save(profile: CalibrationProfile) {
        dao.upsert(profile.toEntity())
    }

    override suspend fun clear(deviceKey: String, inputType: AudioInputType) {
        dao.delete(deviceKey, inputType.name)
    }
}

private fun CalibrationProfileEntity.toModel() = CalibrationProfile(
    id = id,
    deviceKey = deviceKey,
    inputType = enumValueOrDefault(inputType, AudioInputType.UNKNOWN),
    offsetDb = offsetDb,
    method = enumValueOrDefault(method, CalibrationMethod.NONE),
    calibratedAtEpochMs = calibratedAtEpochMs,
)

private fun CalibrationProfile.toEntity() = CalibrationProfileEntity(
    id = id,
    deviceKey = deviceKey,
    inputType = inputType.name,
    offsetDb = offsetDb,
    method = method.name,
    calibratedAtEpochMs = calibratedAtEpochMs,
)

/** 列挙子が消えた場合でも落ちないようにする（DB に古い名前が残っていても読める）。 */
private inline fun <reified T : Enum<T>> enumValueOrDefault(name: String, default: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: default
