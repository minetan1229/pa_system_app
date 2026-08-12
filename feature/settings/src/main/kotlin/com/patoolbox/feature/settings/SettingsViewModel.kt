package com.patoolbox.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patoolbox.core.billing.ProGate
import com.patoolbox.core.data.BuildInfo
import com.patoolbox.core.data.UserPreferencesRepository
import com.patoolbox.core.model.ProStatus
import com.patoolbox.core.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val keepScreenOnWhileMeasuring: Boolean = true,
    val debugProOverride: Boolean = false,
    val isDebugBuild: Boolean = false,
    val proStatus: ProStatus = ProStatus.Free,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val buildInfo: BuildInfo,
    proGate: ProGate,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        userPreferencesRepository.preferences,
        proGate.proStatus,
    ) { preferences, proStatus ->
        SettingsUiState(
            themeMode = preferences.themeMode,
            keepScreenOnWhileMeasuring = preferences.keepScreenOnWhileMeasuring,
            debugProOverride = preferences.debugProOverride,
            isDebugBuild = buildInfo.isDebuggable,
            proStatus = proStatus,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = SettingsUiState(isDebugBuild = buildInfo.isDebuggable),
    )

    fun onThemeModeChange(mode: ThemeMode) {
        viewModelScope.launch { userPreferencesRepository.setThemeMode(mode) }
    }

    fun onKeepScreenOnChange(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setKeepScreenOnWhileMeasuring(enabled)
        }
    }

    fun onDebugProOverrideChange(enabled: Boolean) {
        // リリースビルドでは ProGate 側でも無視されるが、書き込み自体もさせない
        if (!buildInfo.isDebuggable) return
        viewModelScope.launch { userPreferencesRepository.setDebugProOverride(enabled) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
