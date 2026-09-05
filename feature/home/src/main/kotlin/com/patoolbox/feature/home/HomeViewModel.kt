package com.patoolbox.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patoolbox.core.billing.ProGate
import com.patoolbox.core.data.CalibrationRepository
import com.patoolbox.core.data.PlannedShowRepository
import com.patoolbox.core.data.UserPreferencesRepository
import com.patoolbox.core.model.CalibrationConfidence
import com.patoolbox.core.model.CalibrationProfile
import com.patoolbox.core.model.ConsoleType
import com.patoolbox.core.model.ExperienceLevel
import com.patoolbox.core.model.ToolId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    calibrationRepository: CalibrationRepository,
    plannedShowRepository: PlannedShowRepository,
    proGate: ProGate,
) : ViewModel() {

    private val query = MutableStateFlow("")

    val uiState: StateFlow<HomeUiState> = combine(
        query,
        userPreferencesRepository.preferences,
        proGate.proStatus,
        // 校正値は入力機器ごとに保存されている。ホームでは合計を見るだけなので
        // 録音を始めずに済む observeAll を使う（observe は起動中のセッションのキーが要る）
        calibrationRepository.observeAll(),
        // 今日の進行表。本番万能コントローラーと同じものを見て、
        // 「もう始まっています」をホームからも出す
        plannedShowRepository.observeToday(),
    ) { query, preferences, proStatus, profiles, todayShows ->
        HomeUiState(
            query = query,
            proStatus = proStatus,
            favoriteTools = preferences.favoriteToolIds
                .mapNotNull { ToolId.fromIdOrNull(it) }
                .sortedBy { it.ordinal },
            calibration = profiles.toSummary(),
            profile = preferences.profile,
            hasChosenExperienceLevel = preferences.hasChosenExperienceLevel,
            todayShows = todayShows,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = HomeUiState(),
    )

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onToggleFavorite(tool: ToolId) {
        viewModelScope.launch {
            userPreferencesRepository.toggleFavorite(tool)
        }
    }

    /** ホームの札から慣れの度合いを変える。設定画面からも同じ値を書く。 */
    fun onLevelChange(level: ExperienceLevel) {
        viewModelScope.launch {
            userPreferencesRepository.setExperienceLevel(level)
        }
    }

    /** 初回オンボーディングの2問目。設定画面からも同じ値を書く。 */
    fun onConsoleTypeChange(console: ConsoleType) {
        viewModelScope.launch {
            userPreferencesRepository.setConsoleType(console)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/**
 * 保存済みプロファイル → ホームに出す1行。
 *
 * 一番良い1件で代表させる。悪い方に引っ張られると、
 * 測定マイクを繋いでいるのに「手動校正」と出て毎回目に入ることになる。
 */
private fun List<CalibrationProfile>.toSummary(): CalibrationSummary {
    val calibrated = filter { it.isCalibrated }
    return CalibrationSummary(
        calibratedCount = calibrated.size,
        bestConfidence = calibrated
            .maxByOrNull { it.confidence.ordinal }
            ?.confidence
            ?: CalibrationConfidence.UNCALIBRATED,
    )
}
