package com.patoolbox.core.testing

import com.patoolbox.core.data.CalibrationRepository
import com.patoolbox.core.model.AudioInputType
import com.patoolbox.core.model.CalibrationProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * メモリ上の校正値。
 *
 * 本物と同じく [observe] は該当が無くても未校正のプロファイルを返す（null にしない）。
 * ここで null を返すようにすると、テストは通るのに実機で NPE、が起きる。
 */
class FakeCalibrationRepository(
    initial: List<CalibrationProfile> = emptyList(),
) : CalibrationRepository {

    private val profiles = MutableStateFlow(initial)

    override fun observe(deviceKey: String, inputType: AudioInputType): Flow<CalibrationProfile> =
        profiles.map { list ->
            list.firstOrNull { it.deviceKey == deviceKey && it.inputType == inputType }
                ?: CalibrationProfile.uncalibrated(deviceKey, inputType)
        }

    override fun observeAll(): Flow<List<CalibrationProfile>> = profiles

    override suspend fun save(profile: CalibrationProfile) {
        profiles.update { list ->
            list.filterNot {
                it.deviceKey == profile.deviceKey && it.inputType == profile.inputType
            } + profile
        }
    }

    override suspend fun clear(deviceKey: String, inputType: AudioInputType) {
        profiles.update { list ->
            list.filterNot { it.deviceKey == deviceKey && it.inputType == inputType }
        }
    }
}
