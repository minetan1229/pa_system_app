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
import com.patoolbox.feature.calibration.CalibrationScreen
import com.patoolbox.feature.home.HomeScreen
import com.patoolbox.feature.rta.RtaScreen
import com.patoolbox.feature.siggen.SigGenScreen
import com.patoolbox.feature.settings.SettingsScreen
import com.patoolbox.feature.spl.SplScreen
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
                )
            }
        }

        composable<SettingsRoute> {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable<CalibrationRoute> {
            CalibrationScreen(onBack = { navController.popBackStack() })
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
) {
    when (tool) {
        ToolId.SPL_METER -> SplScreen(
            onBack = onBack,
            onOpenCalibration = onOpenCalibration,
        )

        ToolId.RTA -> RtaScreen(onBack = onBack)

        ToolId.SIGNAL_GENERATOR -> SigGenScreen(onBack = onBack)

        else -> PlaceholderScreen(tool = tool, onBack = onBack)
    }
}
