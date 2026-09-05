package com.patoolbox.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.patoolbox.core.model.ToolCategory
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.ui.component.PlaceholderScreen
import com.patoolbox.feature.analyzer.FftScreen
import com.patoolbox.feature.analyzer.SpectrogramScreen
import com.patoolbox.feature.business.BackupScreen
import com.patoolbox.feature.business.GearScreen
import com.patoolbox.feature.business.InvoiceDetailScreen
import com.patoolbox.feature.business.InvoiceListScreen
import com.patoolbox.feature.business.SnapshotDetailScreen
import com.patoolbox.feature.business.SnapshotListScreen
import com.patoolbox.feature.business.WorkLogScreen
import com.patoolbox.feature.calc.CalcScreen
import com.patoolbox.feature.calc.toCalcTabOrNull
import com.patoolbox.feature.calibration.CalibrationGuideScreen
import com.patoolbox.feature.calibration.CalibrationScreen
import com.patoolbox.feature.feedback.FeedbackScreen
import com.patoolbox.feature.home.HomeScreen
import com.patoolbox.feature.home.ToolListScreen
import com.patoolbox.feature.job.JobDetailScreen
import com.patoolbox.feature.job.JobListScreen
import com.patoolbox.feature.measure.DelayFinderScreen
import com.patoolbox.feature.measure.PolarityScreen
import com.patoolbox.feature.measure.RoomMeasureScreen
import com.patoolbox.feature.metronome.MetronomeScreen
import com.patoolbox.feature.patch.PatchListScreen
import com.patoolbox.feature.recorder.RecorderScreen
import com.patoolbox.feature.reference.ReferenceScreen
import com.patoolbox.feature.reference.toReferenceTabOrNull
import com.patoolbox.feature.patch.PatchSheetScreen
import com.patoolbox.feature.rta.RtaScreen
import com.patoolbox.feature.siggen.SigGenScreen
import com.patoolbox.feature.schedule.ScheduleScreen
import com.patoolbox.feature.settings.SettingsScreen
import com.patoolbox.feature.stageplot.StagePlotEditorScreen
import com.patoolbox.feature.stageplot.StagePlotListScreen
import com.patoolbox.feature.sfx.SfxScreen
import com.patoolbox.feature.showrunner.ShowRunnerScreen
import com.patoolbox.feature.showtimer.ShowTimerScreen
import com.patoolbox.feature.spl.SplLogScreen
import com.patoolbox.feature.spl.SplScreen
import com.patoolbox.feature.tuner.TunerScreen
import com.patoolbox.feature.wireless.WirelessScreen
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

/**
 * ツール画面への共通ルート。
 * 実装済みのツールは [ToolDestination] で実画面に振り分け、未実装は PlaceholderScreen に落とす。
 * Phase が進んで画面が増えても、ホーム側は ToolId を渡すだけで済む。
 */
@Serializable
data class ToolRoute(
    val toolId: String,
    /**
     * 開いた直後に走らせるか。いまは本番万能コントローラーだけが見ている
     * （ホームの「もう始まっています。スタートしますか？」から入ったとき true）。
     */
    val autoStart: Boolean = false,
)

/**
 * 道具の一覧。
 * ホームには全部を並べないので、38個を見る画面をここに分けている。
 *
 * @param category 分類で絞った状態で開く。null なら全部
 */
@Serializable
data class ToolListRoute(val category: String? = null)

@Serializable
data object SettingsRoute

/** マイク校正。道具一覧には出さず、ホームと計測画面から入る。 */
@Serializable
data object CalibrationRoute

/** 校正の手順。基準の機材が無いときにどうするかを読む画面。 */
@Serializable
data object CalibrationGuideRoute

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

/** スナップショットの中身。property 名は SnapshotDetailViewModel.KEY_SNAPSHOT_ID と一致させる。 */
@Serializable
data class SnapshotRoute(val snapshotId: Long)

