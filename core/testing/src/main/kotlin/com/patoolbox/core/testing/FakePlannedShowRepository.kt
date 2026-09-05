package com.patoolbox.core.testing

import com.patoolbox.core.data.PlannedShowRepository
import com.patoolbox.core.model.PlannedShow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** 今日の進行表。テストでは入れたものをそのまま返す。 */
class FakePlannedShowRepository(
    shows: List<PlannedShow> = emptyList(),
) : PlannedShowRepository {

    private val state = MutableStateFlow(shows)

    override fun observeToday(): Flow<List<PlannedShow>> = state

    fun emit(shows: List<PlannedShow>) {
        state.value = shows
    }
}
