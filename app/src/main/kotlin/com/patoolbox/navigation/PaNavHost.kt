package com.patoolbox.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.ui.component.PlaceholderScreen
import com.patoolbox.feature.analyzer.FftScreen
import com.patoolbox.feature.analyzer.SpectrogramScreen
import com.patoolbox.feature.calc.CalcScreen
import com.patoolbox.feature.calc.toCalcTabOrNull
import com.patoolbox.feature.calibration.CalibrationScreen
import com.patoolbox.feature.feedback.FeedbackScreen
import com.patoolbox.feature.home.HomeScreen
import com.patoolbox.feature.job.JobDetailScreen
import com.patoolbox.feature.job.JobListScreen
import com.patoolbox.feature.measure.DelayFinderScreen
import com.patoolbox.feature.measure.PolarityScreen
import com.patoolbox.feature.measure.RoomMeasureScreen
import com.patoolbox.feature.metronome.MetronomeScreen
import com.patoolbox.feature.patch.PatchListScreen
import com.patoolbox.feature.reference.ReferenceScreen
import com.patoolbox.feature.reference.toReferenceTabOrNull
import com.patoolbox.feature.patch.PatchSheetScreen
import com.patoolbox.feature.rta.RtaScreen
import com.patoolbox.feature.siggen.SigGenScreen
import com.patoolbox.feature.schedule.ScheduleScreen
import com.patoolbox.feature.settings.SettingsScreen
import com.patoolbox.feature.stageplot.StagePlotEditorScreen
import com.patoolbox.feature.stageplot.StagePlotListScreen
import com.patoolbox.feature.showtimer.ShowTimerScreen
import com.patoolbox.feature.spl.SplLogScreen
import com.patoolbox.feature.spl.SplScreen
import com.patoolbox.feature.tuner.TunerScreen
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

/**
 * ツール画面への共通ルート。
 * 実装済みのツールは [ToolDestination] で実画面に振り分け、未実装は PlaceholderScreen に落とす。
 * Phase が進んで画面が増えても、ホーム側は ToolId を渡すだけで済む。
 */
@Serializable
data class ToolRoute(val toolId: String)

@Serializable
data object SettingsRoute

/** マイク校正。ツール一覧には出さず、計測画面と設定から入る。 */
@Serializable
data object CalibrationRoute

/**
 * パッチ表の編集。
 * property 名は PatchSheetViewModel.KEY_SHEET_ID と一致させる必要がある
 * （SavedStateHandle がこの名前で引数を受け取る）。
 */
@Serializable
data class PatchSheetRoute(val sheetId: Long)

/** 案件の詳細。property 名は JobDetailViewModel.KEY_JOB_ID と一致させる。 */
@Serializable
data class JobDetailRoute(val jobId: Long)

/** 進行表。property 名は ScheduleViewModel.KEY_JOB_ID と一致させる。 */
@Serializable
data class ScheduleRoute(val jobId: Long)

/** ステージプロットの編集。property 名は StagePlotViewModel.KEY_PLOT_ID と一致させる。 */
@Serializable
data class StagePlotRoute(val plotId: Long)

