package com.patoolbox.feature.calc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patoolbox.core.billing.ProGate
import com.patoolbox.core.model.ProStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** 計算機は状態を持たないが、Pro タブの判定にだけ ViewModel を使う。 */
@HiltViewModel
class CalcViewModel @Inject constructor(
    proGate: ProGate,
) : ViewModel() {

    val proStatus: StateFlow<ProStatus> = proGate.proStatus.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProStatus.Free,
    )
}
