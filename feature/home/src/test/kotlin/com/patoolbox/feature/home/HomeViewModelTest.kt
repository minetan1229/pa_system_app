package com.patoolbox.feature.home

import com.google.common.truth.Truth.assertThat
import com.patoolbox.core.model.AudioInputType
import com.patoolbox.core.model.CalibrationConfidence
import com.patoolbox.core.model.CalibrationMethod
import com.patoolbox.core.model.CalibrationProfile
import com.patoolbox.core.model.ConsoleType
import com.patoolbox.core.model.ExperienceLevel
import com.patoolbox.core.model.FieldProfile
import com.patoolbox.core.model.ProSource
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.model.UserPreferences
import com.patoolbox.core.testing.FakeCalibrationRepository
import com.patoolbox.core.testing.FakePlannedShowRepository
import com.patoolbox.core.testing.FakeProGate
import com.patoolbox.core.testing.FakeUserPreferencesRepository
import com.patoolbox.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val proGate = FakeProGate()
    private val calibrationRepository = FakeCalibrationRepository()
    private val plannedShowRepository = FakePlannedShowRepository()

    /**
     * uiState は WhileSubscribed なので、購読者がいないと上流が動かない。
     * テストでは常にコレクタを1つ立ててから value を見る（awaitItem に頼らないので順序が安定する）。
     */
    private fun TestScope.subscribe(viewModel: HomeViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
    }

    @Test
    fun `初期状態は検索なし Free お気に入りなし`() = runTest {
        val viewModel = HomeViewModel(FakeUserPreferencesRepository(), calibrationRepository, plannedShowRepository, proGate)
        subscribe(viewModel)

        val state = viewModel.uiState.value
        assertThat(state.query).isEmpty()
        assertThat(state.proStatus.isPro).isFalse()
        assertThat(state.favoriteTools).isEmpty()
    }

    @Test
    fun `検索文字列が状態に反映される`() = runTest {
        val viewModel = HomeViewModel(FakeUserPreferencesRepository(), calibrationRepository, plannedShowRepository, proGate)
        subscribe(viewModel)

        viewModel.onQueryChange("SPL")

        assertThat(viewModel.uiState.value.query).isEqualTo("SPL")
    }

    @Test
    fun `お気に入りの追加と解除ができる`() = runTest {
        val viewModel = HomeViewModel(FakeUserPreferencesRepository(), calibrationRepository, plannedShowRepository, proGate)
        subscribe(viewModel)

        viewModel.onToggleFavorite(ToolId.SPL_METER)
        assertThat(viewModel.uiState.value.favoriteTools).containsExactly(ToolId.SPL_METER)

        viewModel.onToggleFavorite(ToolId.SPL_METER)
        assertThat(viewModel.uiState.value.favoriteTools).isEmpty()
    }

    @Test
    fun `お気に入りは定義順に並ぶ`() = runTest {
        val repository = FakeUserPreferencesRepository(
            UserPreferences.Default.copy(
                favoriteToolIds = setOf(ToolId.GLOSSARY.name, ToolId.SPL_METER.name),
            ),
        )
        val viewModel = HomeViewModel(repository, calibrationRepository, plannedShowRepository, proGate)
        subscribe(viewModel)

        assertThat(viewModel.uiState.value.favoriteTools)
            .containsExactly(ToolId.SPL_METER, ToolId.GLOSSARY)
            .inOrder()
    }

    @Test
    fun `保存済みの不正なIDは無視する`() = runTest {
        val repository = FakeUserPreferencesRepository(
            UserPreferences.Default.copy(favoriteToolIds = setOf("REMOVED_TOOL")),
        )
        val viewModel = HomeViewModel(repository, calibrationRepository, plannedShowRepository, proGate)
        subscribe(viewModel)

        assertThat(viewModel.uiState.value.favoriteTools).isEmpty()
    }

    @Test
    fun `校正値が無ければ未校正`() = runTest {
        val viewModel = HomeViewModel(FakeUserPreferencesRepository(), calibrationRepository, plannedShowRepository, proGate)
        subscribe(viewModel)

        val calibration = viewModel.uiState.value.calibration
        assertThat(calibration.isCalibrated).isFalse()
        assertThat(calibration.bestConfidence).isEqualTo(CalibrationConfidence.UNCALIBRATED)
    }

    @Test
    fun `校正済みが複数あるときは一番良いものを代表にする`() = runTest {
        val repository = FakeCalibrationRepository(
            listOf(
                // 内蔵マイクを手動校正 → FAIR
                CalibrationProfile(
                    deviceKey = "builtin",
                    inputType = AudioInputType.BUILTIN_MIC,
                    offsetDb = 118.0,
                    method = CalibrationMethod.MANUAL,
                ),
                // USB の測定マイクを校正器で → GOOD
                CalibrationProfile(
                    deviceKey = "USB:UMIK-1",
                    inputType = AudioInputType.USB,
                    offsetDb = 122.0,
                    method = CalibrationMethod.CALIBRATOR,
                ),
            ),
        )
        val viewModel = HomeViewModel(FakeUserPreferencesRepository(), repository, plannedShowRepository, proGate)
        subscribe(viewModel)

        val calibration = viewModel.uiState.value.calibration
        assertThat(calibration.calibratedCount).isEqualTo(2)
        assertThat(calibration.bestConfidence).isEqualTo(CalibrationConfidence.GOOD)
    }

    @Test
    fun `未校正のプロファイルは数えない`() = runTest {
        val repository = FakeCalibrationRepository(
            listOf(CalibrationProfile.uncalibrated("builtin", AudioInputType.BUILTIN_MIC)),
        )
        val viewModel = HomeViewModel(FakeUserPreferencesRepository(), repository, plannedShowRepository, proGate)
        subscribe(viewModel)

        assertThat(viewModel.uiState.value.calibration.isCalibrated).isFalse()
    }

    @Test
    fun `既定は中級者`() = runTest {
        val viewModel = HomeViewModel(FakeUserPreferencesRepository(), calibrationRepository, plannedShowRepository, proGate)
        subscribe(viewModel)

        assertThat(viewModel.uiState.value.level).isEqualTo(ExperienceLevel.INTERMEDIATE)
    }

    @Test
    fun `慣れの度合いを変えると保存されて状態にも出る`() = runTest {
        val repository = FakeUserPreferencesRepository()
        val viewModel = HomeViewModel(repository, calibrationRepository, plannedShowRepository, proGate)
        subscribe(viewModel)

        viewModel.onLevelChange(ExperienceLevel.BEGINNER)

        assertThat(repository.current.profile.level).isEqualTo(ExperienceLevel.BEGINNER)
        assertThat(viewModel.uiState.value.level).isEqualTo(ExperienceLevel.BEGINNER)
    }

    @Test
    fun `卓の種類が状態に流れる`() = runTest {
        val repository = FakeUserPreferencesRepository(
            UserPreferences.Default.copy(
                profile = FieldProfile(console = ConsoleType.ANALOG),
            ),
        )
        val viewModel = HomeViewModel(repository, calibrationRepository, plannedShowRepository, proGate)
        subscribe(viewModel)

        assertThat(viewModel.uiState.value.profile.console).isEqualTo(ConsoleType.ANALOG)
    }

    @Test
    fun `ProGate の状態が UI に伝わる`() = runTest {
        val viewModel = HomeViewModel(FakeUserPreferencesRepository(), calibrationRepository, plannedShowRepository, proGate)
        subscribe(viewModel)

        assertThat(viewModel.uiState.value.proStatus.isPro).isFalse()

        proGate.setPro(true, ProSource.LIFETIME)

        val state = viewModel.uiState.value
        assertThat(state.proStatus.isPro).isTrue()
        assertThat(state.proStatus.source).isEqualTo(ProSource.LIFETIME)
    }
}