@Composable
fun PaNavHost(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onToolClick = { tool -> navController.navigate(ToolRoute(tool.name)) },
                onSettingsClick = { navController.navigate(SettingsRoute) },
            )
        }

        composable<ToolRoute> { backStackEntry ->
            val route: ToolRoute = backStackEntry.toRoute()
            val tool = ToolId.fromIdOrNull(route.toolId)

            if (tool == null) {
                // 保存済みの favorite などから消えた ID が来た場合
                LaunchedEffect(route.toolId) { navController.popBackStack() }
            } else {
                ToolDestination(
                    tool = tool,
                    onBack = { navController.popBackStack() },
                    onOpenCalibration = { navController.navigate(CalibrationRoute) },
                    onOpenPatchSheet = { sheetId ->
                        navController.navigate(PatchSheetRoute(sheetId))
                    },
                    onOpenJob = { jobId -> navController.navigate(JobDetailRoute(jobId)) },
                    onOpenStagePlot = { plotId ->
                        navController.navigate(StagePlotRoute(plotId))
                    },
                )
            }
        }

        composable<SettingsRoute> {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable<CalibrationRoute> {
            CalibrationScreen(onBack = { navController.popBackStack() })
        }

        composable<PatchSheetRoute> {
            PatchSheetScreen(onBack = { navController.popBackStack() })
        }

        composable<JobDetailRoute> {
            JobDetailScreen(
                onOpenSchedule = { jobId -> navController.navigate(ScheduleRoute(jobId)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable<ScheduleRoute> {
            ScheduleScreen(onBack = { navController.popBackStack() })
        }

        composable<StagePlotRoute> {
            StagePlotEditorScreen(onBack = { navController.popBackStack() })
        }
    }
}

/**
 * ToolId → 実画面の振り分け。
 * 実装が済んだツールをここに足し、ToolId 側の implemented を true にする。
 */
@Composable
private fun ToolDestination(
    tool: ToolId,
    onBack: () -> Unit,
    onOpenCalibration: () -> Unit,
    onOpenPatchSheet: (Long) -> Unit,
    onOpenJob: (Long) -> Unit,
    onOpenStagePlot: (Long) -> Unit,
) {
    when (tool) {
        ToolId.SPL_METER -> SplScreen(
            onBack = onBack,
            onOpenCalibration = onOpenCalibration,
        )

        ToolId.SPL_LOGGER -> SplLogScreen(onBack = onBack)

        ToolId.RTA -> RtaScreen(onBack = onBack)

        // FFT とスペクトログラムは同じ解析結果の別の見せ方
        ToolId.FFT -> FftScreen(onBack = onBack)

        ToolId.SPECTROGRAM -> SpectrogramScreen(onBack = onBack)

        ToolId.SIGNAL_GENERATOR -> SigGenScreen(onBack = onBack)

        ToolId.FEEDBACK_FINDER -> FeedbackScreen(onBack = onBack)

        // 3つとも「スイープを鳴らして録る」1回の測定。画面だけ問いに合わせて分けている
        ToolId.DELAY_FINDER -> DelayFinderScreen(onBack = onBack)

        ToolId.POLARITY_CHECK -> PolarityScreen(onBack = onBack)

        ToolId.ROOM_MEASURE -> RoomMeasureScreen(onBack = onBack)

        ToolId.TUNER -> TunerScreen(onBack = onBack)

        ToolId.METRONOME -> MetronomeScreen(onBack = onBack)

        // PDF出力はパッチ表・進行表の画面から行うので、一覧へ送る
        ToolId.PATCH_SHEET, ToolId.PDF_EXPORT -> PatchListScreen(
            onOpenSheet = onOpenPatchSheet,
            onBack = onBack,
        )

        ToolId.STAGE_PLOT -> StagePlotListScreen(
            onOpenPlot = onOpenStagePlot,
            onBack = onBack,
        )

        ToolId.SHOW_TIMER -> ShowTimerScreen(onBack = onBack)

        ToolId.JOB_MANAGER, ToolId.RUN_SHEET -> JobListScreen(
            // 進行表は案件に紐づくので、どちらから来ても案件一覧を経由する
            onOpenJob = onOpenJob,
            onBack = onBack,
        )

        else -> {
            // 計算機とリファレンスはそれぞれ1画面のタブ集合。該当タブを開いた状態で入る
            val calcTab = tool.toCalcTabOrNull()
            val referenceTab = tool.toReferenceTabOrNull()
            when {
                calcTab != null -> CalcScreen(initialTab = calcTab, onBack = onBack)
                referenceTab != null ->
                    ReferenceScreen(initialTab = referenceTab, onBack = onBack)
                else -> PlaceholderScreen(tool = tool, onBack = onBack)
            }
        }
    }
}
