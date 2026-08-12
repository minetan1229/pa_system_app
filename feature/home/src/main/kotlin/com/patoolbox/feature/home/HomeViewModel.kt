package com.patoolbox.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patoolbox.core.billing.ProGate
import com.patoolbox.core.data.UserPreferencesRepository
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
    proGate: ProGate,
) : ViewModel() {

    private val query = MutableStateFlow("")

    val uiState: StateFlow<HomeUiState> = combine(
        query,
        userPreferencesRepository.preferences,
        proGate.proStatus,
    ) { query, preferences, proStatus ->
        HomeUiState(
            query = query,
            proStatus = proStatus,
            favoriteTools = preferences.favoriteToolIds
                .mapNotNull { ToolId.fromIdOrNull(it) }
                .sortedBy { it.ordinal },
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

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
