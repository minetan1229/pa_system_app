package com.patoolbox.core.data

import com.patoolbox.core.model.AudioInputType
import com.patoolbox.core.model.CalibrationProfile
import kotlinx.coroutines.flow.Flow

/**
 * 入力デバイスごとの校正値。
 * 内蔵マイクと USB マイクを差し替えても値を取り違えないよう、キーで分けて持つ。
 */
interface CalibrationRepository {

    /** 該当する校正値。無ければ未校正のプロファイルを返す（null にしない）。 */
    fun observe(deviceKey: String, inputType: AudioInputType): Flow<CalibrationProfile>

    fun observeAll(): Flow<List<CalibrationProfile>>

    suspend fun save(profile: CalibrationProfile)

    suspend fun clear(deviceKey: String, inputType: AudioInputType)
}
