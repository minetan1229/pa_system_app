package com.patoolbox.core.testing

import com.patoolbox.core.billing.ProGate
import com.patoolbox.core.model.ProSource
import com.patoolbox.core.model.ProStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** テストから Free/Pro を自由に切り替えられる ProGate。 */
class FakeProGate(initial: ProStatus = ProStatus.Free) : ProGate {

    private val state = MutableStateFlow(initial)

    override val proStatus: Flow<ProStatus> = state

    fun setPro(isPro: Boolean, source: ProSource = ProSource.SUBSCRIPTION) {
        state.value = if (isPro) {
            ProStatus(isPro = true, source = source)
        } else {
            ProStatus.Free
        }
    }
}