/** 請求書・見積書の編集。property 名は InvoiceDetailViewModel.KEY_INVOICE_ID と一致させる。 */
@Serializable
data class InvoiceRoute(val invoiceId: Long)

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
                onCategoryClick = { category ->
                    navController.navigate(ToolListRoute(category?.name))
                },
                onCalibrationClick = { navController.navigate(CalibrationRoute) },
                onCalibrationGuideClick = { navController.navigate(CalibrationGuideRoute) },
                onSettingsClick = { navController.navigate(SettingsRoute) },
                onStartShow = {
                    navController.navigate(
                        ToolRoute(ToolId.SHOW_RUNNER.name, autoStart = true),
                    )
                },
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
                    autoStart = route.autoStart,
                    onBack = { navController.popBackStack() },
                    onOpenCalibration = { navController.navigate(CalibrationRoute) },
                    onOpenPatchSheet = { sheetId ->
                        navController.navigate(PatchSheetRoute(sheetId))
                    },
                    onOpenJob = { jobId -> navController.navigate(JobDetailRoute(jobId)) },
                    onOpenStagePlot = { plotId ->
                        navController.navigate(StagePlotRoute(plotId))
                    },
                    onOpenSnapshot = { id -> navController.navigate(SnapshotRoute(id)) },
                    onOpenInvoice = { id -> navController.navigate(InvoiceRoute(id)) },
                    // 進行表そのものは案件管理側で作る。本番の画面からもそこへ行けるようにする
                    onOpenSchedules = {
                        navController.navigate(ToolRoute(ToolId.RUN_SHEET.name))
                    },
                )
            }
        }

        composable<ToolListRoute> { backStackEntry ->
            val route: ToolListRoute = backStackEntry.toRoute()
            ToolListScreen(
                initialCategory = route.category?.let { name ->
                    ToolCategory.entries.firstOrNull { it.name == name }
                },
                onToolClick = { tool -> navController.navigate(ToolRoute(tool.name)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable<SettingsRoute> {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable<CalibrationRoute> {
            CalibrationScreen(
                onBack = { navController.popBackStack() },
                onOpenGuide = { navController.navigate(CalibrationGuideRoute) },
            )
        }

        composable<CalibrationGuideRoute> {
            CalibrationGuideScreen(
                onBack = { navController.popBackStack() },
                onOpenCalibration = { navController.navigate(CalibrationRoute) },
            )
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

        composable<SnapshotRoute> {
            SnapshotDetailScreen(onBack = { navController.popBackStack() })
        }

        composable<InvoiceRoute> {
            InvoiceDetailScreen(onBack = { navController.popBackStack() })
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
    autoStart: Boolean,
    onBack: () -> Unit,
    onOpenCalibration: () -> Unit,
    onOpenPatchSheet: (Long) -> Unit,
    onOpenJob: (Long) -> Unit,
    onOpenStagePlot: (Long) -> Unit,
    onOpenSnapshot: (Long) -> Unit,
    onOpenInvoice: (Long) -> Unit,
    onOpenSchedules: () -> Unit,
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

        ToolId.RECORDER -> RecorderScreen(onBack = onBack)

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

        ToolId.WIRELESS_COORD -> WirelessScreen(onBack = onBack)

        ToolId.GEAR_INVENTORY -> GearScreen(onBack = onBack)

        ToolId.SNAPSHOT -> SnapshotListScreen(onOpen = onOpenSnapshot, onBack = onBack)

        // 見積書と請求書は同じ画面。作るときに種別を選ぶ
        ToolId.INVOICE -> InvoiceListScreen(onOpen = onOpenInvoice, onBack = onBack)

        ToolId.WORK_LOG -> WorkLogScreen(onBack = onBack)

        // 自動のクラウド同期ではなく、ファイルの書き出しと復元
        ToolId.CLOUD_BACKUP -> BackupScreen(onBack = onBack)

        ToolId.SHOW_TIMER -> ShowTimerScreen(onBack = onBack)

        ToolId.SHOW_RUNNER -> ShowRunnerScreen(
            onBack = onBack,
            autoStart = autoStart,
            onOpenSchedules = onOpenSchedules,
        )

        ToolId.SFX_PADS -> SfxScreen(onBack = onBack)

        // GLOSSARY はリファレンス（ReferenceScreen）のタブの1つ。else 節の
        // toReferenceTabOrNull() 分岐に自然に落ちる

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
